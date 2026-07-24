#!/usr/bin/env bash
# Génère le briefing puis lance le lecteur web.
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -d .venv ]]; then
  python3 -m venv .venv
  .venv/bin/pip install -q -r requirements.txt
fi

source .venv/bin/activate

if [[ -f .env ]]; then
  python generate.py "$@"
else
  echo "ℹ Pas de .env — génération sans IA (extraits RSS)"
  python generate.py --no-ai "$@"
fi

echo ""
echo "Démarrage du lecteur…"
exec python serve.py "$@"
