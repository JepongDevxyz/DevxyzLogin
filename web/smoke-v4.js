/* DevxyzLogin web — jsdom smoke suite v4 (server wiring edition).
   A stubbed fetch implements the real DevxyzLogin server contract in-memory.
   Covers: login success/failure, register (+duplicate), merge on login,
   mutation calls (method+path+auth headers), rollback on server error,
   offline dirty-flag + flush, token CRUD, real proxy check render,
   local-mode isolation (ZERO fetch calls). */
const fs = require("fs");
const { JSDOM } = require("/tmp/jsdomtest/node_modules/jsdom");
const html = fs.readFileSync("/home/hatch/workspace/devxyzlogin/web/index.html", "utf-8");

let pass = 0, fail = 0; const fails = [];
function ok(name, cond) { if (cond) { pass++; } else { fail++; fails.push(name); console.log("FAIL:", name); } }
function section(t) { console.log("—", t); }
const sleep = ms => new Promise(r => setTimeout(r, ms));
async function until(fn, ms) { const t0 = Date.now(); while (Date.now() - t0 < (ms || 3000)) { if (fn()) return true; await sleep(25); } return fn(); }

/* ---------------- in-memory stub of the real server contract ---------------- */
function makeServer() {
  const calls = [];
  const state = { down: false, failNext: null };
  let seq = 1; const nid = () => "srv_" + (seq++);
  const db = { users: [], workspaces: [], profiles: [], folders: [], proxies: [], tokens: [] };
  const jsonRes = (status, body) => ({ ok: status >= 200 && status < 300, status, json: async () => body });
  const deepMerge = (base, src) => { const out = Array.isArray(base) ? base.slice() : Object.assign({}, base); if (src && typeof src === "object" && !Array.isArray(src)) for (const k of Object.keys(src)) { if (src[k] && typeof src[k] === "object" && !Array.isArray(src[k]) && out[k] && typeof out[k] === "object" && !Array.isArray(out[k])) out[k] = deepMerge(out[k], src[k]); else out[k] = src[k]; } return out; };
  async function fetchStub(url, opts) {
    opts = opts || {};
    const method = (opts.method || "GET").toUpperCase();
    const u = new URL(url);
    const headers = opts.headers || {};
    const body = opts.body ? JSON.parse(opts.body) : undefined;
    calls.push({ method, path: u.pathname + u.search, headers, body });
    if (state.down) throw new TypeError("fetch failed (stubbed offline)");
    if (state.failNext) { const f = state.failNext; state.failNext = null; return jsonRes(f.status, f.body); }
    const seg = u.pathname.split("/").filter(Boolean);
    const authed = String(headers["Authorization"] || "").startsWith("Bearer dl1.");
    const needAuth = () => authed ? null : jsonRes(401, { ok: false, error: "authentication required" });
    if (u.pathname === "/api/health") return jsonRes(200, { ok: true, service: "devxyzlogin-server", version: "1.0.0" });
    if (seg[1] === "auth" && seg[2] === "register" && method === "POST") {
      if (db.users.find(x => x.email === body.email)) return jsonRes(409, { ok: false, error: "email already registered" });
      if (!body.password || body.password.length < 6) return jsonRes(400, { ok: false, error: "password must be at least 6 characters" });
      const user = { id: nid(), email: body.email, password: body.password };
      db.users.push(user);
      const ws = { id: nid(), name: "My workspace" }; db.workspaces.push(ws);
      db.folders.push({ id: nid(), name: "Default" });
      return jsonRes(201, { ok: true, token: "dl1.stub." + user.id, user: { id: user.id, email: user.email, createdAt: new Date().toISOString() }, workspace: ws });
    }
    if (seg[1] === "auth" && seg[2] === "login" && method === "POST") {
      const user = db.users.find(x => x.email === body.email);
      if (!user || user.password !== body.password) return jsonRes(401, { ok: false, error: "invalid email or password" });
      return jsonRes(200, { ok: true, token: "dl1.stub." + user.id, user: { id: user.id, email: user.email }, workspace: db.workspaces[0] });
    }
    const na = needAuth(); if (na) return na;
    if (seg[1] === "workspaces") {
      if (method === "GET") return jsonRes(200, { ok: true, workspaces: db.workspaces });
      if (method === "POST") { const ws = { id: nid(), name: body.name || "Workspace" }; db.workspaces.push(ws); return jsonRes(201, { ok: true, workspace: ws }); }
    }
    if (seg[1] === "profiles") {
      const id = seg[2] || null, action = seg[3] || null;
      if (!id) {
        if (method === "GET") { const wantDel = u.searchParams.get("deleted") === "1"; return jsonRes(200, { ok: true, profiles: db.profiles.filter(p => wantDel ? !!p.deletedAt : !p.deletedAt) }); }
        if (method === "POST") { const p = deepMerge({ name: "Untitled profile", folderIds: [], tags: [], status: "ready", notes: "", os: "linux", fingerprint: {}, proxy: {}, extensions: [], bookmarks: [], deletedAt: null }, body || {}); p.id = nid(); p.createdAt = new Date().toISOString(); p.updatedAt = p.createdAt; db.profiles.push(p); return jsonRes(201, { ok: true, profile: p }); }
      }
      const p = db.profiles.find(x => x.id === id);
      if (!p) return jsonRes(404, { ok: false, error: "profile not found" });
      if (action === "restore" && method === "POST") { p.deletedAt = null; return jsonRes(200, { ok: true, profile: p }); }
      if (action === "clone" && method === "POST") { const c = JSON.parse(JSON.stringify(p)); c.id = nid(); c.name = p.name + " copy"; c.deletedAt = null; db.profiles.push(c); return jsonRes(201, { ok: true, profile: c }); }
      if (method === "GET") return jsonRes(200, { ok: true, profile: p });
      if (method === "PATCH") { const upd = deepMerge(p, body || {}); db.profiles[db.profiles.indexOf(p)] = upd; return jsonRes(200, { ok: true, profile: upd }); }
      if (method === "DELETE") { p.deletedAt = new Date().toISOString(); return jsonRes(200, { ok: true, profile: p }); }
    }
    if (seg[1] === "folders") {
      const id = seg[2] || null;
      if (!id) {
        if (method === "GET") return jsonRes(200, { ok: true, folders: db.folders });
        if (method === "POST") { const f = { id: nid(), name: (body && body.name) || "Folder" }; db.folders.push(f); return jsonRes(201, { ok: true, folder: f }); }
      }
      const f = db.folders.find(x => x.id === id);
      if (!f) return jsonRes(404, { ok: false, error: "folder not found" });
      if (method === "PATCH") { f.name = body.name; return jsonRes(200, { ok: true, folder: f }); }
      if (method === "DELETE") { db.folders.splice(db.folders.indexOf(f), 1); return jsonRes(200, { ok: true, deleted: true }); }
    }
    if (seg[1] === "proxies") {
      const sub = seg[2] || null;
      if (sub === "check" && method === "POST") {
        if (!body.host) return jsonRes(200, { ok: false, error: "host is required" });
        if (body.type === "socks4") return jsonRes(200, { ok: false, error: "socks4 check unsupported in v1" });
        return jsonRes(200, { ok: true, ip: "203.0.113.7", country: "Philippines", countryCode: "PH", city: "Manila", timezone: "Asia/Manila", latencyMs: 42 });
      }
      if (sub === "builtin" && method === "POST") return jsonRes(501, { ok: false, error: "no residential provider configured" });
      if (sub === "catalog" && method === "GET") return jsonRes(200, { ok: true, catalog: [], providers: [], builtinAvailable: false });
      if (!sub) {
        if (method === "GET") return jsonRes(200, { ok: true, proxies: db.proxies });
        if (method === "POST") { const sp = deepMerge({ name: "Proxy", proxy: {} }, body || {}); if (!sp.id) sp.id = nid(); db.proxies.push(sp); return jsonRes(201, { ok: true, proxy: sp }); }
      }
      const sp = db.proxies.find(x => x.id === sub);
      if (!sp) return jsonRes(404, { ok: false, error: "proxy not found" });
      if (method === "PATCH") { const upd = deepMerge(sp, body || {}); db.proxies[db.proxies.indexOf(sp)] = upd; return jsonRes(200, { ok: true, proxy: upd }); }
      if (method === "DELETE") { db.proxies.splice(db.proxies.indexOf(sp), 1); return jsonRes(200, { ok: true, deleted: true }); }
    }
    if (seg[1] === "tokens") {
      const id = seg[2] || null;
      if (!id) {
        if (method === "GET") return jsonRes(200, { ok: true, tokens: db.tokens.map(t => ({ id: t.id, name: t.name, prefix: t.prefix, workspaceId: "ws", createdAt: t.createdAt, lastUsedAt: null })) });
        if (method === "POST") { const raw = "dlk_raw_" + nid(); const rec = { id: nid(), name: (body && body.name) || "API token", prefix: raw.slice(0, 12), createdAt: new Date().toISOString() }; db.tokens.push(rec); return jsonRes(201, { ok: true, token: raw, apiToken: { id: rec.id, name: rec.name, prefix: rec.prefix, createdAt: rec.createdAt, lastUsedAt: null } }); }
      }
      if (method === "DELETE") { const i = db.tokens.findIndex(t => t.id === id); if (i < 0) return jsonRes(404, { ok: false, error: "token not found" }); db.tokens.splice(i, 1); return jsonRes(200, { ok: true, deleted: true }); }
    }
    return jsonRes(404, { ok: false, error: "not found" });
  }
  return { fetchStub, calls, db, state };
}

function boot(server, seedStore, seedAuth) {
  const dom = new JSDOM(html, {
    url: "http://localhost/", runScripts: "dangerously", pretendToBeVisual: true,
    beforeParse(w) {
      w.fetch = server.fetchStub;
      if (seedStore !== undefined) w.localStorage.setItem("devxyzlogin_v1", typeof seedStore === "string" ? seedStore : JSON.stringify(seedStore));
      if (seedAuth) w.localStorage.setItem("devxyzlogin_auth", JSON.stringify(seedAuth));
    }
  });
  return dom.window;
}
const findCall = (srv, method, path) => srv.calls.find(c => c.method === method && c.path === path);
const hasCall = (srv, method, path) => !!findCall(srv, method, path);

(async () => {
  /* ============ 1. local mode: ZERO fetch calls ============ */
  section("local mode isolation");
  {
    const srv = makeServer();
    const w = boot(srv); await sleep(120);
    ok("local boot made zero fetch calls", srv.calls.length === 0);
    ok("sync chip shows Local mode", (w.document.querySelector("#syncChip") || {}).textContent.includes("Local mode"));
    // proxy check in local mode -> sign-in modal, still zero calls
    w.eval("showPage('proxies')"); await sleep(30);
    const chkBtn = w.document.querySelector("[data-pxcheck]");
    ok("proxies page has a check button", !!chkBtn);
    chkBtn.click(); await sleep(30);
    ok("local proxy check shows sign-in state", (w.document.querySelector("#modalOverlay") || { textContent: "" }).textContent.includes("Sign in to run a live proxy check"));
    ok("still zero fetch calls after local check", srv.calls.length === 0);
    w.eval("closeModal()");
    // tokens page in local mode
    w.eval("showPage('pa'); switchPa('api')"); await sleep(30);
    ok("local API page states tokens require sign-in", w.document.querySelector("#content").textContent.includes("Sign in to manage API tokens"));
    ok("zero fetch calls after API page", srv.calls.length === 0);
    // local profile create via editor
    w.eval("openEditor(null,false)"); await sleep(20);
    w.document.querySelector("#edCreate").click(); await sleep(30);
    ok("zero fetch calls after local profile create", srv.calls.length === 0);
  }

  /* ============ 2. login failure surfaces the real error ============ */
  section("login failure");
  {
    const srv = makeServer();
    srv.db.users.push({ id: "u1", email: "jepong@example.com", password: "correct-horse" });
    srv.db.workspaces.push({ id: "ws1", name: "My workspace" });
    const w = boot(srv, { session: null, email: "" }); await sleep(80);
    ok("logged-out boot shows login view", !w.document.querySelector("#loginView").classList.contains("hidden"));
    const em = w.document.querySelector("#loginEmail");
    em.value = "jepong@example.com"; em.dispatchEvent(new w.Event("input", { bubbles: true }));
    w.document.querySelector("#loginBtn").click(); await sleep(20);
    const pw = w.document.querySelector("#loginPw");
    pw.value = "wrong-pass"; pw.dispatchEvent(new w.Event("input", { bubbles: true }));
    w.document.querySelector("#loginBtn").click();
    await until(() => !w.document.querySelector("#loginErr").classList.contains("hidden"));
    ok("wrong password shows login error", w.document.querySelector("#loginErr").textContent.includes("Wrong email or password"));
    ok("still on login view after failure", !w.document.querySelector("#loginView").classList.contains("hidden"));
    const lc = findCall(srv, "POST", "/api/auth/login");
    ok("login POST hit the API with email+password", !!lc && lc.body.email === "jepong@example.com" && lc.body.password === "wrong-pass");
    ok("no auth stored after failed login", !w.localStorage.getItem("devxyzlogin_auth"));
  }

  /* ============ 3. login success + merge ============ */
  section("login success + merge");
  let w3, srv3;
  {
    srv3 = makeServer();
    srv3.db.users.push({ id: "u1", email: "jepong@example.com", password: "correct-horse" });
    srv3.db.workspaces.push({ id: "ws1", name: "My workspace" });
    srv3.db.folders.push({ id: "srvfold1", name: "Server Folder" });
    // server-only profile in pure shared-schema shape (no web extras)
    srv3.db.profiles.push({ id: "srvprof1", name: "Schema Only", folderIds: ["srvfold1"], tags: ["vip"], status: "ready", notes: "from android", os: "android",
      fingerprint: { navigator: { userAgent: "UA-ANDROID", browserVersion: "Chrome 130", screenResolution: "412x915", languages: ["en-US"], platform: "Linux armv81", cpuCores: 8, ram: 8, doNotTrack: false }, fontsMasking: true, mediaDevices: { videoInputs: 1, audioInputs: 1, audioOutputs: 1 }, hardware: { canvasMode: "noise", webglVendor: "Qualcomm", webglRenderer: "ANGLE (Qualcomm, Adreno (TM) 730, OpenGL ES 3.2)", webglNoise: true, audioNoise: true, clientRectsNoise: false }, webrtcMode: "basedOnIp", timezone: { auto: true, zone: "Asia/Manila" }, geolocation: { mode: "prompt", auto: true, lat: 14.6, lon: 121.0 } },
      proxy: { mode: "custom", type: "socks5", host: "10.0.0.9", port: 1080, username: "u9", password: "p9" }, extensions: [], bookmarks: ["Google::https://google.com"], createdAt: "2026-11-01T00:00:00.000Z", updatedAt: "2026-11-02T00:00:00.000Z", deletedAt: null });
    // server profile WITH web extras (round-trip shape)
    srv3.db.profiles.push({ id: "srvprof2", name: "Web Roundtrip", folderIds: [], tags: ["ads"], status: "ready", notes: "", os: "linux", fingerprint: {}, proxy: { mode: "none", type: "http", host: "", port: 0, username: "", password: "" }, extensions: ["uBlock Origin"], bookmarks: [], createdAt: "2026-11-01T00:00:00.000Z", updatedAt: "2026-11-02T00:00:00.000Z", deletedAt: null,
      fp: null, webProxy: null, webTags: [{ text: "ads", color: "#4d7cff" }], state: "ready", pinned: true });
    srv3.db.profiles[1].fp = { os: "Linux", ua: "UA-LINUX", browserVersion: "Chrome 156", screen: "1920x1080", languages: ["en-PH"], platform: "Linux x86_64", cpu: 8, ram: 8, doNotTrack: false, timezoneAuto: true, timezone: "Asia/Manila", fontsMasking: true, fonts: [], fontsCount: 120, mediaVideo: 1, mediaAudioIn: 1, mediaAudioOut: 1, mediaMasking: true, canvas: "Off", webgl: "Noise", webglImage: "Off", webglMetadata: "Mask", webglVendor: "Google Inc. (Qualcomm)", webglRenderer: "ANGLE (Qualcomm, Adreno (TM) 618, OpenGL ES 3.2)", audioContext: "Noise", clientRects: "Off", webrtc: "Based on IP", geoMode: "Prompt", geoAuto: true, geoLat: "14.6", geoLong: "121.0", country: "PH", countryName: "Philippines", flag: "🇵🇭", ip: "1.2.3.4", storageCookies: true, storageLocal: true, storageIndexedDb: false, extStorage: true, pluginsVuln: true, googleServices: true, disableImages: false, disableAutoplay: false, saveTabs: true, saveHistory: true, saveBookmarks: true, savePasswords: true, allowInstallExt: true, systemExt: false, startUrl: "Previously opened tabs", launchArgs: "", customDns: "", seed: 42 };
    srv3.db.profiles[1].webProxy = { mode: "none", proxyId: "", host: "", port: "", type: "SOCKS5", login: "", password: "", buyCountry: "FR", buyType: "Residential", dedicated: false, changeIpUrl: "", ip: "", flag: "", country: "" };
    srv3.db.proxies.push({ id: "srvpx1", name: "Server Proxy", proxy: { mode: "custom", type: "http", host: "proxy.example", port: 8080, username: "pxuser", password: "pxpass" } });
    srv3.db.tokens.push({ id: "tok1", name: "Seed token", prefix: "dlk_seed_123", createdAt: "2026-11-01T00:00:00.000Z" });

    w3 = boot(srv3, { session: null, email: "" }); await sleep(80);
    const em = w3.document.querySelector("#loginEmail");
    em.value = "jepong@example.com"; em.dispatchEvent(new w3.Event("input", { bubbles: true }));
    w3.document.querySelector("#loginBtn").click(); await sleep(20);
    const pw = w3.document.querySelector("#loginPw");
    pw.value = "correct-horse"; pw.dispatchEvent(new w3.Event("input", { bubbles: true }));
    w3.document.querySelector("#loginBtn").click();
    await until(() => !w3.document.querySelector("#app").classList.contains("hidden"));
    ok("login success enters app", true);
    await until(() => w3.eval("S.profiles.some(p=>p.serverId==='srvprof1')"));
    const auth = JSON.parse(w3.localStorage.getItem("devxyzlogin_auth"));
    ok("auth stored under devxyzlogin_auth with token+workspaceId", !!auth && auth.token.startsWith("dl1.") && auth.workspaceId === "ws1" && auth.email === "jepong@example.com");
    ok("sync chip shows Synced", (w3.document.querySelector("#syncChip") || {}).textContent.includes("Synced"));
    ok("workspace adopted from server", w3.eval("S.workspace") === "My workspace" && w3.eval("S.workspaces[0].id") === "ws1");
    const sp1 = w3.eval("S.profiles.find(p=>p.serverId==='srvprof1')");
    ok("schema-only profile merged + mapped (os display, webrtc, ua)", !!sp1 && sp1.fp.os === "Android" && sp1.fp.webrtc === "Based on IP" && sp1.fp.ua === "UA-ANDROID" && sp1.proxy.login === "u9" && sp1.proxy.type === "SOCKS5");
    const sp2 = w3.eval("S.profiles.find(p=>p.serverId==='srvprof2')");
    ok("web-extras profile round-trips (fp, pinned, tags, extensions)", !!sp2 && sp2.fp.ua === "UA-LINUX" && sp2.pinned === true && sp2.tags[0].color === "#4d7cff" && sp2.extensions.includes("uBlock Origin"));
    ok("server folder merged", w3.eval("S.folders.some(f=>f.serverId==='srvfold1'&&f.name==='Server Folder')"));
    ok("server proxy merged + flattened (username->login)", w3.eval("S.proxies.some(x=>x.serverId==='srvpx1'&&x.login==='pxuser'&&x.port==='8080')"));
    await until(() => srv3.db.profiles.filter(p => !p.deletedAt).length >= 5);
    ok("local seed profiles uploaded to server (3 local + 2 server)", srv3.db.profiles.length >= 5);
    const post = findCall(srv3, "POST", "/api/profiles");
    ok("profile upload carried Authorization + X-Workspace-Id headers", !!post && post.headers["Authorization"] === "Bearer " + auth.token && post.headers["X-Workspace-Id"] === "ws1");
    ok("uploaded profile uses shared schema (fingerprint.navigator + proxy.username)", !!post && !!post.body.fingerprint.navigator && typeof post.body.proxy.username === "string" && !!post.body.fp);
    await until(() => w3.eval("S.profiles.filter(p=>!p.serverId).length") === 0);
    ok("all local profiles gained serverIds after merge", true);
  }

  /* ============ 4. mutations while synced ============ */
  section("synced mutations");
  {
    const w = w3, srv = srv3;
    const baseCount = srv.calls.length;
    // create via editor
    w.eval("openEditor(null,false)"); await sleep(20);
    const nameInp = w.document.querySelector("#edName"); nameInp.value = "Mut Created"; nameInp.dispatchEvent(new w.Event("input", { bubbles: true }));
    w.document.querySelector("#edCreate").click();
    await until(() => { const p = w.eval("S.profiles.find(x=>x.name==='Mut Created')"); return p && p.serverId; });
    ok("editor create -> POST /api/profiles + serverId gained", hasCall(srv, "POST", "/api/profiles"));
    const created = w.eval("S.profiles.find(x=>x.name==='Mut Created')");
    // edit via editor (rename)
    w.eval("openEditor('" + created.id + "',false)"); await sleep(20);
    const ni = w.document.querySelector("#edName"); ni.value = "Mut Renamed"; ni.dispatchEvent(new w.Event("input", { bubbles: true }));
    w.document.querySelector("#edCreate").click();
    await until(() => hasCall(srv, "PATCH", "/api/profiles/" + created.serverId));
    ok("editor edit -> PATCH /api/profiles/:serverId", true);
    // clone via the app's own cloneProfile (row-menu path)
    w.eval("cloneProfile(findProfile('" + created.id + "'))");
    await until(() => hasCall(srv, "POST", "/api/profiles/" + created.serverId + "/clone"));
    ok("clone -> POST /api/profiles/:id/clone", true);
    await until(() => w.eval("S.profiles.some(p=>p.name==='Mut Renamed copy'&&p.serverId&&p.serverId!=='" + created.serverId + "')"));
    ok("clone gained its own serverId", true);
    // soft delete via the app's own softDelete (row-menu path)
    const cloneP = w.eval("S.profiles.find(p=>p.name==='Mut Renamed copy')");
    w.eval("softDelete(findProfile('" + cloneP.id + "'))");
    await until(() => hasCall(srv, "DELETE", "/api/profiles/" + cloneP.serverId));
    ok("soft delete -> DELETE /api/profiles/:id", true);
    ok("profile sits in local trash", w.eval("S.deleted.some(p=>p.id==='" + cloneP.id + "')"));
    // restore exactly like the Restore page: local move first, then the DL call
    w.eval("(()=>{const i=S.deleted.findIndex(p=>p.id==='" + cloneP.id + "');const p=S.deleted.splice(i,1)[0];delete p.deletedAt;delete p.deletedBy;S.profiles.unshift(p);save();DL.profileRestored(p)})()");
    await until(() => hasCall(srv, "POST", "/api/profiles/" + cloneP.serverId + "/restore"));
    ok("restore -> POST /api/profiles/:id/restore", true);
    ok("profile back in local list", w.eval("S.profiles.some(p=>p.id==='" + cloneP.id + "')"));
    // folder create via DL (the exact call the folders menu makes)
    w.eval("const f={id:'localfold1',name:'Synced Folder'}; S.folders.push(f); DL.folderCreated(f)");
    await until(() => w.eval("S.folders.some(x=>x.name==='Synced Folder'&&x.serverId)"));
    ok("folder create -> POST /api/folders + server id adopted locally", hasCall(srv, "POST", "/api/folders"));
    // shared proxy create via DL (the exact call the proxy dialog makes)
    w.eval("const rec={id:'localpx1',name:'My PX',type:'SOCKS5',host:'5.6.7.8',port:'1080',login:'me',password:'pw',country:'Philippines',flag:'🇵🇭',ip:'5.6.7.8',state:'Connected',kind:'Custom'}; S.proxies.push(rec); DL.proxySaved(rec,false,null)");
    await until(() => w.eval("S.proxies.find(x=>x.id==='localpx1').serverId"));
    const pxPost = srv.calls.filter(c => c.method === "POST" && c.path === "/api/proxies").pop();
    ok("proxy create -> POST /api/proxies with schema proxy{username}", !!pxPost && pxPost.body.proxy.username === "me" && pxPost.body.proxy.port === 1080);
    ok("mutation calls all carried the auth header", srv.calls.slice(baseCount).filter(c => c.path.startsWith("/api/")).every(c => String(c.headers["Authorization"] || "").startsWith("Bearer dl1.")));
  }

  /* ============ 5. rollback on server error + offline dirty/flush ============ */
  section("rollback + offline");
  {
    const w = w3, srv = srv3;
    // server-error rollback on create
    srv.state.failNext = { status: 500, body: { ok: false, error: "boom" } };
    const nBefore = w.eval("S.profiles.length");
    w.eval("const p=makeProfile('Rollback Me'); S.profiles.unshift(p); save(); DL.profileSaved(p,true,null); window.__rb=p");
    await until(() => w.eval("S.profiles.length") === nBefore);
    ok("500 on create rolls back the optimistic profile", true);
    await until(() => w.document.querySelector("#toastWrap").textContent.includes("was not created"));
    ok("rollback shows an honest toast", true);
    // offline: mutation is kept, flagged dirty, chip shows Offline
    const target = w.eval("S.profiles.find(p=>p.serverId)");
    srv.state.down = true;
    w.eval("DL.profileUpdated(S.profiles.find(p=>p.serverId==='" + target.serverId + "'))");
    await until(() => (w.document.querySelector("#syncChip") || {}).textContent.includes("Offline"));
    ok("network failure flips chip to Offline", true);
    ok("offline change kept + flagged _dirty", w.eval("S.profiles.find(p=>p.serverId==='" + target.serverId + "')._dirty") === true);
    srv.state.down = false;
    w.eval("DL.profileUpdated(S.profiles.find(p=>p.serverId==='" + target.serverId + "'))");
    await until(() => w.eval("S.profiles.find(p=>p.serverId==='" + target.serverId + "')._dirty") !== true);
    ok("back online: dirty flag cleared via PATCH", hasCall(srv, "PATCH", "/api/profiles/" + target.serverId));
    ok("chip back to Synced", (w.document.querySelector("#syncChip") || {}).textContent.includes("Synced"));
  }

  /* ============ 6. API tokens CRUD (synced) ============ */
  section("tokens CRUD");
  {
    const w = w3, srv = srv3;
    w.eval("showPage('pa'); switchPa('api')"); await sleep(30);
    await until(() => w.document.querySelector("#content").textContent.includes("Seed token"));
    ok("tokens list renders server tokens (prefix masked)", w.document.querySelector("#content").textContent.includes("dlk_seed_123"));
    // create via the real UI (New Token -> promptDialog)
    w.document.querySelector("#tokNew").click(); await sleep(30);
    const pd = w.document.querySelector("#pdInput"); pd.value = "CI token";
    w.document.querySelector("#pdYes").click();
    await until(() => (w.document.querySelector("#modalOverlay") || { textContent: "" }).textContent.includes("dlk_raw_"));
    ok("token create shows the dlk_ value once", true);
    ok("token create POST recorded", hasCall(srv, "POST", "/api/tokens"));
    w.eval("closeModal()");
    await until(() => w.document.querySelector("#content").textContent.includes("CI token"));
    ok("token list refreshes with the new token", true);
    // delete via the real UI (Delete -> confirmDialog)
    const newTok = srv.db.tokens.find(t => t.name === "CI token");
    w.document.querySelector('[data-tokdel="' + newTok.id + '"]').click(); await sleep(30);
    w.document.querySelector("#cfYes").click();
    await until(() => hasCall(srv, "DELETE", "/api/tokens/" + newTok.id));
    ok("token delete -> DELETE /api/tokens/:id", true);
    ok("token gone from server db", !srv.db.tokens.find(t => t.id === newTok.id));
  }

  /* ============ 7. real proxy check render (synced) ============ */
  section("proxy check");
  {
    const w = w3, srv = srv3;
    w.eval("checkProxyFlow({mode:'custom',type:'SOCKS5',host:'1.2.3.4',port:'1080',login:'u',password:'p'})");
    await until(() => (w.document.querySelector("#modalOverlay") || { textContent: "" }).textContent.includes("Proxy is working"));
    const mt = w.document.querySelector("#modalOverlay").textContent;
    ok("check renders real exit IP/country/timezone/latency", mt.includes("203.0.113.7") && mt.includes("Philippines") && mt.includes("Asia/Manila") && mt.includes("42"));
    const cc = findCall(srv, "POST", "/api/proxies/check");
    ok("check POST body maps type/host/port/username", !!cc && cc.body.type === "socks5" && cc.body.host === "1.2.3.4" && cc.body.port === 1080 && cc.body.username === "u");
    w.eval("closeModal()");
    w.eval("checkProxyFlow({mode:'builtin',type:'Residential',buyCountry:'FR',host:'geo.devxyzlogin.local',port:10080})");
    await until(() => (w.document.querySelector("#modalOverlay") || { textContent: "" }).textContent.includes("not available yet"));
    ok("builtin check surfaces the honest 501 state", true);
    w.eval("closeModal()");
  }

  /* ============ 8. register flow + duplicate ============ */
  section("register");
  {
    const srv = makeServer();
    const w = boot(srv, { session: null, email: "" }); await sleep(80);
    w.document.querySelector("#modeLink").click(); await sleep(10);
    ok("register mode reveals the name field", !w.document.querySelector("#nameField").classList.contains("hidden"));
    w.document.querySelector("#loginName").value = "Jepong";
    const em = w.document.querySelector("#loginEmail");
    em.value = "new@example.com"; em.dispatchEvent(new w.Event("input", { bubbles: true }));
    w.document.querySelector("#loginBtn").click(); await sleep(20);
    const pw = w.document.querySelector("#loginPw");
    pw.value = "secret-123"; pw.dispatchEvent(new w.Event("input", { bubbles: true }));
    w.document.querySelector("#loginBtn").click();
    await until(() => !w.document.querySelector("#app").classList.contains("hidden"));
    ok("register enters the app", true);
    const rc = findCall(srv, "POST", "/api/auth/register");
    ok("register POST carried name+email+password", !!rc && rc.body.name === "Jepong" && rc.body.email === "new@example.com");
    ok("auth stored after register", !!w.localStorage.getItem("devxyzlogin_auth"));
    // duplicate registration is refused with a real error
    const srv2 = makeServer();
    srv2.db.users.push({ id: "u9", email: "dup@example.com", password: "whatever1" });
    srv2.db.workspaces.push({ id: "ws9", name: "My workspace" });
    const w2 = boot(srv2, { session: null, email: "" }); await sleep(80);
    w2.document.querySelector("#modeLink").click(); await sleep(10);
    const em2 = w2.document.querySelector("#loginEmail");
    em2.value = "dup@example.com"; em2.dispatchEvent(new w2.Event("input", { bubbles: true }));
    w2.document.querySelector("#loginBtn").click(); await sleep(20);
    const pw2 = w2.document.querySelector("#loginPw");
    pw2.value = "whatever1"; pw2.dispatchEvent(new w2.Event("input", { bubbles: true }));
    w2.document.querySelector("#loginBtn").click();
    await until(() => !w2.document.querySelector("#loginErr").classList.contains("hidden"));
    ok("duplicate register shows 'already registered'", w2.document.querySelector("#loginErr").textContent.includes("already registered"));
  }

  /* ============ 9. seeded-auth boot syncs without a login click ============ */
  section("seeded auth boot");
  {
    const srv = makeServer();
    srv.db.workspaces.push({ id: "wsB", name: "My workspace" });
    srv.db.profiles.push({ id: "srvB1", name: "Boot Synced", folderIds: [], tags: [], status: "ready", notes: "", os: "linux", fingerprint: { navigator: { userAgent: "UA-BOOT", browserVersion: "Chrome 156", screenResolution: "1920x1080", languages: ["en-US"], platform: "Linux x86_64", cpuCores: 8, ram: 8, doNotTrack: false }, fontsMasking: true, mediaDevices: { videoInputs: 1, audioInputs: 1, audioOutputs: 1 }, hardware: { canvasMode: "off", webglVendor: "V", webglRenderer: "R", webglNoise: false, audioNoise: false, clientRectsNoise: false }, webrtcMode: "off", timezone: { auto: false, zone: "UTC" }, geolocation: { mode: "block", auto: false, lat: 0, lon: 0 } }, proxy: { mode: "none", type: "http", host: "", port: 0, username: "", password: "" }, extensions: [], bookmarks: [], createdAt: "2026-11-01T00:00:00.000Z", updatedAt: "2026-11-01T00:00:00.000Z", deletedAt: null });
    const w = boot(srv, { session: { email: "boot@example.com" }, email: "boot@example.com" }, { token: "dl1.stub.uB", email: "boot@example.com", workspaceId: "wsB", purged: [] });
    await until(() => w.eval("S.profiles.some(p=>p.serverId==='srvB1')"), 4000);
    ok("boot with stored auth merges server profiles automatically", true);
    ok("chip shows Synced at boot", (w.document.querySelector("#syncChip") || {}).textContent.includes("Synced"));
    // sign out clears the auth key and returns to the login view
    w.eval("DL.signOut()");
    await sleep(30);
    ok("sign out clears devxyzlogin_auth", !w.localStorage.getItem("devxyzlogin_auth"));
    ok("sign out returns to login view", !w.document.querySelector("#loginView").classList.contains("hidden"));
  }

  console.log("\nSMOKE v4: " + pass + " passed, " + fail + " failed");
  if (fails.length) console.log("FAILED:", fails.join(" | "));
  process.exit(fail ? 1 : 0);
})().catch(e => { console.error("SMOKE CRASH:", e); process.exit(2); });
