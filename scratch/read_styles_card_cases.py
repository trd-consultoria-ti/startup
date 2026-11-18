with open('site/styles.css', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i in range(740, 860):
    if i < len(lines):
        print(f'{i+1}: {lines[i]}', end='')
