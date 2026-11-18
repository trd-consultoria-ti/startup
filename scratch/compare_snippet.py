user_snippet = """<div class="trustbar__track">
        <!-- Track Set 1 -->
        <div class="trustbar__badge trustbar__badge--dou">
          <span class="trustbar__logo-chip"><img src="assets/logo-imprensa-nacional.png" alt="Imprensa Nacional DOU" class="trustbar__img" width="30" height="30" loading="lazy"></span>
          <span>Imprensa Nacional (DOU)</span>
        </div>
        <div class="trustbar__badge trustbar__badge--inep">
          <span class="trustbar__logo-chip"><img src="assets/logo-inep.svg" alt="INEP ENADE" class="trustbar__img" width="55" height="20" loading="lazy"></span>
          <span>INEP · ENADE</span>
        </div>
        <div class="trustbar__badge trustbar__badge--mjsp">
          <span class="trustbar__logo-chip"><img src="assets/logo-mjsp.svg" alt="Ministério da Justiça (MJSP)" class="trustbar__img" width="30" height="30" loading="lazy"></span>
          <span>Ministério da Justiça (MJSP)</span>
        </div>
        <div class="trustbar__badge trustbar__badge--serasa">
          <span class="trustbar__logo-chip"><img src="assets/logo-serasa.svg" alt="Serasa Experian" class="trustbar__img" width="70" height="22" loading="lazy"></span>
          <span>Serasa Experian · eConsig</span>
        </div>
        <div class="trustbar__badge trustbar__badge--cvm">
          <span class="trustbar__logo-chip"><img src="assets/logo-cvm.png" alt="Comissão de Valores Mobiliários (CVM)" class="trustbar__img" width="65" height="20" loading="lazy"></span>
          <span>CVM · Mercado de Capitais</span>
        </div>
        <div class="trustbar__badge trustbar__badge--alergs">
          <span class="trustbar__logo-chip"><img src="assets/logo-alergs.png" alt="ALERGS" class="trustbar__img" width="55" height="22" loading="lazy"></span>
          <span>ALERGS · Poder Legislativo</span>
        </div>
        <div class="trustbar__badge trustbar__badge--alertaseg">
          <span class="trustbar__logo-chip"><img src="assets/logo-alertaseg-dark.png" alt="AlertaSeg SST" class="trustbar__img" width="55" height="22" loading="lazy"></span>
          <span>AlertaSeg · Gestão SST</span>
        </div>
        <div class="trustbar__badge trustbar__badge--animalworld">
          <span class="trustbar__logo-chip"><img src="assets/logo-animal-world.png" alt="Animal World Pet ERP" class="trustbar__img" width="30" height="30" loading="lazy"></span>
          <span>Animal World · Hub Pet (sysPet2)</span>
        </div>

        <!-- Track Set 2 (Duplicado para loop infinito fluído estilo Kimi/Linear) -->
        <div class="trustbar__badge trustbar__badge--dou" aria-hidden="true">
          <span class="trustbar__logo-chip"><img src="assets/logo-imprensa-nacional.png" alt="" class="trustbar__img" width="30" height="30" loading="lazy"></span>
          <span>Imprensa Nacional (DOU)</span>
        </div>
        <div class="trustbar__badge trustbar__badge--inep" aria-hidden="true">
          <span class="trustbar__logo-chip"><img src="assets/logo-inep.svg" alt="" class="trustbar__img" width="55" height="20" loading="lazy"></span>
          <span>INEP · ENADE</span>
        </div>
        <div class="trustbar__badge trustbar__badge--mjsp" aria-hidden="true">
          <span class="trustbar__logo-chip"><img src="assets/logo-mjsp.svg" alt="" class="trustbar__img" width="30" height="30" loading="lazy"></span>
          <span>Ministério da Justiça (MJSP)</span>
        </div>
        <div class="trustbar__badge trustbar__badge--serasa" aria-hidden="true">
          <span class="trustbar__logo-chip"><img src="assets/logo-serasa.svg" alt="" class="trustbar__img" width="70" height="22" loading="lazy"></span>
          <span>Serasa Experian · eConsig</span>
        </div>
        <div class="trustbar__badge trustbar__badge--cvm" aria-hidden="true">
          <span class="trustbar__logo-chip"><img src="assets/logo-cvm.png" alt="" class="trustbar__img" width="65" height="20" loading="lazy"></span>
          <span>CVM · Mercado de Capitais</span>
        </div>
        <div class="trustbar__badge trustbar__badge--alergs" aria-hidden="true">
          <span class="trustbar__logo-chip"><img src="assets/logo-alergs.png" alt="" class="trustbar__img" width="55" height="22" loading="lazy"></span>
          <span>ALERGS · Poder Legislativo</span>
        </div>
        <div class="trustbar__badge trustbar__badge--alertaseg" aria-hidden="true">
          <span class="trustbar__logo-chip"><img src="assets/logo-alertaseg-dark.png" alt="" class="trustbar__img" width="55" height="22" loading="lazy"></span>
          <span>AlertaSeg · Gestão SST</span>
        </div>
        <div class="trustbar__badge trustbar__badge--animalworld" aria-hidden="true">
          <span class="trustbar__logo-chip"><img src="assets/logo-animal-world.png" alt="" class="trustbar__img" width="30" height="30" loading="lazy"></span>
          <span>Animal World · Hub Pet (sysPet2)</span>
        </div>
      </div>"""

with open('site/index.html', 'r', encoding='utf-8') as f:
    site_html = f.read()

import re
m = re.search(r'(<div class="trustbar__track">[\s\S]*?</div>\s*</div>)', site_html)
if m:
    track_in_file = m.group(1)
    print("Found track in site/index.html")
    # compare lines
    print("Are they identical (ignoring whitespace)?", "".join(user_snippet.split()) in "".join(track_in_file.split()))
else:
    print("Track not found")
