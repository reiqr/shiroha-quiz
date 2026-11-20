#!/usr/bin/env python3
"""读取 apps/android/app/build.gradle.kts 中指定 productFlavor 的 versionName。

用法：python3 .github/scripts/android-version.py apps/android/app/build.gradle.kts native
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

FLAVOR_BLOCK = re.compile(r'create\("(?P<flavor>[^"]+)"\)\s*\{(?P<body>.*?)\n\s{8}\}', re.S)
VERSION_NAME = re.compile(r'versionName\s*=\s*"([^"]+)"')


def main() -> int:
    parser = argparse.ArgumentParser(description="读取 Android flavor 的 versionName")
    parser.add_argument("gradle_file", type=Path, help="app/build.gradle.kts 路径")
    parser.add_argument("flavor", choices=["web", "native"], help="productFlavor 名称")
    args = parser.parse_args()

    text = args.gradle_file.read_text(encoding="utf-8-sig")
    for match in FLAVOR_BLOCK.finditer(text):
        if match.group("flavor") != args.flavor:
            continue
        version = VERSION_NAME.search(match.group("body"))
        if version:
            print(version.group(1))
            return 0

    print(f"未在 {args.gradle_file} 中找到 flavor {args.flavor} 的 versionName", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
