"""Validate the standalone, enemy-only Avici port and optional SEP coexistence.

Uses only Python's standard library and the installed game's native data files.
Run with --baseline <pre-port commit> to also prove the weapon package was untouched.
This checks definitions/dependencies, not live combat rendering or game balance.
"""
from __future__ import annotations

import argparse
import csv
from collections import Counter
from io import StringIO
import json
from pathlib import Path
import re
import struct
import subprocess
from zipfile import ZipFile


HULLS = {
    "chief_navigator_avici_banshee": "chief_navigator_avici_banshee_Standard",
    "chief_navigator_avici_brain": "chief_navigator_avici_brain_Standard",
    "chief_navigator_avici_hunter": "chief_navigator_avici_hunter_Budai",
}
SYSTEMS = {
    "chief_navigator_avici_banshee_jump",
    "chief_navigator_avici_brain_aura",
    "chief_navigator_avici_hunter_domain",
}
HULLMOD = "chief_navigator_avici_dweller"
FORBIDDEN_IMPORTS = re.compile(
    r"^\s*import\s+(?:static\s+)?(?:tidal\.|kotlin\.|org\.magiclib\.|"
    r"org\.lazywizard\.|second_in_command\.)",
    re.MULTILINE,
)
IMAGE = re.compile(r"graphics/[\w./ -]+\.(?:png|jpe?g|gif|dds)")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def uncomment(source: str) -> str:
    """Strip Starsector JSON comments without damaging quoted paths or prose."""
    result = []
    quoted = escaped = line = block = False
    index = 0
    while index < len(source):
        char = source[index]
        following = source[index:index + 2]
        if line:
            if char in "\r\n":
                line = False
                result.append(char)
        elif block:
            if following == "*/":
                block = False
                index += 1
            elif char in "\r\n":
                result.append(char)
        elif quoted:
            result.append(char)
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
            result.append(char)
        elif char == "#" or following == "//":
            line = True
        elif following == "/*":
            block = True
            index += 1
        else:
            result.append(char)
        index += 1
    return "".join(result)


def read_json(path: Path) -> dict:
    def unique_pairs(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, f"Duplicate JSON key {key!r}: {path}")
            result[key] = value
        return result
    text = uncomment(path.read_text(encoding="utf-8-sig"))
    # Authored new definitions must use strict JSON after comment removal.
    return json.loads(text, object_pairs_hook=unique_pairs)


def read_csv(path: Path) -> list[dict]:
    text = path.read_text(encoding="utf-8-sig")
    return [row for row in csv.DictReader(StringIO(text))
            if row.get("id", "").strip() and not row["id"].startswith("#")]


def selected_rows(path: Path, expected: set[str]) -> dict[str, dict]:
    rows = read_csv(path)
    counts = Counter(row["id"] for row in rows)
    for identifier in expected:
        require(counts[identifier] == 1,
                f"Expected one registration for {identifier}, got {counts[identifier]} in {path}")
    return {row["id"]: row for row in rows if row["id"] in expected}


def find_core(root: Path, requested: Path | None) -> Path:
    candidates = [requested] if requested else [
        root.parent / "starsector-core", root.parent.parent / "starsector-core"]
    for candidate in candidates:
        if candidate and (candidate / "data/weapons/weapon_data.csv").is_file():
            return candidate.resolve()
    raise AssertionError("Cannot locate installed Starsector data; pass --core")


def source_path(root: Path, class_name: str) -> Path:
    require(class_name.startswith("chiefnavigator.dwellers."),
            f"Ported script must have its own namespace: {class_name}")
    path = root / "src" / (class_name.replace(".", "/") + ".java")
    require(path.is_file(), f"Missing ported script: {path}")
    return path


def validate(root: Path, core: Path, baseline: str | None, sep: Path | None,
             jar: Path | None) -> None:
    metadata = read_json(root / "mod_info.json")
    require(metadata["modPlugin"] == "chiefnavigator.ChiefNavigatorModPlugin",
            "Do not import SEP's global mod plugin")
    dependencies = {item["id"] for item in metadata.get("dependencies", [])}
    require(not dependencies.intersection({"shroud_pack", "MagicLib", "lw_lazylib", "kotlin", "second_in_command"}),
            f"The enemy-only port introduced an external dependency: {dependencies}")

    ships = selected_rows(root / "data/hulls/ship_data.csv", set(HULLS))
    systems = selected_rows(root / "data/shipsystems/ship_systems.csv", SYSTEMS)
    hullmods = selected_rows(root / "data/hullmods/hull_mods.csv", {HULLMOD})
    settings = read_json(root / "data/config/settings.json")
    dweller_sprites = settings.get("graphics", {}).get("dweller", {})
    if not dweller_sprites:
        dweller_sprites = settings.get("sprites", {}).get("dweller", {})
    core_weapons = {row["id"] for row in read_csv(core / "data/weapons/weapon_data.csv")}
    weapon_specs = {path.stem: path for path in (core / "data/weapons").rglob("*.wpn")}
    weapon_sizes = {"SMALL": 0, "MEDIUM": 1, "LARGE": 2}
    mount_types = {"ENERGY": {"ENERGY"}, "BALLISTIC": {"BALLISTIC"},
                   "MISSILE": {"MISSILE"}, "HYBRID": {"ENERGY", "BALLISTIC"},
                   "SYNERGY": {"ENERGY", "MISSILE"}, "COMPOSITE": {"BALLISTIC", "MISSILE"},
                   "UNIVERSAL": {"ENERGY", "BALLISTIC", "MISSILE"}}
    core_hullmods = {row["id"] for row in read_csv(core / "data/hullmods/hull_mods.csv")}
    core_hulls = {row["id"] for row in read_csv(core / "data/hulls/ship_data.csv")}
    core_systems = {row["id"] for row in read_csv(core / "data/shipsystems/ship_systems.csv")}
    require(not set(HULLS).intersection(core_hulls), "Port overrides a vanilla hull ID")
    require(not SYSTEMS.intersection(core_systems), "Port overrides a vanilla system ID")
    require(HULLMOD not in core_hullmods, "Port overrides a vanilla hullmod ID")
    definitions = []
    weapons_fitted = 0
    for hull, variant_id in HULLS.items():
        row = ships[hull]
        require(row["system id"] in SYSTEMS, f"Foreign system on {hull}")
        ship_path = root / f"data/hulls/{hull}.ship"
        ship = read_json(ship_path)
        require(ship["hullId"] == hull, f"Hull registration mismatch: {ship_path}")
        require(dweller_sprites.get(hull) == ship["spriteName"],
                f"Missing combat-part sprite registration for {hull}")
        require(HULLMOD in ship.get("builtInMods", []), f"Missing custom enemy hullmod: {hull}")
        require(set(ship.get("builtInMods", [])) <= core_hullmods | {HULLMOD},
                f"Foreign built-in hullmod on {hull}")
        require(float(ship.get("width", 0)) > 0 and float(ship.get("height", 0)) > 0,
                f"Invalid sprite dimensions: {hull}")
        bounds = ship.get("bounds", [])
        require(len(bounds) >= 6, f"Missing collision bounds: {hull}")
        slots = [slot["id"] for slot in ship.get("weaponSlots", [])]
        require(len(slots) == len(set(slots)), f"Duplicate weapon slot: {hull}")
        fitted = dict(ship.get("builtInWeapons", {}))
        variant_path = root / f"data/variants/{variant_id}.variant"
        variant = read_json(variant_path)
        require(variant["variantId"] == variant_id and variant["hullId"] == hull,
                f"Variant identity mismatch: {variant_path}")
        for mod in variant.get("hullMods", []) + variant.get("permaMods", []):
            require(mod in core_hullmods | {HULLMOD}, f"Foreign variant hullmod {mod}: {variant_id}")
        for group in variant.get("weaponGroups", []):
            for slot, weapon in group.get("weapons", {}).items():
                require(slot not in fitted, f"Multiple weapons in slot {slot}: {variant_id}")
                fitted[slot] = weapon
        require(fitted, f"Enemy has no fitted weapons: {variant_id}")
        require(set(fitted) <= set(slots), f"Unknown fitted slot: {variant_id}")
        require(set(fitted.values()) <= core_weapons,
                f"Non-vanilla weapons on {variant_id}: {set(fitted.values()) - core_weapons}")
        slot_specs = {slot["id"]: slot for slot in ship.get("weaponSlots", [])}
        for slot, weapon in fitted.items():
            require(weapon in weapon_specs, f"Missing native weapon spec: {weapon}")
            # Native .wpn files permit bare enum tokens/trailing commas, so read
            # only the explicit top-level string fields needed for fit checks.
            spec_text = uncomment(weapon_specs[weapon].read_text(encoding="utf-8-sig"))
            weapon_size = re.search(r'"size"\s*:\s*"([A-Z]+)"', spec_text)
            weapon_type = re.search(r'"type"\s*:\s*"([A-Z]+)"', spec_text)
            require(weapon_size is not None and weapon_type is not None,
                    f"Missing native weapon mount fields: {weapon}")
            mount = slot_specs[slot]
            require(weapon_sizes[weapon_size[1]] <= weapon_sizes[mount["size"]],
                    f"Weapon {weapon} is too large for {variant_id}/{slot}")
            require(weapon_type[1] in mount_types.get(mount["type"], {weapon_type[1]}),
                    f"Weapon {weapon} cannot fit {mount['type']} slot {variant_id}/{slot}")
        require(not variant.get("wings"), f"Unexpected external fighter package: {variant_id}")
        weapons_fitted += len(fitted)
        definitions.extend((ship_path, variant_path))
        creator = ship.get("shipAI") or ship.get("shipCreator")
        if creator:
            source_path(root, creator)
    require(ships["chief_navigator_avici_hunter"]["designation"] == "Insatiable Hunter",
            "The new boss class must be Insatiable Hunter")
    require(read_json(root / "data/variants/chief_navigator_avici_hunter_Budai.variant")["displayName"] == "Budai",
            "The new Avici boss must be named Budai")
    for identifier in systems:
        path = root / f"data/shipsystems/{identifier}.system"
        definition = read_json(path)
        require(definition["id"] == identifier, f"System identity mismatch: {path}")
        for key in ("statsScript", "aiScript"):
            if definition.get(key):
                source_path(root, definition[key])
        definitions.append(path)
    source_path(root, hullmods[HULLMOD]["script"])
    sources = sorted((root / "src/chiefnavigator/dwellers").glob("*.java"))
    require(sources, "Missing standalone enemy code")
    for path in sources:
        source = path.read_text(encoding="utf-8-sig")
        require(re.search(r"^package chiefnavigator\.dwellers;", source, re.MULTILINE) is not None,
                f"Foreign Java namespace: {path}")
        require(not FORBIDDEN_IMPORTS.search(source), f"External SEP/library dependency: {path}")
        require("tidal.shroud" not in source, f"Hidden SEP linkage: {path}")
    image_count = 0
    for image in sorted(set(IMAGE.findall("\n".join(
            path.read_text(encoding="utf-8-sig") for path in definitions + sources)))):
        local, native = root / image, core / image
        require(local.is_file() or native.is_file(), f"Unresolved enemy image: {image}")
        if local.is_file():
            if local.suffix.lower() == ".png":
                raw = local.read_bytes()
                require(raw[:8] == b"\x89PNG\r\n\x1a\n", f"Invalid PNG: {image}")
                width, height = struct.unpack(">II", raw[16:24])
                require(0 < width <= 16384 and 0 < height <= 16384, f"Invalid PNG dimensions: {image}")
            image_count += 1
    for document in ("README.md", "THIRD_PARTY_NOTICES.md"):
        text = (root / document).read_text(encoding="utf-8-sig")
        require("TIDAL" in text and (document == "README.md" or
                re.search(r"permission", text, re.IGNORECASE)),
                f"Missing TIDAL permission credit: {document}")
    if baseline:
        def git(*args):
            return subprocess.check_output(["git", "-c", f"safe.directory={root}", *args], cwd=root)
        old_weapons = git("show", f"{baseline}:data/weapons/weapon_data.csv")
        require(old_weapons.replace(b"\r\n", b"\n") ==
                (root / "data/weapons/weapon_data.csv").read_bytes().replace(b"\r\n", b"\n"),
                "The enemy-only port changed the existing weapon registry")
        changed = git("diff", "--name-only", baseline, "--", "data/weapons").decode().splitlines()
        untracked = git("ls-files", "--others", "--exclude-standard", "--", "data/weapons").decode().splitlines()
        require(not changed and not untracked, f"Enemy-only port includes weapon files: {changed + untracked}")
    if sep:
        require(read_json(sep / "mod_info.json")["id"] == "shroud_pack", "--sep is not SEP")
        for relative, own_ids in (("data/hulls/ship_data.csv", set(HULLS)),
                                  ("data/shipsystems/ship_systems.csv", SYSTEMS),
                                  ("data/hullmods/hull_mods.csv", {HULLMOD})):
            foreign_ids = {row["id"] for row in read_csv(sep / relative)}
            require(not own_ids.intersection(foreign_ids), f"SEP registration collision: {relative}")
        for path in definitions:
            require(not (sep / path.relative_to(root)).exists(), f"Port shadows an SEP asset: {path}")
    if jar:
        with ZipFile(jar) as archive:
            names = archive.namelist()
            require(not any(name.startswith(("tidal/", "kotlin/", "org/magiclib/", "org/lazywizard/",
                                             "second_in_command/", "com/fs/starfarer/")) for name in names),
                    "Built mod JAR bundles a foreign dependency")
            for source in sources:
                expected = f"chiefnavigator/dwellers/{source.stem}.class"
                require(expected in names, f"Built JAR is missing ported class {expected}")
            for name in names:
                if name.startswith("chiefnavigator/dwellers/") and name.endswith(".class"):
                    raw = archive.read(name)
                    require(not any(link in raw for link in (b"tidal/shroud", b"kotlin/", b"org/magiclib/",
                                                             b"org/lazywizard/", b"second_in_command/")),
                            f"Ported class links to a foreign dependency: {name}")
    print(f"PASS: three independent enemy hulls, three systems, {weapons_fitted} vanilla weapon fits; "
          f"{image_count} ported image references resolve; TIDAL credits and namespace isolation verified")
    if baseline:
        print(f"PASS: no new/modified weapon package relative to {baseline}")
    if sep:
        print("PASS: no hull/system/hullmod or definition-path collisions with installed SEP")
    if jar:
        print("PASS: built enemy classes present without bundled or linked SEP/extra-library dependencies")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--core", type=Path)
    parser.add_argument("--baseline", help="pre-port Git commit to protect the weapon package")
    parser.add_argument("--sep", type=Path, help="optional installed SEP directory for collision checks")
    parser.add_argument("--jar", type=Path, help="optional built JAR for compiled enemy dependency checks")
    args = parser.parse_args()
    root = args.root.resolve()
    validate(root, find_core(root, args.core), args.baseline, args.sep, args.jar)


if __name__ == "__main__":
    main()
