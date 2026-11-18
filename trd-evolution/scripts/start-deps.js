#!/usr/bin/env node
const { spawn } = require('child_process');
const path = require('path');

console.log('[Evolution] Starting dependencies...');

const dockerCompose = spawn('docker', [
  'compose',
  '-f',
  path.join(__dirname, '..', 'docker-compose.deps.yaml'),
  'up',
  '-d',
  'redis',
  'evolution-postgres',
  'whisper'
], {
  stdio: 'inherit'
});

dockerCompose.on('close', (code) => {
  if (code === 0) {
    console.log('[Evolution] Dependencies started successfully');
    process.exit(0);
  } else {
    console.error('[Evolution] Failed to start dependencies');
    process.exit(code);
  }
});
