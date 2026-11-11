# DevxyzLogin — Shared Profile Schema (Web Dashboard + Android)

Both tracks (web dashboard and the Android app in `android/`) serialize profiles
with **exactly** these field names. JSON is the interchange format
(`kotlinx.serialization` on Android, plain JSON objects on web).

## Profile object

| Field | Type | Notes |
|---|---|---|
| `id` | string | UUID, assigned at creation, never changes |
| `name` | string | Display name, editable inline on web / in editor on Android |
| `folderIds` | string[] | IDs of folders this profile belongs to |
| `tags` | string[] | Free-form colored tags |
| `status` | string | Run state: `"ready"` or `"running"` |
| `notes` | string | Free text notes |
| `os` | string | One of `windows10`, `windows11`, `macIntel`, `macArm`, `linux`, `android`. Locked after creation (GoLogin parity) |
| `fingerprint` | object | See below |
| `proxy` | object | See below |
| `extensions` | string[] | Extension names/identifiers attached to this profile |
| `bookmarks` | string[] | One bookmark per line: `folder::name::url`, `name::url`, or bare `url` |
| `createdAt` | string | ISO-8601 UTC timestamp |
| `updatedAt` | string | ISO-8601 UTC timestamp |
| `deletedAt` | string \| null | Soft-delete marker; non-null = in "Restore profiles" trash |

## `fingerprint` object

| Field | Type | Notes |
|---|---|---|
| `navigator` | object | `userAgent` (string), `browserVersion` (string), `screenResolution` (string, `"WxH"`), `languages` (string[]), `platform` (string), `cpuCores` (int, = hardwareConcurrency), `ram` (int GB, = deviceMemory), `doNotTrack` (bool) |
| `fontsMasking` | bool | Fonts masking on/off |
| `mediaDevices` | object | `videoInputs` (int), `audioInputs` (int), `audioOutputs` (int) |
| `hardware` | object | `canvasMode` (`"off"` \| `"block"` \| `"noise"`), `webglVendor` (string), `webglRenderer` (string), `webglNoise` (bool), `audioNoise` (bool), `clientRectsNoise` (bool) |
| `webrtcMode` | string | `"basedOnIp"` \| `"off"` \| `"disabled"` |
| `timezone` | object | `auto` (bool — fill from external IP), `zone` (string, IANA zone, used when `auto=false`) |
| `geolocation` | object | `mode` (`"prompt"` \| `"allow"` \| `"block"`), `auto` (bool — fill from external IP), `lat` (double), `lon` (double) |

## `proxy` object

| Field | Type | Notes |
|---|---|---|
| `mode` | string | `"none"` \| `"devxyz"` (DevxyzLogin-provided) \| `"custom"` |
| `type` | string | `"http"` \| `"https"` \| `"socks4"` \| `"socks5"` |
| `host` | string | |
| `port` | int | |
| `username` | string | |
| `password` | string | Stored locally only in v1 |
| `buyCountry` | string | **v3 additive** (web v3 name): built-in proxy country, ISO alpha-2, default `"FR"`; only for `mode="devxyz"` |
| `buyType` | string | **v3 additive**: `"Residential"` (default) \| `"Mobile"` \| `"Data Center"`; only for `mode="devxyz"` |
| `dedicated` | bool | **v3 additive**: "Buy dedicated IP" segment selected, default `false` |
| `changeIpUrl` | string | **v3 additive**: "Change IP URL for mobile proxy", default `""` |
| `dataGb` | number \| null | **v6 additive**: GB balance the assignment draws from, exactly as declared by the configured provider on the server (`gbAvailable` in providers.json / `RESIDENTIAL_PROVIDERS`). Set ONLY on server-assigned built-in proxies; `null` = no provider declares a balance (never estimated, never invented). Android sync uploads include it only when non-null. |

## v6 additive — built-in proxy catalog & assignment (server)

- `GET /api/proxies/catalog`: each entry gains `gbAvailable`
  (number | null) — the provider-declared GB balance for that type;
  `null` while no provider is configured (or none declares a balance).
- `POST /api/proxies/builtin` accepts an optional location
  **preference** `{ country, type, region, city }`. The preference is
  stored on the created assignment record (the shared-proxy object
  gains top-level `region` / `city` strings, `""` when not requested)
  and may feed the provider's endpoint template via `{region}` /
  `{city}` placeholders. It is **not** the measured exit location —
  the real exit is whatever `POST /api/proxies/check` measures.
  The response proxy payload carries `proxy.dataGb` (above).

Other v3 additive fields (all defaulted, so v1/v2 stores load unchanged): `navigator.languagesAuto` (bool, `true`), `fingerprint.mediaMasking` (bool, `true`), `browser.saveTabs` (bool, `true`), `browser.allowInstallExtensions` (bool, `true`), `SharedProxy.country` / `SharedProxy.flag` (strings, `""`).

## v4 additive fields — Android CLIENT-LOCAL only (never on the wire)

Android v2.2.0 syncs profiles with the live server
(`https://devxyzlogin-server.vercel.app`). Three fields exist only in the
local Android store and are **stripped from every sync upload**, so the
wire schema above is unchanged:

| Field | Type | Notes |
|---|---|---|
| `serverId` | string \| null | The profile's id on the server once uploaded; `null` = not uploaded yet |
| `dirty` | bool | Local edits not yet pushed; sync retries until the push succeeds |
| `lastProxyCheck` | object \| null | Last REAL server proxy check: `ip`, `country`, `countryCode`, `city`, `timezone`, `latencyMs`, `checkedAt` |

`Store` also gains `folderServerIds` (object, local folder id → server
folder id) so profile `folderIds` can be translated between the local and
server id spaces during sync. The session token (`dl1.…`) lives in
app-private SharedPreferences, never in `store.json`.

## Supporting objects (same store file on Android, `store.json`)

- `Folder`: `{ id, name }`
- `Workspace`: `{ id, name }`
- `SharedProxy`: `{ id, name, proxy }` where `proxy` is the proxy object above.
- `Store`: `{ profiles: Profile[], folders: Folder[], workspaces: Workspace[], proxies: SharedProxy[], members: string[] }`

## Android browser-launch honesty notes (WebView limits)

- **Fingerprint depth:** v1 spoofing is JS-injection level (navigator/screen
  overrides, canvas/WebGL/Audio noise, WebRTC stubbing), not engine-level C++
  patching like GoLogin's Orbita. Advanced checkers may detect inconsistencies.
- **WebRTC:** Android WebView exposes no API to disable WebRTC. The injection
  script stubs `RTCPeerConnection` when `webrtcMode` is `off`/`disabled`;
  this blocks JS-initiated peer connections but is not an engine guarantee.
- **Proxy:** applied with `androidx.webkit.ProxyController` (HTTP/HTTPS proxy
  rules). SOCKS proxies and proxy *authentication* are not supported by
  WebView's ProxyController — the app shows a fallback message in that case.
- **Per-profile isolation:** `WebView.setDataDirectorySuffix()` must run before
  any WebView exists **in that process**, and one process has exactly one data
  directory. The app therefore declares **3 browser process slots**
  (`:p1`, `:p2`, `:p3` in the manifest), each with its own data directory.
  A profile is permanently assigned the least-used slot on first launch
  (mapping kept in `slots.json`, outside this schema), so its cookies/storage
  persist across runs. **Limit: profiles that share a slot share WebView
  storage** — true unlimited per-profile isolation is not possible with
  WebView; it would need one process per profile or a GeckoView/Chromium base.
