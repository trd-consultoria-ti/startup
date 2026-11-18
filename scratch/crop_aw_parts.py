from PIL import Image

im = Image.open('E:/projetos/Animal World/sysPet2/docs/assets/images/logo.png')

# Entire image cropped to bbox
bbox = im.getbbox()
cropped_full = im.crop(bbox)
cropped_full.thumbnail((600, 600), Image.Resampling.LANCZOS)
cropped_full.save('scratch/aw_full_clean.png')

# Emblem part (circle/mascot)
emblem = im.crop((bbox[0], bbox[1], bbox[2], 2620))
emblem_box = emblem.getbbox()
if emblem_box:
    emblem = emblem.crop(emblem_box)
    emblem.thumbnail((400, 400), Image.Resampling.LANCZOS)
    emblem.save('scratch/aw_emblem.png')

# Banner part (text)
banner = im.crop((bbox[0], 2550, bbox[2], bbox[3]))
banner_box = banner.getbbox()
if banner_box:
    banner = banner.crop(banner_box)
    banner.thumbnail((600, 200), Image.Resampling.LANCZOS)
    banner.save('scratch/aw_banner.png')

print('Images saved: aw_full_clean.png, aw_emblem.png, aw_banner.png')
