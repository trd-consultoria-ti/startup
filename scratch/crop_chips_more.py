from PIL import Image

im = Image.open('scratch/test_std_chips_cases_dark.png')
# Row 2 (cards 4, 5, 6)
crop_row2 = im.crop((100, 480, 1340, 780))
crop_row2.save('scratch/std_chips_row2_preview.png')

# Sistemas cards (AlertaSeg and Animal World side-by-side)
im_sistemas = Image.open('scratch/test_std_chips_sistemas_dark.png')
# Crop the two cards with logos
crop_sistemas = im_sistemas.crop((100, 200, 1340, 500))
crop_sistemas.save('scratch/std_chips_sistemas_preview.png')
print('Previews saved!')
