from PIL import Image
import os

base = os.path.join('app', 'src', 'main', 'assets', 'layouts')
names = ['strip_2x4_base.png', 'strip_2x3_base.png', 'strip_2x2_base.png']

for name in names:
    path = os.path.join(base, name)
    with Image.open(path) as img:
        img = img.convert('RGBA')
        w, h = img.size
        print('\nFILE', name, 'size', w, h)
        pixels = img.load()
        rows = [y for y in range(h) if any(pixels[x,y][3] > 0 and pixels[x,y][:3] != (255,255,255) for x in range(w))]
        cols = [x for x in range(w) if any(pixels[x,y][3] > 0 and pixels[x,y][:3] != (255,255,255) for y in range(h))]
        if rows:
            print('row range', rows[0], rows[-1], 'count', len(rows))
        if cols:
            print('col range', cols[0], cols[-1], 'count', len(cols))
        # collect all nonwhite pixel coordinates
        coords = [(x,y) for y in rows for x in range(w) if pixels[x,y][3] > 0 and pixels[x,y][:3] != (255,255,255)]
        if not coords:
            continue
        xs = [c[0] for c in coords]
        ys = [c[1] for c in coords]
        print('bounds', min(xs), min(ys), max(xs), max(ys))
        # compute vertical line segments by scanning columns
        active_cols = [x for x in range(w) if any(pixels[x,y][3] > 0 and pixels[x,y][:3] != (255,255,255) for y in range(h))]
        active_rows = [y for y in range(h) if any(pixels[x,y][3] > 0 and pixels[x,y][:3] != (255,255,255) for x in range(w))]
        print('active cols segments:')
        segs = []
        start = None
        for x in active_cols:
            if start is None:
                start = x
                end = x
            elif x == end + 1:
                end = x
            else:
                segs.append((start,end))
                start = x
                end = x
        if start is not None:
            segs.append((start,end))
        print(segs)
        print('active rows segments:')
        segs = []
        start = None
        for y in active_rows:
            if start is None:
                start = y
                end = y
            elif y == end + 1:
                end = y
            else:
                segs.append((start,end))
                start = y
                end = y
        if start is not None:
            segs.append((start,end))
        print(segs)
