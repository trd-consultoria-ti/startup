from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')

    # Test in Dark Mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    
    links = [
        ("Sistemas Sob Medida", "#sistemas"),
        ("Diagnóstico", "#solucoes"),
        ("Planos & Preços", "#planos"),
        ("Contato", "#contato")
    ]
    
    print("CHECKING LIVE NAVIGATION COLORS (DARK MODE):")
    for label, target in links:
        link = page.locator(f'.nav__links a[href="{target}"]')
        link.click()
        page.wait_for_timeout(400)
        
        info = page.evaluate(f'''() => {{
            const el = document.querySelector('.nav__links a[href="{target}"]');
            const cs = window.getComputedStyle(el);
            const afterCs = window.getComputedStyle(el, '::after');
            return {{
                text: el.innerText.trim(),
                color: cs.color,
                fontWeight: cs.fontWeight,
                afterBg: afterCs.backgroundColor,
                afterOpacity: afterCs.opacity
            }};
        }}''')
        print(f"[{target}] {info['text']}: color={info['color']} (font-weight: {info['fontWeight']}), underline={info['afterBg']} (opacity: {info['afterOpacity']})")

    # Screenshot in Dark Mode
    page.locator('.nav').screenshot(path='scratch/nav_zero_orange_dark.png')

    # Test in Light Mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'light')")
    page.locator('.nav__links a[href="#sistemas"]').click()
    page.wait_for_timeout(400)
    info_light = page.evaluate('''() => {
        const el = document.querySelector('.nav__links a[href="#sistemas"]');
        const cs = window.getComputedStyle(el);
        const afterCs = window.getComputedStyle(el, '::after');
        return {
            color: cs.color,
            afterBg: afterCs.backgroundColor
        };
    }''')
    print("\nCHECKING LIVE NAVIGATION COLORS (LIGHT MODE):")
    print(f"[#sistemas] color={info_light['color']}, underline={info_light['afterBg']}")
    page.locator('.nav').screenshot(path='scratch/nav_zero_orange_light.png')

    browser.close()
