'use strict';
/**
 * DevxyzLogin server — Vercel entry point.
 *
 * Vercel maps this file to the /api function; it simply re-exports the same
 * zero-dependency handler used by the local server (../app.js).
 *
 * Before deploying, set on the Vercel project:
 *   TOKEN_SECRET  — so any function instance can verify dl1. session tokens
 *   DATA_FILE     — e.g. /tmp/devxyzlogin-data.json (see README: on Vercel
 *                   the filesystem is per-instance and ephemeral, so the JSON
 *                   store is a starter setup, not durable multi-instance
 *                   storage — same warm-instance limit as the DevxyzText
 *                   server).
 */
module.exports = require('../app');
