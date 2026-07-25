# Dépannage

## GitHub Actions

### Le workflow ne se lance pas

- Vérifier **Actions** activées sur le dépôt
- Déclencher manuellement : Actions → Briefing quotidien → Run workflow

### `GEMINI_API_KEY` manquant

```
Secret GEMINI_API_KEY non configuré
```

→ Settings → Secrets → `GEMINI_API_KEY` (nom exact, majuscules)

### Push wiki échoue

```
fatal: remote error: ... is empty
```

→ Le wiki n’a jamais été initialisé. Lancer une fois :

```bash
python scripts/sync_github_wiki.py
```

avec `GITHUB_TOKEN` ayant droit `repo`.

### Push refusé (403)

Le compte qui pousse doit avoir les droits **write** sur `lionelclercq/Actus`. Le bot Cursor n’a pas toujours ces droits — pousser avec votre PAT.

---

## Termux / mobile

### `python` introuvable

```bash
pkg install python
```

### Pas de réseau

```bash
termux-wake-lock
./scripts/mobile.sh
```

### Gemini 429 / quota

Réduire la fréquence ou attendre. Sans clé, les extraits RSS sont utilisés.

### Cookies expirés

Ré-exporter depuis le navigateur (extension « Get cookies.txt »). Fichiers dans `~/.actus/cookies/`.

---

## Flux RSS

### Charente Libre : peu d’articles

Certains flux CL sont instables. Vérifier `config/sources.yaml` — plusieurs URLs sont listées.

### Doublons

Normal entre flux proches ; le dédoublonnage par URL limite les répétitions.

---

## Wiki

### Liens Home cassés

Utiliser des noms ASCII (`Politique.md`). Relancer génération + push wiki.

### Page 404

URL directe : `https://github.com/lionelclercq/Actus/wiki/Politique`

---

## Lecteur local (`serve.py`)

### Port 8765 occupé

```bash
python serve.py --port 9000
```

### Pas de contenu

Lancer d’abord `python mobile.py` ou `python generate.py`.
