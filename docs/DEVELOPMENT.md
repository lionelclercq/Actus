# Développement

## Prérequis

- Python 3.11+
- `pip install -r requirements.txt`

## Structure du code

```
src/
  fetcher.py          # RSS → Article
  fetch_all.py        # CLI fetch
  enricher.py         # Cookies → texte intégral
  summarizer.py       # Résumés (Gemini ou RSS)
  gemini_briefing.py  # Briefing + classification thèmes
  wiki_builder.py     # Markdown wiki
  generator.py        # Orchestration cloud
  credentials.py      # ~/.actus/credentials.yaml
  sources_config.py   # config/sources.yaml
  config_loader.py    # config/feeds.yaml
```

## Commandes utiles

```bash
# Fetch seul
python -m src.fetch_all --config config/sources.yaml -o data/articles.json

# Enrichissement
python -m src.enricher data/articles.json -o data/enriched.json

# Génération locale
python generate.py --no-push

# Mobile complet
python mobile.py --no-push

# Lecteur
python serve.py
```

## Tests manuels

```bash
python -m src.fetch_all --config config/feeds.yaml -o /tmp/t.json
python -c "import json; print(len(json.load(open('/tmp/t.json'))))"
```

## Variables d’environnement

| Variable | Usage |
|----------|--------|
| `GEMINI_API_KEY` | Résumés IA |
| `GITHUB_TOKEN` | Push wiki |
| `ACTUS_CONFIG` | Chemin feeds.yaml |
| `ACTUS_SOURCES` | Chemin sources.yaml |

## Ajouter une source RSS

1. Éditer `config/sources.yaml` ou `config/feeds.yaml`
2. Tester : `python -m src.fetch_all --config ...`
3. Commit

## Modifier les thèmes (mobile)

Éditer la liste dans `src/gemini_briefing.py` → `classify_articles_into_themes` (prompt Gemini).

## Contribuer

1. Fork / branche
2. Changements + test local
3. Pull request sur `lionelclercq/Actus`
