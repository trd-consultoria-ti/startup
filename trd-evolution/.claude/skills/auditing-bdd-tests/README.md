# auditing-bdd-tests-skill

A Claude Code skill for auditing BDD (Gherkin) + Playwright test solutions.

Scores 8 key aspects from 0-100 with ASCII progress bars, highlights problems by severity, and creates a phased improvement roadmap.

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                       BDD TEST SOLUTION REPORT                               ║
║                       Repository: my-tests                                   ║
╠══════════════════════════════════════════════════════════════════════════════╣
║  OVERALL GRADE: C     SCORE: 68/100     ↑+8 from last run                    ║
╠══════════════════════════════════════════════════════════════════════════════╣
║  1. Executable Gherkin      ███████░░░ 73/100 ↑+5                            ║
║  2. Step Definitions        █████░░░░░ 55/100 →0                             ║
║  3. Test Architecture       ██████░░░░ 64/100 ↑+3                            ║
║  4. Selector Strategy       ████████░░ 81/100 →0                             ║
║  5. Flake Resistance        █████░░░░░ 52/100 ↑+10                           ║
║  6. Data & Environment      ██████░░░░ 61/100 →0                             ║
║  7. CI & Artifacts          █████░░░░░ 48/100 ↑+2                            ║
║  8. AI-Agent Operability    ██████░░░░ 66/100 →0                             ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

## Features

- **8 Aspect Analysis** - Executable Gherkin, Step Definitions, Test Architecture, Selector Strategy, Flake Resistance, Data & Environment, CI & Artifacts, AI-Agent Operability
- **69 Sub-criteria** - Deep analysis with 8-12 checks per aspect
- **ASCII Dashboard** - Visual progress bars and overall A-F grade
- **Severity Classification** - Critical / Warning / Info issue grouping
- **Interactive Survey** - Choose which issues to address
- **Phased Roadmap** - Quick Wins → Foundation → Advanced
- **HTML Report Generation** - Standalone HTML report with charts
- **Progress Tracking** - Delta comparison between runs (↑+5, ↓-3)
- **Gherkin + Playwright Focus** - Specialized for BDD test solutions

## Installation

### Via skills.sh (recommended)

```bash
npx skills add viktor-silakov/bdd-best-practices
```

> [skills.sh](https://skills.sh) — universal skills manager for AI agents

### Via npx

```bash
npx auditing-bdd-tests-skill
```

### Via npm

```bash
npm install -g auditing-bdd-tests-skill
auditing-bdd-tests-skill install
```

## Usage

After installation, use in Claude Code:

```bash
# Analyze current directory
/auditing-bdd-tests

# Analyze specific repository
/auditing-bdd-tests /path/to/repo
```

## CLI Commands

```bash
npx auditing-bdd-tests-skill install   # Install skill (default)
npx auditing-bdd-tests-skill check     # Check if installed
npx auditing-bdd-tests-skill update    # Update to latest version
npx auditing-bdd-tests-skill remove    # Remove skill
npx auditing-bdd-tests-skill help      # Show help
```

## Workflow

1. **Discovery** - Detects runner/stack, configs, BDD assets, artifacts
2. **Context Questions** - Asks about success criteria, pain points, constraints
3. **Analysis** - Evaluates all 8 aspects with sub-criteria (0/5/10 scoring)
4. **Scoring** - Calculates weighted scores and overall A-F grade
5. **Dashboard** - Displays ASCII progress bars
6. **Problems** - Lists issues grouped by severity with evidence
7. **Prioritization** - Asks which issues to fix first
8. **Plan Mode** - Creates phased improvement roadmap
9. **Report** - Saves Markdown + JSON to `.bddready/`
10. **HTML Report** - Generates standalone HTML report

## Aspects & Weights

| Aspect | Weight | Key Checks |
|--------|--------|------------|
| Executable Gherkin | 16% | Scenario names, atomicity, explicit assertions, tags |
| Step Definitions | 14% | Thin steps, no branching, error clarity, naming |
| Test Architecture | 14% | Layering, page objects, fixtures, isolation |
| Selector Strategy | 12% | Test IDs, roles, forbidden patterns, consistency |
| Flake Resistance | 14% | No sleeps, condition waits, network sync, retries |
| Data & Environment | 10% | Factories, cleanup, secrets, env matrix |
| CI & Artifacts | 10% | Pipelines, sharding, traces, reporting |
| AI-Agent Operability | 10% | No ambiguity, deterministic runs, conventions doc |

## Grade Scale

| Grade | Score | Description |
|-------|-------|-------------|
| A | 90-100 | Excellent - Production-ready BDD |
| B | 75-89 | Good - Minor improvements needed |
| C | 60-74 | Moderate - Notable gaps |
| D | 45-59 | Poor - Significant work needed |
| F | 0-44 | Critical - Major overhaul required |

## Severity Levels

**Critical** (blocks reliable execution)
- Widespread `sleep`/`wait(…)` without conditions
- Unstable locators (generated classes, nth-child)
- Hidden asserts inside When/And steps
- Steps with device/state branching

**Warning** (hinders speed and maintainability)
- Long scenarios (>10-12 steps)
- Duplicate steps/locators
- Hardcoded test data
- Missing "how to write test" doc

**Info** (optimizations)
- No step catalog
- No explicit flaky policy
- Missing locator map

## Output Files

After running, the skill creates:

```
your-repo/
└── .bddready/
    └── history/
        ├── index.json
        └── reports/
            ├── {REPORT_ID}.md
            ├── {REPORT_ID}.html
            └── {REPORT_ID}.json
```

## What's Inside

- `skills/auditing-bdd-tests/SKILL.md` - Main analysis workflow and output format
- `skills/auditing-bdd-tests/criteria/aspects.md` - Scoring rubrics (0/5/10)
- `skills/auditing-bdd-tests/reference/severity.md` - Critical/Warning/Info classification
- `skills/auditing-bdd-tests/reference/bdd-best-practices.md` - Best practices guide
- `skills/auditing-bdd-tests/templates/report.html` - Standalone HTML template
- `skills/auditing-bdd-tests/examples/` - Example reports

## Requirements

- [Claude Code](https://claude.ai/claude-code) CLI
- Node.js >= 18.0.0

## License

MIT License - see [LICENSE](LICENSE)

## Links

- [GitHub Repository](https://github.com/viktor-silakov/bdd-best-practices)
- [Report Issues](https://github.com/viktor-silakov/bdd-best-practices/issues)
- [Claude Code Documentation](https://docs.anthropic.com/claude-code)
