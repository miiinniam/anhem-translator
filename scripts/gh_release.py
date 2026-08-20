#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""anhem release 创建/上传助手（供 release.sh 调用，规避 shell JSON 转义问题）"""
import json
import os
import re
import sys

import requests

TOKEN = os.environ.get("GH_TOKEN", "")
REPO = "miiinniam/anhem-translator"
HDRS = {"Authorization": "token " + TOKEN, "Accept": "application/vnd.github+json"}


def main():
    version = sys.argv[1]
    notes = sys.argv[2]
    files = sys.argv[3:]  # 剩余参数为文件路径

    # 1. 创建 release
    payload = {
        "tag_name": version,
        "name": "anhem " + version,
        "body": notes,
        "draft": False,
        "prerelease": False,
    }
    resp = requests.post(
        "https://api.github.com/repos/%s/releases" % REPO,
        headers=HDRS,
        json=payload,
        timeout=30,
    )
    if resp.status_code != 201:
        print("创建 Release 失败:", resp.status_code, resp.text[:400])
        sys.exit(1)
    rid = resp.json()["id"]
    print("✅ Release #%s 已创建: %s" % (rid, version))

    # 2. 上传资产（文件名统一转 ASCII，避免中文编码问题；版本取自 tag）
    def ascii_asset_name(path, version):
        base = os.path.basename(path)
        low = base.lower()
        ver = version.lstrip("v")  # v2.5 -> 2.5
        if low.endswith(".apk"):
            return "anhem-%s.apk" % ver
        if "setup" in low:
            return "anhem-setup-%s.exe" % ver
        if "portable" in low or "便携" in low:
            return "anhem-portable-%s.exe" % ver
        # 兜底：保 ASCII + 扩展名
        keep = re.sub(r"[^a-zA-Z0-9._-]+", "-", base).strip("-.")
        return keep

    for f in files:
        name = ascii_asset_name(f, version)
        print("上传 %s -> %s ..." % (os.path.basename(f), name))
        with open(f, "rb") as fh:
            up = requests.post(
                "https://uploads.github.com/repos/%s/releases/%s/assets"
                % (REPO, rid),
                params={"name": name},
                headers={**HDRS, "Content-Type": "application/octet-stream"},
                data=fh,
                timeout=300,
            )
        if up.status_code == 201:
            print("  ✅", up.json().get("name", "?"))
        else:
            print("  ❌ 上传失败:", up.status_code, up.text[:200])
            sys.exit(1)

    print("🎉 发布完成")


if __name__ == "__main__":
    main()
