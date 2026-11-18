from PIL import Image

# Check the dimensions of the saved screenshots
for name in ['screen_trustbar', 'screen_sistemas_grid2', 'screen_cases_grid3', 'screen_sistemas_grid2_light']:
    im = Image.open(f'scratch/{name}.png')
    print(f'{name}: size={im.size}')
