# Actus

**Briefing d'actualité personnel et familial** — agrégation RSS, résumés IA (Gemini), publication sur [GitHub Wiki](https://github.com/lionelclercq/Actus/wiki).

Sans publicité. Lecture libre sans compte. Trois modes : **cloud** (GitHub Actions), **Android** (APK Actus Sync), **local** (Python).

---

## Lire vos actualités

| Interface | URL | Compte requis |
|-----------|-----|---------------|
| **Wiki (recommandé)** | https://github.com/lionelclercq/Actus/wiki | Non |
| Dossier `wiki/` | https://github.com/lionelclercq/Actus/tree/main/wiki | Non |
| Lecteur web local | `http://localhost:8080/reader/` | Non |

---

## Démarrage rapide

### Mode cloud (GitHub Actions — automatique)

1. Secret `GEMINI_API_KEY` dans [Settings → Secrets](https://github.com/lionelclercq/Actus/settings/secrets/actions)
2. [Lancer le workflow](https://github.com/lionelclercq/Actus/actions/workflows/briefing.yml) ou attendre le cron (7h Paris)

### Mode Android (APK Actus Sync)

**Télécharger l’APK** (sans ordinateur) :

👉 **https://github.com/lionelclercq/SPIKE/releases/tag/actus-sync-v1.1.0**

Appuyez sur `actus-sync-v1.1.0.apk` dans la page Release. Voir [releases/README.md](releases/README.md).

1. Installer l’APK sur le téléphone
2. Renseigner clé **Gemini** + token **GitHub** dans l’app
3. Se connecter à **Le Monde** et **Charente Libre** (abonnements)
4. Appuyer sur **Synchroniser maintenant**

→ Guide complet : [docs/ANDROID.md](docs/ANDROID.md)

---

## Documentation

**Index complet : [docs/INDEX.md](docs/INDEX.md)**

| Document | Description |
|----------|-------------|
| [INDEX.md](docs/INDEX.md) | Sommaire de la documentation |
| [ANDROID.md](docs/ANDROID.md) | Application Android (APK sync → wiki) |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture et flux de données |
| [INSTALLATION.md](docs/INSTALLATION.md) | Installation (cloud, PC, Android) |
| [CONFIGURATION.md](docs/CONFIGURATION.md) | Fichiers de config et secrets |
| [WIKI.md](docs/WIKI.md) | Structure et lecture du wiki |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | Développement et contribution |
| [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Dépannage |
| [SECURITY.md](docs/SECURITY.md) | Sécurité et bonnes pratiques |

---

## Structure du projet

```
Actus/
├── android/               # APK Actus Sync (Kotlin)
├── generate.py            # Génération briefing (cloud / local)
├── config/
│   ├── feeds.yaml         # Flux RSS mode cloud (thèmes fixes)
│   └── sources.yaml       # Tous les flux (Le Monde + CL)
├── src/                   # Modules Python
├── scripts/
│   └── sync_github_wiki.py
├── reader/                # Lecteur web local
├── briefings/             # Markdown générés
├── wiki/                  # Export wiki (miroir)
├── docs/                  # Documentation
└── .github/workflows/     # CI GitHub Actions
```

---

## Modes comparés

| | Cloud (Actions) | Android (APK) | Local (Python) |
|--|-----------------|-----------------|----------------|
| **Où** | GitHub | Téléphone | PC |
| **Identifiants** | Secrets GitHub | App (chiffré) | `~/.actus/credentials.yaml` |
| **Sources** | `feeds.yaml` | Le Monde + CL | `sources.yaml` |
| **Thèmes** | Fixes | Auto (Gemini) | Auto (Gemini) |
| **Automatisation** | Cron quotidien | Manuel (sync) | Script |

---

## Prérequis

- Clé API [Google Gemini](https://aistudio.google.com/apikey)
- Token GitHub (`repo`) pour le push wiki (Android ou mobile)
- Python 3.10+ (modes cloud/local uniquement)

---

## Licence

Usage personnel et familial.
