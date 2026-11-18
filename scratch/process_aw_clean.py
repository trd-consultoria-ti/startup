from PIL import Image, ImageDraw
import numpy as np

src_path = 'E:/projetos/Animal World/sysPet2/docs/assets/images/logo.png'
im = Image.open(src_path)

# Let's find the circle center and radius of the emblem
# Bounding box of full image
bbox = im.getbbox()
print('Full bbox:', bbox)

# The emblem is in the upper part (y: 127 to 2600)
# Let's inspect the emblem region
emblem_region = im.crop((bbox[0], bbox[1], bbox[2], 2620))
em_box = emblem_region.getbbox()
print('Emblem box within region:', em_box)

# Crop emblem tightly
emblem = emblem_region.crop(em_box)
print('Emblem size:', emblem.size)

# Let's create a perfect high-res square emblem (512x512)
# Make it square by centering on transparent canvas
max_dim = max(emblem.size)
square_emblem = Image.new('RGBA', (max_dim, max_dim), (0, 0, 0, 0))
offset_x = (max_dim - emblem.width) // 2
offset_y = (max_dim - emblem.height) // 2
square_emblem.paste(emblem, (offset_x, offset_y), emblem)

# Resize to high-DPI 256x256 and 512x512
emblem_512 = square_emblem.resize((512, 512), Image.Resampling.LANCZOS)
emblem_256 = square_emblem.resize((256, 256), Image.Resampling.LANCZOS)

emblem_512.save('scratch/aw_emblem_512.png')
emblem_256.save('scratch/aw_emblem_256.png')

# Also let's inspect the banner text part
banner_region = im.crop((bbox[0], 2550, bbox[2], bbox[3]))
b_box = banner_region.getbbox()
if b_box:
    banner = banner_region.crop(b_box)
    banner.thumbnail((800, 300), Image.Resampling.LANCZOS)
    banner.save('scratch/aw_banner_clean.png')
    print('Banner clean size:', banner.size)

print('Done!')
