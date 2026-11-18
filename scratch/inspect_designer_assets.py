from PIL import Image
import os

p1 = r'C:\Users\Thiago Duarte\.gemini\antigravity-cli\brain\9eb6f428-2b94-48f9-8194-60390591d5a1\animal_world_horizontal_lockup_1795010195867.jpg'
p2 = r'C:\Users\Thiago Duarte\.gemini\antigravity-cli\brain\9eb6f428-2b94-48f9-8194-60390591d5a1\animal_world_mascot_appicon_1795010163554.jpg'

im1 = Image.open(p1)
im2 = Image.open(p2)

# Make thumbnails to inspect
im1.thumbnail((400, 300))
im1.save('scratch/thumb_designer_lockup.jpg')

im2.thumbnail((300, 300))
im2.save('scratch/thumb_designer_appicon.jpg')

print('Thumbnails saved successfully!')
