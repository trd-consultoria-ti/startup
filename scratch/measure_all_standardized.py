from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    
    # Desktop
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Scroll through the whole page to load all lazy images
    page.evaluate('''async () => {
        const distance = 500;
        const delay = 50;
        while (document.scrollingElement.scrollTop + window.innerHeight < document.scrollingElement.scrollHeight) {
            document.scrollingElement.scrollBy(0, distance);
            await new Promise(resolve => setTimeout(resolve, delay));
        }
    }''')
    page.wait_for_timeout(500)
    
    # Measure all 11 chips
    chips = page.evaluate('''() => {
        const els = Array.from(document.querySelectorAll('.card__logo-chip'));
        return els.map((el, i) => {
            const rect = el.getBoundingClientRect();
            const img = el.querySelector('img');
            const imgRect = img ? img.getBoundingClientRect() : null;
            const cardTitle = el.closest('.card') ? el.closest('.card').querySelector('h3').innerText.trim() : 'N/A';
            return {
                index: i,
                title: cardTitle,
                chipWidth: rect.width,
                chipHeight: rect.height,
                imgWidth: imgRect ? imgRect.width : 0,
                imgHeight: imgRect ? imgRect.height : 0
            };
        });
    }''')
    print("ALL 11 STANDARDIZED CHIPS ON DESKTOP:")
    for c in chips:
        print(f"[{c['index']}] {c['title']}: chip={c['chipWidth']:.1f}x{c['chipHeight']:.1f}px, img={c['imgWidth']:.1f}x{c['imgHeight']:.1f}px")

    # Screenshots in light mode
    page.locator('#sistemas').screenshot(path='scratch/chips_sistemas_final_light.png')
    page.locator('#cases').screenshot(path='scratch/chips_cases_final_light.png')

    # Screenshots in dark mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page.locator('#sistemas').screenshot(path='scratch/chips_sistemas_final_dark.png')
    page.locator('#cases').screenshot(path='scratch/chips_cases_final_dark.png')
    page.close()

    # Mobile
    page_mob = browser.new_page(viewport={'width': 375, 'height': 812})
    page_mob.goto('http://localhost:8080', wait_until='networkidle')
    page_mob.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page_mob.evaluate('''async () => {
        const distance = 500;
        const delay = 50;
        while (document.scrollingElement.scrollTop + window.innerHeight < document.scrollingElement.scrollHeight) {
            document.scrollingElement.scrollBy(0, distance);
            await new Promise(resolve => setTimeout(resolve, delay));
        }
    }''')
    page_mob.wait_for_timeout(500)
    chips_m = page_mob.evaluate('''() => {
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
    print("\nALL 11 STANDARDIZED CHIPS ON MOBILE (375px):")
    for c in chips_m:
        print(f"[{c['index']}] chip={c['chipWidth']:.1f}x{c['chipHeight']:.1f}px")
        
    page_mob.locator('#sistemas').screenshot(path='scratch/chips_sistemas_final_mobile.png')
    page_mob.locator('#cases').screenshot(path='scratch/chips_cases_final_mobile.png')
    page_mob.close()

    browser.close()
