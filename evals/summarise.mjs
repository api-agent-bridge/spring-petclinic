/**
 * Prints the results of a promptfoo JSON output, a question in each row and a
 * model in each column.
 *
 * Every check in the configs belongs to one of two metrics, so a run is scored twice:
 *
 *   T  tools   the agent called the right tools, or answered without tools where that is right
 *   A  answer  the answer holds the expected facts and only true claims
 *
 * A cell reads "T3/3 A1/3": with --repeat 3, the tools were right in all three runs and
 * the answer in one. A run passes only when both are right. "A-" marks a question
 * without an answer check, such as a write that is judged by its calls. "E1" counts a
 * run that ended in an error, such as a model that ran out of time, before any check ran.
 *
 * Then a grid of the tool calls and the rounds with the model, a table for each model
 * with its tokens, time, rounds, calls and repeated calls, and the reasons of each
 * failed run.
 *
 *   node summarise.mjs results/queries.json
 */
import { readFileSync } from 'node:fs';

const file = process.argv[2] ?? 'results/queries.json';
const rows = JSON.parse(readFileSync(file, 'utf8')).results.results;
const METRICS = ['tools', 'answer'];

const modelOf = (row) => row.provider.label ?? row.provider.id;
const callsOf = (row) => row.response?.metadata?.toolCalls ?? [];
const roundsOf = (row) => row.response?.metadata?.rounds?.length ?? 0;

// calls with the same tool and the same arguments as an earlier call of the run
function repeatedCalls(row) {
  const seen = new Set();
  let repeated = 0;
  for (const call of callsOf(row)) {
    const key = `${call.name} ${JSON.stringify(call.input)}`;
    if (seen.has(key)) repeated += 1;
    seen.add(key);
  }
  return repeated;
}

// for one run: true or false for each metric, or undefined for a metric the run does not check
function verdicts(row) {
  const results = row.gradingResult?.componentResults ?? [];
  const verdict = {};
  for (const metric of METRICS) {
    const checks = results.filter((result) => result.assertion?.metric === metric);
    verdict[metric] = checks.length ? checks.every((result) => result.pass) : undefined;
  }
  return verdict;
}

const cells = new Map();
const misses = [];

for (const row of rows) {
  const id = row.testCase.vars.id;
  const key = `${id} ${modelOf(row)}`;
  const cell = cells.get(key) ?? { runs: 0, errors: 0, calls: 0, rounds: 0, tools: 0, answer: 0, checked: { tools: false, answer: false } };
  const verdict = verdicts(row);
  cell.runs += 1;
  // failureReason 2 is a provider error, 1 is a failed check
  if (row.failureReason === 2) cell.errors += 1;
  cell.calls += callsOf(row).length;
  cell.rounds += roundsOf(row);
  for (const metric of METRICS) {
    if (verdict[metric] !== undefined) {
      cell.checked[metric] = true;
      cell[metric] += verdict[metric] ? 1 : 0;
    }
  }
  cells.set(key, cell);

  if (!row.success) {
    const reasons = (row.gradingResult?.componentResults ?? [])
      .filter((result) => !result.pass)
      .map((result) => `${result.assertion?.metric ?? 'check'}: ${result.reason}`);
    misses.push(`  ${id} on ${modelOf(row)}: ${reasons.join(' | ') || row.error || 'failed'}`);
  }
}

const models = [...new Set(rows.map(modelOf))];
const questions = [...new Set(rows.map((row) => row.testCase.vars.id))];
const average = (total, runs) => (total / runs).toFixed(1).replace(/\.0$/, '');
const mark = (cell, metric, letter) => (cell.checked[metric] ? `${letter}${cell[metric]}/${cell.runs}` : `${letter}-`);
const cellText = {
  checks: (cell) => `${mark(cell, 'tools', 'T')} ${mark(cell, 'answer', 'A')}${cell.errors ? ` E${cell.errors}` : ''}`,
  calls: (cell) => `${average(cell.calls, cell.runs)} in ${average(cell.rounds, cell.runs)}`,
};
const first = Math.max(...questions.map((id) => id.length)) + 2;
const column = Math.max(...models.map((model) => model.length), 12) + 2;

function grid(title, value) {
  console.log(`\n${title}`);
  console.log(''.padEnd(first) + models.map((model) => model.padEnd(column)).join(''));
  for (const id of questions) {
    const line = models.map((model) => {
      const cell = cells.get(`${id} ${model}`);
      return (cell ? value(cell) : '-').padEnd(column);
    });
    console.log(id.padEnd(first) + line.join(''));
  }
}

grid('checks passed, T for the tools and A for the answer, E for runs that ended in an error', cellText.checks);
grid('tool calls in rounds with the model, average: "11 in 2" is eleven calls over two rounds', cellText.calls);

// one line for each model: how well it did, and what it cost in tokens and time
const seconds = (ms) => `${(ms / 1000).toFixed(1)}s`;
const share = (count, total) => `${count}/${total} (${total ? Math.round((100 * count) / total) : 0}%)`;
const table = [['model', 'passed', 'tools right', 'answer right', 'errors', 'prompt tokens', 'completion tokens', 'time', 'loading', 'model time', 'tool time', 'rounds', 'tool calls', 'repeated calls', 'tool result chars']];
for (const model of models) {
  const runs = rows.filter((row) => modelOf(row) === model);
  const sum = (value) => runs.reduce((total, row) => total + (value(row) ?? 0), 0);
  const judged = (metric) => runs.map((row) => verdicts(row)[metric]).filter((verdict) => verdict !== undefined);
  const right = (metric) => share(judged(metric).filter(Boolean).length, judged(metric).length);
  table.push([
    model,
    share(runs.filter((row) => row.success).length, runs.length),
    right('tools'),
    right('answer'),
    // failureReason 2 is a provider error, 1 is a failed check
    String(runs.filter((row) => row.failureReason === 2).length),
    String(sum((row) => row.response?.tokenUsage?.prompt)),
    String(sum((row) => row.response?.tokenUsage?.completion)),
    seconds(sum((row) => row.latencyMs)),
    seconds(sum((row) => row.response?.metadata?.loadMs)),
    seconds(sum((row) => row.response?.metadata?.modelMs)),
    seconds(sum((row) => row.response?.metadata?.toolMs)),
    String(sum(roundsOf)),
    String(sum((row) => callsOf(row).length)),
    String(sum(repeatedCalls)),
    String(sum((row) => row.response?.metadata?.toolResultChars)),
  ]);
}
const widths = table[0].map((_, index) => Math.max(...table.map((line) => line[index].length)) + 2);
console.log('\nper model');
for (const line of table) {
  console.log(line.map((value, index) => value.padEnd(widths[index])).join(''));
}

if (misses.length > 0) {
  console.log('\nwhy runs failed');
  console.log(misses.join('\n'));
}
