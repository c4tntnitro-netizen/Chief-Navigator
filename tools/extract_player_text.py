#!/usr/bin/env python3
"""Extract likely player-facing text from a Starsector mod for review.

The scanner is deliberately read-only. It writes a Markdown inventory containing
the source file, approximate line number, extraction category, and text. It uses
only Python's standard library.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


TEXT_EXTENSIONS = {
    ".csv",
    ".faction",
    ".java",
    ".json",
    ".json5",
    ".hull",
    ".ink",
    ".md",
    ".mission",
    ".proj",
    ".ship",
    ".skill",
    ".skin",
    ".system",
    ".variant",
    ".wpn",
}

SKIP_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    ".vscode",
    "build",
    "dist",
    "jars",
    "lib",
    "libs",
    "node_modules",
    "out",
    "target",
}

PLAYER_TEXT_KEYS = re.compile(
    r"(?:^|_)(?:name|title|text|desc|description|tooltip|option|options|"
    r"label|message|flavor|blurb|summary|tagline|prompt|response|dialogue|"
    r"dialog|greeting|intel|commodity|condition|industry|ability)(?:$|_)",
    re.IGNORECASE,
)

JAVA_CONTEXT = re.compile(
    r"(?:addPara|addOption|setText|setName|setTitle|setTooltip|setDescription|"
    r"setPrompt|showMessage|addMessage|addSectionHeading|setSound|printf|format)",
    re.IGNORECASE,
)

WORD_RE = re.compile(r"[A-Za-z][A-Za-z'’-]*")
JAVA_STRING_RE = re.compile(r'"(?:\\.|[^"\\])*"')
INK_CONTROL_RE = re.compile(
    r"^(?:INCLUDE|EXTERNAL|VAR|CONST|LIST)\b|^(?:===|=)|^->|^~|^\s*[{}]$"
)


@dataclass(frozen=True)
class Entry:
    path: str
    line: int
    category: str
    text: str


def is_human_text(value: str) -> bool:
    """Reject identifiers, paths, asset names, and other obvious plumbing."""
    value = value.strip()
    if not value or len(value) < 2:
        return False
    words = WORD_RE.findall(value)
    if not words:
        return False
    if len(words) >= 2:
        return True
    # One-word display names and choices (for example "Charybdis" or "Leave")
    # are common, while plumbing identifiers are normally lowercase or contain
    # underscores/slashes.
    if words[0][0].isupper() and re.fullmatch(r"[A-Za-z][A-Za-z'’-]*", value):
        return True
    if re.search(r"[.!?,:;…]", value) or " " in value:
        return True
    return False


def clean(value: str) -> str:
    value = value.replace("\r\n", "\n").replace("\r", "\n").strip()
    return re.sub(r"[ \t]+", " ", value)


def escaped_markdown(value: str) -> str:
    return value.replace("```", "`\u200b``")


def line_for_value(source: str, value: str, start: int = 0) -> tuple[int, int]:
    """Return an approximate 1-based line and next search offset."""
    needle = value[: min(80, len(value))]
    offset = source.find(needle, start)
    if offset < 0:
        offset = source.find(needle)
    if offset < 0:
        return 1, start
    return source.count("\n", 0, offset) + 1, offset + len(needle)


def scan_csv(path: Path, relative: str) -> list[Entry]:
    entries: list[Entry] = []
    with path.open("r", encoding="utf-8-sig", errors="replace", newline="") as fh:
        reader = csv.reader(fh)
        try:
            headers = next(reader)
        except StopIteration:
            return entries
        for row in reader:
            for index, value in enumerate(row):
                header = headers[index].strip() if index < len(headers) else f"column_{index + 1}"
                if PLAYER_TEXT_KEYS.search(header) and is_human_text(value):
                    entries.append(Entry(relative, reader.line_num, f"csv:{header}", clean(value)))
    return entries


def walk_json(value: object, key_path: str = "") -> Iterable[tuple[str, str]]:
    if isinstance(value, dict):
        for key, child in value.items():
            child_path = f"{key_path}.{key}" if key_path else str(key)
            yield from walk_json(child, child_path)
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from walk_json(child, f"{key_path}[{index}]")
    elif isinstance(value, str):
        yield key_path, value


def scan_json(path: Path, relative: str, source: str) -> list[Entry]:
    try:
        data = json.loads(source)
    except json.JSONDecodeError:
        return scan_structured_lines(relative, source)
    entries: list[Entry] = []
    offset = 0
    for key_path, value in walk_json(data):
        leaf_key = re.split(r"[.[]", key_path)[-1].rstrip("]")
        if PLAYER_TEXT_KEYS.search(leaf_key) and is_human_text(value):
            line, offset = line_for_value(source, value, offset)
            entries.append(Entry(relative, line, f"json:{key_path}", clean(value)))
    return entries


def strip_ink_markup(line: str) -> str:
    line = re.sub(r"^\s*[+*]\s*", "", line)
    line = re.sub(r"^\s*-\s*", "", line)
    line = re.sub(r"^\s*\[[^]]*]\s*", "", line)
    line = re.sub(r"\s*->.*$", "", line)
    line = re.sub(r"\s*#\S+(?:\s+#\S+)*\s*$", "", line)
    return clean(line)


def scan_ink(relative: str, source: str) -> list[Entry]:
    entries: list[Entry] = []
    in_block_comment = False
    for number, original in enumerate(source.splitlines(), 1):
        line = original.strip()
        if in_block_comment:
            if "*/" in line:
                in_block_comment = False
            continue
        if line.startswith("/*"):
            in_block_comment = "*/" not in line
            continue
        if not line or line.startswith("//") or INK_CONTROL_RE.search(line):
            continue
        text = strip_ink_markup(line)
        if is_human_text(text):
            category = "ink:choice" if line.startswith(("+", "*")) else "ink:prose"
            entries.append(Entry(relative, number, category, text))
    return entries


def decode_java_string(token: str) -> str:
    body = token[1:-1]
    try:
        return bytes(body, "utf-8").decode("unicode_escape")
    except UnicodeDecodeError:
        return body


def scan_java(relative: str, source: str) -> list[Entry]:
    entries: list[Entry] = []
    lines = source.splitlines()
    for index, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith(("//", "import ", "package ")):
            continue
        context = " ".join(lines[max(0, index - 2) : index + 1])
        for match in JAVA_STRING_RE.finditer(line):
            value = decode_java_string(match.group(0))
            if is_human_text(value):
                category = "java:ui-string" if JAVA_CONTEXT.search(context) else "java:possible-text"
                entries.append(Entry(relative, index + 1, category, clean(value)))
    return entries


def scan_structured_lines(relative: str, source: str) -> list[Entry]:
    """Best-effort scanner for JSON-like Starsector formats with comments."""
    entries: list[Entry] = []
    pattern = re.compile(r'["\'](?P<key>[^"\']+)["\']\s*:\s*["\'](?P<value>.*?)["\']\s*[,}]?')
    for number, line in enumerate(source.splitlines(), 1):
        match = pattern.search(line)
        if match and PLAYER_TEXT_KEYS.search(match.group("key")):
            value = match.group("value")
            if is_human_text(value):
                entries.append(Entry(relative, number, f"config:{match.group('key')}", clean(value)))
    return entries


def scan_file(path: Path, root: Path) -> list[Entry]:
    relative = path.relative_to(root).as_posix()
    if path.suffix.lower() == ".csv":
        try:
            return scan_csv(path, relative)
        except csv.Error as exc:
            print(f"warning: could not parse {relative}: {exc}", file=sys.stderr)
            return []
    source = path.read_text(encoding="utf-8-sig", errors="replace")
    suffix = path.suffix.lower()
    if suffix == ".ink":
        return scan_ink(relative, source)
    if suffix == ".java":
        return scan_java(relative, source)
    if suffix in {".json", ".json5"}:
        return scan_json(path, relative, source)
    if suffix in {".faction", ".hull", ".mission", ".proj", ".ship", ".skill", ".skin", ".system", ".variant", ".wpn"}:
        return scan_structured_lines(relative, source)
    return []


def iter_source_files(root: Path, output: Path) -> Iterable[Path]:
    for path in sorted(root.rglob("*")):
        if not path.is_file() or path.resolve() == output.resolve():
            continue
        if any(part.lower() in SKIP_DIRS for part in path.relative_to(root).parts[:-1]):
            continue
        if path.suffix.lower() in TEXT_EXTENSIONS:
            yield path


def write_markdown(output: Path, root: Path, entries: list[Entry], scanned: int) -> None:
    grouped: dict[str, list[Entry]] = {}
    for entry in entries:
        grouped.setdefault(entry.path, []).append(entry)

    lines = [
        "# Player-facing text review",
        "",
        f"Source mod: `{root}`",
        "",
        f"Scanned files: **{scanned}**  ",
        f"Extracted items: **{len(entries)}**  ",
        f"Approximate extracted words: **{sum(len(WORD_RE.findall(e.text)) for e in entries)}**",
        "",
        "> This is a best-effort inventory. Dynamic text assembled from variables or many code fragments may need manual review.",
        "",
    ]
    for path, file_entries in grouped.items():
        lines.extend([f"## `{path}`", ""])
        for entry in file_entries:
            lines.extend(
                [
                    f"### Line {entry.line} — `{entry.category}`",
                    "",
                    "```text",
                    escaped_markdown(entry.text),
                    "```",
                    "",
                ]
            )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mod", type=Path, help="Path to the Starsector mod folder to scan")
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        help="Markdown output path (default: <mod>/player_text_review.md)",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    root = args.mod.expanduser().resolve()
    if not root.is_dir():
        print(f"error: mod folder does not exist: {root}", file=sys.stderr)
        return 2
    output = (args.output or root / "player_text_review.md").expanduser().resolve()
    files = list(iter_source_files(root, output))
    entries: list[Entry] = []
    for path in files:
        try:
            entries.extend(scan_file(path, root))
        except OSError as exc:
            print(f"warning: could not read {path}: {exc}", file=sys.stderr)
    entries.sort(key=lambda item: (item.path.lower(), item.line, item.category, item.text))
    write_markdown(output, root, entries, len(files))
    print(f"Wrote {len(entries)} items from {len(files)} files to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
