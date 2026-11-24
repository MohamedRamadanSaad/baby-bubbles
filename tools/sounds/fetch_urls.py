"""Download files listed by direct URL (sources outside Commons).

Usage: python3 fetch_urls.py urls.txt OUT_DIR
Writes OUT_DIR/<key>.<ext> and appends to OUT_DIR/url_credits.json.
"""
import json
import os
import sys
import urllib.request

UA = "Mozilla/5.0 (X11; Linux x86_64) BabyBubblesSoundFetcher/1.0"
out = sys.argv[2]
os.makedirs(out, exist_ok=True)
credits_path = os.path.join(out, "url_credits.json")
credits = json.load(open(credits_path, encoding="utf-8")) if os.path.exists(credits_path) else []
have = {c["key"] for c in credits}
for line in open(sys.argv[1], encoding="utf-8"):
    line = line.strip()
    if not line or line.startswith("#"):
        continue
    key, url, note = (s.strip() for s in line.split("|", 2))
    if key in have:
        continue
    ext = os.path.splitext(url.split("?")[0])[1] or ".bin"
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Referer": "https://pixabay.com/"})
    with urllib.request.urlopen(req, timeout=120) as r:
        data = r.read()
    open(os.path.join(out, key + ext), "wb").write(data)
    credits.append({"key": key, "file": key + ext, "url": url, "note": note})
    print("ok", key, len(data))
json.dump(credits, open(credits_path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
