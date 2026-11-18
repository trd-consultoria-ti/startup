#!/usr/bin/env node

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { cpSync, rmSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const SKILL_NAME = 'auditing-bdd-tests';

function printHelp() {
  console.log(`
auditing-bdd-tests-skill - Install BDD test-solution audit skill into a project

Usage:
  node bin/cli.js [command]

Commands:
  install        Install skill to .claude/skills/${SKILL_NAME}/ (default)
  check          Check if skill is already installed
  update         Overwrite existing skill files
  remove         Remove skill from project

Examples:
  node bin/cli.js
  node bin/cli.js install
  node bin/cli.js check
  node bin/cli.js update
  node bin/cli.js remove
`);
}

function getTargetDir() {
  return join(process.cwd(), '.claude', 'skills', SKILL_NAME);
}

function getSourceDir() {
  return join(__dirname, '..', 'skills', SKILL_NAME);
}

function checkInstalled() {
  const targetDir = getTargetDir();
  return existsSync(join(targetDir, 'SKILL.md'));
}

function updateIndex() {
  const indexPath = join(process.cwd(), '.claude', 'skills', 'index.md');
  const indexDir = dirname(indexPath);
  const entry = `- [${SKILL_NAME}](#${SKILL_NAME}) — [SKILL.md](./${SKILL_NAME}/SKILL.md)`;
  const section = `
## ${SKILL_NAME}

Analyzes BDD (Gherkin) + Playwright test solutions: aspect scoring (0/5/10), ASCII dashboard, issues by severity, Markdown + HTML report format.

Skill file: [./${SKILL_NAME}/SKILL.md](./${SKILL_NAME}/SKILL.md)
`;

  if (!existsSync(indexPath)) {
    mkdirSync(indexDir, { recursive: true });
    writeFileSync(
      indexPath,
      `# Skills Index

${entry}
${section.trimStart()}
`
    );
    return;
  }

  const content = readFileSync(indexPath, 'utf-8');
  if (content.includes(`./${SKILL_NAME}/SKILL.md`)) return;

  const listMatch = content.match(/^- \\[.+\\].+$/m);
  let newContent = content;
  if (listMatch) {
    const insertPos = content.indexOf(listMatch[0]) + listMatch[0].length;
    newContent = content.slice(0, insertPos) + `\n${entry}` + content.slice(insertPos);
  } else {
    newContent = content.trimEnd() + `\n\n${entry}\n`;
  }

  newContent = newContent.trimEnd() + `\n${section}\n`;
  writeFileSync(indexPath, newContent);
}

function install(force = false) {
  const sourceDir = getSourceDir();
  const targetDir = getTargetDir();

  if (!existsSync(sourceDir)) {
    console.error(`Source skill dir not found: ${sourceDir}`);
    process.exit(2);
  }

  if (checkInstalled() && !force) {
    console.log('Skill already installed. Use "update" to overwrite.');
    return;
  }

  mkdirSync(targetDir, { recursive: true });
  cpSync(sourceDir, targetDir, { recursive: true, force: true });
  updateIndex();

  console.log(`Installed ${SKILL_NAME} skill to ${join('.claude', 'skills', SKILL_NAME)}/`);
}

function remove() {
  const targetDir = getTargetDir();
  if (!existsSync(targetDir)) {
    console.log('Skill not installed.');
    return;
  }
  rmSync(targetDir, { recursive: true, force: true });
  console.log(`Removed ${SKILL_NAME} skill from ${join('.claude', 'skills', SKILL_NAME)}/`);
}

const args = process.argv.slice(2);
const command = args[0] || 'install';

switch (command) {
  case 'install':
    install(false);
    break;
  case 'check':
    process.exit(checkInstalled() ? 0 : 1);
  case 'update':
    install(true);
    break;
  case 'remove':
    remove();
    break;
  case '-h':
  case '--help':
  case 'help':
    printHelp();
    break;
  default:
    console.error(`Unknown command: ${command}`);
    printHelp();
    process.exit(1);
}
