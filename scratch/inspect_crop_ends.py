from PIL import Image

im = Image.open('C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/.user_uploaded/uploaded_media_1795011261055.png')
print('Image width:', im.width, 'height:', im.height)

# Crop the left side (first 40 px) and right side (last 40 px)
left = im.crop((0, 0, 45, im.height))
right = im.crop((im.width - 45, 0, im.width, im.height))

left.resize((left.width * 5, left.height * 5), Image.Resampling.NEAREST).save('scratch/snippet_left.png')
right.resize((right.width * 5, right.height * 5), Image.Resampling.NEAREST).save('scratch/snippet_right.png')

print('Saved snippet_left.png and snippet_right.png')
