'use strict';
/**
 * DevxyzLogin server — the working backend for the DevxyzLogin antidetect
 * browser (Jepong's GoLogin-style app). Zero external dependencies: Node
 * built-in http/fs/crypto only.
 *
 * This module IS the request handler. It runs two ways:
 *   - local / self-host: `node server.js` (PORT env, default 8787)
 *   - Vercel: api/index.js re-exports this handler
 *
 * Auth tokens are HMAC-signed (`dl1.` prefix, 30-day expiry) so any instance
 * that shares TOKEN_SECRET (env) can verify them without a DB lookup.
 * Programmatic API tokens (`dlk_` prefix, GoLogin API & MCP parity) are
 * stored only as SHA-256 hashes and authenticate the same way.
 *
 * All entity shapes match ../SHARED_SCHEMA.md exactly.
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { DATA_FILE, loadDb, saveDb } = require('./store');
const { checkProxy } = require('./proxycheck');

const VERSION = '1.0.0';
const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000; // 30 days
const MAX_BODY_BYTES = 1024 * 1024;

let db = loadDb();

// ---------------------------------------------------------------------------
// small helpers
function nowIso() { return new Date().toISOString(); }
function newId() { return crypto.randomUUID(); }

function isPlainObject(v) { return v !== null && typeof v === 'object' && !Array.isArray(v); }

// Deep-merge `src` over `base`: plain objects merge recursively, everything
// else (including arrays) is replaced by the source value. Source wins.
function deepMerge(base, src) {
  const out = Array.isArray(base) ? base.slice() : Object.assign({}, base);
  if (!isPlainObject(src)) return out;
  for (const k of Object.keys(src)) {
    if (isPlainObject(src[k]) && isPlainObject(out[k])) out[k] = deepMerge(out[k], src[k]);
    else out[k] = src[k];
  }
  return out;
}
function deepClone(v) { return JSON.parse(JSON.stringify(v)); }

function send(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Workspace-Id',
    'Access-Control-Allow-Methods': 'GET, POST, PATCH, DELETE, OPTIONS',
  });
  res.end(body);
}
function ok(res, obj, status) { send(res, status || 200, Object.assign({ ok: true }, obj)); }
function fail(res, status, error) { send(res, status, { ok: false, error }); }

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', (c) => {
      size += c.length;
      if (size > MAX_BODY_BYTES) {
        reject(Object.assign(new Error('request body too large'), { status: 413 }));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8').trim();
      if (!raw) return resolve({});
      try { resolve(JSON.parse(raw)); }
      catch (e) { reject(Object.assign(new Error('invalid JSON body'), { status: 400 })); }
    });
    req.on('error', reject);
  });
}

// ---------------------------------------------------------------------------
// passwords (scrypt, never stored or returned in plaintext)
function hashPassword(password) {
  const salt = crypto.randomBytes(16);
  const hash = crypto.scryptSync(String(password), salt, 64);
  return 'scrypt$' + salt.toString('base64') + '$' + hash.toString('base64');
}
function verifyPassword(password, stored) {
  try {
    const parts = String(stored).split('$');
    if (parts.length !== 3 || parts[0] !== 'scrypt') return false;
    const salt = Buffer.from(parts[1], 'base64');
    const expected = Buffer.from(parts[2], 'base64');
    const actual = crypto.scryptSync(String(password), salt, expected.length);
    return crypto.timingSafeEqual(expected, actual);
  } catch (e) { return false; }
}

// ---------------------------------------------------------------------------
// signed session tokens: dl1.<base64url payload>.<base64url HMAC-SHA256>
function tokenSecret() {
  if (process.env.TOKEN_SECRET) return process.env.TOKEN_SECRET;
  if (!db.meta.tokenSecret) {
    db.meta.tokenSecret = crypto.randomBytes(32).toString('hex');
    saveDb(db);
  }
  return db.meta.tokenSecret;
}
function signToken(payload) {
  const body = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
  const sig = crypto.createHmac('sha256', tokenSecret()).update('dl1.' + body).digest('base64url');
  return 'dl1.' + body + '.' + sig;
}
function verifyToken(token) {
  const parts = String(token).split('.');
  if (parts.length !== 3 || parts[0] !== 'dl1') return null;
  const expected = crypto.createHmac('sha256', tokenSecret()).update('dl1.' + parts[1]).digest('base64url');
  const a = Buffer.from(expected);
  const b = Buffer.from(parts[2]);
  if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) return null;
  let payload;
  try { payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8')); }
  catch (e) { return null; }
  if (!payload || !payload.uid || !payload.exp || Date.now() > payload.exp) return null;
  return payload;
}
function hashApiToken(raw) { return crypto.createHash('sha256').update(String(raw)).digest('hex'); }

// ---------------------------------------------------------------------------
// auth: resolves a request to { user, workspaceId } or null.
// Accepts session tokens (dl1.) and API tokens (dlk_) in `Authorization: Bearer`.
function userWorkspaces(userId) {
  return db.workspaces.filter((w) => db.owners.workspaces[w.id] === userId);
}
function authenticate(req) {
  const header = req.headers['authorization'] || '';
  const m = header.match(/^Bearer\s+(.+)$/i);
  if (!m) return null;
  const token = m[1].trim();

  if (token.startsWith('dl1.')) {
    const payload = verifyToken(token);
    if (!payload) return null;
    const user = db.users.find((u) => u.id === payload.uid);
    if (!user) return null;
    let workspaceId = payload.wid;
    if (!workspaceId || db.owners.workspaces[workspaceId] !== user.id) {
      const mine = userWorkspaces(user.id);
      workspaceId = mine.length ? mine[0].id : null;
    }
    return { user, workspaceId, via: 'session' };
  }

  if (token.startsWith('dlk_')) {
    const rec = db.apiTokens.find((t) => t.hash === hashApiToken(token));
    if (!rec) return null;
    const user = db.users.find((u) => u.id === rec.userId);
    if (!user) return null;
    rec.lastUsedAt = nowIso();
    saveDb(db);
    return { user, workspaceId: rec.workspaceId, via: 'apiToken' };
  }

  return null;
}

// Applies the optional X-Workspace-Id override (must be one of the user's own
// workspaces). Returns a workspaceId, or null after sending an error.
function resolveWorkspace(req, res, auth) {
  const wanted = req.headers['x-workspace-id'];
  if (!wanted) return auth.workspaceId;
  if (db.owners.workspaces[wanted] === auth.user.id) return wanted;
  fail(res, 403, 'workspace not found for this account');
  return null;
}

// ---------------------------------------------------------------------------
// schema defaults (../SHARED_SCHEMA.md)
function defaultFingerprint() {
  return {
    navigator: {
      userAgent: '',
      browserVersion: '',
      screenResolution: '1920x1080',
      languages: ['en-US'],
      platform: 'Win32',
      cpuCores: 8,
      ram: 8,
      doNotTrack: false,
    },
    fontsMasking: true,
    mediaDevices: { videoInputs: 1, audioInputs: 1, audioOutputs: 1 },
    hardware: {
      canvasMode: 'noise',
      webglVendor: '',
      webglRenderer: '',
      webglNoise: true,
      audioNoise: true,
      clientRectsNoise: true,
    },
    webrtcMode: 'basedOnIp',
    timezone: { auto: true, zone: 'UTC' },
    geolocation: { mode: 'prompt', auto: true, lat: 0, lon: 0 },
  };
}
function defaultProxy() {
  return { mode: 'none', type: 'http', host: '', port: 0, username: '', password: '' };
}
function buildProfile(body) {
  const base = {
    id: newId(),
    name: 'Untitled profile',
    folderIds: [],
    tags: [],
    status: 'ready',
    notes: '',
    os: 'linux',
    fingerprint: defaultFingerprint(),
    proxy: defaultProxy(),
    extensions: [],
    bookmarks: [],
    createdAt: nowIso(),
    updatedAt: nowIso(),
    deletedAt: null,
  };
  const profile = deepMerge(base, isPlainObject(body) ? body : {});
  if (typeof profile.id !== 'string' || !profile.id) profile.id = newId();
  if (typeof profile.name !== 'string' || !profile.name) profile.name = 'Untitled profile';
  profile.deletedAt = null; // a freshly created profile is never in the trash
  return profile;
}
function buildSharedProxy(body) {
  const base = { id: newId(), name: 'Proxy', proxy: defaultProxy() };
  const sp = deepMerge(base, isPlainObject(body) ? body : {});
  if (typeof sp.id !== 'string' || !sp.id) sp.id = newId();
  if (typeof sp.name !== 'string' || !sp.name) sp.name = 'Proxy';
  if (sp.proxy && sp.proxy.mode === 'none') sp.proxy.mode = 'custom';
  return sp;
}

// ---------------------------------------------------------------------------
// built-in proxy catalog + provider plug-ins
const CATALOG_COUNTRIES = [
    ['US', 'United States'], ['GB', 'United Kingdom'], ['DE', 'Germany'],
    ['FR', 'France'], ['NL', 'Netherlands'], ['ES', 'Spain'], ['IT', 'Italy'],
    ['PL', 'Poland'], ['TR', 'Turkey'], ['AE', 'United Arab Emirates'],
    ['ZA', 'South Africa'], ['BR', 'Brazil'], ['MX', 'Mexico'], ['CA', 'Canada'],
    ['AU', 'Australia'], ['IN', 'India'], ['JP', 'Japan'], ['KR', 'South Korea'],
    ['SG', 'Singapore'], ['PH', 'Philippines'], ['ID', 'Indonesia'],
    ['VN', 'Vietnam'], ['TH', 'Thailand'], ['MY', 'Malaysia'],
].map(([code, name]) => ({ code, name }));

function loadProviders() {
  let list = null;
  if (process.env.RESIDENTIAL_PROVIDERS) {
    try {
      const parsed = JSON.parse(process.env.RESIDENTIAL_PROVIDERS);
      if (Array.isArray(parsed)) list = parsed;
    } catch (e) { /* fall through to the file */ }
  }
  if (!list) {
    try {
      const parsed = JSON.parse(fs.readFileSync(path.join(__dirname, 'providers.json'), 'utf8'));
      if (parsed && Array.isArray(parsed.providers)) list = parsed.providers;
    } catch (e) { /* no provider file = no providers */ }
  }
  return (list || []).map((p) => ({
    id: p.id || 'provider',
    name: p.name || 'Provider',
    types: Array.isArray(p.types) ? p.types : [],
    endpointTemplate: typeof p.endpointTemplate === 'string' ? p.endpointTemplate : '',
    configured: !!(p.endpointTemplate && String(p.endpointTemplate).trim()),
  }));
}
function publicProviders() {
  // The endpoint template itself is never echoed back (it may embed a password).
  return loadProviders().map((p) => ({ id: p.id, name: p.name, types: p.types, configured: p.configured }));
}

// ---------------------------------------------------------------------------
// route handlers
function publicUser(u) { return { id: u.id, email: u.email, createdAt: u.createdAt }; }

async function handleAuth(req, res, seg) {
  if (seg[2] === 'register' && req.method === 'POST') {
    const body = await readBody(req);
    const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : '';
    const password = typeof body.password === 'string' ? body.password : '';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return fail(res, 400, 'a valid email is required');
    if (password.length < 6) return fail(res, 400, 'password must be at least 6 characters');
    if (db.users.some((u) => u.email === email)) return fail(res, 409, 'email already registered');

    const user = { id: newId(), email, passwordHash: hashPassword(password), createdAt: nowIso() };
    db.users.push(user);
    const workspace = { id: newId(), name: 'My workspace' };
    db.workspaces.push(workspace);
    db.owners.workspaces[workspace.id] = user.id;
    const folder = { id: newId(), name: 'Default' };
    db.folders.push(folder);
    db.owners.folders[folder.id] = workspace.id;
    saveDb(db);

    const token = signToken({ uid: user.id, wid: workspace.id, iat: Date.now(), exp: Date.now() + TOKEN_TTL_MS, typ: 'session' });
    return ok(res, { token, user: publicUser(user), workspace }, 201);
  }

  if (seg[2] === 'login' && req.method === 'POST') {
    const body = await readBody(req);
    const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : '';
    const password = typeof body.password === 'string' ? body.password : '';
    const user = db.users.find((u) => u.email === email);
    if (!user || !verifyPassword(password, user.passwordHash)) {
      return fail(res, 401, 'invalid email or password');
    }
    const mine = userWorkspaces(user.id);
    const workspace = mine.length ? mine[0] : null;
    const token = signToken({ uid: user.id, wid: workspace ? workspace.id : null, iat: Date.now(), exp: Date.now() + TOKEN_TTL_MS, typ: 'session' });
    return ok(res, { token, user: publicUser(user), workspace });
  }

  if (seg[2] === 'me' && req.method === 'GET') {
    const auth = authenticate(req);
    if (!auth) return fail(res, 401, 'authentication required');
    const workspace = db.workspaces.find((w) => w.id === auth.workspaceId) || null;
    return ok(res, { user: publicUser(auth.user), workspace });
  }

  return fail(res, 404, 'not found');
}

async function handleWorkspaces(req, res, auth, workspaceId) {
  if (req.method === 'GET') {
    return ok(res, { workspaces: userWorkspaces(auth.user.id) });
  }
  if (req.method === 'POST') {
    const body = await readBody(req);
    const name = typeof body.name === 'string' && body.name.trim() ? body.name.trim() : 'Workspace';
    const workspace = { id: newId(), name };
    db.workspaces.push(workspace);
    db.owners.workspaces[workspace.id] = auth.user.id;
    saveDb(db);
    return ok(res, { workspace }, 201);
  }
  return fail(res, 404, 'not found');
}

async function handleProfiles(req, res, auth, workspaceId, seg, query) {
  const id = seg[2] || null;

  if (!id) {
    if (req.method === 'GET') {
      const wantDeleted = query.get('deleted') === '1';
      const profiles = db.profiles.filter((p) =>
        db.owners.profiles[p.id] === workspaceId && (wantDeleted ? !!p.deletedAt : !p.deletedAt));
      return ok(res, { profiles });
    }
    if (req.method === 'POST') {
      const body = await readBody(req);
      if (!isPlainObject(body)) return fail(res, 400, 'profile must be a JSON object');
      if (body.id && db.profiles.some((p) => p.id === body.id)) {
        return fail(res, 409, 'a profile with this id already exists');
      }
      const profile = buildProfile(body);
      db.profiles.push(profile);
      db.owners.profiles[profile.id] = workspaceId;
      saveDb(db);
      return ok(res, { profile }, 201);
    }
    return fail(res, 404, 'not found');
  }

  const profile = db.profiles.find((p) => p.id === id);
  if (!profile || db.owners.profiles[id] !== workspaceId) {
    return fail(res, 404, 'profile not found');
  }

  const action = seg[3] || null;
  if (action === 'restore' && req.method === 'POST') {
    profile.deletedAt = null;
    profile.updatedAt = nowIso();
    saveDb(db);
    return ok(res, { profile });
  }
  if (action === 'clone' && req.method === 'POST') {
    const copy = deepClone(profile);
    copy.id = newId();
    copy.name = (profile.name || 'Profile') + ' copy';
    copy.status = 'ready';
    copy.deletedAt = null;
    copy.createdAt = nowIso();
    copy.updatedAt = nowIso();
    db.profiles.push(copy);
    db.owners.profiles[copy.id] = workspaceId;
    saveDb(db);
    return ok(res, { profile: copy }, 201);
  }
  if (action) return fail(res, 404, 'not found');

  if (req.method === 'GET') return ok(res, { profile });

  if (req.method === 'PATCH') {
    const body = await readBody(req);
    if (!isPlainObject(body)) return fail(res, 400, 'patch must be a JSON object');
    const patch = Object.assign({}, body);
    delete patch.id;
    delete patch.createdAt;
    delete patch.deletedAt; // trash state changes only via DELETE / restore
    const updated = deepMerge(profile, patch);
    updated.updatedAt = nowIso();
    db.profiles[db.profiles.indexOf(profile)] = updated;
    saveDb(db);
    return ok(res, { profile: updated });
  }

  if (req.method === 'DELETE') {
    profile.deletedAt = nowIso(); // soft-delete (Restore profiles parity)
    profile.updatedAt = nowIso();
    saveDb(db);
    return ok(res, { profile });
  }

  return fail(res, 404, 'not found');
}

async function handleFolders(req, res, auth, workspaceId, seg) {
  const id = seg[2] || null;
  if (!id) {
    if (req.method === 'GET') {
      const folders = db.folders.filter((f) => db.owners.folders[f.id] === workspaceId);
      return ok(res, { folders });
    }
    if (req.method === 'POST') {
      const body = await readBody(req);
      const name = typeof body.name === 'string' && body.name.trim() ? body.name.trim() : 'Folder';
      const folder = { id: newId(), name };
      db.folders.push(folder);
      db.owners.folders[folder.id] = workspaceId;
      saveDb(db);
      return ok(res, { folder }, 201);
    }
    return fail(res, 404, 'not found');
  }

  const folder = db.folders.find((f) => f.id === id);
  if (!folder || db.owners.folders[id] !== workspaceId) {
    return fail(res, 404, 'folder not found');
  }
  if (req.method === 'PATCH') {
    const body = await readBody(req);
    if (typeof body.name === 'string' && body.name.trim()) folder.name = body.name.trim();
    saveDb(db);
    return ok(res, { folder });
  }
  if (req.method === 'DELETE') {
    db.folders.splice(db.folders.indexOf(folder), 1);
    delete db.owners.folders[id];
    // Unlink the deleted folder from every profile in this workspace.
    for (const p of db.profiles) {
      if (db.owners.profiles[p.id] === workspaceId && Array.isArray(p.folderIds) && p.folderIds.includes(id)) {
        p.folderIds = p.folderIds.filter((f) => f !== id);
        p.updatedAt = nowIso();
      }
    }
    saveDb(db);
    return ok(res, { deleted: true });
  }
  return fail(res, 404, 'not found');
}

async function handleProxies(req, res, auth, workspaceId, seg) {
  const sub = seg[2] || null;

  if (sub === 'catalog' && req.method === 'GET') {
    const providers = publicProviders();
    return ok(res, {
      catalog: [
        { type: 'residential', label: 'Residential', unit: 'GB', pricePerGb: 3.0, currency: 'USD', countries: CATALOG_COUNTRIES, note: 'Real residential IPs — requires a configured residential provider (see providers).' },
        { type: 'mobile', label: 'Mobile', unit: 'GB', pricePerGb: 6.0, currency: 'USD', countries: CATALOG_COUNTRIES, note: 'Mobile-carrier IPs — requires a configured provider.' },
        { type: 'datacenter', label: 'Datacenter', unit: 'GB', pricePerGb: 1.0, currency: 'USD', countries: CATALOG_COUNTRIES, note: 'Datacenter IPs — requires a configured provider; free third-party datacenter proxies can also be added as shared proxies.' },
      ],
      providers,
      builtinAvailable: providers.some((p) => p.configured),
    });
  }

  if (sub === 'check' && req.method === 'POST') {
    const body = await readBody(req);
    const result = await checkProxy(isPlainObject(body) ? body : {});
    return send(res, 200, result);
  }

  if (sub === 'builtin' && req.method === 'POST') {
    const body = await readBody(req);
    const type = typeof body.type === 'string' && body.type ? body.type.toLowerCase() : 'residential';
    const country = typeof body.country === 'string' && body.country ? body.country.toUpperCase() : 'US';
    const provider = loadProviders().find((p) => p.configured && p.types.includes(type));
    if (!provider) {
      // Honest by design: without a plugged-in supplier there are no built-in
      // IPs to hand out — nobody can conjure a residential pool.
      return fail(res, 501, type === 'residential' ? 'no residential provider configured' : 'no ' + type + ' provider configured');
    }
    const session = crypto.randomBytes(4).toString('hex');
    const rendered = provider.endpointTemplate
      .replace(/\{country\}/gi, country.toLowerCase())
      .replace(/\{session\}/gi, session);
    let url;
    try { url = new URL(rendered); } catch (e) {
      return fail(res, 500, 'provider endpoint template is not a valid proxy URL');
    }
    const pType = (url.protocol || 'http:').replace(':', '').toLowerCase();
    const proxy = {
      mode: 'devxyz',
      type: ['http', 'https', 'socks4', 'socks5'].includes(pType) ? pType : 'http',
      host: url.hostname || '',
      port: parseInt(url.port, 10) || 0,
      username: decodeURIComponent(url.username || ''),
      password: decodeURIComponent(url.password || ''),
    };
    if (!proxy.host || !proxy.port) return fail(res, 500, 'provider endpoint template is missing host/port');
    const created = { id: newId(), name: type[0].toUpperCase() + type.slice(1) + ' • ' + country, proxy };
    db.proxies.push(created);
    db.owners.proxies[created.id] = workspaceId;
    saveDb(db);
    return ok(res, { proxy: created }, 201);
  }

  if (!sub) {
    if (req.method === 'GET') {
      const proxies = db.proxies.filter((p) => db.owners.proxies[p.id] === workspaceId);
      return ok(res, { proxies });
    }
    if (req.method === 'POST') {
      const body = await readBody(req);
      if (!isPlainObject(body)) return fail(res, 400, 'proxy must be a JSON object');
      if (body.id && db.proxies.some((p) => p.id === body.id)) {
        return fail(res, 409, 'a proxy with this id already exists');
      }
      const sp = buildSharedProxy(body);
      db.proxies.push(sp);
      db.owners.proxies[sp.id] = workspaceId;
      saveDb(db);
      return ok(res, { proxy: sp }, 201);
    }
    return fail(res, 404, 'not found');
  }

  const sp = db.proxies.find((p) => p.id === sub);
  if (!sp || db.owners.proxies[sub] !== workspaceId) {
    return fail(res, 404, 'proxy not found');
  }
  if (req.method === 'PATCH') {
    const body = await readBody(req);
    if (!isPlainObject(body)) return fail(res, 400, 'patch must be a JSON object');
    const patch = Object.assign({}, body);
    delete patch.id;
    const updated = deepMerge(sp, patch);
    db.proxies[db.proxies.indexOf(sp)] = updated;
    saveDb(db);
    return ok(res, { proxy: updated });
  }
  if (req.method === 'DELETE') {
    db.proxies.splice(db.proxies.indexOf(sp), 1);
    delete db.owners.proxies[sub];
    saveDb(db);
    return ok(res, { deleted: true });
  }
  return fail(res, 404, 'not found');
}

async function handleTokens(req, res, auth, workspaceId, seg) {
  const id = seg[2] || null;
  if (!id) {
    if (req.method === 'GET') {
      const tokens = db.apiTokens
        .filter((t) => t.userId === auth.user.id)
        .map((t) => ({ id: t.id, name: t.name, prefix: t.prefix, workspaceId: t.workspaceId, createdAt: t.createdAt, lastUsedAt: t.lastUsedAt }));
      return ok(res, { tokens });
    }
    if (req.method === 'POST') {
      const body = await readBody(req);
      const name = typeof body.name === 'string' && body.name.trim() ? body.name.trim() : 'API token';
      const raw = 'dlk_' + crypto.randomBytes(24).toString('hex');
      const rec = {
        id: newId(),
        name,
        userId: auth.user.id,
        workspaceId,
        hash: hashApiToken(raw),
        prefix: raw.slice(0, 12),
        createdAt: nowIso(),
        lastUsedAt: null,
      };
      db.apiTokens.push(rec);
      saveDb(db);
      // The raw token is shown exactly once, here — only its hash is stored.
      return ok(res, {
        token: raw,
        apiToken: { id: rec.id, name: rec.name, prefix: rec.prefix, workspaceId: rec.workspaceId, createdAt: rec.createdAt, lastUsedAt: null },
      }, 201);
    }
    return fail(res, 404, 'not found');
  }
  if (req.method === 'DELETE') {
    const rec = db.apiTokens.find((t) => t.id === id && t.userId === auth.user.id);
    if (!rec) return fail(res, 404, 'token not found');
    db.apiTokens.splice(db.apiTokens.indexOf(rec), 1);
    saveDb(db);
    return ok(res, { deleted: true });
  }
  return fail(res, 404, 'not found');
}

// ---------------------------------------------------------------------------
// main handler
async function handler(req, res) {
  try {
    if (req.method === 'OPTIONS') {
      res.writeHead(204, {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Workspace-Id',
        'Access-Control-Allow-Methods': 'GET, POST, PATCH, DELETE, OPTIONS',
      });
      return res.end();
    }

    const url = new URL(req.url, 'http://localhost');
    const seg = url.pathname.split('/').filter(Boolean).map(decodeURIComponent);

    if (req.method === 'GET' && (url.pathname === '/' || url.pathname === '/api/health')) {
      return ok(res, { service: 'devxyzlogin-server', version: VERSION });
    }

    if (seg[0] !== 'api') return fail(res, 404, 'not found');

    if (seg[1] === 'auth') return await handleAuth(req, res, seg);

    // everything below requires auth
    const auth = authenticate(req);
    if (!auth) return fail(res, 401, 'authentication required');
    const workspaceId = resolveWorkspace(req, res, auth);
    if (workspaceId === null) return; // error already sent
    if (!workspaceId) return fail(res, 409, 'no workspace for this account');

    switch (seg[1]) {
      case 'workspaces': return await handleWorkspaces(req, res, auth, workspaceId);
      case 'profiles': return await handleProfiles(req, res, auth, workspaceId, seg, url.searchParams);
      case 'folders': return await handleFolders(req, res, auth, workspaceId, seg);
      case 'proxies': return await handleProxies(req, res, auth, workspaceId, seg);
      case 'tokens': return await handleTokens(req, res, auth, workspaceId, seg);
      default: return fail(res, 404, 'not found');
    }
  } catch (e) {
    const status = e && e.status ? e.status : 500;
    if (status === 500) console.error('handler error:', e);
    return fail(res, status, status === 500 ? 'internal server error' : e.message);
  }
}

module.exports = handler;
module.exports.VERSION = VERSION;
module.exports.DATA_FILE = DATA_FILE;
