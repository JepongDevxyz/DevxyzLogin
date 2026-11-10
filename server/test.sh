#!/usr/bin/env bash
# DevxyzLogin server — end-to-end test.
# Starts the server on a scratch port with a throwaway data file, runs the
# full flow with curl, and asserts every step. Also spins up the mini test
# proxies (test/miniproxy.js) so the proxy checker is verified with REAL
# connections (ok:true), not just failure shapes.
set -u
cd "$(dirname "$0")"

PORT="${TEST_PORT:-8899}"
BASE="http://127.0.0.1:$PORT"
export DATA_FILE="$(mktemp -u /tmp/devxyzlogin-test-XXXXXX.json)"
PASS=0; FAIL=0

say()  { printf '%s\n' "$*"; }
pass() { PASS=$((PASS+1)); say "PASS  $1"; }
fail() { FAIL=$((FAIL+1)); say "FAIL  $1"; }

# jget '<python expr on d>' — reads JSON from stdin, prints value or ERR
jget() { python3 -c "import sys,json
try:
    d=json.load(sys.stdin); print($1)
except Exception as e:
    print('ERR')"; }

# req METHOD PATH TOKEN BODY -> sets RESP (body) and CODE (http status)
req() {
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local args=(-s -o /tmp/dl-resp.json -w '%{http_code}' -X "$method" "$BASE$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  if [ -n "$body" ]; then args+=(-H 'Content-Type: application/json' -d "$body"); fi
  CODE="$(curl "${args[@]}")"
  RESP="$(cat /tmp/dl-resp.json)"
}

expect_code() { # label expected actual
  if [ "$2" = "$3" ]; then pass "$1 (HTTP $3)"; else fail "$1 — expected HTTP $2, got $3: $RESP"; fi
}
expect_eq() { # label expected actual
  if [ "$2" = "$3" ]; then pass "$1"; else fail "$1 — expected [$2], got [$3]"; fi
}
expect_contains() { # label needle haystack
  case "$3" in *"$2"*) pass "$1";; *) fail "$1 — [$2] not in response: $3";; esac
}

# ---------------------------------------------------------------- start server
PORT="$PORT" node server.js > /tmp/dl-server.log 2>&1 &
SRV=$!
node test/miniproxy.js http   18098 > /tmp/dl-proxy-http.log 2>&1 &
PHTTP=$!
node test/miniproxy.js socks5 18099 > /tmp/dl-proxy-socks.log 2>&1 &
PSOCKS=$!
cleanup() { kill $SRV $PHTTP $PSOCKS 2>/dev/null; rm -f "$DATA_FILE"; }
trap cleanup EXIT

for i in $(seq 1 50); do
  curl -s -o /dev/null "$BASE/api/health" && break
  sleep 0.2
done

say "== health =="
req GET /api/health
expect_code "health" 200 "$CODE"
expect_eq "health service name" "devxyzlogin-server" "$(printf '%s' "$RESP" | jget "d['service']")"
expect_eq "health ok flag" "True" "$(printf '%s' "$RESP" | jget "d['ok']")"

say "== auth =="
req POST /api/auth/register "" '{"email":"alice@example.com","password":"hunter2pass"}'
expect_code "register alice" 201 "$CODE"
TOKA="$(printf '%s' "$RESP" | jget "d['token']")"
WSA="$(printf '%s' "$RESP" | jget "d['workspace']['id']")"
case "$TOKA" in dl1.*) pass "register token has dl1. prefix";; *) fail "register token prefix: $TOKA";; esac
expect_eq "register returns default workspace" "My workspace" "$(printf '%s' "$RESP" | jget "d['workspace']['name']")"
expect_contains "password hash never returned" "alice@example.com" "$RESP"
case "$RESP" in *passwordHash*|*hunter2pass*) fail "register leaks password material";; *) pass "register leaks no password material";; esac

req POST /api/auth/register "" '{"email":"alice@example.com","password":"hunter2pass"}'
expect_code "duplicate register rejected" 409 "$CODE"

req POST /api/auth/login "" '{"email":"alice@example.com","password":"wrong-password"}'
expect_code "login with wrong password" 401 "$CODE"

req POST /api/auth/login "" '{"email":"alice@example.com","password":"hunter2pass"}'
expect_code "login alice" 200 "$CODE"
TOKA="$(printf '%s' "$RESP" | jget "d['token']")"

req GET /api/auth/me "$TOKA"
expect_code "me" 200 "$CODE"
expect_eq "me email" "alice@example.com" "$(printf '%s' "$RESP" | jget "d['user']['email']")"

req GET /api/profiles ""
expect_code "profiles without token" 401 "$CODE"
req GET /api/profiles "dl1.garbage.token"
expect_code "profiles with garbage token" 401 "$CODE"

say "== workspaces & folders =="
req GET /api/workspaces "$TOKA"
expect_eq "one default workspace" "1" "$(printf '%s' "$RESP" | jget "len(d['workspaces'])")"
req GET /api/folders "$TOKA"
expect_eq "one default folder" "Default" "$(printf '%s' "$RESP" | jget "d['folders'][0]['name']")"
req POST /api/workspaces "$TOKA" '{"name":"Team"}'
expect_code "create second workspace" 201 "$CODE"

req POST /api/folders "$TOKA" '{"name":"Social"}'
expect_code "create folder" 201 "$CODE"
FOLDER="$(printf '%s' "$RESP" | jget "d['folder']['id']")"
req PATCH "/api/folders/$FOLDER" "$TOKA" '{"name":"Socials"}'
expect_eq "folder renamed" "Socials" "$(printf '%s' "$RESP" | jget "d['folder']['name']")"

say "== profiles CRUD =="
PROFILE_JSON='{"name":"FB Ads US","os":"windows11","folderIds":["'$FOLDER'"],"tags":["ads"],"fingerprint":{"navigator":{"userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64)","cpuCores":12}},"proxy":{"mode":"custom","type":"http","host":"1.2.3.4","port":8080}}'
req POST /api/profiles "$TOKA" "$PROFILE_JSON"
expect_code "create profile (full schema)" 201 "$CODE"
P1="$(printf '%s' "$RESP" | jget "d['profile']['id']")"
expect_eq "profile keeps client os" "windows11" "$(printf '%s' "$RESP" | jget "d['profile']['os']")"
expect_eq "profile keeps client cpuCores" "12" "$(printf '%s' "$RESP" | jget "d['profile']['fingerprint']['navigator']['cpuCores']")"
expect_eq "profile defaults fill screenResolution" "1920x1080" "$(printf '%s' "$RESP" | jget "d['profile']['fingerprint']['navigator']['screenResolution']")"
expect_eq "profile deletedAt null" "None" "$(printf '%s' "$RESP" | jget "d['profile']['deletedAt']")"
expect_eq "profile keys are schema-only" "['bookmarks', 'createdAt', 'deletedAt', 'extensions', 'fingerprint', 'folderIds', 'id', 'name', 'notes', 'os', 'proxy', 'status', 'tags', 'updatedAt']" "$(printf '%s' "$RESP" | jget "sorted(d['profile'].keys())")"

req POST /api/profiles "$TOKA" '{"name":"Bare"}'
expect_code "create profile (name only)" 201 "$CODE"
expect_eq "bare profile gets default os" "linux" "$(printf '%s' "$RESP" | jget "d['profile']['os']")"

req GET /api/profiles "$TOKA"
expect_eq "list has 2 profiles" "2" "$(printf '%s' "$RESP" | jget "len(d['profiles'])")"

req GET "/api/profiles/$P1" "$TOKA"
expect_code "get profile" 200 "$CODE"

req PATCH "/api/profiles/$P1" "$TOKA" '{"name":"FB Ads US 2","fingerprint":{"navigator":{"cpuCores":16}}}'
expect_eq "patch renames" "FB Ads US 2" "$(printf '%s' "$RESP" | jget "d['profile']['name']")"
expect_eq "patch deep-merges fingerprint" "16" "$(printf '%s' "$RESP" | jget "d['profile']['fingerprint']['navigator']['cpuCores']")"
expect_eq "patch preserves other fingerprint fields" "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" "$(printf '%s' "$RESP" | jget "d['profile']['fingerprint']['navigator']['userAgent']")"

req POST "/api/profiles/$P1/clone" "$TOKA" '{}'
expect_code "clone profile" 201 "$CODE"
PCLONE="$(printf '%s' "$RESP" | jget "d['profile']['id']")"
expect_eq "clone name" "FB Ads US 2 copy" "$(printf '%s' "$RESP" | jget "d['profile']['name']")"
if [ "$PCLONE" != "$P1" ] && [ -n "$PCLONE" ]; then pass "clone got a new id"; else fail "clone id same as source"; fi

req DELETE "/api/profiles/$P1" "$TOKA"
expect_code "soft-delete profile" 200 "$CODE"
expect_contains "delete sets deletedAt" '"deletedAt":"20' "$RESP"
req GET /api/profiles "$TOKA"
expect_eq "list excludes deleted" "2" "$(printf '%s' "$RESP" | jget "len(d['profiles'])")" # Bare + clone
req GET "/api/profiles?deleted=1" "$TOKA"
expect_eq "deleted=1 shows trash only" "1" "$(printf '%s' "$RESP" | jget "len(d['profiles'])")"
req POST "/api/profiles/$P1/restore" "$TOKA" '{}'
expect_code "restore profile" 200 "$CODE"
expect_eq "restore clears deletedAt" "None" "$(printf '%s' "$RESP" | jget "d['profile']['deletedAt']")"
req GET /api/profiles "$TOKA"
expect_eq "list includes restored" "3" "$(printf '%s' "$RESP" | jget "len(d['profiles'])")"

say "== folder delete unlinks profiles =="
req PATCH "/api/profiles/$P1" "$TOKA" '{"folderIds":["'$FOLDER'"]}'
req DELETE "/api/folders/$FOLDER" "$TOKA"
expect_code "delete folder" 200 "$CODE"
req GET "/api/profiles/$P1" "$TOKA"
expect_eq "profile unlinked from deleted folder" "[]" "$(printf '%s' "$RESP" | jget "d['profile']['folderIds']")"

say "== shared proxies =="
req POST /api/proxies "$TOKA" '{"name":"Home resi","proxy":{"mode":"custom","type":"socks5","host":"127.0.0.1","port":18099}}'
expect_code "create shared proxy" 201 "$CODE"
PX="$(printf '%s' "$RESP" | jget "d['proxy']['id']")"
expect_eq "shared proxy shape keys" "['id', 'name', 'proxy']" "$(printf '%s' "$RESP" | jget "sorted(d['proxy'].keys())")"
req PATCH "/api/proxies/$PX" "$TOKA" '{"proxy":{"port":18100}}'
expect_eq "proxy patch merges port" "18100" "$(printf '%s' "$RESP" | jget "d['proxy']['proxy']['port']")"
expect_eq "proxy patch preserves host" "127.0.0.1" "$(printf '%s' "$RESP" | jget "d['proxy']['proxy']['host']")"
req DELETE "/api/proxies/$PX" "$TOKA"
expect_code "delete shared proxy" 200 "$CODE"
req GET /api/proxies "$TOKA"
expect_eq "proxy list empty after delete" "0" "$(printf '%s' "$RESP" | jget "len(d['proxies'])")"

say "== proxy catalog & builtin (honest no-provider) =="
req GET /api/proxies/catalog "$TOKA"
expect_code "catalog" 200 "$CODE"
expect_eq "catalog has residential first" "residential" "$(printf '%s' "$RESP" | jget "d['catalog'][0]['type']")"
expect_eq "catalog has 3 types" "3" "$(printf '%s' "$RESP" | jget "len(d['catalog'])")"
expect_eq "builtin unavailable without provider" "False" "$(printf '%s' "$RESP" | jget "d['builtinAvailable']")"
req POST /api/proxies/builtin "$TOKA" '{"country":"US","type":"residential"}'
expect_code "builtin without provider is 501" 501 "$CODE"
expect_eq "builtin error is honest" "no residential provider configured" "$(printf '%s' "$RESP" | jget "d['error']")"

say "== builtin WITH a configured provider (positive path) =="
RESIDENTIAL_PROVIDERS='[{"id":"test-resi","name":"Test Resi","types":["residential"],"endpointTemplate":"http://user-{country}-{session}:pw@gw.example.test:8080"}]' DATA_FILE="$(mktemp -u /tmp/devxyzlogin-test3-XXXXXX.json)" PORT=8896 node server.js > /tmp/dl-server3.log 2>&1 &
SRV3=$!
for i in $(seq 1 50); do curl -s -o /dev/null "http://127.0.0.1:8896/api/health" && break; sleep 0.2; done
TOK3="$(curl -s -X POST http://127.0.0.1:8896/api/auth/register -H 'Content-Type: application/json' -d '{"email":"prov@example.com","password":"provpass1"}' | jget "d['token']")"
RESP="$(curl -s http://127.0.0.1:8896/api/proxies/catalog -H "Authorization: Bearer $TOK3")"
expect_eq "catalog reports builtin available with provider" "True" "$(printf '%s' "$RESP" | jget "d['builtinAvailable']")"
case "$RESP" in *gw.example.test*) fail "catalog leaks provider endpoint template";; *) pass "catalog does not leak provider endpoint template";; esac
RESP="$(curl -s -X POST http://127.0.0.1:8896/api/proxies/builtin -H "Authorization: Bearer $TOK3" -H 'Content-Type: application/json' -d '{"country":"PH","type":"residential"}')"
expect_eq "builtin with provider returns a proxy" "gw.example.test" "$(printf '%s' "$RESP" | jget "d['proxy']['proxy']['host']")"
expect_eq "builtin proxy mode is devxyz" "devxyz" "$(printf '%s' "$RESP" | jget "d['proxy']['proxy']['mode']")"
expect_contains "builtin username got country substituted" '"username":"user-ph-' "$RESP"
RESP="$(curl -s http://127.0.0.1:8896/api/proxies -H "Authorization: Bearer $TOK3")"
expect_eq "builtin proxy landed in shared list" "1" "$(printf '%s' "$RESP" | jget "len(d['proxies'])")"
kill $SRV3 2>/dev/null

say "== proxy checker (real connections) =="
req POST /api/proxies/check "$TOKA" '{"type":"http","host":"127.0.0.1","port":18098}'
expect_code "check via local http proxy" 200 "$CODE"
expect_eq "http proxy check ok" "True" "$(printf '%s' "$RESP" | jget "d['ok']")"
IPHTTP="$(printf '%s' "$RESP" | jget "d['ip']")"
case "$IPHTTP" in *.*.*.*) pass "http proxy check returned a real exit IP ($IPHTTP)";; *) fail "http proxy check ip: $RESP";; esac
expect_contains "http proxy check has countryCode" '"countryCode":"' "$RESP"
expect_contains "http proxy check has timezone" '"timezone":"' "$RESP"
expect_contains "http proxy check has latencyMs" '"latencyMs":' "$RESP"

# NOTE on the SOCKS5 live check: this VM intercepts raw egress (its DNS maps
# public hosts to 198.18.x fake IPs — see AGENTS.md), so a raw tunneled
# request can never reach the real ip-api.com from here. The handshake +
# CONNECT tunnel are therefore verified end-to-end against a local ip-api
# stub on a second server instance (identical checker code path); the HTTP
# check above already proves the real-internet path with a real exit IP.
node test/miniproxy.js ipapi 18096 > /tmp/dl-ipapi.log 2>&1 &
PIPAPI=$!
DATA_FILE="$(mktemp -u /tmp/devxyzlogin-test2-XXXXXX.json)" PROXYCHECK_TARGET_HOST=127.0.0.1 PROXYCHECK_TARGET_PORT=18096 PORT=8897 node server.js > /tmp/dl-server2.log 2>&1 &
SRV2=$!
for i in $(seq 1 50); do curl -s -o /dev/null "http://127.0.0.1:8897/api/health" && break; sleep 0.2; done
TOK2="$(curl -s -X POST http://127.0.0.1:8897/api/auth/register -H 'Content-Type: application/json' -d '{"email":"socks@example.com","password":"sockspass1"}' | jget "d['token']")"
RESP="$(curl -s -X POST http://127.0.0.1:8897/api/proxies/check -H "Authorization: Bearer $TOK2" -H 'Content-Type: application/json' -d '{"type":"socks5","host":"127.0.0.1","port":18099}')"
expect_eq "socks5 proxy check ok (handshake + tunnel, stub lookup)" "True" "$(printf '%s' "$RESP" | jget "d['ok']")"
expect_eq "socks5 check parsed stub exit IP" "203.0.113.7" "$(printf '%s' "$RESP" | jget "d['ip']")"
expect_eq "socks5 check parsed stub country" "Testland" "$(printf '%s' "$RESP" | jget "d['country']")"
expect_contains "socks5 check has latencyMs" '"latencyMs":' "$RESP"
kill $SRV2 $PIPAPI 2>/dev/null

req POST /api/proxies/check "$TOKA" '{"type":"http","host":"127.0.0.1","port":18097}'
expect_eq "unreachable http proxy -> ok:false" "False" "$(printf '%s' "$RESP" | jget "d['ok']")"
expect_contains "unreachable proxy error is clean" 'connection refused' "$RESP"

req POST /api/proxies/check "$TOKA" '{"type":"socks5","host":"127.0.0.1","port":18097}'
expect_eq "unreachable socks5 -> ok:false" "False" "$(printf '%s' "$RESP" | jget "d['ok']")"

req POST /api/proxies/check "$TOKA" '{"type":"socks4","host":"127.0.0.1","port":18099}'
expect_eq "socks4 -> honest unsupported" "socks4 check unsupported in v1" "$(printf '%s' "$RESP" | jget "d['error']")"

req POST /api/proxies/check "$TOKA" '{"type":"http"}'
expect_eq "missing host/port -> ok:false" "False" "$(printf '%s' "$RESP" | jget "d['ok']")"

say "== API tokens =="
req POST /api/tokens "$TOKA" '{"name":"automation"}'
expect_code "create api token" 201 "$CODE"
APITOK="$(printf '%s' "$RESP" | jget "d['token']")"
APITOKID="$(printf '%s' "$RESP" | jget "d['apiToken']['id']")"
case "$APITOK" in dlk_*) pass "api token has dlk_ prefix";; *) fail "api token prefix: $APITOK";; esac
req GET /api/tokens "$TOKA"
expect_eq "token list shows metadata" "automation" "$(printf '%s' "$RESP" | jget "d['tokens'][0]['name']")"
case "$RESP" in *"$APITOK"*) fail "token list leaks the raw token";; *) pass "token list does not leak raw token";; esac
req GET /api/profiles "$APITOK"
expect_code "api token can list profiles" 200 "$CODE"
expect_eq "api token sees same workspace data" "3" "$(printf '%s' "$RESP" | jget "len(d['profiles'])")"
req DELETE "/api/tokens/$APITOKID" "$TOKA"
expect_code "revoke api token" 200 "$CODE"
req GET /api/profiles "$APITOK"
expect_code "revoked api token rejected" 401 "$CODE"

say "== cross-user isolation =="
req POST /api/auth/register "" '{"email":"bob@example.com","password":"bobpassword1"}'
expect_code "register bob" 201 "$CODE"
TOKB="$(printf '%s' "$RESP" | jget "d['token']")"
req GET /api/profiles "$TOKB"
expect_eq "bob sees zero of alice's profiles" "0" "$(printf '%s' "$RESP" | jget "len(d['profiles'])")"
req GET "/api/profiles/$P1" "$TOKB"
expect_code "bob cannot read alice's profile" 404 "$CODE"
req PATCH "/api/profiles/$P1" "$TOKB" '{"name":"hijacked"}'
expect_code "bob cannot patch alice's profile" 404 "$CODE"
req DELETE "/api/profiles/$P1" "$TOKB"
expect_code "bob cannot delete alice's profile" 404 "$CODE"
req GET /api/proxies "$TOKB"
expect_eq "bob sees zero of alice's proxies" "0" "$(printf '%s' "$RESP" | jget "len(d['proxies'])")"
req GET "/api/profiles/$P1" "$TOKA"
expect_eq "alice's profile untouched by bob" "FB Ads US 2" "$(printf '%s' "$RESP" | jget "d['profile']['name']")"

say "== misc =="
req GET /api/nope "$TOKA"
expect_code "unknown route" 404 "$CODE"
RESP="$(curl -s -X OPTIONS -o /dev/null -w '%{http_code}' "$BASE/api/profiles")"
expect_eq "CORS preflight" "204" "$RESP"

say ""
say "RESULT: $PASS passed, $FAIL failed"
[ "$FAIL" = "0" ]
