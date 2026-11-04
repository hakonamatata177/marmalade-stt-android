#!/usr/bin/env python3
"""Lager modellisten appen leser (norske-modeller.json).

  fragment: én modell → én oppføring (filene er allerede lastet opp til releasen)
  merge:    eksisterende liste + nye oppføringer → ny liste (samme id erstattes)

Formatet leses av app/src/main/java/app/marmalade/stt/install/ExtraEngines.kt.
"""

import argparse
import hashlib
import json
import sys
from pathlib import Path

BESKRIVELSER = {
    "tiny": "Minste NB-Whisper fra Nasjonalbiblioteket. Svært rask, men gjør flere feil.",
    "base": "Liten og rask NB-Whisper fra Nasjonalbiblioteket.",
    "small": "NB-Whisper fra Nasjonalbiblioteket. God balanse mellom fart og treffsikkerhet.",
    "medium": "NB-Whisper fra Nasjonalbiblioteket. Mer treffsikker, men tregere.",
    "large": "Største NB-Whisper fra Nasjonalbiblioteket. Mest treffsikker, men treg på telefon.",
}
# Re-dekoding av ytringen hvert 200. ms for løpende tekst: bare for de små.
LIVE = {"tiny", "base", "small"}

ROLES = [("ENCODER", "encoder.int8.onnx"), ("DECODER", "decoder.int8.onnx"), ("TOKENS", "tokens.txt")]


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def size_of(engine_id: str) -> str:
    for size in BESKRIVELSER:
        if f"-{size}" in engine_id:
            return size
    return ""


def display_name(engine_id: str) -> str:
    rest = engine_id.removeprefix("nb-whisper").strip("-")
    return " ".join(["NB-Whisper"] + [p.capitalize() for p in rest.split("-") if p])


def fragment(a):
    info = json.loads(Path(a.info).read_text())
    size = size_of(a.id)
    files = []
    for role, name in ROLES:
        p = Path(a.dir) / name
        files.append({
            "role": role,
            "name": name,
            "url": f"{a.base_url}/{a.prefix}-{name}",
            "sha256": sha256(p),
            "size": p.stat().st_size,
        })
    entry = {
        "id": a.id,
        "name": display_name(a.id),
        "description": BESKRIVELSER.get(size, f"Whisper-modell for norsk fra {info['repo']}."),
        "family": "WHISPER",
        "languages": ["nb", "nn"],
        "languageOptions": {"codes": ["nb", "nn"], "default": "nb"},
        "livePartials": size in LIVE,
        "license": "Apache-2.0",
        "source": f"https://huggingface.co/{info['repo']}",
        "revision": info["revision"],
        "files": files,
    }
    Path(a.out).write_text(json.dumps(entry, indent=2, ensure_ascii=False))
    print(json.dumps(entry, indent=2, ensure_ascii=False))


ORDER = ["tiny", "base", "small", "medium", "large"]


def merge(a):
    engines = {}
    if a.existing and Path(a.existing).is_file():
        for e in json.loads(Path(a.existing).read_text()).get("engines", []):
            engines[e["id"]] = e
    for f in a.fragments:
        e = json.loads(Path(f).read_text())
        engines[e["id"]] = e
    ordered = sorted(engines.values(), key=lambda e: (ORDER.index(size_of(e["id"])) if size_of(e["id"]) else 99, e["id"]))
    Path(a.out).write_text(json.dumps({"version": 1, "engines": ordered}, indent=2, ensure_ascii=False) + "\n")
    print(f"{len(ordered)} modeller: {[e['id'] for e in ordered]}")


def revision_of(a):
    """Skriv revisjonen listen har for --id (tom hvis ingen), så uendrede modeller hoppes over."""
    if Path(a.existing).is_file():
        for e in json.loads(Path(a.existing).read_text()).get("engines", []):
            if e["id"] == a.id:
                print(e.get("revision", ""))
                return
    print("")


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    f = sub.add_parser("fragment")
    f.add_argument("--info", required=True)
    f.add_argument("--id", required=True)
    f.add_argument("--dir", required=True)
    f.add_argument("--base-url", required=True)
    f.add_argument("--prefix", required=True)
    f.add_argument("--out", required=True)
    m = sub.add_parser("merge")
    m.add_argument("--existing")
    m.add_argument("--out", required=True)
    m.add_argument("fragments", nargs="*")
    r = sub.add_parser("revision")
    r.add_argument("--existing", required=True)
    r.add_argument("--id", required=True)
    a = ap.parse_args()
    {"fragment": fragment, "merge": merge, "revision": revision_of}[a.cmd](a)


if __name__ == "__main__":
    sys.exit(main())
