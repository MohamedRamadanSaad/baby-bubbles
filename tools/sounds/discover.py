"""List candidate audio files on Wikimedia Commons with license, length and size.

Run on CI (open internet). Output: one block per query, tab-separated rows.
"""
import json
import sys
import time
import urllib.parse
import urllib.request

API = "https://commons.wikimedia.org/w/api.php"
UA = "BabyBubblesSoundFinder/1.0 (https://github.com/MohamedRamadanSaad/baby-bubbles; m.ramadansaad@gmail.com)"

QUERIES = [line.strip() for line in open(sys.argv[1], encoding="utf-8") if line.strip() and not line.startswith("#")]


def call(params):
    params = {**params, "format": "json", "formatversion": "2"}
    req = urllib.request.Request(API + "?" + urllib.parse.urlencode(params), headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def strip(html):
    out, skip = [], False
    for ch in html or "":
        if ch == "<":
            skip = True
        elif ch == ">":
            skip = False
        elif not skip:
            out.append(ch)
    return "".join(out).replace("\n", " ").strip()[:40]


for q in QUERIES:
    key, _, search = q.partition("|")
    print(f"=== {key.strip()} :: {search.strip()}")
    try:
        data = call({
            "action": "query",
            "generator": "search",
            "gsrsearch": f"{search.strip()} filetype:audio",
            "gsrnamespace": "6",
            "gsrlimit": "12",
            "prop": "imageinfo|videoinfo",
            "iiprop": "url|size|mime|extmetadata",
            "viprop": "url|size|mime|extmetadata",
            "iiextmetadatafilter": "LicenseShortName|Artist",
            "viextmetadatafilter": "LicenseShortName|Artist",
        })
    except Exception as e:  # noqa: BLE001
        print(f"ERROR\t{e}")
        continue
    pages = sorted((data.get("query") or {}).get("pages", []), key=lambda p: p.get("index", 0))
    for p in pages:
        info = (p.get("videoinfo") or p.get("imageinfo") or [{}])[0]
        meta = info.get("extmetadata") or {}
        lic = strip((meta.get("LicenseShortName") or {}).get("value"))
        artist = strip((meta.get("Artist") or {}).get("value"))
        dur = info.get("duration")
        dur = f"{dur:.1f}s" if isinstance(dur, (int, float)) else "?"
        size = info.get("size", 0) // 1024
        print(f"{p['title']}\t{lic}\t{dur}\t{size}KB\t{artist}")
    time.sleep(0.5)
