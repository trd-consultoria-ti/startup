import urllib.request
import re

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) TRD-Consultoria/1.0'}

# 1. Download ALERGS logo
try:
    url = 'https://ww4.al.rs.gov.br/themes/custom/alergs_custom/logo-alergs.png'
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=10) as r:
        with open(r'E:\projetos\TRD- CONSUTORIA DE TI\site\assets\logo-alergs.png', 'wb') as f:
            f.write(r.read())
    print('Downloaded logo-alergs.png!')
except Exception as e:
    print('Error downloading ALERGS logo:', e)

# 2. Extract and save SENAR SVG from cnabrasil.org.br
try:
    url = 'https://www.cnabrasil.org.br/'
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=10) as r:
        html = r.read().decode('utf-8', errors='ignore')
        # Find the <svg class="logo-senar" ... </svg>
        idx = html.find('class="logo-senar"')
        if idx != -1:
            start = html.rfind('<svg', 0, idx)
            end = html.find('</svg>', idx) + 6
            svg_content = html[start:end]
            with open(r'E:\projetos\TRD- CONSUTORIA DE TI\site\assets\logo-senar.svg', 'w', encoding='utf-8') as f:
                f.write(svg_content)
            print('Extracted and saved logo-senar.svg!')
        else:
            print('Could not find class logo-senar')
except Exception as e:
    print('Error SENAR:', e)
