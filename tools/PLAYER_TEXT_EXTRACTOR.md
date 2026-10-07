# Player-facing text extractor

Run from PowerShell with Python 3:

```powershell
python .\tools\extract_player_text.py .
```

This creates `player_text_review.md` in the scanned mod folder. To scan a
different mod or place the report elsewhere:

```powershell
python .\tools\extract_player_text.py "..\Some Other Mod" --output ".\review.md"
```

The scanner reads Ink prose and choices, likely player-facing CSV columns,
description-like JSON/config fields, and all human-looking Java strings. Java
strings near common UI methods are labeled `java:ui-string`; less certain ones
are retained as `java:possible-text` so prose stored in constants or assembled
across lines is not silently missed. It never edits source files. The report is
a review aid rather than a compiler: text assembled dynamically in code can
still require manual inspection.
