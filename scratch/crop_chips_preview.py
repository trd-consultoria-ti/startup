from PIL import Image

im = Image.open('scratch/test_std_chips_cases_dark.png')
# Let's crop the header area of the first 3 cards in the grid
# cases section width is 1440
crop_headers = im.crop((100, 150, 1340, 450))
crop_headers.save('scratch/std_chips_row1_preview.png')
print('Preview saved to scratch/std_chips_row1_preview.png')
