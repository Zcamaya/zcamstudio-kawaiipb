from PIL import Image
import os
base = os.path.join('app', 'src', 'main', 'assets', 'layouts')
names = ['strip_2x4_base.png', 'strip_2x3_base.png', 'strip_2x2_base.png']
for name in names:
    path = os.path.join(base, name)
    with Image.open(path) as img:
        print(name, img.size, img.mode)
        pixels = img.load()
        w, h = img.size
        # detect non-white or opaque pixels
        xs = []
        ys = []
        for y in range(h):
            for x in range(w):
                if img.mode == 'RGBA':
                    if pixels[x,y][3] != 0:
                        xs.append(x); ys.append(y)
                else:
                    if pixels[x,y] != (255,255,255):
                        xs.append(x); ys.append(y)
        if xs and ys:
            print('bounds:', min(xs), min(ys), max(xs), max(ys))
        else:
            print('bounds: none')
        print()