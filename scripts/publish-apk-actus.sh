#!/usr/bin/env bash
# Publie l'APK Actus Sync sur le dépôt PUBLIC lionelclercq/Actus (GitHub Releases).
# À lancer UNE FOIS avec votre token GitHub (scope repo).
#
# Usage :
#   GH_TOKEN=ghp_votre_token ./scripts/publish-apk-actus.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/releases/actus-sync-v1.1.0.apk"
ACTUS_REPO="lionelclercq/Actus"
TAG="actus-sync-v1.1.0"

if [[ ! -f "$APK" ]]; then
  echo "❌ APK introuvable : $APK"
  echo "   Compilez d'abord : cd android && ./gradlew :app:assembleDebug"
  exit 1
fi

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "❌ Variable GH_TOKEN manquante."
  echo "   Créez un token : https://github.com/settings/tokens (scope repo)"
  echo "   Puis : GH_TOKEN=ghp_... ./scripts/publish-apk-actus.sh"
  exit 1
fi

export GH_TOKEN

echo "→ Publication Release sur https://github.com/$ACTUS_REPO/releases …"
gh release view "$TAG" --repo "$ACTUS_REPO" >/dev/null 2>&1 && \
  gh release delete "$TAG" --repo "$ACTUS_REPO" --yes || true

gh release create "$TAG" "$APK" \
  --repo "$ACTUS_REPO" \
  --title "Actus Sync v1.1.0" \
  --notes "## Actus Sync — application Android

Connexion abonné **Le Monde** + **Charente Libre**, résumés **Gemini**, publication **wiki GitHub**.

### Installation (téléphone)

1. Téléchargez **actus-sync-v1.1.0.apk** ci-dessous
2. Ouvrez le fichier → Installez
3. Autorisez *sources inconnues* si demandé
4. Configurez Gemini + token GitHub dans l'app
5. Connectez vos abonnements
6. **Synchroniser maintenant**

Wiki : https://github.com/lionelclercq/Actus/wiki/Home"

echo ""
echo "✅ APK publié !"
echo ""
echo "📱 Lien de téléchargement (PUBLIC, sans login) :"
echo "   https://github.com/$ACTUS_REPO/releases/tag/$TAG"
echo ""
