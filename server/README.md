# DevxyzLogin Server

The working backend for **DevxyzLogin** (Jepong's GoLogin-style antidetect
browser): accounts, workspaces, cloud profile storage/sync for the web
dashboard + Android app, folders, a shared proxy list with a **real** proxy
checker, a GoLogin-shaped built-in proxy catalog, and programmatic API tokens
(GoLogin API & MCP parity).

**Zero external dependencies** — Node.js built-ins (`http`, `fs`, `crypto`)
only, same pattern as the DevxyzText server. All data shapes match
[`../SHARED_SCHEMA.md`](../SHARED_SCHEMA.md) exactly; no server bookkeeping
fields ever leak into the serialized profiles/folders/proxies.

## Run it

```bash
node server.js            # http://127.0.0.1:8787
PORT=9000 node server.js  # custom port
npm test                  # full end-to-end test (bash + curl, 95 assertions)
```

| Env var | Default | Purpose |
|---|---|---|
| `PORT` | `8787` | Local listen port (ignored on Vercel) |
| `DATA_FILE` | `./data.json` | The one JSON file holding all state |
| `TOKEN_SECRET` | *(generated, persisted in the data file)* | HMAC key for `dl1.` session tokens. Set it explicitly when running more than one instance so all of them can verify tokens |
| `RESIDENTIAL_PROVIDERS` | *(reads `providers.json`)* | JSON array of proxy provider plug-ins (see below) |
| `PROXYCHECK_TARGET_HOST` / `PROXYCHECK_TARGET_PORT` | `ip-api.com` / `80` | Test-only override of the checker's IP-lookup target |

The store starts **empty** — no seeded/fake users. Registering creates the
account, a default workspace (`My workspace`) and a default folder.

## Auth

- Passwords are hashed with **scrypt** (random salt) — never stored or
  returned in plaintext.
- `POST /api/auth/login` returns a signed session token: `dl1.<payload>.<HMAC>`,
  30-day expiry. Verification is stateless (HMAC), so any instance sharing
  `TOKEN_SECRET` can verify it.
- Programmatic access: `POST /api/tokens` mints an API token (`dlk_…`) that is
  **shown exactly once**; only its SHA-256 hash is stored. Use it the same way:
  `Authorization: Bearer dlk_…`. Revoke with `DELETE /api/tokens/:id`.
- Every data route requires auth, and a token only ever sees **its own
  workspace's** data (other users' entities answer `404`, not `403`, so
  existence isn't leaked). Switch workspaces with the `X-Workspace-Id` header.

## Endpoints

All responses are JSON. Success: `{ "ok": true, ... }`. Errors:
`{ "ok": false, "error": "..." }`. CORS is open (the web dashboard and the
Android app call this API from other origins).

| Method | Path | What it does |
|---|---|---|
| GET | `/api/health` | `{ ok, service: "devxyzlogin-server", version }` — no auth |
| POST | `/api/auth/register` | `{email, password}` → user + default workspace/folder + session token (201) |
| POST | `/api/auth/login` | `{email, password}` → session token |
| GET | `/api/auth/me` | Current user + active workspace |
| GET | `/api/workspaces` | My workspaces |
| POST | `/api/workspaces` | Create workspace `{name}` |
| GET | `/api/profiles` | Profiles in the active workspace (soft-deleted excluded; `?deleted=1` lists the trash instead) |
| POST | `/api/profiles` | Create from a full/partial schema profile; server assigns `id`/`createdAt`/`updatedAt` when absent and fills schema defaults |
| GET | `/api/profiles/:id` | One profile |
| PATCH | `/api/profiles/:id` | Deep-merge patch (fingerprint/proxy merge field-by-field; `id`/`createdAt`/`deletedAt` are protected) |
| DELETE | `/api/profiles/:id` | **Soft**-delete (sets `deletedAt`) |
| POST | `/api/profiles/:id/restore` | Undo a soft-delete |
| POST | `/api/profiles/:id/clone` | Copy with a new id, name + `" copy"` |
| GET | `/api/folders` | Folders in the active workspace |
| POST | `/api/folders` | Create folder `{name}` |
| PATCH | `/api/folders/:id` | Rename |
| DELETE | `/api/folders/:id` | Delete + unlink from all profiles |
| GET | `/api/proxies` | Shared proxy list (`{id, name, proxy}` objects) |
| POST | `/api/proxies` | Add a shared proxy |
| PATCH | `/api/proxies/:id` | Edit a shared proxy |
| DELETE | `/api/proxies/:id` | Remove a shared proxy |
| POST | `/api/proxies/check` | **Real** proxy check (below) |
| GET | `/api/proxies/catalog` | Built-in proxy catalog + configured providers |
| POST | `/api/proxies/builtin` | Get a built-in proxy `{country, type}` (501 when no provider is configured — see below) |
| GET | `/api/tokens` | My API tokens (metadata only, never the raw token) |
| POST | `/api/tokens` | Create an API token `{name}` — raw token shown once |
| DELETE | `/api/tokens/:id` | Revoke an API token |

### The proxy checker is real

`POST /api/proxies/check` with `{type, host, port, username, password}`
actually connects **through** the proxy and asks `ip-api.com` who it sees:

- `http` — absolute-URI request through the proxy
- `https` — HTTP `CONNECT` tunnel, then the lookup inside it
- `socks5` — a real RFC 1928/1929 handshake (no-auth or username/password),
  `CONNECT`, then the lookup inside the tunnel
- `socks4` — honestly returns `{ ok:false, error:"socks4 check unsupported in v1" }`

Success returns measured data — `{ ok:true, ip, country, countryCode, city,
timezone, latencyMs }` (10-second timeout). Failures return a clean
`{ ok:false, error }`. Nothing is ever faked.

## The residential-proxy supply (read this)

GoLogin's "built-in" proxies are a **resold residential pool** — real
residential IPs come from a supplier with real households' connections behind
it. Nobody can conjure that pool in software, and this server doesn't pretend
to:

- **"Your proxy" works end-to-end today.** Any residential/mobile/datacenter
  proxy she buys or already owns can be saved in the shared list, assigned to
  profiles, and verified with the real checker above.
- **The built-in catalog is shaped exactly like GoLogin's Buy-proxy**
  (Residential / Mobile / Datacenter, per-GB pricing placeholders, country
  list), and lights up the moment a provider is plugged in: add an entry to
  `providers.json` (or the `RESIDENTIAL_PROVIDERS` env var) with an
  `endpointTemplate` — a proxy URL with `{country}` and `{session}`
  placeholders (plus `{region}` / `{city}` for the client's location
  preference), e.g.
  `http://user-{country}-{session}:PASSWORD@gw.provider.example:8080`.
  From then on, `POST /api/proxies/builtin {country:"PH", type:"residential"}`
  mints a real endpoint from that provider into the caller's shared list.
  A provider entry may also declare `gbAvailable` — a number (GB, for
  every type it serves) or an object keyed by type — which the catalog
  reports per type and built-in assignments carry as `proxy.dataGb`.
  It is the operator-declared balance of the provider account; when no
  provider declares one, both stay `null` (never estimated).
  The builtin request may carry a `region` / `city` **preference**,
  stored on the assignment record; it is not the measured exit — the
  checker still measures the real one.
- **Until a provider is configured**, `builtin` honestly answers
  `501 { ok:false, error:"no residential provider configured" }` — no fake
  IPs, ever. (Provider templates are never echoed back by the catalog
  endpoint, since they can embed passwords.)

## Deploy on Vercel

`api/index.js` re-exports the same handler (Vercel maps it to the `/api`
function); there are no dependencies to install. Set on the project:

- `TOKEN_SECRET` — a long random string (so every function instance verifies
  session tokens identically)
- `DATA_FILE` — e.g. `/tmp/devxyzlogin-data.json`

**Warm-instance limit (honest):** all state lives in one JSON file. Locally
and on a single self-hosted instance that is exact and durable. On Vercel the
filesystem is **per-instance and ephemeral** — a cold instance starts from
whatever its own `/tmp` holds, so data can differ or vanish between
instances. That's the same known limit as the DevxyzText server's in-memory
queue; treat the Vercel JSON setup as a starter/personal deployment. A
multi-instance production deployment needs a real database behind the same
API — the route surface above is designed so only `store.js` would change.

## Files

| File | Role |
|---|---|
| `server.js` | Local/self-host entry (`node server.js`) |
| `app.js` | The whole API handler (auth, routes, catalog) |
| `store.js` | One-JSON-file store (atomic tmp+rename writes) |
| `proxycheck.js` | The real proxy checker (HTTP / CONNECT / SOCKS5) |
| `providers.json` | Built-in proxy provider plug-ins (empty template by default) |
| `api/index.js` | Vercel entry — re-exports `app.js` |
| `test.sh` | End-to-end curl test suite (95 assertions) |
| `test/miniproxy.js` | Test-only mini HTTP/SOCKS5 proxies + ip-api stub |

## Live deployment

Deployed on Vercel (project `devxyzlogin-server`, git-linked to the DevxyzLogin repo, root directory `server/`):

- Base URL: https://devxyzlogin-server.vercel.app
- Health: GET /api/health
- Env vars set on the project: TOKEN_SECRET, DATA_FILE=/tmp/devxyzlogin-data.json
