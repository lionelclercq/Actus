# Installation

## 1. Mode cloud (GitHub Actions)

Aucune installation locale. Configuration sur GitHub :

1. Fork ou clone https://github.com/lionelclercq/Actus
2. Activer le **Wiki** : Settings → Features → Wiki
3. Secret `GEMINI_API_KEY` : Settings → Secrets and variables → Actions
4. Le workflow `.github/workflows/briefing.yml` tourne automatiquement

Déclenchement manuel : Actions → *Mise à jour briefing + Wiki* → Run workflow.

---

## 2. PC / Mac / Linux

```bash
git clone https://github.com/lionelclercq/Actus.git
cd Actus
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
# Éditer .env : GEMINI_API_KEY=...
python generate.py
python serve.py
```

Ouvrir http://localhost:8080/reader/

---

## 3. Termux (Android) — mode mobile

```bash
pkg update && pkg upgrade -y
pkg install -y git python nano curl

cd ~
git clone https://github.com/lionelclercq/Actus.git
cd Actus
chmod +x scripts/mobile.sh
./scripts/mobile.sh
```

Le premier lancement crée `~/.actus/credentials.yaml`. Éditez-le :

```bash
nano ~/.actus/credentials.yaml
```

Relancez :

```bash
./scripts/mobile.sh
```

Voir [MOBILE.md](MOBILE.md) pour le détail.

---

## 4. Docker (optionnel)

```bash
docker compose up -d
```

Expose le lecteur sur le port 8080. La génération reste manuelle ou via cron dans le conteneur.

---

## 5. Première initialisation du Wiki

Si le push wiki échoue avec « Repository not found » :

1. Ouvrir https://github.com/lionelclercq/Actus/wiki/_new
2. Titre : `Home` → contenu : `Actus` → **Save**
3. Relancer le workflow ou `mobile.py`

---

## Vérification

```bash
python generate.py --no-ai    # test sans API
ls briefings/latest.md
ls wiki/
```
