from PIL import Image

im = Image.open('scratch/trustbar_current.png')
# Crop around one badge in the trustbar
# Bounding box of marquee was y: 1057 (relative to page)
# Trustbar y was 982, height 220
# So marquee y within trustbar is 1057.5 - 982.8 = 74.7px, height ~108px
# Badge bottom is at 74.7 + 108.3 = 183px within the 220px trustbar.
crop_badge = im.crop((100, 60, 400, 205))
crop_badge.save('scratch/crop_badge.png')
print('Cropped badge saved to scratch/crop_badge.png')
