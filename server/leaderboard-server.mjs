import http from "node:http";
import { readFileSync, writeFileSync, existsSync, mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, "data");
const PORT = Number(process.env.PORT || 8787);
const MAX_TOTAL = 5000;

mkdirSync(DATA_DIR, { recursive: true });
const dataFile = path.join(DATA_DIR, "rankings.json");
let store = { entries: [] };
if (existsSync(dataFile)) {
  try {
    store = JSON.parse(readFileSync(dataFile, "utf8"));
    if (!Array.isArray(store.entries)) store = { entries: [] };
  } catch (e) {
    store = { entries: [] };
  }
}

function save() {
  writeFileSync(dataFile, JSON.stringify(store, null, 2));
}

const MODES = [
  "easy", "normal", "expert", "super", "daily",
  "x_easy", "x_normal", "x_expert",
  "hyper_easy", "hyper_normal", "hyper_expert",
];

function modeKey(m) {
  return MODES.includes(m) ? m : "easy";
}

function sanitize(s, fallback) {
  const v = String(s || fallback).slice(0, 24).replace(/[|;\n\r]/g, "");
  return v || fallback;
}

function sendJson(res, code, obj) {
  res.writeHead(code, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
  });
  res.end(JSON.stringify(obj));
}

const server = http.createServer((req, res) => {
  if (req.method === "OPTIONS") {
    sendJson(res, 204, {});
    return;
  }
  const url = new URL(req.url, "http://localhost");
  const p = url.pathname;

  if (req.method === "POST" && p === "/api/score") {
    let body = "";
    req.on("data", (c) => {
      body += c;
      if (body.length > 10000) req.destroy();
    });
    req.on("end", () => {
      try {
        const d = JSON.parse(body || "{}");
        const elapsed = Number(d.elapsed);
        if (!Number.isFinite(elapsed) || elapsed < 0) {
          sendJson(res, 400, { error: "bad elapsed" });
          return;
        }
        const entry = {
          mode: modeKey(d.mode),
          name: sanitize(d.name, "玩家"),
          address: sanitize(d.address, "本地"),
          elapsed: Math.round(elapsed),
          mistakes: Number(d.mistakes) || 0,
          ts: Date.now(),
        };
        store.entries.push(entry);
        if (store.entries.length > MAX_TOTAL) {
          store.entries = store.entries.slice(-MAX_TOTAL);
        }
        save();
        sendJson(res, 200, { ok: true });
      } catch (e) {
        sendJson(res, 400, { error: "bad json" });
      }
    });
    return;
  }

  if (req.method === "GET" && p === "/api/ranking") {
    const mode = modeKey(url.searchParams.get("mode") || "easy");
    const top = store.entries
      .filter((e) => e.mode === mode)
      .sort((a, b) => a.elapsed - b.elapsed || a.ts - b.ts)
      .slice(0, 10);
    sendJson(res, 200, { mode, entries: top });
    return;
  }

  if (req.method === "GET" && p === "/api/me") {
    const mode = modeKey(url.searchParams.get("mode") || "easy");
    const name = sanitize(url.searchParams.get("name"), "");
    // 保持插入顺序，"最近一次"即最后一条
    const mine = store.entries.filter((e) => e.mode === mode && e.name === name);
    const best = mine.length ? Math.min(...mine.map((e) => e.elapsed)) : -1;
    const latest = mine.length ? mine[mine.length - 1].elapsed : -1;
    const avg = mine.length
      ? Math.round(mine.reduce((s, e) => s + e.elapsed, 0) / mine.length)
      : -1;
    sendJson(res, 200, { mode, count: mine.length, best, latest, avg });
    return;
  }

  if (req.method === "GET" && (p === "/" || p === "/health")) {
    sendJson(res, 200, { ok: true, service: "sudoku-leaderboard" });
    return;
  }

  sendJson(res, 404, { error: "not found" });
});

server.listen(PORT, () => {
  console.log("数独排行榜服务器已启动: http://0.0.0.0:" + PORT);
});

export { server as serverInstance };
