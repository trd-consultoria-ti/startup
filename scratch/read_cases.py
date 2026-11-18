with open('site/index.html', 'r', encoding='utf-8') as f:
    lines = f.readlines()

in_cases = False
for i, line in enumerate(lines):
    if 'id="cases"' in line:
        in_cases = True
    if in_cases:
        if '<article class="card' in line or '<h3' in line or 'card__logo-chip' in line or 'card__brand-logo' in line:
            print(f'{i+1}: {line}', end='')
        if '</section>' in line:
            break
