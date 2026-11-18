from PIL import Image

for name in ['aw_full_clean', 'aw_emblem', 'aw_banner']:
    im = Image.open(f'scratch/{name}.png')
    print(f'{name}: size={im.size}, aspect_ratio={im.width/im.height:.2f}')
