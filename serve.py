#!/usr/bin/env python3
"""Serveur web minimal : lecteur + fichiers briefing."""

from __future__ import annotations

import argparse
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent


class Handler(SimpleHTTPRequestHandler):
  def __init__(self, *args, directory: str | None = None, **kwargs):
    super().__init__(*args, directory=directory, **kwargs)

  def end_headers(self) -> None:
    self.send_header("Cache-Control", "no-cache")
    super().end_headers()


def main() -> None:
  parser = argparse.ArgumentParser(description="Serveur lecteur Actu Maison")
  parser.add_argument("-p", "--port", type=int, default=8080)
  args = parser.parse_args()

  handler = partial(Handler, directory=str(ROOT))
  server = ThreadingHTTPServer(("0.0.0.0", args.port), handler)

  print(f"📖 Lecteur : http://localhost:{args.port}/reader/")
  print(f"📄 Briefing : http://localhost:{args.port}/briefings/latest.md")
  print("   Ctrl+C pour arrêter")
  server.serve_forever()


if __name__ == "__main__":
  main()
