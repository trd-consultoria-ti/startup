from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    
    # 1. Desktop Test (1440px)
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Check all 8 badges dimensions
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
    print("LIVE DESKTOP (1440px) BADGE SIZES:")
    for b in badges_desktop:
        print(f"[{b['index']}] {b['width']:.2f}px x {b['height']:.2f}px (overflow? {b['scrollHeight'] > b['clientHeight']}) - '{b['text']}'")
        
    page.evaluate("() => { document.querySelector('.trustbar__track').style.animationPlayState = 'paused'; }")
    page.locator('.trustbar').screenshot(path='scratch/trustbar_uniform_desktop_light.png')
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page.locator('.trustbar').screenshot(path='scratch/trustbar_uniform_desktop_dark.png')
    page.close()

    # 2. Tablet Test (768px)
    page_tab = browser.new_page(viewport={'width': 768, 'height': 1024})
    page_tab.goto('http://localhost:8080', wait_until='networkidle')
    page_tab.evaluate("() => { document.querySelector('.trustbar__track').style.animationPlayState = 'paused'; }")
    page_tab.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page_tab.locator('.trustbar').screenshot(path='scratch/trustbar_uniform_tablet_dark.png')
    badges_tab = page_tab.evaluate('''() => {
        const elements = Array.from(document.querySelectorAll('.trustbar__badge'));
        return elements.slice(0, 8).map((el, i) => {
            const rect = el.getBoundingClientRect();
            return { index: i, width: rect.width, height: rect.height };
        });
    }''')
    print("\nLIVE TABLET (768px) BADGE SIZES:")
    for b in badges_tab:
        print(f"[{b['index']}] {b['width']:.2f}px x {b['height']:.2f}px")
    page_tab.close()

    # 3. Mobile Test (375px)
    page_mob = browser.new_page(viewport={'width': 375, 'height': 812})
    page_mob.goto('http://localhost:8080', wait_until='networkidle')
    page_mob.evaluate("() => { document.querySelector('.trustbar__track').style.animationPlayState = 'paused'; }")
    page_mob.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page_mob.locator('.trustbar').screenshot(path='scratch/trustbar_uniform_mobile_dark.png')
    badges_mob = page_mob.evaluate('''() => {
        const elements = Array.from(document.querySelectorAll('.trustbar__badge'));
        return elements.slice(0, 8).map((el, i) => {
            const rect = el.getBoundingClientRect();
            const textEl = el.querySelector('span:last-child');
            return {
                index: i,
                text: textEl.innerText.trim(),
                width: rect.width,
                height: rect.height,
                overflow: textEl.scrollHeight > textEl.clientHeight
            };
        });
    }''')
    print("\nLIVE MOBILE (375px) BADGE SIZES:")
    for b in badges_mob:
        print(f"[{b['index']}] {b['width']:.2f}px x {b['height']:.2f}px (overflow? {b['overflow']}) - '{b['text']}'")
    page_mob.close()

    browser.close()
