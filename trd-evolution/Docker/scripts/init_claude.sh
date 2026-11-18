#!/bin/bash

CLAUDE_HOME="/home/claudeuser/.claude"

mkdir -p "$CLAUDE_HOME"

if [ ! -f "$CLAUDE_HOME/settings.json" ]; then
  cat > "$CLAUDE_HOME/settings.json" << 'EOF'
{
  "anthropic": {
    "baseUrl": "http://host.docker.internal:11434",
    "apiKey": "1a81279f0ff24b36baebdbc29ebdc66a",
    "defaultModel": "deepseek-v4-flash:0731-cloud",
    "haikuModel": "qwen2.5-coder:14b",
    "sonnetModel": "deepseek-v4-flash:0731-cloud",
    "opusModel": "deepseek-v4-flash:0731-cloud"
  },
  "instructions": "Responda sempre em Portugues do Brasil. Seja conciso e direto."
}
EOF
  echo "[Claude] settings.json restored"
fi

if [ ! -f "$CLAUDE_HOME/ollama-settings.json" ]; then
  cat > "$CLAUDE_HOME/ollama-settings.json" << 'EOF'
{
  "anthropic": {
    "baseUrl": "http://host.docker.internal:11434",
    "apiKey": "1a81279f0ff24b36baebdbc29ebdc66a",
    "defaultModel": "deepseek-v4-flash:0731-cloud",
    "haikuModel": "qwen2.5-coder:14b",
    "sonnetModel": "deepseek-v4-flash:0731-cloud",
    "opusModel": "deepseek-v4-flash:0731-cloud"
  }
}
EOF
  echo "[Claude] ollama-settings.json restored"
fi

chown -R 1001:1001 /home/claudeuser

echo "[Claude] Init complete"
