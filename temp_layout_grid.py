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
    black = arr < 128
    row_counts = black.sum(axis=1)
    col_counts = black.sum(axis=0)

    row_lines = [i for i, count in enumerate(row_counts) if count > 20]
    col_lines = [i for i, count in enumerate(col_counts) if count > 20]

    def clusters(indices):
        groups = []
        for i in indices:
            if not groups or i > groups[-1][-1] + 1:
                groups.append([i])
            else:
                groups[-1].append(i)
        return [(g[0], g[-1]) for g in groups]

    row_clusters = clusters(row_lines)
    col_clusters = clusters(col_lines)
    print('FILE', p.name, 'size', w, h)
    print('row clusters', row_clusters)
    print('col clusters', col_clusters)
    print('---')
