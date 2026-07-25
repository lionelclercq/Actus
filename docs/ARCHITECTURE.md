# Architecture

## Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────┐
│                        SOURCES D'ACTUALITÉ                       │
│  Le Monde (RSS)  ·  Charente Libre (RSS)  ·  [futurs flux]      │
└────────────────────────────┬────────────────────────────────────┘
                             │
         ┌───────────────────┴───────────────────┐
         ▼                                       ▼
┌─────────────────┐                   ┌─────────────────┐
│  MODE CLOUD     │                   │  MODE MOBILE    │
│  GitHub Actions │                   │  Termux (phone) │
│  generate.py    │                   │  mobile.py      │
└────────┬────────┘                   └────────┬────────┘
         │                                     │
         │  feeds.yaml (thèmes fixes)          │  sources.yaml (tous flux)
         │  RSS seul                           │  + cookies abonné
         │                                     │  + Gemini chapitrage auto
         ▼                                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                     TRAITEMENT                                   │
│  fetcher.py  →  enricher.py (opt)  →  summarizer / gemini_briefing │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SORTIE                                       │
│  briefings/latest.md  ·  wiki/*.md  ·  GitHub Wiki (push)       │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     LECTURE                                      │
│  GitHub Wiki  ·  reader/ (PWA locale)  ·  GitJournal / Markor   │
└─────────────────────────────────────────────────────────────────┘
```

## Modules Python (`src/`)

| Module | Rôle |
|--------|------|
| `config_loader.py` | Charge `config/feeds.yaml` (mode cloud) |
| `sources_config.py` | Charge `config/sources.yaml` (mode mobile) |
| `credentials.py` | Charge `~/.actus/credentials.yaml` (local) |
| `fetcher.py` | Parse RSS → objets `Article` |
| `fetch_all.py` | Tous les flux sans filtre thématique |
| `enricher.py` | Texte intégral via cookies exportés |
| `summarizer.py` | Résumé Gemini par article (mode cloud) |
| `gemini_briefing.py` | Classification + résumé + chapitrage (mobile) |
| `generator.py` | Rendu Markdown briefing thématique |
| `wiki_builder.py` | Portail news + pages par rubrique |

## Points d'entrée

| Fichier | Usage |
|---------|-------|
| `generate.py` | Pipeline cloud / local classique |
| `mobile.py` | Pipeline smartphone complet |
| `serve.py` | Serveur web lecteur local |
| `scripts/sync_github_wiki.py` | Push vers dépôt git du wiki |
| `scripts/mobile.sh` | Wrapper Termux |

## Modèle de données `Article`

```python
Article(
    feed_id, feed_name, themes[],
    title, link, published,
    excerpt,           # chapô RSS
    image_url,         # image flux RSS
    full_text,         # texte intégral (enrichisseur)
)
```

## GitHub Wiki

Le wiki GitHub est un **dépôt git séparé** : `Actus.wiki.git`.

- Initialisation : créer une page `Home` manuellement une fois
- Push : `scripts/sync_github_wiki.py` ou `mobile.py`
- Lecture : publique, sans authentification

## Sécurité des données

| Donnée | Où | Jamais dans Git |
|--------|-----|-----------------|
| `GEMINI_API_KEY` | Secret GitHub ou `~/.actus/` | ✓ |
| Token GitHub | Secret ou credentials.yaml | ✓ |
| Cookies abonné | `~/.actus/cookies/` | ✓ |
| Mots de passe | `~/.actus/credentials.yaml` | ✓ |
