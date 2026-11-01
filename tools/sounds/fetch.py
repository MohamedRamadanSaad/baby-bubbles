"""Download chosen Wikimedia Commons audio files and record their licenses.

Usage: python3 fetch.py fetch.txt OUT_DIR
Writes OUT_DIR/<key>.<ext> and OUT_DIR/credits.json.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

API = "https://commons.wikimedia.org/w/api.php"
UA = "BabyBubblesSoundFetcher/1.0 (https://github.com/MohamedRamadanSaad/baby-bubbles; m.ramadansaad@gmail.com)"


def get(url):
    for attempt in range(6):
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code != 429 or attempt == 5:
                raise
            wait = int(e.headers.get("Retry-After") or 0) or 20 * (attempt + 1)
            print(f"  429, waiting {wait}s")
            time.sleep(wait)


def strip(html):
    out, skip = [], False
    for ch in html or "":
        if ch == "<":
            skip = True
        elif ch == ">":
            skip = False
        elif not skip:
            out.append(ch)
    return " ".join("".join(out).split())


entries = []
for line in open(sys.argv[1], encoding="utf-8"):
    line = line.strip()
    if not line or line.startswith("#"):
        continue
    key, _, title = (s.strip() for s in line.partition("|"))
    entries.append((key, "File:" + title))

out = sys.argv[2]
os.makedirs(out, exist_ok=True)
credits_path = os.path.join(out, "credits.json")
credits = json.load(open(credits_path, encoding="utf-8")) if os.path.exists(credits_path) else []
have = {c["key"] for c in credits}
failed = []
for key, title in entries:
    if key in have:
        continue
    params = {
        "action": "query", "titles": title, "prop": "imageinfo|videoinfo",
        "iiprop": "url|extmetadata", "viprop": "url|extmetadata",
        "format": "json", "formatversion": "2",
    }
    try:
        data = json.loads(get(API + "?" + urllib.parse.urlencode(params)))
        page = data["query"]["pages"][0]
        info = (page.get("videoinfo") or page.get("imageinfo"))[0]
        meta = info.get("extmetadata") or {}
        url = info["url"]
        ext = os.path.splitext(urllib.parse.unquote(url))[1].lower()
        blob = get(url)
        with open(os.path.join(out, key + ext), "wb") as f:
            f.write(blob)
        credits.append({
            "key": key,
            "file": key + ext,
            "title": title,
            "page": info.get("descriptionurl"),
            "license": strip((meta.get("LicenseShortName") or {}).get("value")),
            "licenseUrl": strip((meta.get("LicenseUrl") or {}).get("value")),
            "artist": strip((meta.get("Artist") or {}).get("value")),
        })
        print(f"ok\t{key}\t{len(blob)//1024}KB\t{credits[-1]['license']}")
    except Exception as e:  # noqa: BLE001
        failed.append(key)
        print(f"FAIL\t{key}\t{title}\t{e}")
    time.sleep(3)

with open(os.path.join(out, "credits.json"), "w", encoding="utf-8") as f:
    json.dump(credits, f, ensure_ascii=False, indent=1)
print("failed:", failed)
