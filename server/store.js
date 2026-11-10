'use strict';
/**
 * DevxyzLogin server — JSON file store.
 *
 * All state lives in ONE JSON file (same approach as the DevxyzText server):
 *   - local / self-host: ./data.json next to this file (override: DATA_FILE env)
 *   - Vercel: set DATA_FILE=/tmp/devxyzlogin-data.json (see README for the
 *     honest warm-instance / ephemeral-filesystem limits of that setup).
 *
 * Entity objects (profiles, folders, workspaces, shared proxies) are stored
 * EXACTLY in the shared-schema shape (see ../SHARED_SCHEMA.md). Ownership is
 * kept in a separate `owners` map so no server bookkeeping fields ever leak
 * into the serialized entities the web dashboard / Android app consume.
 */
const fs = require('fs');
const path = require('path');

const DATA_FILE = process.env.DATA_FILE
  ? path.resolve(process.env.DATA_FILE)
  : (process.env.VERCEL
      ? '/tmp/devxyzlogin-data.json'
      : path.join(__dirname, 'data.json'));

function emptyDb() {
  return {
    version: 1,
    meta: { tokenSecret: null },
    users: [],        // { id, email, passwordHash, createdAt }
    workspaces: [],   // { id, name }                      (schema shape)
    folders: [],      // { id, name }                      (schema shape)
    profiles: [],     // { ...shared-schema profile }      (schema shape)
    proxies: [],      // { id, name, proxy }               (schema shape)
    apiTokens: [],    // { id, name, userId, workspaceId, hash, prefix, createdAt, lastUsedAt }
    owners: { workspaces: {}, profiles: {}, folders: {}, proxies: {} }, // id -> workspaceId (users for workspaces)
  };
}

function loadDb() {
  if (fs.existsSync(DATA_FILE)) {
    try {
      const raw = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
      if (raw && typeof raw === 'object') {
        const db = Object.assign(emptyDb(), raw);
        db.meta = Object.assign({ tokenSecret: null }, raw.meta || {});
        db.owners = Object.assign(
          { workspaces: {}, profiles: {}, folders: {}, proxies: {} },
          raw.owners || {}
        );
        for (const k of ['users', 'workspaces', 'folders', 'profiles', 'proxies', 'apiTokens']) {
          if (!Array.isArray(db[k])) db[k] = [];
        }
        return db;
      }
    } catch (e) {
      console.error('Warning: data file unreadable, starting fresh:', e.message);
    }
  }
  return emptyDb();
}

function saveDb(db) {
  const dir = path.dirname(DATA_FILE);
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  const tmp = DATA_FILE + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(db, null, 2));
  fs.renameSync(tmp, DATA_FILE); // atomic replace on the same filesystem
}

module.exports = { DATA_FILE, emptyDb, loadDb, saveDb };
