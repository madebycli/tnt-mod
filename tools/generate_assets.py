#!/usr/bin/env python3
"""Generate original, deterministic 16px textures and their resource definitions."""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
COLORS = [
    ("ruby", 0xEB4D63), ("amber", 0xF59B42), ("solar", 0xEEC84A), ("lime", 0xA6D74E),
    ("emerald", 0x36B778), ("mint", 0x66D9B1), ("turquoise", 0x35C5C9), ("ocean", 0x4099D9),
    ("cobalt", 0x6176E8), ("violet", 0x9C70E8), ("orchid", 0xCA6BE0), ("pink", 0xED77B0),
    ("copper", 0xBF8055), ("silver", 0xB4C5D3), ("obsidian", 0x636580), ("ivory", 0xE4DFC8),
]


def write_json(path, value):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def png(path, pixels, size=16):
    def chunk(name, data):
        return struct.pack(">I", len(data)) + name + data + struct.pack(">I", zlib.crc32(name + data))
    data = b"".join(b"\0" + bytes(c for pixel in row for c in pixel) for row in pixels)
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
                 + chunk(b"IDAT", zlib.compress(data, 9)) + chunk(b"IEND", b""))


def shade(rgb, factor):
    return tuple(min(255, max(0, round(((rgb >> shift) & 255) * factor))) for shift in (16, 8, 0)) + (255,)


for name, rgb in COLORS:
    block = name + "_tnt"
    for face in ("side", "top", "bottom"):
        pixels = [[shade(rgb, (0.72 if x % 4 == 0 else 1.0) * (0.9 if y in (0, 15) else 1.0))
                   for x in range(16)] for y in range(16)]
        if face == "side":
            for y in range(5, 12):
                for x in range(16):
                    pixels[y][x] = (226, 231, 224, 255) if y not in (5, 11) else (167, 178, 176, 255)
            letters = {"T": ["111", "010", "010", "010", "010"], "N": ["101", "111", "111", "111", "101"]}
            for offset, letter in zip((2, 6, 10), "TNT"):
                for y, row in enumerate(letters[letter]):
                    for x, bit in enumerate(row):
                        if bit == "1":
                            pixels[y + 6][x + offset] = (31, 40, 49, 255)
        else:
            for x in range(16):
                pixels[3][x] = pixels[12][x] = shade(rgb, 0.55)
            if face == "top":
                for y in range(6, 10):
                    for x in range(6, 10):
                        pixels[y][x] = (43, 46, 54, 255)
                pixels[6][8] = (245, 206, 112, 255)
                pixels[5][9] = (245, 206, 112, 255)
            for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
                pixels[y][x] = (219, 226, 229, 255)
        png(f"assets/tnt_editions/textures/block/{block}_{face}.png", pixels)
    write_json(f"assets/tnt_editions/blockstates/{block}.json", {"variants": {"": {"model": f"tnt_editions:block/{block}"}}})
    write_json(f"assets/tnt_editions/models/block/{block}.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {face: f"tnt_editions:block/{block}_{face}" for face in ("side", "top", "bottom")}})
    write_json(f"assets/tnt_editions/items/{block}.json", {"model": {"type": "minecraft:model", "model": f"tnt_editions:block/{block}"}})
    write_json(f"data/tnt_editions/loot_table/blocks/{block}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": f"tnt_editions:{block}",
            "functions": [{"function": "minecraft:copy_state", "block": f"tnt_editions:{block}", "properties": ["strength", "range"]}]
        }]}]})

tuner = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
for y in range(1, 15):
    for x in range(3, 13):
        tuner[y][x] = (166, 135, 78, 255) if x in (3, 12) or y in (1, 14) else (31, 43, 62, 255)
for y, handle in ((4, 7), (7, 10), (10, 5)):
    for x in range(5, 11):
        tuner[y][x] = (95, 118, 143, 255)
    tuner[y][handle] = tuner[y - 1][handle] = (103, 236, 220, 255)
tuner[12][7] = tuner[12][8] = (229, 96, 130, 255)
png("assets/tnt_editions/textures/item/tnt_tuner.png", tuner)
write_json("assets/tnt_editions/models/item/tnt_tuner.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "tnt_editions:item/tnt_tuner"}})
write_json("assets/tnt_editions/items/tnt_tuner.json", {"model": {"type": "minecraft:model", "model": "tnt_editions:item/tnt_tuner"}})
write_json("data/tnt_editions/recipe/tnt_tuner.json", {
    "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["ICI", "RDR", " I "],
    "key": {"I": "minecraft:iron_ingot", "C": "minecraft:compass", "R": "minecraft:redstone", "D": "minecraft:diamond"},
    "result": {"id": "tnt_editions:tnt_tuner", "count": 1}})
write_json("data/tnt_editions/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"tnt_editions:{name}_tnt" for name, _ in COLORS]})
for language in ("de_de", "en_us"):
    path = f"assets/tnt_editions/lang/{language}.json"
    translations = json.loads((ROOT / path).read_text(encoding="utf-8"))
    for name, _ in COLORS:
        label = translations[f"color.tnt_editions.{name}"]
        translations[f"block.tnt_editions.{name}_tnt"] = translations["item.tnt_editions.edition"].replace("%s", label)
    write_json(path, translations)
print("Generated 16 TNT editions, tuner texture, models, loot tables and recipe.")
