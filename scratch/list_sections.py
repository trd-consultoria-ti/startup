with open('site/index.html', encoding='utf-8') as f:
    text = f.read()

import re
sections = re.findall(r'<section[^>]*id="([^"]+)"', text)
print('Sections:', sections)
