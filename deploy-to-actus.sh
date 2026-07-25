#!/usr/bin/env bash
# Déploie Actus vers https://github.com/lionelclercq/Actus et publie l'APK.
# Usage : GH_TOKEN=ghp_votre_token ./deploy-to-actus.sh
set -euo pipefail

ACTUS_OWNER="${ACTUS_OWNER:-lionelclercq}"
ACTUS_NAME="${ACTUS_NAME:-Actus}"
TAG="actus-sync-v1.1.2"
ROOT="$(cd "$(dirname "$0")" && pwd)"
WORKDIR="${TMPDIR:-/tmp}/actus-deploy-$$"

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "❌ GH_TOKEN requis."
  echo "   GH_TOKEN=ghp_... ./deploy-to-actus.sh"
  exit 1
fi
export GH_TOKEN

ACTUS_REPO_URL="https://x-access-token:${GH_TOKEN}@github.com/${ACTUS_OWNER}/${ACTUS_NAME}.git"

echo "→ Clone Actus…"
git clone "$ACTUS_REPO_URL" "$WORKDIR"
cd "$WORKDIR"
git remote set-url origin "$ACTUS_REPO_URL"

echo "→ Copie du projet (sans _vendor, .venv, build)…"
shopt -s dotglob nullglob
for item in "$ROOT"/*; do
  base="$(basename "$item")"
  case "$base" in
    _vendor|.venv|actus-push.bundle) continue ;;
  esac
  cp -a "$item" .
done
find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
rm -rf android/app/build android/.gradle 2>/dev/null || true

echo "→ Commit…"
git add -A
git diff --staged --quiet || git commit -m "feat: Actus Sync APK v1.1.2 — signature correcte + sauvegarde config"

echo "→ Push main…"
git push origin main

APK="releases/actus-sync-v1.1.2.apk"
if [[ -f "$APK" ]]; then
  echo "→ Publication Release…"
  gh release view "$TAG" --repo "${ACTUS_OWNER}/${ACTUS_NAME}" >/dev/null 2>&1 && \
    gh release delete "$TAG" --repo "${ACTUS_OWNER}/${ACTUS_NAME}" --yes || true
  gh release create "$TAG" "$APK" \
    --repo "${ACTUS_OWNER}/${ACTUS_NAME}" \
    --title "Actus Sync v1.1.2" \
    --notes "Correctif mise à jour APK (signature) + export/import de la configuration. Crash sync Android 14+."
  echo ""
  echo "✅ Lien de téléchargement :"
  echo "   https://github.com/${ACTUS_OWNER}/${ACTUS_NAME}/releases/tag/$TAG"
fi

rm -rf "$WORKDIR"
