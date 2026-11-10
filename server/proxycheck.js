'use strict';
/**
 * DevxyzLogin server — REAL proxy checker.
 *
 * Actually connects through the given proxy and asks ip-api.com who we are,
 * so the returned exit IP / country / city / timezone are measured, never
 * invented:
 *   - type "http"   : plain HTTP proxying (absolute-URI GET through the proxy)
 *   - type "https"  : HTTP CONNECT tunnel to ip-api.com:80, then a plain GET
 *   - type "socks5" : minimal RFC 1928/1929 SOCKS5 handshake (no-auth or
 *                     username/password), CONNECT, then a plain GET
 *   - type "socks4" : honestly reported as unsupported in v1
 *
 * Resolves to { ok:true, ip, country, countryCode, city, timezone, latencyMs }
 * or { ok:false, error } — failures are never dressed up as successes.
 */
const net = require('net');

// Lookup target. Overridable via env ONLY so tests can point the checker at a
// local ip-api stub on machines whose raw egress is intercepted (the checker
// logic is identical either way — it still connects through the proxy).
const TARGET_HOST = process.env.PROXYCHECK_TARGET_HOST || 'ip-api.com';
const TARGET_PORT = parseInt(process.env.PROXYCHECK_TARGET_PORT || '80', 10);

// ---------------------------------------------------------------------------
// Small buffered-socket helper so the multi-step handshakes stay readable.
class Conn {
  constructor(sock) {
    this.sock = sock;
    this.buf = Buffer.alloc(0);
    this.err = null;
    this.ended = false;
    this.waiters = [];
    sock.on('data', (d) => { this.buf = Buffer.concat([this.buf, d]); this._pump(); });
    sock.on('error', (e) => { this.err = e; this._pump(); });
    sock.on('close', () => { this.ended = true; this._pump(); });
  }
  _pump() {
    const w = this.waiters;
    this.waiters = [];
    for (const fn of w) fn();
  }
  _wait() { return new Promise((r) => this.waiters.push(r)); }
  async read(n) {
    while (this.buf.length < n) {
      if (this.err) throw this.err;
      if (this.ended) throw new Error('connection closed by proxy');
      await this._wait();
    }
    const out = this.buf.subarray(0, n);
    this.buf = this.buf.subarray(n);
    return out;
  }
  async readUntil(delim) {
    const d = Buffer.from(delim, 'latin1');
    for (;;) {
      const idx = this.buf.indexOf(d);
      if (idx >= 0) {
        const out = this.buf.subarray(0, idx + d.length);
        this.buf = this.buf.subarray(idx + d.length);
        return out;
      }
      if (this.err) throw this.err;
      if (this.ended) throw new Error('connection closed by proxy');
      await this._wait();
    }
  }
  async readToEnd() {
    while (!this.ended && !this.err) await this._wait();
    if (this.err && this.buf.length === 0) throw this.err;
    const out = this.buf;
    this.buf = Buffer.alloc(0);
    return out;
  }
  write(data) { this.sock.write(data); }
}

function tcpConnect(host, port) {
  return new Promise((resolve, reject) => {
    const sock = net.connect(port, host);
    sock.once('connect', () => resolve(sock));
    sock.once('error', reject);
  });
}

function decodeChunked(buf) {
  const parts = [];
  let off = 0;
  while (off < buf.length) {
    const lineEnd = buf.indexOf('\r\n', off);
    if (lineEnd < 0) break;
    const size = parseInt(buf.subarray(off, lineEnd).toString('latin1').split(';')[0], 16);
    if (Number.isNaN(size) || size === 0) break;
    parts.push(buf.subarray(lineEnd + 2, lineEnd + 2 + size));
    off = lineEnd + 2 + size + 2;
  }
  return Buffer.concat(parts);
}

// Reads one HTTP/1.x response from the connection; returns { status, body }.
async function readHttpResponse(conn) {
  const headBuf = await conn.readUntil('\r\n\r\n');
  const headText = headBuf.toString('latin1');
  const firstLine = headText.split('\r\n')[0] || '';
  const m = firstLine.match(/^HTTP\/\d(?:\.\d)?\s+(\d{3})/);
  const status = m ? parseInt(m[1], 10) : 0;
  const rest = await conn.readToEnd();
  const body = /transfer-encoding\s*:[^\r\n]*chunked/i.test(headText)
    ? decodeChunked(rest)
    : rest;
  return { status, body: body.toString('utf8') };
}

function proxyAuthHeader(input) {
  if (!input.username) return '';
  const cred = Buffer.from(String(input.username) + ':' + String(input.password || ''), 'utf8').toString('base64');
  return 'Proxy-Authorization: Basic ' + cred + '\r\n';
}

function cleanErr(e) {
  if (!e) return 'unknown error';
  if (e.code === 'ECONNREFUSED') return 'connection refused by proxy host';
  if (e.code === 'ENOTFOUND' || e.code === 'EAI_AGAIN') return 'proxy host not found (DNS)';
  if (e.code === 'ETIMEDOUT') return 'connection to proxy timed out';
  return String(e.message || e);
}

// ---------------------------------------------------------------------------
async function checkProxy(input, timeoutMs = 10000) {
  const started = Date.now();
  const type = String((input && input.type) || 'http').toLowerCase();
  const host = input && input.host ? String(input.host).trim() : '';
  const port = parseInt(input && input.port, 10);

  if (!host || !port || port < 1 || port > 65535) {
    return { ok: false, error: 'host and a valid port are required' };
  }
  if (!['http', 'https', 'socks5', 'socks4'].includes(type)) {
    return { ok: false, error: 'unsupported proxy type: ' + type };
  }
  if (type === 'socks4') {
    return { ok: false, error: 'socks4 check unsupported in v1' };
  }

  let sock = null;
  const run = async () => {
    sock = await tcpConnect(host, port);
    const conn = new Conn(sock);
    const getReqTunneled =
      'GET /json/ HTTP/1.1\r\n' +
      'Host: ' + TARGET_HOST + '\r\n' +
      'Accept: application/json\r\n' +
      'Connection: close\r\n\r\n';

    let resp;
    if (type === 'http') {
      const req =
        'GET http://' + TARGET_HOST + '/json/ HTTP/1.1\r\n' +
        'Host: ' + TARGET_HOST + '\r\n' +
        'Accept: application/json\r\n' +
        'Connection: close\r\n' +
        proxyAuthHeader(input) + '\r\n';
      conn.write(req);
      resp = await readHttpResponse(conn);
      if (resp.status === 407) return { ok: false, error: 'proxy requires authentication (407)' };
      if (resp.status !== 200) return { ok: false, error: 'proxy returned HTTP ' + resp.status };
    } else if (type === 'https') {
      conn.write(
        'CONNECT ' + TARGET_HOST + ':' + TARGET_PORT + ' HTTP/1.1\r\n' +
        'Host: ' + TARGET_HOST + ':' + TARGET_PORT + '\r\n' +
        proxyAuthHeader(input) + '\r\n'
      );
      const headBuf = await conn.readUntil('\r\n\r\n');
      const firstLine = headBuf.toString('latin1').split('\r\n')[0] || '';
      const m = firstLine.match(/^HTTP\/\d(?:\.\d)?\s+(\d{3})/);
      const status = m ? parseInt(m[1], 10) : 0;
      if (status === 407) return { ok: false, error: 'proxy requires authentication (407)' };
      if (status !== 200) return { ok: false, error: 'CONNECT failed: proxy returned HTTP ' + status };
      conn.write(getReqTunneled);
      resp = await readHttpResponse(conn);
      if (resp.status !== 200) return { ok: false, error: 'tunneled request returned HTTP ' + resp.status };
    } else {
      // socks5
      const methods = input.username ? [0x00, 0x02] : [0x00];
      conn.write(Buffer.from([0x05, methods.length, ...methods]));
      const sel = await conn.read(2);
      if (sel[0] !== 0x05) return { ok: false, error: 'proxy did not answer as SOCKS5' };
      if (sel[1] === 0x02) {
        const u = Buffer.from(String(input.username), 'utf8');
        const p = Buffer.from(String(input.password || ''), 'utf8');
        if (u.length > 255 || p.length > 255) return { ok: false, error: 'proxy credentials too long' };
        conn.write(Buffer.concat([Buffer.from([0x01, u.length]), u, Buffer.from([p.length]), p]));
        const authResp = await conn.read(2);
        if (authResp[1] !== 0x00) return { ok: false, error: 'proxy rejected username/password' };
      } else if (sel[1] === 0xff) {
        return { ok: false, error: 'proxy rejected all authentication methods' };
      } else if (sel[1] !== 0x00) {
        return { ok: false, error: 'proxy chose an unsupported authentication method' };
      }
      const hostBuf = Buffer.from(TARGET_HOST, 'utf8');
      conn.write(Buffer.concat([
        Buffer.from([0x05, 0x01, 0x00, 0x03, hostBuf.length]),
        hostBuf,
        Buffer.from([(TARGET_PORT >> 8) & 0xff, TARGET_PORT & 0xff]),
      ]));
      const rep = await conn.read(4);
      if (rep[0] !== 0x05) return { ok: false, error: 'bad SOCKS5 reply' };
      if (rep[1] !== 0x00) return { ok: false, error: 'SOCKS5 connect failed (reply code ' + rep[1] + ')' };
      const atyp = rep[3];
      if (atyp === 0x01) await conn.read(4 + 2);
      else if (atyp === 0x03) { const l = await conn.read(1); await conn.read(l[0] + 2); }
      else if (atyp === 0x04) await conn.read(16 + 2);
      else return { ok: false, error: 'SOCKS5 reply had a bad address type' };
      conn.write(getReqTunneled);
      resp = await readHttpResponse(conn);
      if (resp.status !== 200) return { ok: false, error: 'tunneled request returned HTTP ' + resp.status };
    }

    let j;
    try { j = JSON.parse(resp.body); } catch (e) {
      return { ok: false, error: 'proxy returned a non-JSON response' };
    }
    if (!j || j.status === 'fail' || !j.query) {
      return { ok: false, error: 'IP lookup failed' + (j && j.message ? ': ' + j.message : '') };
    }
    return {
      ok: true,
      ip: j.query,
      country: j.country || '',
      countryCode: j.countryCode || '',
      city: j.city || '',
      timezone: j.timezone || '',
      latencyMs: Date.now() - started,
    };
  };

  const work = run();
  work.catch(() => {}); // the race loser must never surface as unhandled
  try {
    return await Promise.race([
      work,
      new Promise((_, reject) =>
        setTimeout(() => reject(new Error('proxy check timed out after ' + Math.round(timeoutMs / 1000) + 's')), timeoutMs)
      ),
    ]);
  } catch (e) {
    return { ok: false, error: cleanErr(e) };
  } finally {
    if (sock) sock.destroy();
  }
}

module.exports = { checkProxy };
