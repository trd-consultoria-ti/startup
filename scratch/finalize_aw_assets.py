from PIL import Image

# 1. Clean centered circular emblem
emblem_raw = Image.open('scratch/aw_emblem.png')
bbox = emblem_raw.getbbox()
emblem_tight = emblem_raw.crop(bbox)

# Make 512x512 with equal padding
size = max(emblem_tight.size) + 20
canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
canvas.paste(emblem_tight, ((size - emblem_tight.width) // 2, (size - emblem_tight.height) // 2), emblem_tight)

out_emblem = canvas.resize((512, 512), Image.Resampling.LANCZOS)
out_emblem.save('site/assets/logo-animal-world.png')
print('Saved site/assets/logo-animal-world.png (512x512 RGBA)')

# 2. Also save lockup
lockup = Image.open('scratch/aw_horizontal_lockup.png')
lockup.save('site/assets/logo-animal-world-lockup.png')
print('Saved site/assets/logo-animal-world-lockup.png')
