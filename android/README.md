# Actus Sync — application Android

APK de synchronisation avec **connexion abonné** Le Monde et Charente Libre, résumés **Gemini**, publication sur le **GitHub Wiki**.

## Installation

1. Compiler l’APK : `./gradlew :app:assembleDebug`
2. Installer `actus-sync-debug.apk` sur Android 8+
3. Configurer **Gemini** + **token GitHub** dans l’app
4. **Se connecter** à Le Monde et Charente Libre (WebView intégrée)
5. Appuyer sur **Synchroniser maintenant**

Wiki : https://github.com/lionelclercq/Actus/wiki/Home

## Connexion abonné

L’app ouvre une WebView sur le site de connexion de chaque journal. Après login :

1. Naviguez jusqu’à être connecté (page d’accueil ou mon compte)
2. Appuyez sur **Enregistrer la session**

Les cookies de session sont stockés de façon **chiffrée** sur le téléphone. Ils servent à récupérer le **texte intégral** des articles avant envoi à Gemini.

Sans connexion abonné, seuls les **extraits RSS** sont utilisés.

## Pipeline

```
RSS (Le Monde + Charente Libre)
  → Enrichissement (cookies abonné)
  → Gemini (résumés + rubriques)
  → Wiki (Home, rubriques, briefing)
  → Push GitHub Wiki
```

## Configuration requise

| Champ | Description |
|-------|-------------|
| Clé Gemini | https://aistudio.google.com/apikey |
| Token GitHub | Scope `repo` |
| Dépôt | `lionelclercq/Actus` |
| Abonnements | Le Monde +/ou Charente Libre |

## Compiler

```bash
cd android
./gradlew :app:assembleDebug
```

## Sécurité

- Identifiants et cookies **uniquement sur l’appareil** (EncryptedSharedPreferences)
- Ne jamais partager l’APK avec session pré-enregistrée
- Respecter les CGU des éditeurs (usage personnel)

Voir [docs/ANDROID.md](../docs/ANDROID.md).
