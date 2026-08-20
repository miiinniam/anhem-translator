#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""给已有 release 补传资产：python scripts/gh_upload_asset.py <release_id> <file> [<file>...]"""
import os
import sys

import requests

TOKEN = os.environ.get("GH_TOKEN", "")
REPO = "miiinniam/anhem-translator"
HDRS = {"Authorization": "token " + TOKEN, "Accept": "application/vnd.github+json"}


def main():
    rid = sys.argv[1]
    files = sys.argv[2:]
    for f in files:
        name = os.path.basename(f)
        print("上传 %s ..." % name)
        with open(f, "rb") as fh:
            up = requests.post(
                "https://uploads.github.com/repos/%s/releases/%s/assets?name=%s"
                % (REPO, rid, name),
                headers={**HDRS, "Content-Type": "application/octet-stream"},
                data=fh,
                timeout=900,
            )
        if up.status_code == 201:
            print("  ✅", up.json().get("name"))
        else:
            print("  ❌", up.status_code, up.text[:200])


if __name__ == "__main__":
    main()
