# Documentation Actus — Index

Bienvenue dans la documentation d'**Actus**, votre briefing d'actualité personnel.

---

## Par où commencer ?

| Profil | Lire en premier |
|--------|-----------------|
| **Je veux juste lire** | [WIKI.md](WIKI.md) → https://github.com/lionelclercq/Actus/wiki |
| **Je configure pour la famille** | [INSTALLATION.md](INSTALLATION.md) → [CONFIGURATION.md](CONFIGURATION.md) |
| **Je synchronise depuis mon téléphone** | [ANDROID.md](ANDROID.md) → [../android/README.md](../android/README.md) |
| **Je développe / modifie le code** | [ARCHITECTURE.md](ARCHITECTURE.md) → [DEVELOPMENT.md](DEVELOPMENT.md) |

---

## Sommaire

### Vue d'ensemble

| Document | Contenu |
|----------|---------|
| [../README.md](../README.md) | Présentation, démarrage rapide, structure |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Schémas, modules, flux de données |
| [SECURITY.md](SECURITY.md) | Tokens, cookies, ce qu'il ne faut jamais committer |

### Installation et configuration

| Document | Contenu |
|----------|---------|
| [INSTALLATION.md](INSTALLATION.md) | Cloud, PC, Android, Docker |
| [CONFIGURATION.md](CONFIGURATION.md) | `feeds.yaml`, `sources.yaml`, secrets |
| [ANDROID.md](ANDROID.md) | APK Actus Sync, compilation, utilisation |

### Utilisation

| Document | Contenu |
|----------|---------|
| [WIKI.md](WIKI.md) | Lecture du wiki, structure des pages |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Problèmes fréquents et solutions |

### Développement

| Document | Contenu |
|----------|---------|
| [DEVELOPMENT.md](DEVELOPMENT.md) | Setup dev, tests, commandes, contribution |

---

## Commandes essentielles

```bash
# Cloud / local
python generate.py
python serve.py

# Compiler l'APK Android
cd android && ./gradlew :app:assembleDebug

# Sync wiki manuel (Python)
python scripts/sync_github_wiki.py --push --repo lionelclercq/Actus --token TOKEN
```

---

## Liens externes

- **Wiki live** : https://github.com/lionelclercq/Actus/wiki
- **Actions CI** : https://github.com/lionelclercq/Actus/actions
- **Gemini API** : https://aistudio.google.com/apikey
- **Repo** : https://github.com/lionelclercq/Actus
