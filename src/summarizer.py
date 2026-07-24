"""Résumés IA optionnels (Gemini ou OpenAI)."""

from __future__ import annotations

import os
from typing import Protocol

import requests

from .fetcher import Article


class Summarizer(Protocol):
    def summarize(self, article: Article) -> str: ...


class PassthroughSummarizer:
    """Sans API : utilise l'extrait RSS."""

    def summarize(self, article: Article) -> str:
        if article.excerpt:
            return article.excerpt
        return "_Pas d'extrait disponible pour cet article._"


class GeminiSummarizer:
    def __init__(self, api_key: str, model: str, language: str = "fr") -> None:
        self.api_key = api_key
        self.model = model or "gemini-2.0-flash"
        self.language = language

    def summarize(self, article: Article) -> str:
        prompt = (
            f"Tu es un journaliste professionnel. Rédige un résumé complet de cet article "
            f"de presse en {self.language}.\n\n"
            "Consignes :\n"
            "- 8 à 12 phrases, style article de presse clair\n"
            "- Couvre les faits, le contexte, les enjeux et les acteurs mentionnés\n"
            "- Reste factuel, sans opinion ni formule d'accroche\n"
            "- Pas de titre, pas de listes à puces\n\n"
            f"Titre : {article.title}\n"
            f"Source : {article.feed_name}\n"
            f"Lien : {article.link}\n"
            f"Extrait / chapô : {article.excerpt or '(non fourni)'}"
        )
        url = (
            f"https://generativelanguage.googleapis.com/v1beta/models/"
            f"{self.model}:generateContent?key={self.api_key}"
        )
        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {"temperature": 0.3, "maxOutputTokens": 1024},
        }
        resp = requests.post(url, json=payload, timeout=60)
        resp.raise_for_status()
        data = resp.json()
        candidates = data.get("candidates", [])
        if not candidates:
            return PassthroughSummarizer().summarize(article)
        parts = candidates[0].get("content", {}).get("parts", [])
        text = parts[0].get("text", "").strip() if parts else ""
        return text or PassthroughSummarizer().summarize(article)


class OpenAISummarizer:
    def __init__(self, api_key: str, model: str, language: str = "fr") -> None:
        self.api_key = api_key
        self.model = model or "gpt-4o-mini"
        self.language = language

    def summarize(self, article: Article) -> str:
        prompt = (
            f"Rédige un résumé complet en {self.language} (8 à 12 phrases factuelles).\n\n"
            f"Titre : {article.title}\nSource : {article.feed_name}\n"
            f"Lien : {article.link}\nExtrait : {article.excerpt or '(non fourni)'}"
        )
        resp = requests.post(
            "https://api.openai.com/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": self.model,
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.3,
                "max_tokens": 1024,
            },
            timeout=60,
        )
        resp.raise_for_status()
        data = resp.json()
        text = data["choices"][0]["message"]["content"].strip()
        return text or PassthroughSummarizer().summarize(article)


def build_summarizer() -> Summarizer:
    provider = os.getenv("AI_PROVIDER", "none").lower()
    language = os.getenv("SUMMARY_LANGUAGE", "fr")

    if provider == "gemini":
        key = os.getenv("GEMINI_API_KEY", "")
        if key:
            return GeminiSummarizer(key, os.getenv("AI_MODEL", ""), language)

    if provider == "openai":
        key = os.getenv("OPENAI_API_KEY", "")
        if key:
            return OpenAISummarizer(key, os.getenv("AI_MODEL", ""), language)

    print("ℹ Pas de clé API — résumés = extraits RSS")
    return PassthroughSummarizer()
