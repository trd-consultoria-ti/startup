with open('site/index.html', 'r', encoding='utf-8') as f:
    lines = f.readlines()

in_sistemas = False
for i, line in enumerate(lines):
    if 'id="sistemas"' in line:
        in_sistemas = True
    if in_sistemas:
        print(f'{i+1}: {line}', end='')
        if '</section>' in line:
            break
