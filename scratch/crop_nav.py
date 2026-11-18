from PIL import Image

im = Image.open('scratch/nav_clean_dark_sistemas.png')
print("Image size:", im.size)
# Crop the nav links area
crop_links = im.crop((200, 10, 1000, 65))
crop_links.save('scratch/crop_clean_nav_links.png')
print("Cropped nav links saved!")
