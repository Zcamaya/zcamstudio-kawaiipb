from pathlib import Path
from PIL import Image
import numpy as np

p = Path('app/src/main/assets/layouts/pb_card_uncut_3p_left.png')
im = Image.open(p).convert('L')
arr = np.array(im)
mask = arr < 128
h,w = arr.shape
print('size', w,h)
# detect top-left slot component via seed at top-left area
for y in range(h):
    for x in range(w):
        if mask[y,x] and not mask[y-1,x] if y>0 else False:
            pass

# compute thickness at left edge of component 0
# we'll inspect columns near x=35..45, rows near 33..40
for y in range(33, 45):
    line = ''.join('X' if mask[y,x] else '.' for x in range(30, 60))
    print(y, line)

for x in range(35, 45):
    col = ''.join('X' if mask[y,x] else '.' for y in range(33, 60))
    print('col', x, col[:30])
