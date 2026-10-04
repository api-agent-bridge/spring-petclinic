/**
 * A promptfoo provider that answers a question the way an agent does: with the
 * Petclinic tools, over MCP, in as many rounds as the model needs.
 *
 * promptfoo's own MCP support, the `mcp:` block of the OpenAI provider, runs one
 * round. It executes the tool calls of the model's first answer and returns
 * their results as the output, and those results stay out of the conversation.
 * Most questions in this folder need a search and then the details, so this
 * provider runs the loop itself:
 *
 *   1. read the tools from the server's tools/list
 *   2. send the question and the tools to the model
 *   3. call every tool the model asks for, through MCP, and send the results back
 *   4. repeat from 2 until the model answers in text
 *
 * The models run in Ollama, through its own chat endpoint, /api/chat. Every call
 * lands on `metadata.toolCalls` as `{ id, name, input, output, is_error }`, the
 * shape promptfoo's own MCP support writes, so the assertions read it from there.
 *
 * promptfoo asks every model the first question before it asks the second, so Ollama
 * loads a model again for most questions. The provider therefore loads the model
 * before the clock starts: a chat request without messages makes Ollama load it and
 * answers once the model is in memory. The agent then has `timeoutSeconds` for the
 * question. A model that runs out of time ends the run with an error, which the grid
 * shows as E.
 *
 * The requests leave the sampling options out, so each model runs with the settings it
 * ships with, the way `ollama run` runs it: Ollama starts from its defaults
 * (temperature 0.8, top_k 40, top_p 0.9) and applies the parameters of the model's
 * Modelfile over them, such as temperature 1.0 for Qwen and 0.15 for Ministral.
 * Ollama's OpenAI-compatible endpoint, /v1/chat/completions, sets temperature 1.0
 * and top_p 1.0 when a request leaves them out, which overrides those parameters.
 * A `temperature` in the config of a model still overrides its own.
 *
 * A test with `metadata.readOnly` only reads, and every model answers it against the
 * same data. On such a test the provider declines each call of a tool that the server
 * marks as a write (`readOnlyHint: false`), the way a client that asks before a write
 * answers when the user says no: the call stays away from the server, and the model
 * reads "The user declined the call of updatePet." Every call of a write tool carries
 * `writes: true`, so the tools check can fail the run.
 */
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';

const DEFAULTS = {
  ollamaUrl: process.env.OLLAMA_URL ?? 'http://localhost:11434',
  mcpUrl: process.env.MCP_URL ?? 'http://127.0.0.1:8080/mcp',
  maxTurns: 25,
  // in the first two runs, one right answer took 61 seconds and every other answer 51 or less
  timeoutSeconds: 60,
  // the largest model of the first run took 83 seconds to load
  loadTimeoutSeconds: 120,
};

// Node's fetch reports "fetch failed" and keeps the reason, such as ECONNREFUSED, in the cause.
function describeError(error) {
  const cause = error.cause?.code ?? error.cause?.message;
  return cause ? `${error.message} (${cause})` : String(error.message ?? error);
}

// The date matters: "tomorrow" and "next Monday" have to become a date.
function systemPrompt() {
  const today = new Date();
  const weekday = today.toLocaleDateString('en-GB', { weekday: 'long' });
  const date = today.toLocaleDateString('en-CA');
  return `You are the assistant of a veterinary clinic. You can use the tools to read and change the clinic's data. Today is ${weekday}, ${date}.`;
}

export default class McpAgentProvider {
  constructor(options = {}) {
    this.config = { ...DEFAULTS, ...options.config };
    this.label = options.label;
  }

  id() {
    return `mcp-agent:${this.config.model}`;
  }

  async callApi(prompt, context) {
    const readOnly = Boolean(context?.test?.metadata?.readOnly);
    const client = new Client({ name: 'petclinic-evals', version: '1.0.0' });
    try {
      let tools;
      try {
        await client.connect(new StreamableHTTPClientTransport(new URL(this.config.mcpUrl)));
        ({ tools } = await client.listTools());
      } catch (error) {
        return { error: `MCP server at ${this.config.mcpUrl}: ${describeError(error)}` };
      }
      return await this.answer(client, tools, prompt, readOnly);
    } finally {
      await client.close();
    }
  }

  async answer(client, tools, prompt, readOnly) {
    const functions = tools.map((tool) => ({
      type: 'function',
      function: { name: tool.name, description: tool.description ?? '', parameters: tool.inputSchema },
    }));
    const writes = new Set(tools.filter((tool) => tool.annotations?.readOnlyHint !== true).map((tool) => tool.name));
    const messages = [
      { role: 'system', content: systemPrompt() },
      { role: 'user', content: prompt },
    ];
    const toolCalls = [];
    const tokenUsage = { prompt: 0, completion: 0, total: 0, numRequests: 0 };
    // what the viewer shows for each run, beside the answer; loadMs is the time Ollama
    // spent loading the model into memory, which stays out of modelMs
    const metadata = { model: this.config.model, toolCalls, rounds: [], modelMs: 0, loadMs: 0, toolMs: 0, toolResultChars: 0 };

    const loadStarted = Date.now();
    try {
      const response = await fetch(`${this.config.ollamaUrl}/api/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ model: this.config.model, messages: [] }),
        signal: AbortSignal.timeout(this.config.loadTimeoutSeconds * 1000),
      });
      if (!response.ok) {
        return { error: `Ollama could not load ${this.config.model}: ${await response.text()}`, tokenUsage, metadata };
      }
    } catch (error) {
      const reason =
        error.name === 'TimeoutError'
          ? `Ollama did not load ${this.config.model} within ${this.config.loadTimeoutSeconds} seconds`
          : `Ollama at ${this.config.ollamaUrl}: ${describeError(error)}`;
      return { error: reason, tokenUsage, metadata };
    }
    metadata.loadMs = Date.now() - loadStarted;

    const deadline = Date.now() + this.config.timeoutSeconds * 1000;
    for (let round = 1; round <= this.config.maxTurns; round++) {
      const started = Date.now();
      let response;
      let body;
      try {
        response = await fetch(`${this.config.ollamaUrl}/api/chat`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            model: this.config.model,
            messages,
            tools: functions,
            stream: false,
            ...(this.config.temperature === undefined ? {} : { options: { temperature: this.config.temperature } }),
          }),
          signal: AbortSignal.timeout(Math.max(0, deadline - Date.now())),
        });
        body = await response.text();
      } catch (error) {
        metadata.modelMs += Date.now() - started;
        const reason =
          error.name === 'TimeoutError'
            ? `${this.config.model} did not answer within ${this.config.timeoutSeconds} seconds, in round ${round}`
            : `Ollama at ${this.config.ollamaUrl}: ${describeError(error)}`;
        return { error: reason, tokenUsage, metadata: { ...metadata, timedOut: error.name === 'TimeoutError' } };
      }
      const modelMs = Date.now() - started;
      metadata.modelMs += modelMs;
      if (!response.ok) {
        return { error: `Ollama answered ${response.status}: ${body}`, tokenUsage, metadata };
      }
      const data = JSON.parse(body);
      const promptTokens = data.prompt_eval_count ?? 0;
      const completionTokens = data.eval_count ?? 0;
      // Ollama reports durations in nanoseconds; a model that Ollama unloaded during the
      // question loads again here, inside the time limit
      const loadMs = Math.round((data.load_duration ?? 0) / 1e6);
      tokenUsage.prompt += promptTokens;
      tokenUsage.completion += completionTokens;
      tokenUsage.total += promptTokens + completionTokens;
      tokenUsage.numRequests += 1;
      metadata.loadMs += loadMs;

      // the whole message goes back into the conversation, thinking included
      const message = data.message;
      messages.push(message);
      metadata.rounds.push({
        round,
        modelMs,
        loadMs,
        promptTokens,
        completionTokens,
        tools: (message.tool_calls ?? []).map((call) => call.function?.name),
        // thinking models return their reasoning beside the answer
        ...(message.thinking ? { reasoning: message.thinking } : {}),
      });
      if (!message.tool_calls?.length) {
        return { output: message.content ?? '', tokenUsage, metadata };
      }
      for (const call of message.tool_calls) {
        const toolStarted = Date.now();
        const write = writes.has(call.function?.name);
        const result = await this.callTool(client, call, toolCalls.length + 1, readOnly && write);
        if (write) result.writes = true;
        metadata.toolMs += Date.now() - toolStarted;
        metadata.toolResultChars += result.output.length;
        toolCalls.push(result);
        messages.push({ role: 'tool', tool_name: result.name, ...(call.id ? { tool_call_id: call.id } : {}), content: result.output });
      }
    }
    return {
      output: `The agent stopped after ${this.config.maxTurns} rounds without an answer.`,
      tokenUsage,
      metadata: { ...metadata, stoppedAfterMaxRounds: true },
    };
  }

  // A failed call goes back to the model as text, the way an MCP client reports it,
  // so the model can correct the call or give up. A declined call stays in the provider.
  async callTool(client, call, number, decline) {
    const id = call.id ?? `call_${number}`;
    const name = call.function?.name;
    const rawArguments = call.function?.arguments ?? '{}';
    let input;
    try {
      input = typeof rawArguments === 'string' ? JSON.parse(rawArguments || '{}') : rawArguments;
    } catch {
      return { id, name, input: rawArguments, output: `The arguments are not valid JSON: ${rawArguments}`, is_error: true };
    }
    if (decline) {
      return { id, name, input, output: `The user declined the call of ${name}.`, is_error: true, declined: true };
    }
    try {
      const result = await client.callTool({ name, arguments: input });
      const output = (result.content ?? []).map((part) => part.text ?? JSON.stringify(part)).join('\n');
      return { id, name, input, output, is_error: Boolean(result.isError) };
    } catch (error) {
      return { id, name, input, output: String(error.message ?? error), is_error: true };
    }
  }
}
