"""Minimal stdio client for `figcraft-cli mcp` (headless)."""
import json, subprocess, os, sys, itertools

CLI = os.path.expanduser("~/.local/share/figcraft-cli/figcraft-cli")

class FigCraftError(RuntimeError):
    pass

class FC:
    def __init__(self, read_roots=(), write_roots=(), log=None):
        args = [CLI, "mcp"]
        for r in read_roots: args += ["--automation-read-root", r]
        for w in write_roots: args += ["--automation-write-root", w]
        self.p = subprocess.Popen(args, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE if log is None else log, text=True, bufsize=1)
        self.ids = itertools.count(1)
        r = self._rpc("initialize", {"protocolVersion": "2025-06-18", "capabilities": {},
                                     "clientInfo": {"name": "attic-figbuild", "version": "1"}})
        self.instructions = r.get("instructions", "")
        self._send({"jsonrpc": "2.0", "method": "notifications/initialized"})

    def _send(self, msg):
        self.p.stdin.write(json.dumps(msg) + "\n"); self.p.stdin.flush()

    def _rpc(self, method, params):
        i = next(self.ids)
        self._send({"jsonrpc": "2.0", "id": i, "method": method, "params": params})
        while True:
            line = self.p.stdout.readline()
            if not line:
                raise FigCraftError("server closed: " + (self.p.stderr.read() if self.p.stderr else ""))
            msg = json.loads(line)
            if msg.get("id") == i:
                if "error" in msg: raise FigCraftError(json.dumps(msg["error"]))
                return msg["result"]

    def tool(self, _tool, **arguments):
        r = self._rpc("tools/call", {"name": _tool, "arguments": arguments})
        if r.get("isError"):
            raise FigCraftError(f"{_tool}: " + " ".join(c.get("text", "") for c in r.get("content", [])))
        out = []
        for c in r.get("content", []):
            if c.get("type") == "text":
                try: out.append(json.loads(c["text"]))
                except Exception: out.append(c["text"])
            else: out.append(c)
        if "structuredContent" in r: return r["structuredContent"]
        return out[0] if len(out) == 1 else out

    def cmd(self, command, **params):
        return self.tool("run_command", command=command, params=params)

    def batch(self, commands, label=None):
        a = {"commands": commands}
        if label: a["label"] = label
        return self.tool("batch", **a)

    def tools(self):
        return self._rpc("tools/list", {})

    def close(self):
        try: self.p.stdin.close(); self.p.wait(5)
        except Exception: self.p.kill()
