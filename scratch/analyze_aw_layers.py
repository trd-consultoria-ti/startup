from PIL import Image
import numpy as np

im = Image.open('E:/projetos/Animal World/sysPet2/docs/assets/images/logo.png')
# Let's inspect where the main circular badge is and where the text is
arr = np.array(im)
# Find alpha > 0
alpha = arr[:, :, 3]
y_indices, x_indices = np.where(alpha > 20)
print(f'Y range: {y_indices.min()} to {y_indices.max()} (total height {im.height})')
print(f'X range: {x_indices.min()} to {x_indices.max()} (total width {im.width})')

# Let's check vertical slices: where does the icon end and text begin?
row_counts = np.sum(alpha > 20, axis=1)
# print profile
for y in range(0, im.height, 200):
    print(f'y={y}: non-zero pixels={row_counts[y]}')
