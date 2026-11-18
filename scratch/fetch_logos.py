import urllib.request
import re
import os
import json

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) TRD-Consultoria/1.0'}

# 1. Inspect ww4.al.rs.gov.br
try:
    req = urllib.request.Request('https://ww4.al.rs.gov.br/', headers=headers)
    with urllib.request.urlopen(req, timeout=10) as r:
        html = r.read().decode('utf-8', errors='ignore')
        for line in html.split('\n'):
            if 'logo' in line.lower() or 'alrs' in line.lower() or 'brasao' in line.lower():
                print('ALRS Line:', line.strip()[:150])
except Exception as e:
    print('Error ALRS:', e)

# 2. Check SENAR
try:
    req = urllib.request.Request('https://www.cnabrasil.org.br/', headers=headers)
    with urllib.request.urlopen(req, timeout=10) as r:
        html = r.read().decode('utf-8', errors='ignore')
        for line in html.split('\n'):
            if 'senar' in line.lower() and ('src=' in line or 'svg' in line or 'png' in line):
                print('SENAR Line:', line.strip()[:150])
except Exception as e:
    print('Error SENAR:', e)
