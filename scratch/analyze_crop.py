from PIL import Image

im = Image.open('scratch/crop_badge.png')
w, h = im.size
print(f"Crop image size: {w}x{h}")

# Let's inspect rows around y=120 to y=145 to see where the white card ends and what follows
# The card is white (255, 255, 255)
for y in range(h - 30, h, 2):
    row_pixels = [im.getpixel((x, y)) for x in range(50, 150, 10)]
    # print sample colors in this row
    print(f"y={y}: {[p[:3] for p in row_pixels[:4]]}")
