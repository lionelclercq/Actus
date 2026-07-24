#!/usr/bin/env bash
# Déploie Actu Maison vers https://github.com/lionelclercq/Actus
# À lancer UNE FOIS depuis votre PC (avec droits push sur Actus).
set -euo pipefail

ACTUS_REPO="${1:-https://github.com/lionelclercq/Actus.git}"
SPIKE_BRANCH="cursor/actus-deploy-974a"
SPIKE_TARBALL="https://github.com/lionelclercq/SPIKE/archive/refs/heads/${SPIKE_BRANCH}.tar.gz"
WORKDIR="${TMPDIR:-/tmp}/actus-deploy-$$"

echo "→ Téléchargement du code depuis SPIKE (${SPIKE_BRANCH})…"
mkdir -p "$WORKDIR/src" "$WORKDIR/dst"
curl -fsSL "$SPIKE_TARBALL" | tar xz -C "$WORKDIR/src" --strip-components=2 "SPIKE-${SPIKE_BRANCH}/actu-maison"

echo "→ Clone Actus…"
git clone "$ACTUS_REPO" "$WORKDIR/dst"
cd "$WORKDIR/dst"

shopt -s dotglob
cp -a "$WORKDIR/src"/* .

git add -A
if git diff --staged --quiet; then
  echo "Rien à committer (déjà à jour ?)"
else
  git commit -m "feat: briefing RSS + wiki + résumés IA Gemini"
  git push origin main
  echo "✅ Poussé sur $ACTUS_REPO"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  PROCHAINES ÉTAPES (2 min)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "1. Clé Gemini (résumés complets 8-12 phrases) :"
echo "   https://github.com/lionelclercq/Actus/settings/secrets/actions"
echo "   → New secret : GEMINI_API_KEY = votre clé Google AI"
echo ""
echo "2. Lancer la génération :"
echo "   https://github.com/lionelclercq/Actus/actions/workflows/briefing.yml"
echo "   → Run workflow"
echo ""
echo "3. Lire vos actus (SANS compte, SANS identifiant) :"
echo "   https://github.com/lionelclercq/Actus/wiki"
echo ""
rm -rf "$WORKDIR"
