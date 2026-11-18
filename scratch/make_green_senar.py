# Create green SENAR SVG and check AlertaSeg dark logo
with open(r'E:\projetos\TRD- CONSUTORIA DE TI\site\assets\logo-senar.svg', 'r', encoding='utf-8') as f:
    text = f.read()

green_svg = text.replace('fill="white"', 'fill="#006C4C"').replace('fill="none"', 'fill="none"')
with open(r'E:\projetos\TRD- CONSUTORIA DE TI\site\assets\logo-senar-green.svg', 'w', encoding='utf-8') as f:
    f.write(green_svg)
print('Saved logo-senar-green.svg!')
