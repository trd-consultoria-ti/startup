from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Inject desktop prototype
    page.add_style_tag(content='''
        .trustbar__badge {
            width: 172px !important;
            height: 114px !important;
            flex: 0 0 172px !important;
            min-width: 172px !important;
            max-width: 172px !important;
            padding: .85rem .75rem .75rem !important;
            justify-content: center !important;
            gap: .45rem !important;
            box-sizing: border-box !important;
        }
        .trustbar__logo-chip {
            height: 46px !important;
            width: 100% !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
        }
        .trustbar__badge > span:last-child {
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
            height: 30px !important;
            width: 100% !important;
            max-width: 100% !important;
            font-size: .6875rem !important;
            line-height: 1.25 !important;
            text-align: center !important;
        }
    ''')
    
    badges_desktop = page.evaluate('''() => {
        const elements = Array.from(document.querySelectorAll('.trustbar__badge'));
        return elements.slice(0, 8).map((el, i) => {
            const rect = el.getBoundingClientRect();
            const textEl = el.querySelector('span:last-child');
            return {
                index: i,
                text: textEl.innerText.trim(),
                width: rect.width,
                height: rect.height,
                scrollWidth: textEl.scrollWidth,
                clientWidth: textEl.clientWidth,
                scrollHeight: textEl.scrollHeight,
                clientHeight: textEl.clientHeight
            };
        });
    }''')
    print("PROTOTYPE BADGE SIZES (DESKTOP 1440px):")
    for b in badges_desktop:
        print(f"[{b['index']}] {b['width']:.2f}px x {b['height']:.2f}px (text overflow? h: {b['scrollHeight'] > b['clientHeight']}, w: {b['scrollWidth'] > b['clientWidth']}) - '{b['text']}'")
        
    browser.close()
