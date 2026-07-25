# Wiki GitHub — structure et publication

## URL

https://github.com/lionelclercq/Actus/wiki

## Pages générées

| Page | Fichier source | Contenu |
|------|----------------|---------|
| **Home** | `wiki/Home.md` | Portail : liens thèmes + dernier briefing |
| **Politique** | `wiki/Politique.md` | Articles classés politique |
| **Économie** | `wiki/Économie.md` | Idem |
| **Sport** | `wiki/Sport.md` | Idem |
| **Culture** | `wiki/Culture.md` | Idem |
| **Société** | `wiki/Société.md` | Idem |
| **Local** | `wiki/Local.md` | Charente Libre |
| **Briefing-YYYY-MM-DD** | `wiki/Briefing-*.md` | Synthèse du jour |

Les noms de fichiers sont **ASCII** (sans emoji) pour des liens wiki fiables.

## Format d’un article

```markdown
### Titre de l’article

![aperçu](https://...)

Résumé en 8–12 phrases…

**Source** : Le Monde · [Lire l’article](https://…)
```

## Publication automatique

### GitHub Actions (cloud)

Le workflow `.github/workflows/briefing.yml` :

1. Génère `wiki/` et `briefings/`
2. Commit sur `main`
3. Pousse vers le dépôt wiki (`*.wiki.git`)

### Mobile (`mobile.py`)

```bash
python mobile.py --push-wiki
```

Utilise `GITHUB_TOKEN` depuis `~/.actus/credentials.yaml`.

### Script dédié

```bash
export GITHUB_TOKEN=ghp_...
python scripts/sync_github_wiki.py
```

## Consulter le wiki

- Navigateur : https://github.com/lionelclercq/Actus/wiki/Home
- GitHub Mobile : onglet **Wiki** du dépôt
- Lecteur local : `python serve.py` (fichiers `wiki/` du clone)

## Historique

Chaque briefing daté reste accessible :

`https://github.com/lionelclercq/Actus/wiki/Briefing-2026-07-24`

## Dépannage liens Home

Si les liens thème ne fonctionnent pas :

1. Vérifier que les pages s’appellent `Politique.md` (pas `🏛️-Politique.md`)
2. Relancer `python mobile.py --push-wiki` ou le workflow Actions
3. Accès direct : `.../wiki/Politique`
