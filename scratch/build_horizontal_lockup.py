from PIL import Image

# Create horizontal lockup: Emblem on left, banner on right
emblem = Image.open('scratch/aw_emblem_512.png')
banner = Image.open('scratch/aw_banner_clean.png')

# Target height: 200px
target_h = 200
emblem_resized = emblem.resize((int(emblem.width * (target_h / emblem.height)), target_h), Image.Resampling.LANCZOS)
banner_resized = banner.resize((int(banner.width * ((target_h * 0.75) / banner.height)), int(target_h * 0.75)), Image.Resampling.LANCZOS)

gap = 30
total_w = emblem_resized.width + gap + banner_resized.width
total_h = target_h

horiz = Image.new('RGBA', (total_w, total_h), (0, 0, 0, 0))
horiz.paste(emblem_resized, (0, 0), emblem_resized)
banner_y = (target_h - banner_resized.height) // 2
horiz.paste(banner_resized, (emblem_resized.width + gap, banner_y), banner_resized)

horiz.save('scratch/aw_horizontal_lockup.png')
print('Horizontal lockup saved! Size:', horiz.size)
