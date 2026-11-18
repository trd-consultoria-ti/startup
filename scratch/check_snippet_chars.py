from PIL import Image

im = Image.open('C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/.user_uploaded/uploaded_media_1795011261055.png')
print('Size:', im.size)

# Let's inspect the RGB of non-background pixels
# Background is dark (~ #0B1120 or #0F172A)
# Let's print out what is visible
# Save as high-res with contrast enhanced
enh = im.resize((im.width * 3, im.height * 3), Image.Resampling.BOX)
enh.save('scratch/snippet_box3x.png')

print('Snippet saved')
