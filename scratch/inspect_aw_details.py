from PIL import Image

im = Image.open('E:/projetos/Animal World/sysPet2/docs/assets/images/logo.png')
print('Dimensions:', im.size)

# Let's inspect the colors and content
# Let's see if there is text in the image or if it is just a circular badge
# Let's make a clear 800x800 preview saved to scratch/aw_preview.png
thumb = im.resize((800, 800), Image.Resampling.LANCZOS)
thumb.save('scratch/aw_preview.png')

# Let's also check if there is an icon part vs text part
# Let's calculate horizontal and vertical projection of alpha/colors
bbox = im.getbbox()
print('Bounding box:', bbox)
