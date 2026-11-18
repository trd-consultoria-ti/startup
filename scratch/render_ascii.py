from PIL import Image

def to_ascii(path, thresh=100):
    im = Image.open(path).convert('L')
    # downsample slightly if needed
    w, h = im.size
    print(f'=== {path} ({w}x{h}) ===')
    for y in range(0, h, 2):
        row = ''
        for x in range(0, w, 2):
            val = im.getpixel((x, y))
            row += '#' if val > thresh else ' '
        print(row)

to_ascii('scratch/snippet_left.png', 120)
to_ascii('scratch/snippet_right.png', 120)
