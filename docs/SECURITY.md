# Sécurité

## Secrets

| Secret | Où le stocker | Ne jamais |
|--------|---------------|-----------|
| `GEMINI_API_KEY` | GitHub Secrets, `~/.actus/credentials.yaml` | Commiter, partager en chat |
| `GITHUB_TOKEN` | Idem | Pousser dans le dépôt |
| Cookies sites | `~/.actus/cookies/*.txt` | Commiter |
| Mots de passe presse | Optionnel dans credentials (local) | GitHub public |

Le fichier `config/credentials.example.yaml` est un modèle sans valeurs réelles.

## Dépôt public

- Le code et la config **feeds/sources** sont publics
- Les **briefings générés** sur le wiki sont publics
- Aucun identifiant d’abonnement ne doit apparaître dans l’historique git

## Token GitHub

Créer un PAT **fine-grained** ou classique avec scope minimal :

- `repo` (pour push wiki + code)

Révoquer tout token exposé accidentellement.

## Cookies

Les cookies donnent accès à votre session presse. Traiter comme un mot de passe :

- Permissions fichier `chmod 600`
- Ne pas synchroniser via cloud non chiffré sans précaution

## GitHub Actions

Les secrets ne sont pas loggés dans les sorties de workflow. Éviter `echo $GEMINI_API_KEY` dans les scripts.

## Abonnements payants

L’enrichissement local utilise **votre** session. Respecter les CGU des éditeurs. Pas de contournement automatisé de paywall sur l’infrastructure GitHub.
