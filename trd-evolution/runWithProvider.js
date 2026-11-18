const { exec } = require('child_process');

const provider = process.env.DATABASE_PROVIDER || 'postgresql';
const command = process.argv[2];

if (!command) {
  console.error('No command provided');
  process.exit(1);
}

const replaced = command.replace(/DATABASE_PROVIDER/g, provider);

console.log(`Running: ${replaced}`);

exec(replaced, { cwd: process.cwd() }, (error, stdout, stderr) => {
  if (stdout) console.log(stdout);
  if (stderr) console.error(stderr);
  if (error) {
    console.error(`Error: ${error.message}`);
    process.exit(error.code || 1);
  }
});
