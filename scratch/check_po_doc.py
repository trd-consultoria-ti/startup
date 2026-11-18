import re
with open(r'E:\projetos\Animal World\documentacao teste\Roteiro_Homologacao_PO.html', encoding='utf-8') as f:
    text = f.read()

imgs = re.findall(r'<img[^>]+src=["\']([^"\']+)["\']', text)
print('Images in HTML doc:', set(imgs))
