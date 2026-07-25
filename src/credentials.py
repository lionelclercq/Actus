"""Chargement des identifiants locaux (~/.actus/credentials.yaml)."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import yaml

CREDENTIALS_PATH = Path.home() / ".actus" / "credentials.yaml"


@dataclass
class SiteCredentials:
    email: str = ""
    password: str = ""


@dataclass
class Credentials:
    github_token: str = ""
    github_repo: str = "lionelclercq/Actus"
    gemini_api_key: str = ""
    lemonde: SiteCredentials = field(default_factory=SiteCredentials)
    charente_libre: SiteCredentials = field(default_factory=SiteCredentials)


def load_credentials(path: Path | None = None) -> Credentials:
    path = path or CREDENTIALS_PATH
    if not path.exists():
        return Credentials()

    with path.open(encoding="utf-8") as f:
        raw = yaml.safe_load(f) or {}

    gh = raw.get("github", {})
    gem = raw.get("gemini", {})
    lm = raw.get("lemonde", {})
    cl = raw.get("charente_libre", {})

    return Credentials(
        github_token=gh.get("token", ""),
        github_repo=gh.get("repo", "lionelclercq/Actus"),
        gemini_api_key=gem.get("api_key", ""),
        lemonde=SiteCredentials(lm.get("email", ""), lm.get("password", "")),
        charente_libre=SiteCredentials(cl.get("email", ""), cl.get("password", "")),
    )
