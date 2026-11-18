from PIL import Image

im = Image.open('scratch/screen_sistemas_grid2.png')
# Animal world is the right card (grid--2), so x is from ~600 to 1230, y from 0 to 150
crop_aw = im.crop((616, 0, 1232, 130))
crop_aw.save('scratch/crop_sistemas_aw_header.png')

crop_as = im.crop((0, 0, 616, 130))
crop_as.save('scratch/crop_sistemas_as_header.png')

print('Cropped successfully')
