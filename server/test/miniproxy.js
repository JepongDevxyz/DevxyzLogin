'use strict';
/**
 * Test-only mini proxies (used by test.sh to prove the proxy checker makes
 * REAL connections — never shipped as part of the server runtime).
 *
 *   node test/miniproxy.js http   18098   -> tiny HTTP proxy
 *   node test/miniproxy.js socks5 18099   -> tiny SOCKS5 proxy (no-auth)
 *   node test/miniproxy.js ipapi  18096   -> local ip-api.com stub (fixed JSON)
 */
const http = require('http');
const net = require('net');

const mode = process.argv[2];
const port = parseInt(process.argv[3], 10);

if (mode === 'http') {
  const srv = http.createServer((req, res) => {
    let target;
    try { target = new URL(req.url); } catch (e) { res.writeHead(400); return res.end(); }
    const opts = {
      host: target.hostname,
      port: parseInt(target.port, 10) || 80,
      path: target.pathname + target.search,
      method: req.method,
      headers: { Host: target.host, Accept: req.headers.accept || '*/*', Connection: 'close' },
    };
    const up = http.request(opts, (pr) => {
      res.writeHead(pr.statusCode, pr.headers);
      pr.pipe(res);
    });
    up.on('error', () => { if (!res.headersSent) res.writeHead(502); res.end(); });
    up.end();
  });
  srv.listen(port, '127.0.0.1', () => console.log('mini http proxy on ' + port));
} else if (mode === 'socks5') {
  const srv = net.createServer((client) => {
    let buf = Buffer.alloc(0);
    let stage = 'greeting';
    let upstream = null;
    client.on('data', (d) => {
      if (upstream) { upstream.write(d); return; }
      buf = Buffer.concat([buf, d]);
      for (;;) {
        if (stage === 'greeting') {
          if (buf.length < 2) return;
          const n = buf[1];
          if (buf.length < 2 + n) return;
          buf = buf.subarray(2 + n);
          client.write(Buffer.from([0x05, 0x00])); // no-auth
          stage = 'request';
          continue;
        }
        if (stage === 'request') {
          if (buf.length < 4) return;
          const atyp = buf[3];
          let addr; let addrLen; let portOff;
          if (atyp === 0x01) { addrLen = 4; portOff = 4 + 4; addr = [...buf.subarray(4, 8)].join('.'); }
          else if (atyp === 0x03) {
            if (buf.length < 5) return;
            addrLen = buf[4]; portOff = 4 + 1 + addrLen;
            addr = buf.subarray(5, 5 + addrLen).toString('utf8');
          } else { client.destroy(); return; }
          if (buf.length < portOff + 2) return;
          const tport = buf.readUInt16BE(portOff);
          const rest = buf.subarray(portOff + 2);
          stage = 'connecting';
          upstream = net.connect(tport, addr, () => {
            client.write(Buffer.from([0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0]));
            if (rest.length) upstream.write(rest);
            upstream.on('data', (x) => client.write(x));
            upstream.on('close', () => client.end());
            upstream.on('error', () => client.destroy());
          });
          upstream.on('error', () => client.destroy());
          return;
        }
        return;
      }
    });
    client.on('error', () => { if (upstream) upstream.destroy(); });
  });
  srv.listen(port, '127.0.0.1', () => console.log('mini socks5 proxy on ' + port));
} else if (mode === 'ipapi') {
  // Deterministic stand-in for http://ip-api.com/json/ — used because some
  // test machines intercept raw egress, so a tunneled request can never reach
  // the real service. The SOCKS5/ CONNECT machinery being tested is identical.
  const srv = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'application/json', Connection: 'close' });
    res.end(JSON.stringify({
      status: 'success', country: 'Testland', countryCode: 'TS', city: 'Stub City',
      timezone: 'Etc/UTC', query: '203.0.113.7',
    }));
  });
  srv.listen(port, '127.0.0.1', () => console.log('ip-api stub on ' + port));
} else {
  console.error('usage: node test/miniproxy.js <http|socks5|ipapi> <port>');
  process.exit(1);
}
