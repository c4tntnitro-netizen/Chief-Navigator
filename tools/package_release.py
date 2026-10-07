"""Package the committed release with runtime assets and verify every ZIP member."""
from collections import defaultdict
from hashlib import sha1, sha256
import json
from pathlib import Path
import re
import subprocess
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo


ROOT = Path(__file__).resolve().parents[1]
CORE = ROOT.parent.parent / "starsector-core"
IMAGE = re.compile(r"graphics/[\w./ -]+\.(?:png|jpe?g|gif|dds)")


def git(*args):
    return subprocess.check_output(
        ["git", "-c", f"safe.directory={ROOT.as_posix()}", *args], cwd=ROOT
    )


def main():
    metadata = json.loads((ROOT / "mod_info.json").read_text(encoding="utf-8-sig"))
    version = metadata["version"]
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise SystemExit("A numeric release version is required")
    if metadata["jars"] != ["jars/ChiefNavigator.jar"]:
        raise SystemExit("Build the stable release jar before packaging")
    commit = git("rev-parse", "HEAD").decode().strip()
    committed = {}
    for entry in git("ls-tree", "-r", "-z", "HEAD").split(b"\0"):
        if entry:
            info, name = entry.split(b"\t", 1)
            committed[name.decode()] = info.split()[2].decode()

    def content(name):
        path = ROOT / name
        if not path.resolve().is_relative_to(ROOT) or not path.is_file():
            raise SystemExit(f"Missing or unsafe release input: {name}")
        data = path.read_bytes()
        blob = sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        # Git text normalization makes LF/CRLF equivalent for committed text.
        normalized = data.replace(b"\r\n", b"\n")
        text_blob = sha1(b"blob " + str(len(normalized)).encode() + b"\0" + normalized).hexdigest()
        if committed.get(name) not in {blob, text_blob}:
            raise SystemExit(f"Commit the release input before packaging: {name}")
        return data

    selected = {"mod_info.json", "README.md", "LICENSE", "THIRD_PARTY_NOTICES.md",
                "jars/ChiefNavigator.jar", "sounds/chief_navigator/AUDIO_SOURCES.md"}
    selected.update(name for name in committed if name.startswith("data/"))
    selected.update(name for name in committed if name.startswith("docs/") and name.endswith(".md"))
    images = set()
    for name in committed:
        if not (name.startswith("data/") or name.startswith("src/") and name.endswith(".java")):
            continue
        try:
            text = content(name).decode("utf-8-sig")
        except UnicodeDecodeError:
            continue  # Binary data files are still included in the runtime payload.
        images.update(IMAGE.findall(text))
        # Keep all matching local assets for dynamically composed path prefixes.
        for literal in re.findall(r'"(graphics/[^"\r\n]*)"', text):
            if IMAGE.fullmatch(literal):
                continue
            images.update(asset for asset in committed if asset.startswith(literal))
    for image in images:
        if image in committed:
            selected.add(image)
        elif not (CORE / image).is_file():
            raise SystemExit(f"Missing referenced image in mod and base game: {image}")

    sounds = json.loads(content("data/config/sounds.json").decode("utf-8-sig"))
    for cues in sounds["music"].values():
        for cue in cues:
            selected.add(f'{cue["source"]}/{cue["file"]}')
    for key, cues in sounds.items():
        if key != "music":
            selected.update(cue["file"] for cue in cues)
    selected.update(metadata["jars"])
    payloads = {name: content(name) for name in sorted(selected)}
    manifest = {"version": version, "commit": commit,
                "files": {name: sha256(data).hexdigest() for name, data in payloads.items()}}

    output = ROOT / "dist"
    output.mkdir(exist_ok=True)
    if not output.resolve().is_relative_to(ROOT):
        raise SystemExit("Release output must remain inside the project")
    archive = output / f"Chief-Navigator-{version}.zip"
    if archive.exists():
        raise SystemExit(f"Release archive already exists: {archive}")
    payloads["RELEASE_MANIFEST.json"] = (json.dumps(manifest, indent=2) + "\n").encode()
    prefix = "Chief Navigator/"
    with ZipFile(archive, "w", ZIP_DEFLATED, compresslevel=9) as zipped:
        for name, data in payloads.items():
            info = ZipInfo(prefix + name, (2026, 1, 1, 0, 0, 0))
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            zipped.writestr(info, data, compresslevel=9)
    with ZipFile(archive) as zipped:
        if zipped.testzip() is not None:
            raise SystemExit("Release archive failed CRC verification")
        if set(zipped.namelist()) != {prefix + name for name in payloads}:
            raise SystemExit("Unexpected archive contents")
        for name, data in payloads.items():
            if zipped.read(prefix + name) != data:
                raise SystemExit(f"Archive member verification failed: {name}")
    digest = sha256(archive.read_bytes()).hexdigest()
    checksum = output / f"Chief-Navigator-{version}.zip.sha256"
    if not checksum.resolve().is_relative_to(output.resolve()):
        raise SystemExit("Checksum output must remain inside the release directory")
    checksum.write_text(f"{digest}  {archive.name}\n", encoding="utf-8")
    totals = defaultdict(int)
    for name, data in payloads.items():
        totals[name.split("/", 1)[0]] += len(data)
    print(f"PASS: {len(payloads)} verified files; commit {commit}")
    print(f"ZIP: {archive} ({archive.stat().st_size / 1024 / 1024:.1f} MiB)")
    print(f"SHA256: {digest}")
    print("Uncompressed MiB:", {name: round(size / 1024 / 1024, 1) for name, size in totals.items()})


if __name__ == "__main__":
    main()
