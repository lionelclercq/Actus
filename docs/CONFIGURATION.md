# Configuration

## Fichiers de configuration

| Fichier | Mode | Description |
|---------|------|-------------|
| `config/feeds.yaml` | Cloud | Flux RSS + thèmes fixes |
| `config/sources.yaml` | Mobile | Tous les flux, rubriques Gemini |
| `.env` | Local | `GEMINI_API_KEY`, `AI_PROVIDER` |
| `~/.actus/credentials.yaml` | Mobile | Token GitHub, Gemini, logins |
| `~/.actus/cookies/*.txt` | Enrichissement | Sessions abonné exportées |

---

## `config/feeds.yaml` (mode cloud)

```yaml
themes:
  - id: politique
    label: Politique
    icon: "🏛️"

feeds:
  - id: lemonde-politique
    name: Le Monde — Politique
    url: https://www.lemonde.fr/politique/rss_full.xml
    themes: [politique]

max_articles_per_feed: 5
max_age_hours: 48
```

**Le Monde** : ajouter `rss_full.xml` à l'URL d'une rubrique.  
Liste officielle : https://www.lemonde.fr/le-monde-et-vous/

---

## `config/sources.yaml` (mode mobile)

Liste élargie sans thèmes — Gemini classe automatiquement.

```yaml
max_articles_per_feed: 8
max_total_articles: 40

feeds:
  - id: lm-politique
    name: Le Monde — Politique
    url: https://www.lemonde.fr/politique/rss_full.xml

rubriques:
  - Politique
  - Économie
  - Sport
  - Local
  - Autres
```

---

## Secrets GitHub Actions

| Secret | Obligatoire | Description |
|--------|-------------|-------------|
| `GEMINI_API_KEY` | Recommandé | Clé Google AI Studio |
| `GH_PAT` | Optionnel | Token pour push wiki si `GITHUB_TOKEN` insuffisant |

Nom **exact** : `GEMINI_API_KEY` (majuscules).

---

## `~/.actus/credentials.yaml`

```yaml
github:
  token: ghp_xxxxxxxx
  repo: lionelclercq/Actus

gemini:
  api_key: AIzaSy...

lemonde:
  email: vous@example.com
  password: ""    # optionnel

charente_libre:
  email: ""
  password: ""
```

```bash
chmod 600 ~/.actus/credentials.yaml
```

Modèle : `config/credentials.example.yaml`

---

## Variables `.env`

```ini
GEMINI_API_KEY=
AI_PROVIDER=gemini
AI_MODEL=gemini-2.0-flash
SUMMARY_LANGUAGE=fr
```

---

## Cookies abonné

Répertoire : `~/.actus/cookies/`

| Fichier | Site |
|---------|------|
| `lemonde.fr.txt` | abonnement Le Monde |
| `charentelibre.fr.txt` | Charente Libre |

Format : Netscape (extension « Get cookies.txt LOCALLY »).

Voir [ENRICHISSEMENT.md](ENRICHISSEMENT.md).
