from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    
    # 1. Desktop Test (1440px)
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    chips_desktop = page.evaluate('''() => {
        const els = Array.from(document.querySelectorAll('.card__logo-chip'));
        return els.map((el, i) => {
            const rect = el.getBoundingClientRect();
            const img = el.querySelector('img');
            const cardTitle = el.closest('.card') ? el.closest('.card').querySelector('h3').innerText.trim() : 'N/A';
            return {
                index: i,
                title: cardTitle,
                className: el.className,
                chipWidth: rect.width,
                chipHeight: rect.height,
                imgWidth: img ? img.getBoundingClientRect().width : 0,
                imgHeight: img ? img.getBoundingClientRect().height : 0
            };
        });
    }''')
    print("LIVE DESKTOP CHIP SIZES:")
    for c in chips_desktop:
        print(f"[{c['index']}] {c['title']}: chip={c['chipWidth']:.1f}x{c['chipHeight']:.1f}px (class='{c['className']}'), img={c['imgWidth']:.1f}x{c['imgHeight']:.1f}px")
        
    # Screenshots
    page.locator('#sistemas').screenshot(path='scratch/chips_sistemas_standard_light.png')
    page.locator('#cases').screenshot(path='scratch/chips_cases_standard_light.png')

    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page.locator('#sistemas').screenshot(path='scratch/chips_sistemas_standard_dark.png')
    page.locator('#cases').screenshot(path='scratch/chips_cases_standard_dark.png')
    page.close()

    # 2. Mobile Test (375px)
    page_mob = browser.new_page(viewport={'width': 375, 'height': 812})
    page_mob.goto('http://localhost:8080', wait_until='networkidle')
    page_mob.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    chips_mob = page_mob.evaluate('''() => {
        const els = Array.from(document.querySelectorAll('.card__logo-chip'));
        return els.map((el, i) => {
            const rect = el.getBoundingClientRect();
            return {
                index: i,
                chipWidth: rect.width,
                chipHeight: rect.height
            };
        });
    }''')
    print("\nLIVE MOBILE (375px) CHIP SIZES:")
    for c in chips_mob:
        print(f"[{c['index']}] chip={c['chipWidth']:.1f}x{c['chipHeight']:.1f}px")
    page_mob.locator('#sistemas').screenshot(path='scratch/chips_sistemas_standard_mobile.png')
    page_mob.locator('#cases').screenshot(path='scratch/chips_cases_standard_mobile.png')
    page_mob.close()

    browser.close()
