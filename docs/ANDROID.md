# Actus Sync — application Android

Application Android native qui remplace Termux. Elle se connecte à vos **abonnements** Le Monde et Charente Libre, récupère les articles, les résume via **Gemini**, et publie sur le **wiki GitHub**.

## Fonctionnalités

- Connexion abonné via **WebView** (Le Monde, Charente Libre)
- Récupération du **texte intégral** avec session abonné
- Fetch RSS de tous les flux configurés
- Résumés Gemini (8–12 phrases) + classification par rubrique
- Publication automatique sur GitHub Wiki
- Stockage chiffré des clés API et cookies

## Installation

```bash
git clone https://github.com/lionelclercq/Actus.git
cd Actus/android
./gradlew :app:assembleDebug
```

APK : `android/app/build/outputs/apk/debug/actus-sync-debug.apk`

## Première utilisation

### 1. Configuration API

| Champ | Où l’obtenir |
|-------|----------------|
| Clé Gemini | https://aistudio.google.com/apikey |
| Token GitHub | https://github.com/settings/tokens (scope **repo**) |
| Dépôt | `lionelclercq/Actus` |

### 2. Connexion abonné

1. **Se connecter — Le Monde** → login sur secure.lemonde.fr
2. Une fois connecté → **Enregistrer la session**
3. Idem pour **Charente Libre**

L’écran principal affiche le statut : Connecté / Non connecté.

### 3. Synchroniser

**Synchroniser maintenant** lance :

1. Fetch RSS (12 flux)
2. Enrichissement (texte intégral si abonné connecté)
3. Analyse Gemini (~5–15 min)
4. Push wiki

## Sans abonnement connecté

La sync fonctionne quand même avec les **extraits RSS** (qualité de résumé moindre).

## Dépannage

| Problème | Solution |
|----------|----------|
| 0 article enrichi | Reconnecter via WebView, enregistrer session |
| Cookies expirés | Refaire la connexion abonné |
| Wiki 404 | Créer page Home sur le wiki GitHub |
| Gemini erreur | Vérifier clé API et quota |

## Architecture

```
MainActivity
  ├── LoginWebViewActivity (cookies abonné)
  └── SyncWorker
        ├── RssFetcher
        ├── ArticleEnricher (cookies → texte intégral)
        ├── GeminiClient
        ├── WikiBuilder
        └── WikiPusher
```

Code : `android/app/src/main/kotlin/fr/actus/sync/`
