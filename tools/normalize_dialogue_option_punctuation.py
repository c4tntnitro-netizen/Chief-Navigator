#!/usr/bin/env python3
"""Add terminal punctuation to rules.csv options and mirrored Ink choices."""

from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RULES_PATH = ROOT / "data" / "campaign" / "rules.csv"
DIALOGUE_ROOT = ROOT / "dialogue"
OPTION_RE = re.compile(
    r"^(\s*\d+:[^:\r\n]+:)(.*?)([ \t]*)(\r?\n)?$"
)
INK_CHOICE_RE = re.compile(
    r"^(\s*[*+]\s*\[)(.*)(\](?:\s*->.*)?[ \t]*)(\r?\n)?$"
)
TRAILING_NOTE_RE = re.compile(r"\s+\([^()]*\)$")


def has_terminal_punctuation(label: str) -> bool:
    probe = label.rstrip()
    note = TRAILING_NOTE_RE.search(probe)
    if note:
        probe = probe[: note.start()].rstrip()
    if probe.endswith(("\"", "'", "”", "’")):
        probe = probe[:-1].rstrip()
    return probe.endswith((".", "?", "!"))


def punctuate(label: str) -> str:
    if has_terminal_punctuation(label):
        return label
    stripped = label.rstrip()
    whitespace = label[len(stripped) :]
    if stripped.endswith(("\"", "'", "”", "’")):
        stripped = stripped[:-1] + "." + stripped[-1]
    else:
        stripped += "."
    return stripped + whitespace


def csv_field_spans(text: str, target_column: int) -> list[tuple[int, int]]:
    spans: list[tuple[int, int]] = []
    row = 0
    column = 0
    start = 0
    in_quotes = False
    index = 0

    while index < len(text):
        char = text[index]
        if char == '"':
            if in_quotes and index + 1 < len(text) and text[index + 1] == '"':
                index += 2
                continue
            in_quotes = not in_quotes
            index += 1
            continue

        if not in_quotes and char == ",":
            if row > 0 and column == target_column:
                spans.append((start, index))
            column += 1
            start = index + 1
            index += 1
            continue

        if not in_quotes and char in "\r\n":
            if row > 0 and column == target_column:
                spans.append((start, index))
            if char == "\r" and index + 1 < len(text) and text[index + 1] == "\n":
                index += 2
            else:
                index += 1
            row += 1
            column = 0
            start = index
            continue

        index += 1

    if start < len(text) and row > 0 and column == target_column:
        spans.append((start, len(text)))
    if in_quotes:
        raise ValueError("rules.csv ends inside a quoted field")
    return spans


def transform_option_field(raw_field: str) -> tuple[str, int]:
    quoted = (
        len(raw_field) >= 2
        and raw_field.startswith('"')
        and raw_field.endswith('"')
    )
    value = raw_field[1:-1].replace('""', '"') if quoted else raw_field
    changed = 0
    output: list[str] = []

    for line in value.splitlines(keepends=True):
        match = OPTION_RE.match(line)
        if not match:
            output.append(line)
            continue
        label = match.group(2)
        updated = punctuate(label)
        if updated != label:
            changed += 1
        output.append(
            match.group(1)
            + updated
            + match.group(3)
            + (match.group(4) or "")
        )

    new_value = "".join(output)
    if quoted:
        return '"' + new_value.replace('"', '""') + '"', changed
    return new_value, changed


def normalize_rules() -> int:
    original_bytes = RULES_PATH.read_bytes()
    has_bom = original_bytes.startswith(b"\xef\xbb\xbf")
    payload = original_bytes[3:] if has_bom else original_bytes
    text = payload.decode("utf-8")
    spans = csv_field_spans(text, 5)
    changed = 0

    for start, end in reversed(spans):
        replacement, field_changes = transform_option_field(text[start:end])
        if field_changes:
            text = text[:start] + replacement + text[end:]
            changed += field_changes

    encoded = text.encode("utf-8")
    RULES_PATH.write_bytes((b"\xef\xbb\xbf" if has_bom else b"") + encoded)
    return changed


def normalize_ink() -> tuple[int, int]:
    changed_files = 0
    changed_choices = 0
    for path in sorted(DIALOGUE_ROOT.glob("*.ink")):
        original_bytes = path.read_bytes()
        has_bom = original_bytes.startswith(b"\xef\xbb\xbf")
        payload = original_bytes[3:] if has_bom else original_bytes
        text = payload.decode("utf-8")
        output: list[str] = []
        file_changes = 0

        for line in text.splitlines(keepends=True):
            match = INK_CHOICE_RE.match(line)
            if not match:
                output.append(line)
                continue
            label = match.group(2)
            updated = punctuate(label)
            if updated != label:
                file_changes += 1
            output.append(
                match.group(1)
                + updated
                + match.group(3)
                + (match.group(4) or "")
            )

        if file_changes:
            encoded = "".join(output).encode("utf-8")
            path.write_bytes((b"\xef\xbb\xbf" if has_bom else b"") + encoded)
            changed_files += 1
            changed_choices += file_changes
    return changed_files, changed_choices


def main() -> None:
    rules_changes = normalize_rules()
    ink_files, ink_changes = normalize_ink()
    print(
        f"Updated {rules_changes} rules.csv options and "
        f"{ink_changes} choices across {ink_files} Ink files."
    )


if __name__ == "__main__":
    main()
