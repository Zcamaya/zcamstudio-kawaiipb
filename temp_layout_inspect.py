from pathlib import Path
from PIL import Image
import numpy as np

files = [
    'app/src/main/assets/layouts/pb_card_uncut_2p_stack.png',
    'app/src/main/assets/layouts/pb_card_uncut_3p_left.png',
    'app/src/main/assets/layouts/pb_card_uncut_3p_right.png',
    'app/src/main/assets/layouts/pb_card_uncut_4p_banner.png',
    'app/src/main/assets/layouts/pb_split_horiz_2p_grid.png'
]

for f in files:
    p = Path(f)
    im = Image.open(p).convert('L')
    arr = np.array(im)
    h, w = arr.shape
    mask = arr < 128
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

    boxes.sort(key=lambda b: (b[1], b[0]))
    print(p.name, 'size', w, h, 'components', len(boxes))
    for i, b in enumerate(boxes):
        print(' ', i, b, 'sz', b[2] - b[0] + 1, b[3] - b[1] + 1)
    print('---')
