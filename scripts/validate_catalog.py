#!/usr/bin/env python3
"""بررسی سلامت catalog/catalog.json پیش از ساخت اپ؛ خطاها را به فارسی گزارش می‌کند."""
import json
import sys
from pathlib import Path

path = Path(__file__).resolve().parent.parent / "catalog" / "catalog.json"
errors = []
try:
    data = json.loads(path.read_text(encoding="utf-8"))
except json.JSONDecodeError as exc:
    print(f"✗ فایل catalog.json خراب است (خط {exc.lineno}، ستون {exc.colno}): {exc.msg}")
    print("  معمولاً یک ویرگول اضافه/جاافتاده یا گیومه بسته‌نشده است.")
    sys.exit(1)

maddahs = data.get("maddahs", [])
ids = [m.get("id", "") for m in maddahs]
dupes = {i for i in ids if ids.count(i) > 1}
if dupes:
    errors.append(f"شناسه مداح تکراری: {', '.join(sorted(dupes))}")
for m in maddahs:
    if not m.get("id") or not m.get("name"):
        errors.append(f"مداح بدون id یا name: {m}")
    for link in m.get("links", []):
        if not str(link.get("url", "")).startswith("https://"):
            errors.append(f"پیوند مداح {m.get('id')} باید با https:// شروع شود: {link.get('url')}")

tids = set()
for t in data.get("tracks", []):
    tid = t.get("id", "")
    if not tid:
        errors.append(f"اثر بدون id: {t.get('title')}")
    elif tid in tids:
        errors.append(f"شناسه اثر تکراری: {tid}")
    tids.add(tid)
    if t.get("maddahId") not in ids:
        errors.append(f"اثر {tid}: maddahId «{t.get('maddahId')}» در فهرست مداحان نیست")
    for key in ("streamUrl", "downloadUrl", "sourceUrl", "artwork"):
        value = t.get(key, "")
        if value and not value.startswith("https://"):
            errors.append(f"اثر {tid}: {key} باید با https:// شروع شود")
    if not t.get("streamUrl") and not t.get("sourceUrl"):
        errors.append(f"اثر {tid}: حداقل یکی از streamUrl یا sourceUrl لازم است")

if errors:
    print("✗ ایرادهای کاتالوگ:")
    for e in errors:
        print("  - " + e)
    sys.exit(1)
playable = sum(1 for t in data.get("tracks", []) if t.get("streamUrl"))
print(f"✓ کاتالوگ سالم است: {len(maddahs)} مداح، {len(tids)} اثر ({playable} قابل پخش مستقیم)")
