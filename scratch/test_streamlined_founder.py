from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1280, 'height': 900})
    page.goto('http://localhost:8080')
    
    # Test setting the streamlined text and text-wrap: pretty
    test_html = '''
    Engenheiro de software e arquiteto de sistemas especializado em ambientes regulados, alta disponibilidade e esteiras de alta volumetria. Histórico comprovado de entregas críticas na <strong>Imprensa Nacional (DOU)</strong>, <strong>INEP (ENADE)</strong>, <strong>Ministério da Justiça (CNPD)</strong>, <strong>Serasa Experian (eConsig)</strong>, <strong>CVM</strong>, <strong>ALERGS</strong> e <strong>Senar</strong> — além da arquitetura e desenvolvimento de plataformas proprietárias como <strong>AlertaSeg (SST)</strong> e <strong>Animal World (sysPet2)</strong>.
    '''
    
    page.evaluate(f'''() => {{
        const p = document.querySelector('.founder-card p.small');
        p.innerHTML = `{test_html.strip()}`;
        p.style.textWrap = 'pretty';
        p.style.wordBreak = 'normal';
        p.style.overflowWrap = 'normal';
        p.style.hyphens = 'none';
    }}''')
    
    # Check across various widths: 1280, 1024, 768, 500, 375, 320
    for w in [1280, 1024, 850, 768, 600, 480, 412, 375, 320]:
        page.set_viewport_size({'width': w, 'height': 800})
        page.wait_for_timeout(100)
        page.locator('.founder-card').screenshot(path=f'scratch/founder_test_{w}.png')
        
        # Check text lines
        lines = page.evaluate('''() => {
            const p = document.querySelector('.founder-card p.small');
            const range = document.createRange();
            const walker = document.createTreeWalker(p, NodeFilter.SHOW_TEXT, null, false);
            let n;
            let words = [];
            while(n = walker.nextNode()) {
                const text = n.textContent;
                for(let i=0; i<text.length; i++) {
                    range.setStart(n, i);
                    range.setEnd(n, i+1);
                    const rect = range.getBoundingClientRect();
                    words.push({ char: text[i], top: Math.round(rect.top) });
                }
            }
            let lMap = {};
            words.forEach(w => {
                if(!lMap[w.top]) lMap[w.top] = '';
                lMap[w.top] += w.char;
            });
            return Object.values(lMap);
        }''')
        print(f'=== Width {w}px ===')
        for idx, l in enumerate(lines):
            print(f'  L{idx+1}: {repr(l.strip())}')
            
    browser.close()
