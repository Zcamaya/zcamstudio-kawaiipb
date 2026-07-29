from pathlib import Path
from PIL import Image
import numpy as np

path = Path('d:/My Program/KawaiiPB/app/src/main/assets/layouts/strip_2x4_base.png')
img = np.array(Image.open(path).convert('RGB'))
mask = np.max(img, axis=2) < 80
cols = np.where(np.any(mask, axis=0))[0]
rows = np.where(np.any(mask, axis=1))[0]

def groups(vals):
    out = []
    if len(vals) == 0:
        return out
    start = int(vals[0])
    prev = int(vals[0])
    for x in vals[1:]:
        x = int(x)
        if x != prev + 1:
            out.append((start, prev))
            start = x
        prev = x
    out.append((start, prev))
    return out

col_groups = groups(cols)
row_groups = groups(rows)
print('col_groups', col_groups)
print('row_groups', row_groups)

# print interior candidate rectangles as used in app (x,y,width,height)
rects = []
for ix, (x0, x1) in enumerate(col_groups):
    for iy, (y0, y1) in enumerate(row_groups):
        # shrink inside border by 1 pixel
        rects.append((ix + 1, iy + 1, x0 + 1, y0 + 1, x1 - x0 - 1, y1 - y0 - 1, x1 - 1, y1 - 1))

for r in rects:
    print('cell', r[0], r[1], 'x', r[2], 'y', r[3], 'w', r[4], 'h', r[5], 'x2', r[6], 'y2', r[7])

# print a few sample rows for the top-left region
for y in range(30, 55):
    row = img[y, 30:75]
    dark = [i + 30 for i, p in enumerate(row) if max(p) < 80]
    if dark:
        print('row', y, dark[:10])

for x in range(30, 75):
    col = img[30:75, x]
    dark = [i + 30 for i, p in enumerate(col) if max(p) < 80]
    if dark:
        print('col', x, dark[:10])
