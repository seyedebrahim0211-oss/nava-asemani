#!/usr/bin/env python3
"""Metadata-only approved RSS/Atom discovery: NEVER fetch or mirror media files.

Environment: NAVA_FEEDS_JSON (array of {url,source,category}), NAVA_API_URL,
NAVA_ADMIN_TOKEN. To use, you must verify the feed's terms and obtain permission.
"""
from __future__ import annotations
import hashlib
import json
import os
import urllib.request
import xml.etree.ElementTree as ET
from urllib.parse import urlparse

CATEGORIES = {"مداحی", "روضه", "سخنرانی", "ادعیه", "مناجات", "قرآن"}


def get_json(url: str, token: str):
    request = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.load(response)


def approved_feed_items(raw: bytes):
    """Use RSS/Atom metadata only. No enclosure/media URL is copied."""
    if len(raw) > 1_000_000 or b"<!DOCTYPE" in raw.upper() or b"<!ENTITY" in raw.upper():
        raise ValueError("Feed too large or contains entity declarations")
    root = ET.fromstring(raw)
    if root.tag.endswith("feed"):  # Atom
        for entry in root.findall("{http://www.w3.org/2005/Atom}entry"):
            title = entry.findtext("{http://www.w3.org/2005/Atom}title", "").strip()
            link = next((e.attrib.get("href", "") for e in entry.findall("{http://www.w3.org/2005/Atom}link")
                         if e.attrib.get("rel", "alternate") == "alternate"), "")
            if title and link.startswith("https://"):
                yield title, link
    else:
        for entry in root.findall(".//channel/item"):
            title = entry.findtext("title", "").strip()
            link = entry.findtext("link", "").strip()
            if title and link.startswith("https://"):
                yield title, link


def main():
    feeds = json.loads(os.environ.get("NAVA_FEEDS_JSON", "[]"))
    api = os.environ.get("NAVA_API_URL", "").rstrip("/")
    token = os.environ.get("NAVA_ADMIN_TOKEN", "")
    if not feeds:
        print("No approved feeds configured; no discovery performed.")
        return
    if not api.startswith("https://") or len(token) < 32:
        raise SystemExit("Requires HTTPS API and a strong admin token")
    existing = {row["item"]["id"] for row in get_json(api + "/admin/items", token)}
    count = 0
    for feed in feeds:
        url = feed.get("url", "")
        cat = feed.get("category", "")
        if not url.startswith("https://") or cat not in CATEGORIES or not feed.get("source"):
            print("Skipping invalid feed config")
            continue
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "NavaAsemaniMetadataDiscoverer/0.1 (+contact-source-owner)"})
            with urllib.request.urlopen(request, timeout=10) as response:
                final = response.geturl()
                if not final.startswith("https://"):
                    raise ValueError("Insecure redirect")
                xml = response.read(1_000_001)
            for title, link in approved_feed_items(xml):
                identifier = "rss-" + hashlib.sha256(link.encode()).hexdigest()[:20]
                if identifier in existing:
                    continue
                item = {"id": identifier, "title": title[:250], "artist": feed["source"][:180],
                        "category": cat, "sourceName": feed["source"], "sourceUrl": link,
                        "description": "کشف شده از خوراک مجاز؛ نیازمند بررسی انسانی و اخذ مجوز صوت", "streamUrl": "",
                        "downloadAllowed": False}
                payload = json.dumps(item, ensure_ascii=False).encode()
                req = urllib.request.Request(api + "/admin/items/" + identifier, data=payload,
                    headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"}, method="PUT")
                with urllib.request.urlopen(req, timeout=10):
                    pass
                existing.add(identifier)
                count += 1
        except (OSError, ValueError, ET.ParseError) as exc:
            print(f"Feed failed: {feed.get('source', '?')} ({type(exc).__name__})")
    print(f"New draft items: {count}. Publishing requires human review and rights evidence.")


if __name__ == "__main__":
    main()
