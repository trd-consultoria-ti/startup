from PIL import Image

banner = Image.open('scratch/aw_banner_clean.png')
print('Banner size:', banner.size)
print('Banner mode:', banner.mode)
