from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 375, 'height': 812})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Inject mobile prototype
    page.add_style_tag(content='''
        .trustbar__badge {
            width: 148px !important;
            height: 104px !important;
            flex: 0 0 148px !important;
            min-width: 148px !important;
            max-width: 148px !important;
            padding: .75rem .5rem .65rem !important;
            justify-content: center !important;
            gap: .35rem !important;
            box-sizing: border-box !important;
            border-radius: 12px !important;
        }
        .trustbar__logo-chip {
            height: 42px !important;
            width: 100% !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
        }
        .trustbar__badge--dou .trustbar__img,
        .trustbar__badge--mjsp .trustbar__img,
        .trustbar__badge--animalworld .trustbar__img {
            height: 38px !important;
            width: 38px !important;
            max-width: 38px !important;
            max-height: 38px !important;
        }
        .trustbar__badge--serasa .trustbar__img {
            height: 26px !important;
            max-height: 26px !important;
            max-width: 85px !important;
        }
        .trustbar__badge--cvm .trustbar__img,
        .trustbar__badge--inep .trustbar__img,
        .trustbar__badge--alergs .trustbar__img,
        .trustbar__badge--alertaseg .trustbar__img {
            height: 24px !important;
            max-height: 24px !important;
            max-width: 70px !important;
        }
        .trustbar__badge > span:last-child {
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
            height: 26px !important;
            width: 100% !important;
            max-width: 100% !important;
            font-size: .625rem !important;
            line-height: 1.2 !important;
            text-align: center !important;
        }
    ''')
    
    # Pause marquee and screenshot
    page.evaluate("() => { document.querySelector('.trustbar__track').style.animationPlayState = 'paused'; }")
    page.locator('.trustbar').screenshot(path='scratch/test_uniform_mobile_375_light.png')
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page.locator('.trustbar').screenshot(path='scratch/test_uniform_mobile_375_dark.png')

    badges_mobile = page.evaluate('''() => {
        const elements = Array.from(document.querySelectorAll('.trustbar__badge'));
        return elements.slice(0, 8).map((el, i) => {
            const rect = el.getBoundingClientRect();
            return {
                index: i,
                text: el.querySelector('span:last-child').innerText.trim(),
                width: rect.width,
                height: rect.height,
                scrollWidth: el.querySelector('span:last-child').scrollWidth,
                clientWidth: el.querySelector('span:last-child').clientWidth,
                scrollHeight: el.querySelector('span:last-child').scrollHeight,
                clientHeight: el.querySelector('span:last-child').clientHeight
            };
        });
    }''')
    print("PROTOTYPE BADGE SIZES (MOBILE 375px):")
    for b in badges_mobile:
        print(f"[{b['index']}] {b['width']:.2f}px x {b['height']:.2f}px (text overflow? h: {b['scrollHeight'] > b['clientHeight']}, w: {b['scrollWidth'] > b['clientWidth']}) - '{b['text']}'")
        
    browser.close()
