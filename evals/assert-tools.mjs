/**
 * Checks the tool calls of one answer against the `config` of the assertion:
 *
 *   noTools:   true when the question is answered without tools, so any call fails the run
 *   called:    tools the agent has to call at least once
 *   calledAny: tools of which the agent has to call at least one, for data that two tools
 *              hold, such as the pets in findPets and in getOwnerDetails
 *   notCalled: tools the agent must leave alone, such as a write before the user confirmed it
 *   once:      tools the agent has to call exactly once, such as a write that adds a record
 *   args:      for a tool, the arguments its first call has to carry, which suits a write.
 *              Values compare as text, ignoring case, and NEXT_MONDAY stands for the
 *              date of next Monday
 *   argsAnyCall: for a tool, the arguments one of its calls has to carry, which suits a
 *              read that the agent may correct after a wrong first try
 *
 * On a test with `metadata.readOnly`, every call of a tool that writes fails the run. The
 * provider marks those calls with `writes: true`.
 *
 * The reason lists every call in order. Three named scores show how much work each
 * model did for its answer, and the promptfoo viewer averages them for each model:
 *
 *   toolCalls      every tool call
 *   rounds         every request to the model, the last one being the answer
 *   repeatedCalls  calls with the same tool and the same arguments as an earlier call
 */

// People say "next Monday" for the coming Monday and for the Monday a week later,
// so both dates count.
function nextMondays() {
  const date = new Date();
  date.setDate(date.getDate() + (((8 - date.getDay()) % 7) || 7));
  const coming = date.toLocaleDateString('en-CA');
  date.setDate(date.getDate() + 7);
  return [coming, date.toLocaleDateString('en-CA')];
}

function sameValue(actual, expected) {
  const wanted = expected === 'NEXT_MONDAY' ? nextMondays() : [String(expected)];
  const value = String(actual ?? '').trim().toLowerCase();
  return wanted.some((candidate) => value === candidate.toLowerCase());
}

// findOwners, getOwnerDetails x10
function describe(names) {
  const runs = [];
  for (const name of names) {
    const last = runs.at(-1);
    if (last?.name === name) last.count += 1;
    else runs.push({ name, count: 1 });
  }
  return runs.map((run) => (run.count > 1 ? `${run.name} x${run.count}` : run.name)).join(', ');
}

export default function (output, context) {
  const calls = context.metadata?.toolCalls ?? context.providerResponse?.metadata?.toolCalls ?? [];
  const names = calls.map((call) => call.name);
  const config = context.config ?? {};
  const problems = [];

  if (config.noTools && calls.length > 0) {
    problems.push('the question is answered without tools, and the agent called one');
  }
  for (const tool of config.called ?? []) {
    if (!names.includes(tool)) problems.push(`${tool} was not called`);
  }
  const anyOf = config.calledAny ?? [];
  if (anyOf.length && !anyOf.some((tool) => names.includes(tool))) {
    problems.push(`no call of ${anyOf.join(' or ')}`);
  }
  for (const tool of config.notCalled ?? []) {
    if (names.includes(tool)) problems.push(`${tool} was called`);
  }
  if (context.test?.metadata?.readOnly) {
    for (const tool of new Set(calls.filter((call) => call.writes).map((call) => call.name))) {
      problems.push(`${tool} changes data, and the question only reads`);
    }
  }
  for (const tool of config.once ?? []) {
    const count = names.filter((name) => name === tool).length;
    if (count !== 1) problems.push(`${tool} was called ${count} times`);
  }
  for (const [tool, expected] of Object.entries(config.args ?? {})) {
    const first = calls.find((call) => call.name === tool);
    if (!first) continue;
    for (const [key, value] of Object.entries(expected)) {
      if (!sameValue(first.input?.[key], value)) {
        problems.push(`${tool} got ${key} ${JSON.stringify(first.input?.[key])}`);
      }
    }
  }

  // a call with the same tool and the same arguments as an earlier one repeats work
  const seen = new Set();
  let repeatedCalls = 0;
  for (const call of calls) {
    const key = `${call.name} ${JSON.stringify(call.input)}`;
    if (seen.has(key)) repeatedCalls += 1;
    seen.add(key);
  }
  const rounds = context.metadata?.rounds?.length ?? 0;

  for (const [tool, expected] of Object.entries(config.argsAnyCall ?? {})) {
    const matching = calls.some(
      (call) => call.name === tool && Object.entries(expected).every(([key, value]) => sameValue(call.input?.[key], value)),
    );
    if (!matching) problems.push(`no call of ${tool} had ${JSON.stringify(expected)}`);
  }

  const summary = calls.length ? `${describe(names)} (${calls.length} calls in ${rounds} rounds)` : 'zero tool calls';
  return {
    pass: problems.length === 0,
    score: problems.length === 0 ? 1 : 0,
    reason: problems.length ? `${problems.join('; ')}. Calls: ${summary}` : `Calls: ${summary}`,
    namedScores: { toolCalls: calls.length, rounds, repeatedCalls },
  };
}
