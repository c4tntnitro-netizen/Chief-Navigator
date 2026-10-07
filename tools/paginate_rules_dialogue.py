#!/usr/bin/env python3
"""Split oversized rules.csv prose into deterministic Continue pages.

Ink remains the narrative authoring source. This tool adds runtime-only paging
scaffolding at paragraph boundaries so a long knot does not become a clipped or
scroll-heavy Starsector dialogue page.
"""

from __future__ import annotations

import argparse
import csv
import io
import math
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RULES_PATH = ROOT / "data" / "campaign" / "rules.csv"

WRAP_COLUMNS = 52
TARGET_LINES = 18
PAGINATE_OVER_LINES = 30

CUSTOM_OPTION_PREFIXES = (
    "ChiefNavigatorBudaiSalvage_",
    "ChiefNavigatorExiles_",
    "ChiefNavigatorLeagueRescue_",
    "ChiefNavigatorFobIthacaApproach_",
    "ChiefNavigatorLabor5Ending_",
    "ChiefNavigatorSinniVignette_",
)


def split_raw_records(source: str) -> tuple[list[str], str]:
    newline = "\r\n" if "\r\n" in source else "\n"
    records: list[str] = []
    start = 0
    index = 0
    quoted = False
    while index < len(source):
        char = source[index]
        if char == '"':
            if quoted and index + 1 < len(source) and source[index + 1] == '"':
                index += 2
                continue
            quoted = not quoted
        if not quoted and char in "\r\n":
            records.append(source[start:index])
            if char == "\r" and index + 1 < len(source) and source[index + 1] == "\n":
                index += 1
            start = index + 1
        index += 1
    if start < len(source):
        records.append(source[start:])
    elif source.endswith(("\n", "\r")):
        records.append("")
    return records, newline


def parse_record(record: str) -> list[str]:
    return next(csv.reader(io.StringIO(record, newline="")))


def serialize_record(fields: list[str], newline: str) -> str:
    output = io.StringIO(newline="")
    writer = csv.writer(output, lineterminator=newline)
    writer.writerow(fields)
    return output.getvalue()[: -len(newline)]


def paragraphs(text: str) -> list[str]:
    return [
        part.strip()
        for part in re.split(r"(?:\r?\n){2,}", text.strip())
        if part.strip()
    ]


def paragraph_lines(paragraph: str) -> int:
    flattened = re.sub(r"\s+", " ", paragraph.strip())
    return max(1, math.ceil(len(flattened) / WRAP_COLUMNS)) + 1


def option_count(options: str) -> int:
    return len([line for line in options.splitlines() if line.strip()])


def estimated_lines(text: str, options: str) -> int:
    return sum(paragraph_lines(part) for part in paragraphs(text)) + option_count(options)


def split_pages(text: str, final_option_count: int) -> list[str]:
    parts = paragraphs(text)
    if not parts:
        return [""]
    budget = max(8, TARGET_LINES - final_option_count)
    pages: list[list[str]] = []
    current: list[str] = []
    used = 0
    for part in parts:
        cost = paragraph_lines(part)
        if current and used + cost > budget:
            pages.append(current)
            current = []
            used = 0
        current.append(part)
        used += cost
    if current:
        pages.append(current)
    return ["\n\n".join(page) for page in pages]


def snake_case(rule_id: str) -> str:
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", rule_id)
    value = re.sub(r"[^A-Za-z0-9]+", "_", value)
    return value.strip("_").lower()


def continuation_route(original_trigger: str, option_id: str) -> tuple[str, str]:
    for prefix in CUSTOM_OPTION_PREFIXES:
        if original_trigger.startswith(prefix):
            return prefix + option_id, ""
    return "DialogOptionSelected", f"$option == {option_id}"


def split_script(script: str) -> tuple[str, str]:
    immediate: list[str] = []
    deferred: list[str] = []
    for line in script.splitlines():
        stripped = line.strip()
        if stripped.startswith("FireAll ") or stripped.startswith("SetOptionColor "):
            deferred.append(line)
        elif stripped:
            immediate.append(line)
    return "\n".join(immediate), "\n".join(deferred)


def paginate(fields: list[str], newline: str) -> list[str]:
    rule_id, trigger, conditions, script, text, options, notes = fields
    pages = split_pages(text, option_count(options))
    if len(pages) < 2:
        return [serialize_record(fields, newline)]

    immediate_script, deferred_script = split_script(script)
    generated: list[list[str]] = []
    total = len(pages)
    option_base = "chief_navigator_auto_page_" + snake_case(rule_id)

    for index, page in enumerate(pages):
        page_number = index + 1
        final = page_number == total
        if index == 0:
            page_id = rule_id
            page_trigger = trigger
            page_conditions = conditions
            page_script = immediate_script
            page_notes = f"{notes} Auto-paged {page_number}/{total}.".strip()
        else:
            page_id = f"{rule_id}Page{page_number}"
            previous_option = f"{option_base}_{page_number}"
            page_trigger, page_conditions = continuation_route(trigger, previous_option)
            page_script = ""
            page_notes = f"Automatic runtime continuation {page_number}/{total} for {rule_id}."

        if final:
            if deferred_script:
                page_script = "\n".join(
                    part for part in (page_script, deferred_script) if part
                )
            page_options = options
        else:
            next_option = f"{option_base}_{page_number + 1}"
            page_options = f"0:{next_option}:Continue."

        generated.append(
            [
                page_id,
                page_trigger,
                page_conditions,
                page_script,
                page,
                page_options,
                page_notes,
            ]
        )

    return [serialize_record(row, newline) for row in generated]


def process(apply_changes: bool) -> int:
    with RULES_PATH.open("r", encoding="utf-8", newline="") as handle:
        source = handle.read()
    records, newline = split_raw_records(source)
    if not records:
        raise ValueError("rules.csv is empty")

    output: list[str] = [records[0]]
    changed: list[tuple[str, int, int]] = []
    for record in records[1:]:
        if not record:
            output.append(record)
            continue
        fields = parse_record(record)
        if len(fields) != 7:
            raise ValueError(f"Malformed rules.csv record with {len(fields)} fields: {fields[:1]}")
        before = estimated_lines(fields[4], fields[5])
        if before <= PAGINATE_OVER_LINES:
            output.append(record)
            continue
        replacement = paginate(fields, newline)
        output.extend(replacement)
        changed.append((fields[0], before, len(replacement)))

    if changed and apply_changes:
        rebuilt = newline.join(output)
        with RULES_PATH.open("w", encoding="utf-8", newline="") as handle:
            handle.write(rebuilt)

    for rule_id, lines, pages in changed:
        print(f"{rule_id}: {lines} estimated lines -> {pages} pages")
    print(f"{'Updated' if apply_changes else 'Found'} {len(changed)} oversized rule(s).")
    return 1 if changed and not apply_changes else 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--apply",
        action="store_true",
        help="rewrite rules.csv; without this flag, only report outliers",
    )
    args = parser.parse_args()
    return process(args.apply)


if __name__ == "__main__":
    raise SystemExit(main())
