# Actus

Briefing d'actualité — résumés IA, publiés automatiquement chaque matin.

## Lire sans identifiant

| Interface | URL |
|-----------|-----|
| **Lecteur web (recommandé)** | **https://lionelclercq.github.io/Actus/reader/** |
| Pages markdown par thème | https://github.com/lionelclercq/Actus/tree/main/wiki |
| Wiki GitHub | https://github.com/lionelclercq/Actus/wiki |

## Première utilisation du Wiki

Si le wiki est vide, créez **une fois** une page d'accueil :  
https://github.com/lionelclercq/Actus/wiki/_new → titre `Home` → Save.

Puis relancez [le workflow Actions](https://github.com/lionelclercq/Actus/actions/workflows/briefing.yml).

## Résumés IA (Gemini)

Secret requis dans **Settings → Secrets and variables → Actions** :

- Nom exact : `GEMINI_API_KEY`
- Valeur : clé https://aistudio.google.com/apikey

## Mise à jour manuelle

https://github.com/lionelclercq/Actus/actions/workflows/briefing.yml → **Run workflow**
