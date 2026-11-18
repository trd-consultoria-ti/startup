from PIL import Image

aw_curr = Image.open('site/assets/logo-animal-world.png')
print('Current site/assets/logo-animal-world.png:')
print('Size:', aw_curr.size)
print('Mode:', aw_curr.mode)

aw_orig = Image.open('E:/projetos/Animal World/sysPet2/docs/assets/images/logo.png')
print('\nOriginal E:/projetos/Animal World/sysPet2/docs/assets/images/logo.png:')
print('Size:', aw_orig.size)
print('Mode:', aw_orig.mode)

# Let's inspect what colors/regions are in aw_orig
# Does aw_orig have text 'Animal World' or is it just the character mascot or both?
print('Orig bbox:', aw_orig.getbbox())
