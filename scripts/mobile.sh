#!/data/data/com.termux/files/usr/bin/bash
# Actus Mobile — lance le briefing complet depuis Termux
set -euo pipefail
cd "$(dirname "$0")"

CREDS="$HOME/.actus/credentials.yaml"
if [[ ! -f "$CREDS" ]]; then
  echo "⚠ Première utilisation : configurez vos identifiants"
  mkdir -p "$HOME/.actus/cookies"
  cp config/credentials.example.yaml "$CREDS"
  chmod 600 "$CREDS"
  echo "   Éditez : nano $CREDS"
  echo "   Puis relancez : ./scripts/mobile.sh"
  exit 1
fi

if [[ ! -d .venv ]]; then
  python -m venv .venv
  .venv/bin/pip install -q -r requirements.txt
fi
source .venv/bin/activate

python mobile.py "$@"
