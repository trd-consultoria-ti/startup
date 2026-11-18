from PIL import Image

aw_curr = Image.open('site/assets/logo-animal-world.png')
# let's save a large version to see what is in aw_curr
print('aw_curr size:', aw_curr.size)

# Let's inspect where in the original 3375x3375 this came from
# Let's resize original to 500x500 and check
aw_orig = Image.open('E:/projetos/Animal World/sysPet2/docs/assets/images/logo.png')
thumb = aw_orig.resize((500, 500), Image.Resampling.LANCZOS)
thumb.save('scratch/aw_orig_500.png')

# What about logo.original.png?
orig_jpg = Image.open('E:/projetos/Animal World/sysPet2/docs/assets/images/logo.original.png')
thumb_jpg = orig_jpg.resize((500, 500), Image.Resampling.LANCZOS)
thumb_jpg.save('scratch/aw_orig_jpg_500.jpg')

print('Thumbnails generated!')
