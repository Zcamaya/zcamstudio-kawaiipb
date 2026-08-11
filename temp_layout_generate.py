import argparse
import json
from pathlib import Path

from PIL import Image
import numpy as np


def parse_args():
    parser = argparse.ArgumentParser(
        description="Auto-generate layout JSON files from layout PNG assets."
    )
    parser.add_argument(
        "--assets-dir",
        default="app/src/main/assets/layouts",
        help="Directory containing layout PNG assets.",
    )
    parser.add_argument(
        "--output-dir",
        default="app/src/main/assets/layouts",
        help="Directory to write generated JSON files.",
    )
    parser.add_argument(
        "--pattern",
        default="*.png",
        help="Glob pattern for layout PNG files to process.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite existing JSON files if they already exist.",
    )
    return parser.parse_args()


def load_mask(image: Image.Image) -> np.ndarray:
    if image.mode == "RGBA":
        alpha = np.array(image.split()[-1])
        grayscale = np.array(image.convert("L"))
        return (alpha > 0) & (grayscale < 245)

    arr = np.array(image.convert("L"))
    return arr < 245


def find_components(mask: np.ndarray) -> list[tuple[int, int, int, int]]:
    h, w = mask.shape
    visited = np.zeros(mask.shape, bool)
    boxes = []

    for y in range(h):
        for x in range(w):
            if mask[y, x] and not visited[y, x]:
                stack = [(y, x)]
                visited[y, x] = True
                miny = maxy = y
                minx = maxx = x
                while stack:
                    cy, cx = stack.pop()
                    for ny, nx in ((cy + 1, cx), (cy - 1, cx), (cy, cx + 1), (cy, cx - 1)):
                        if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not visited[ny, nx]:
                            visited[ny, nx] = True
                            stack.append((ny, nx))
                            miny = min(miny, ny)
                            maxy = max(maxy, ny)
                            minx = min(minx, nx)
                            maxx = max(maxx, nx)
                boxes.append((minx, miny, maxx, maxy))

    return boxes


def filter_slot_boxes(boxes: list[tuple[int, int, int, int]], canvas_size: tuple[int, int]) -> list[tuple[int, int, int, int]]:
    w, h = canvas_size
    filtered = []
    for x0, y0, x1, y1 in boxes:
        width = x1 - x0 + 1
        height = y1 - y0 + 1
        area = width * height
        if area < 10000:
            continue
        if x0 < 8 or y0 < 8 or x1 > w - 9 or y1 > h - 9:
            continue
        if width > w * 0.95 and height > h * 0.95:
            continue
        filtered.append((x0, y0, x1, y1))
    return filtered


def cluster_positions(values: list[float], tolerance: float = 0.1) -> list[float]:
    if not values:
        return []
    clusters = [[values[0]]]
    for value in values[1:]:
        if abs(value - np.mean(clusters[-1])) <= tolerance:
            clusters[-1].append(value)
        else:
            clusters.append([value])
    return [float(np.mean(cluster)) for cluster in clusters]


def strip_assignment(file_stem: str, slots: list[dict], canvas: tuple[int, int]) -> list[dict]:
    width, height = canvas
    names = file_stem.lower()
    use_column = None
    if any(k in names for k in ["vert", "left", "right"]):
        use_column = True
    elif any(k in names for k in ["horiz", "banner", "stack"]):
        use_column = False
    else:
        row_centers = cluster_positions([slot["y"] + slot["height"] / 2 for slot in slots], tolerance=height * 0.1)
        col_centers = cluster_positions([slot["x"] + slot["width"] / 2 for slot in slots], tolerance=width * 0.1)
        use_column = len(col_centers) > len(row_centers)

    strip_threshold = width / 2 if use_column else height / 2
    for slot in slots:
        center = slot["x"] + slot["width"] / 2 if use_column else slot["y"] + slot["height"] / 2
        slot["strip"] = 1 if center < strip_threshold else 2
    return slots


def build_json(name: str, canvas: tuple[int, int], slots: list[dict]) -> dict:
    return {
        "version": 1,
        "layoutId": name,
        "layoutName": name.replace("_", " ").title(),
        "layoutType": name,
        "paper": {
            "width": canvas[0],
            "height": canvas[1],
            "unit": "px",
            "dpi": 300,
            "orientation": "portrait",
        },
        "output": {
            "doubleStrip": True,
            "stripWidth": canvas[0] // 2,
            "stripHeight": canvas[1],
        },
        "backgroundImage": f"layouts/{name}.png",
        "render": {
            "backgroundColor": "#FFFFFF",
            "photoFit": "cover",
            "photoShape": "rectangle",
            "cornerRadius": 0,
            "clipPhotos": True,
        },
        "safeArea": {
            "left": 40,
            "top": 40,
            "right": 40,
            "bottom": 40,
        },
        "brandingArea": {
            "enabled": True,
            "height": 280,
        },
        "slots": slots,
    }


def main() -> None:
    args = parse_args()
    assets_dir = Path(args.assets_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    png_files = sorted(assets_dir.glob(args.pattern))
    if not png_files:
        raise SystemExit(f"No PNG files found in {assets_dir!r} matching {args.pattern!r}.")

    for png in png_files:
        if png.parent.name != assets_dir.name:
            continue
        if png.name.startswith("."):
            continue

        stem = png.stem
        json_path = output_dir / f"{stem}.json"
        if json_path.exists() and not args.force:
            print(f"Skipping existing JSON: {json_path.name} (use --force to overwrite)")
            continue

        print(f"Processing {png.name}...")
        image = Image.open(png)
        canvas = image.size
        mask = load_mask(image)
        components = find_components(mask)
        slot_boxes = filter_slot_boxes(components, canvas)

        if not slot_boxes:
            print(f"  WARNING: no valid slot boxes found for {png.name}. Skipping.")
            continue

        slots = []
        for idx, (x0, y0, x1, y1) in enumerate(sorted(slot_boxes, key=lambda b: (b[1], b[0])), start=1):
            slots.append(
                {
                    "id": idx,
                    "strip": 1,
                    "x": x0,
                    "y": y0,
                    "width": x1 - x0 + 1,
                    "height": y1 - y0 + 1,
                    "rotation": 0,
                    "visible": True,
                }
            )

        slots = strip_assignment(stem, slots, canvas)
        json_data = build_json(stem, canvas, slots)
        json_path.write_text(json.dumps(json_data, indent=2))
        print(f"  Created {json_path.name} with {len(slots)} slots.")


if __name__ == "__main__":
    main()
