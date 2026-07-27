#!/usr/bin/env bash
# Déploie le code source Actus vers https://github.com/lionelclercq/Actus
# et publie l'APK comme GitHub Release (jamais dans git).
#
# Usage : GH_TOKEN=ghp_token_classique_scope_repo ./deploy-to-actus.sh [apk-path]
#
#   apk-path  (optionnel) chemin vers l'APK à publier en Release.
#             Par défaut : cherche releases/actus-sync-*.apk dans ce dossier.
set -euo pipefail

ACTUS_OWNER="${ACTUS_OWNER:-lionelclercq}"
ACTUS_NAME="${ACTUS_NAME:-Actus}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
WORKDIR="${TMPDIR:-/tmp}/actus-deploy-$$"

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "❌ GH_TOKEN requis (token classique avec scope 'repo')."
  echo "   GH_TOKEN=ghp_... ./deploy-to-actus.sh"
  exit 1
fi
export GH_TOKEN

ACTUS_REPO_URL="https://x-access-token:${GH_TOKEN}@github.com/${ACTUS_OWNER}/${ACTUS_NAME}.git"

# ── Trouver l'APK à publier ────────────────────────────────────────────────
if [[ -n "${1:-}" ]]; then
  APK_PATH="$1"
else
  # Prend le plus récent par nom (v1.1.X trié lexicographiquement)
  APK_PATH=$(ls -1 "$ROOT"/releases/actus-sync-*.apk 2>/dev/null | sort -V | tail -1 || true)
fi

if [[ -z "$APK_PATH" || ! -f "$APK_PATH" ]]; then
  echo "⚠️  Aucun APK trouvé — seul le code source sera poussé."
  APK_PATH=""
fi

# ── Déduire le tag depuis le nom de l'APK ─────────────────────────────────
if [[ -n "$APK_PATH" ]]; then
  APK_NAME="$(basename "$APK_PATH")"                 # actus-sync-v1.1.2.apk
  TAG="${APK_NAME%.apk}"                              # actus-sync-v1.1.2
  VERSION="${TAG#actus-sync-}"                        # v1.1.2
else
  TAG=""
  VERSION=""
fi

echo "→ Clone Actus…"
git clone "$ACTUS_REPO_URL" "$WORKDIR"
cd "$WORKDIR"
git remote set-url origin "$ACTUS_REPO_URL"

echo "→ Copie du code source (sans binaires, build, venv)…"
shopt -s dotglob nullglob
for item in "$ROOT"/*; do
  base="$(basename "$item")"
  case "$base" in
    _vendor|.venv|actus-push.bundle|releases) continue ;;
  esac
  cp -a "$item" .
done
find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
rm -rf android/app/build android/.gradle 2>/dev/null || true

# S'assurer que les APK ne sont jamais committés dans Actus
grep -qx "*.apk" .gitignore 2>/dev/null || echo "*.apk" >> .gitignore

echo "→ Commit code source…"
git add -A
if git diff --staged --quiet; then
  echo "  (aucune modification du code source)"
else
  MSG="chore: mise à jour code source Actus"
  [[ -n "$VERSION" ]] && MSG="feat: Actus Sync ${VERSION} — mise à jour code source"
  git commit -m "$MSG"
  echo "→ Push main…"
  git push origin main
fi

# ── Publication GitHub Release ─────────────────────────────────────────────
if [[ -n "$APK_PATH" ]]; then
  echo "→ Publication Release ${TAG}…"
  gh release view "$TAG" --repo "${ACTUS_OWNER}/${ACTUS_NAME}" >/dev/null 2>&1 && \
    gh release delete "$TAG" --repo "${ACTUS_OWNER}/${ACTUS_NAME}" --yes || true
  gh release create "$TAG" "$APK_PATH" \
    --repo "${ACTUS_OWNER}/${ACTUS_NAME}" \
    --title "Actus Sync ${VERSION}" \
    --notes "Application Android — Le Monde + Charente Libre + Gemini → Wiki GitHub."
  echo ""
  echo "✅ Lien de téléchargement :"
  echo "   https://github.com/${ACTUS_OWNER}/${ACTUS_NAME}/releases/tag/${TAG}"
fi

rm -rf "$WORKDIR"
