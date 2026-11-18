from PIL import Image

im = Image.open('scratch/crop_clean_nav_links.png')
w, h = im.size

orange_pixels = 0
for x in range(w):
    for y in range(h):
        r, g, b = im.getpixel((x, y))[:3]
        # Check for orange / amber (high red, medium green, low blue)
        if r > 180 and g > 100 and b < 60:
            orange_pixels += 1

print("Orange pixels found in clean nav links area:", orange_pixels)
