# Enrichissement (texte intégral)

Récupère le contenu complet des articles derrière abonnement ou paywall, **en local**, via vos cookies de session.

## Principe

1. Le fetch RSS fournit titre, lien, extrait court
2. L’enrichisseur charge les cookies du site
3. Il télécharge la page HTML et en extrait le texte principal
4. Gemini résume le texte enrichi (meilleure qualité)

## Configuration cookies

Répertoire : `~/.actus/cookies/`

| Fichier | Site |
|---------|------|
| `lemonde.txt` | lemonde.fr |
| `charentelibre.txt` | charentelibre.fr |

Format : Netscape cookies.txt (export navigateur).

## Utilisation

```bash
# Cloud / local
python generate.py --enrich

# Mobile
python mobile.py --enrich
```

Sans cookies, le pipeline utilise les extraits RSS uniquement.

## Limitations

- Cookies expirent → ré-exporter régulièrement
- Certains articles (vidéo, apps) peuvent échouer
- **Non exécuté sur GitHub Actions** (pas de cookies sur le runner)
- Respecter les CGU des éditeurs

## Dépannage

| Problème | Action |
|----------|--------|
| Texte vide | Vérifier connexion site + cookies |
| 403 | Session expirée, ré-exporter cookies |
| Article court | Paywall dur ou format non reconnu |

Code : `src/enricher.py`
