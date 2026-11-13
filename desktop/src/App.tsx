import { useEffect, useState } from "react";
import { DesktopAPI } from "./mygo";

type Command = {
  id: string; name: string; kind: "builtin" | "custom";
  executable: string; args: string[]; workingDir: string; timeoutSeconds: number;
};
type RunResult = {
  commandId: string; command: string; exitCode: number;
  stdout: string; stderr: string; durationMs: number;
};
type History = {
  id: number; command: string; exitCode: number; createdAt: string; durationMs: number;
};

export default function App() {
  const [commands, setCommands] = useState<Command[]>([]);
  const [history, setHistory] = useState<History[]>([]);
  const [baseURL, setBaseURL] = useState("");
  const [token, setToken] = useState("");
  const [backendPath, setBackendPath] = useState("/health");
  const [backendResult, setBackendResult] = useState("");
  const [result, setResult] = useState<RunResult | null>(null);
  const [error, setError] = useState("");
  const [name, setName] = useState("");
  const [executable, setExecutable] = useState("");
  const [argsJSON, setArgsJSON] = useState("[]");
  const [workingDir, setWorkingDir] = useState("");
  const [timeoutSeconds, setTimeoutSeconds] = useState(30);

  async function refresh() {
    try {
      const [cs, hs, url] = await Promise.all([
        DesktopAPI.listCommands(), DesktopAPI.listHistory(30), DesktopAPI.backendURL()
      ]);
      setCommands(cs); setHistory(hs); setBaseURL(url);
    } catch (e) { setError(String(e)); }
  }

  useEffect(() => { void refresh(); }, []);

  async function run(id: string) {
    setError("");
    try { setResult(await DesktopAPI.runCommand(id)); await refresh(); }
    catch (e) { setError(String(e)); }
  }

  async function saveCommand(e: React.FormEvent) {
    e.preventDefault(); setError("");
    try {
      await DesktopAPI.createCustomCommand(name, executable, argsJSON, workingDir, timeoutSeconds);
      setName(""); setExecutable(""); setArgsJSON("[]"); setWorkingDir("");
      await refresh();
    } catch (e) { setError(String(e)); }
  }

  async function saveBackend(e: React.FormEvent) {
    e.preventDefault(); setError("");
    try { await DesktopAPI.setBackend(baseURL, token); setToken(""); setBackendResult("Configuration saved. Token remains in memory only."); }
    catch (e) { setError(String(e)); }
  }

  async function requestBackend() {
    setError(""); setBackendResult("Requesting…");
    try {
      const r = await DesktopAPI.requestBackend("GET", backendPath, "");
      setBackendResult(`HTTP ${r.statusCode}\n${r.body}`);
    } catch (e) { setBackendResult(String(e)); }
  }

  async function remove(id: string) {
    try { await DesktopAPI.deleteCustomCommand(id); await refresh(); }
    catch (e) { setError(String(e)); }
  }

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark">S</div><div><b>TheSourceBox</b><small>Desktop toolbox</small></div></div>
      <div className="nav-label">WORKSPACE</div>
      <a className="nav-item active">▦　Overview</a>
      <a className="nav-item">⌘　Commands</a>
      <a className="nav-item">◷　History</a>
      <div className="sidebar-bottom"><span className="status-dot"/> Local runtime ready</div>
    </aside>
    <main className="main">
      <header className="topbar"><div><span className="eyebrow">WORKSPACE / OVERVIEW</span><h1>Desktop overview</h1></div><span className="version">v0.1.0 · Local</span></header>
      {error && <div className="alert">{error}</div>}
      <section className="stats">
        <div className="stat-card"><span>Registered commands</span><strong>{commands.length}</strong><small>Built-in + custom</small></div>
        <div className="stat-card"><span>Recent executions</span><strong>{history.length}</strong><small>Stored in SQLite</small></div>
        <div className="stat-card"><span>Backend</span><strong className="stat-small">{baseURL ? "Configured" : "Not configured"}</strong><small>HTTP API connection</small></div>
      </section>

      <section className="panel">
        <div className="panel-heading"><div><h2>Command center</h2><p>Run local commands through the Go host process.</p></div></div>
        <div className="command-grid">
          {commands.map(c => <div className="command-card" key={c.id}>
            <div className="command-icon">{c.kind === "builtin" ? "⌘" : "$"}</div>
            <div className="command-info"><b>{c.name}</b><small>{c.kind === "builtin" ? "Built-in command" : c.executable}</small></div>
            <button className="button primary compact" onClick={() => void run(c.id)}>Run</button>
            {c.kind === "custom" && <button className="icon-button" title="Delete" onClick={() => void remove(c.id)}>×</button>}
          </div>)}
        </div>
        {result && <div className="output"><div className="output-head"><b>{result.command}</b><span>Exit {result.exitCode} · {result.durationMs} ms</span></div><pre>{[result.stdout, result.stderr].filter(Boolean).join("\n") || "(no output)"}</pre></div>}
      </section>

      <section className="two-col">
        <div className="panel">
          <div className="panel-heading"><div><h2>Add custom command</h2><p>Arguments are passed separately; no shell interpolation.</p></div></div>
          <form className="form" onSubmit={saveCommand}>
            <label>Name<input value={name} onChange={e => setName(e.target.value)} placeholder="My script" required /></label>
            <label>Executable<input value={executable} onChange={e => setExecutable(e.target.value)} placeholder="git or /usr/bin/python3" required /></label>
            <label>Arguments (JSON array)<textarea value={argsJSON} onChange={e => setArgsJSON(e.target.value)} rows={2}/></label>
            <label>Working directory<input value={workingDir} onChange={e => setWorkingDir(e.target.value)} placeholder="Optional absolute or relative path" /></label>
            <label>Timeout (seconds)<input type="number" min={1} max={300} value={timeoutSeconds} onChange={e => setTimeoutSeconds(Number(e.target.value))}/></label>
            <button className="button primary" type="submit">Add command</button>
          </form>
        </div>
        <div className="panel">
          <div className="panel-heading"><div><h2>HTTP backend</h2><p>Connect to your existing REST service.</p></div></div>
          <form className="form" onSubmit={saveBackend}>
            <label>Base URL<input value={baseURL} onChange={e => setBaseURL(e.target.value)} placeholder="https://api.example.com" required /></label>
            <label>Bearer token (session only)<input type="password" value={token} onChange={e => setToken(e.target.value)} placeholder="Optional token" /></label>
            <button className="button secondary" type="submit">Save connection</button>
          </form>
          <div className="request-row"><input value={backendPath} onChange={e => setBackendPath(e.target.value)} placeholder="/health"/><button className="button primary" onClick={() => void requestBackend()}>GET</button></div>
          {backendResult && <pre className="backend-output">{backendResult}</pre>}
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading"><div><h2>Recent command history</h2><p>Execution records persisted locally in SQLite.</p></div><button className="button ghost" onClick={() => void refresh()}>Refresh</button></div>
        <div className="history-list">
          {history.map(h => <div className="history-row" key={h.id}><span className={h.exitCode === 0 ? "result-ok" : "result-fail"}>{h.exitCode === 0 ? "✓" : "!"}</span><b>{h.command}</b><span>{h.durationMs} ms</span><time>{new Date(h.createdAt).toLocaleString()}</time></div>)}
          {history.length === 0 && <div className="empty">No command executions yet.</div>}
        </div>
      </section>
    </main>
  </div>;
}
