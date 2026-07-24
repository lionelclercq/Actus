# Actus

Briefing d'actualité familial — flux RSS centralisés, **résumés IA complets**, publiés sur le **Wiki GitHub**.

## Lire vos actualités (sans identifiant)

Repo public → lecture libre, aucun compte requis.

| Quoi | URL |
|------|-----|
| **Wiki (lecture)** | **https://github.com/lionelclercq/Actus/wiki** |
| Page d'accueil wiki | https://github.com/lionelclercq/Actus/wiki/Home |
| Code source | https://github.com/lionelclercq/Actus |

Sur Android : ouvrez l'URL wiki dans Chrome, ou utilisez **GitJournal** en clonant `https://github.com/lionelclercq/Actus.wiki.git`.

## Mise à jour automatique

GitHub Actions génère le briefing **chaque matin à 7h** (Paris) et met à jour le wiki.

**Déclencher à la main** : [Actions → Mise à jour briefing + Wiki → Run workflow](https://github.com/lionelclercq/Actus/actions/workflows/briefing.yml)

## Configuration requise (une fois) — résumés IA

Pour des résumés complets (8–12 phrases par article), ajoutez votre clé Gemini :

1. https://github.com/lionelclercq/Actus/settings/secrets/actions
2. **New repository secret**
3. Nom : `GEMINI_API_KEY` — Valeur : votre clé API Google AI
4. Relancez le workflow Actions

Sans ce secret, seuls les extraits RSS (souvent tronqués) sont utilisés.

## Wiki

Le wiki est **déjà activé** sur ce repo. Rien à installer côté serveur.

Structure générée automatiquement :

- `Home` — sommaire avec liens vers les thèmes
- Une page par thème (Politique, Local, Sport…)
- `Briefing-AAAA-MM-JJ` — archive du jour

## Admin : modifier les flux

Éditez `config/feeds.yaml` puis relancez le workflow Actions.

## Déploiement initial (depuis SPIKE)

```bash
curl -sL https://raw.githubusercontent.com/lionelclercq/SPIKE/cursor/actus-deploy-974a/actu-maison/deploy-to-actus.sh | bash
```

## Développement local

```bash
git clone https://github.com/lionelclercq/Actus.git
cd Actus
cp .env.example .env   # GEMINI_API_KEY=...
pip install -r requirements.txt
python generate.py
python serve.py        # http://localhost:8080/reader/
```
