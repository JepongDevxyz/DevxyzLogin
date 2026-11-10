'use strict';
/**
 * DevxyzLogin server — local / self-host entry point.
 * Run:  node server.js
 * Env:  PORT (default 8787), DATA_FILE (default ./data.json),
 *       TOKEN_SECRET (optional; a random one is generated + persisted
 *       in the data file when unset), RESIDENTIAL_PROVIDERS (optional JSON).
 */
const http = require('http');
const handler = require('./app');

const PORT = parseInt(process.env.PORT || '8787', 10);

http.createServer(handler).listen(PORT, () => {
  console.log('DevxyzLogin server v' + handler.VERSION + ' listening on http://127.0.0.1:' + PORT);
  console.log('Data file: ' + handler.DATA_FILE);
});
