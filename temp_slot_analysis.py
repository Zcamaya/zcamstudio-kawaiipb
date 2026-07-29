from PIL import Image
import numpy as np
from pathlib import Path
p = Path(r'd:/My Program/KawaiiPB/app/src/main/assets/layouts/strip_2x4_base.png')
img = np.array(Image.open(p).convert('RGB'))
# detect strong dark borders
mask = np.max(img, axis=2) < 40
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

# Calculate inner rectangle positions for each grid cell
for ci, cg in enumerate(col_groups):
    for ri, rg in enumerate(row_groups):
        x0, x1 = cg
        y0, y1 = rg
        # shrink in from border if needed
        while x0 < x1 and np.any(mask[y0:y1+1, x0]):
            x0 += 1
        while x1 > x0 and np.any(mask[y0:y1+1, x1]):
            x1 -= 1
        while y0 < y1 and np.any(mask[y0, x0:x1+1]):
            y0 += 1
        while y1 > y0 and np.any(mask[y1, x0:x1+1]):
            y1 -= 1
        print('cell', ci+1, ri+1, 'inner', x0, y0, x1, y1, 'w', x1-x0+1, 'h', y1-y0+1)

# print a few sample coordinates around the top-left cell border
for y in range(30, 60):
    row = mask[y, 30:70]
    if row.any():
        xs = np.where(row)[0]
        print('row', y, xs[0]+30, xs[-1]+30)

for x in range(30, 70):
    col = mask[30:70, x]
    if col.any():
        ys = np.where(col)[0]
        print('col', x, ys[0]+30, ys[-1]+30)
