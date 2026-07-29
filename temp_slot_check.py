from PIL import Image
import numpy as np
from pathlib import Path
p = Path('d:/My Program/KawaiiPB/app/src/main/assets/layouts/strip_2x4_base.png')
img = np.array(Image.open(p).convert('RGB'))
# dark border detection
mask = np.any(img < 200, axis=2)
cols = np.where(np.any(mask,axis=0))[0]
rows = np.where(np.any(mask,axis=1))[0]
# group contiguous runs

def groups(vals):
    out=[]
    if len(vals)==0:
        return out
    start=vals[0]
    prev=vals[0]
    for v in vals[1:]:
        if v!=prev+1:
            out.append((start,prev))
            start=v
        prev=v
    out.append((start,prev))
    return out

col_groups = groups(cols)
row_groups = groups(rows)
print('col_groups', col_groups)
print('row_groups', row_groups)
# estimate interior frames as just inside border
for i, cg in enumerate(col_groups):
    print('cg', i, cg, 'width', cg[1]-cg[0]+1)
for j, rg in enumerate(row_groups):
    print('rg', j, rg, 'height', rg[1]-rg[0]+1)
# approximate slot positions from top-left of border plus 2
x_positions = [cg[0] for cg in col_groups]
y_positions = [rg[0] for rg in row_groups]
print('approx x', x_positions)
print('approx y', y_positions)
# compute interior white bounds for each slot by scanning inside the border
for ix in range(len(col_groups)):
    for iy in range(len(row_groups)):
        x0 = col_groups[ix][0]
        x1 = col_groups[ix][1]
        y0 = row_groups[iy][0]
        y1 = row_groups[iy][1]
        interior = mask[y0:y1+1, x0:x1+1]
        # shrink to first all-false inside
        while x0 < x1 and np.any(mask[y0:y1+1,x0]):
            x0 += 1
        while x1 > x0 and np.any(mask[y0:y1+1,x1]):
            x1 -= 1
        while y0 < y1 and np.any(mask[y0,x0:x1+1]):
            y0 += 1
        while y1 > y0 and np.any(mask[y1,x0:x1+1]):
            y1 -= 1
        print('slot', ix+1, iy+1, 'interior', x0, y0, x1, y1, 'w', x1-x0+1, 'h', y1-y0+1)
