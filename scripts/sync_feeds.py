#!/usr/bin/env python3
"""آرشیو زنده نوای آسمانی: خواندن فیدهای رسمی (RSS/Atom) و افزودن آثار تازه به catalog/catalog.json

- فقط منابعی که در catalog/sources.json با enabled=true ثبت شده‌اند خوانده می‌شوند.
- فایل صوتی دانلود یا بازنشر نمی‌شود؛ فقط لینک رسمی (enclosure) و صفحه اثر ثبت می‌شود.
- اگر فید فایل صوتی نداشته باشد (مثلاً یوتیوب/آپارات) اثر به صورت «پیوند به صفحه ناشر» اضافه می‌شود.
اجرا: python3 scripts/sync_feeds.py   (در GitHub Actions هر ۶ ساعت)
"""
from __future__ import annotations

import datetime as dt
import email.utils
import hashlib
import json
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CATALOG = ROOT / "catalog" / "catalog.json"
SOURCES = ROOT / "catalog" / "sources.json"
UA = "NavaAsemaniArchiveBot/1.0 (+https://github.com; metadata only, 6h interval)"
MAX_NEW_PER_SOURCE = 40
AUDIO_EXT = (".mp3", ".m4a", ".ogg", ".oga", ".opus", ".aac", ".wav")
NS = {
    "atom": "http://www.w3.org/2005/Atom",
    "itunes": "http://www.itunes.com/dtds/podcast-1.0.dtd",
    "media": "http://search.yahoo.com/mrss/",
}
OCCASIONS = ["فاطمیه", "محرم", "صفر", "اربعین", "عاشورا", "تاسوعا", "رمضان", "شب قدر", "غدیر", "مبعث",
             "نیمه شعبان", "امام زمان", "امام رضا", "ولادت", "شهادت", "میلاد"]


def guess_category(text: str, default: str) -> str:
    rules = [("سخنرانی", "سخنرانی"), ("منبر", "سخنرانی"), ("روضه", "روضه"), ("نوحه", "نوحه"), ("مناجات", "مناجات"),
             ("دعای", "ادعیه"), ("زیارت", "ادعیه"), ("تلاوت", "قرآن"), ("سوره", "قرآن")]
    for key, cat in rules:
        if key in text:
            return cat
    return default or "مداحی"


def guess_occasion(text: str) -> str:
    for o in OCCASIONS:
        if o in text:
            return "امام زمان" if o == "نیمه شعبان" else o
    return ""


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/rss+xml, application/atom+xml, application/xml, text/xml"})
    with urllib.request.urlopen(req, timeout=25) as r:
        data = r.read(6_000_001)
    if len(data) > 6_000_000:
        raise ValueError("feed too large")
    if b"<!ENTITY" in data.upper():
        raise ValueError("entity declarations are not allowed")
    return data


def iso_date(value: str) -> str:
    value = (value or "").strip()
    if not value:
        return dt.date.today().isoformat()
    try:
        return email.utils.parsedate_to_datetime(value).date().isoformat()
    except Exception:
        pass
    try:
        return dt.datetime.fromisoformat(value.replace("Z", "+00:00")).date().isoformat()
    except Exception:
        return dt.date.today().isoformat()


def fmt_duration(value: str) -> str:
    value = (value or "").strip()
    if value.isdigit():
        s = int(value)
        return f"{s // 3600}:{s % 3600 // 60:02d}:{s % 60:02d}" if s >= 3600 else f"{s // 60}:{s % 60:02d}"
    return value if ":" in value else ""


def entries(raw: bytes):
    root = ET.fromstring(raw)
    if root.tag.endswith("feed"):  # Atom (یوتیوب، بسیاری از سایت‌ها)
        for e in root.findall("atom:entry", NS):
            link = ""
            audio = ""
            for l in e.findall("atom:link", NS):
                href = l.attrib.get("href", "")
                rel = l.attrib.get("rel", "alternate")
                if rel == "enclosure" or l.attrib.get("type", "").startswith("audio/"):
                    audio = href
                elif rel == "alternate" and not link:
                    link = href
            thumb = e.find("media:group/media:thumbnail", NS)
            yield {
                "guid": e.findtext("atom:id", "", NS) or link,
                "title": (e.findtext("atom:title", "", NS) or "").strip(),
                "link": link, "audio": audio,
                "date": e.findtext("atom:published", "", NS) or e.findtext("atom:updated", "", NS),
                "duration": "", "image": thumb.attrib.get("url", "") if thumb is not None else "",
            }
    else:  # RSS 2.0 (پادکست‌ها، وردپرس، آپارات)
        for it in root.findall(".//channel/item"):
            enc = it.find("enclosure")
            audio = ""
            if enc is not None:
                url = enc.attrib.get("url", "")
                if enc.attrib.get("type", "").startswith("audio/") or url.lower().split("?")[0].endswith(AUDIO_EXT):
                    audio = url
            img = it.find("itunes:image", NS)
            yield {
                "guid": (it.findtext("guid") or it.findtext("link") or "").strip(),
                "title": (it.findtext("title") or "").strip(),
                "link": (it.findtext("link") or "").strip(), "audio": audio.strip(),
                "date": it.findtext("pubDate") or "",
                "duration": it.findtext("itunes:duration", "", NS),
                "image": img.attrib.get("href", "") if img is not None else "",
            }


def main() -> int:
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    sources = json.loads(SOURCES.read_text(encoding="utf-8")).get("sources", [])
    maddah_ids = {m["id"] for m in catalog.get("maddahs", [])}
    tracks = catalog.setdefault("tracks", [])
    known_ids = {t["id"] for t in tracks}
    known_urls = {t.get("sourceUrl") for t in tracks} | {t.get("streamUrl") for t in tracks}
    added = 0
    for src in sources:
        if not src.get("enabled"):
            continue
        mid = src.get("maddahId", "")
        url = src.get("url", "")
        if mid not in maddah_ids or not url.startswith("https://"):
            print(f"! منبع نامعتبر رد شد: {mid} {url}")
            continue
        try:
            raw = fetch(url)
            items = list(entries(raw))
        except Exception as exc:  # منبع در دسترس نبود؛ بقیه ادامه می‌یابند
            print(f"! خطا در خواندن {url}: {exc}")
            continue
        new_here = 0
        for e in items:
            if new_here >= MAX_NEW_PER_SOURCE:
                break
            title = e["title"]
            link = e["link"] if e["link"].startswith("https://") else ""
            audio = e["audio"] if e["audio"].startswith("https://") else ""
            if not title or not (link or audio):
                continue
            tid = "rss-" + hashlib.sha256((mid + "|" + (e["guid"] or link or audio)).encode()).hexdigest()[:12]
            if tid in known_ids or (link and link in known_urls) or (audio and audio in known_urls):
                continue
            text = title
            tracks.append({
                "id": tid, "maddahId": mid, "title": title[:160],
                "category": guess_category(text, src.get("category", "مداحی")),
                "occasion": guess_occasion(text), "year": "",
                "duration": fmt_duration(e["duration"]),
                "streamUrl": audio, "downloadUrl": "",
                "artwork": e["image"] if e["image"].startswith("https://") else "",
                "sourceUrl": link or audio, "sourceName": src.get("sourceName", ""),
                "license": src.get("license", "پیوند به منبع رسمی"),
                "downloadAllowed": bool(src.get("downloadAllowed")) and bool(audio),
                "tags": "", "featured": False, "popular": False,
                "addedAt": iso_date(e["date"]), "featuring": "",
            })
            known_ids.add(tid)
            new_here += 1
        added += new_here
        print(f"✓ {mid}: {new_here} اثر تازه از {len(items)} مورد فید")
        time.sleep(1.0)
    if added:
        catalog["updated"] = dt.date.today().isoformat()
        CATALOG.write_text(json.dumps(catalog, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"مجموع آثار تازه: {added}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
