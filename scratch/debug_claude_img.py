from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    info = page.evaluate('''() => {
        const img = document.querySelector('img[src*="logo-claude"]');
        if (!img) return 'IMG NOT FOUND';
        const cs = window.getComputedStyle(img);
        return {
            src: img.src,
            naturalWidth: img.naturalWidth,
            naturalHeight: img.naturalHeight,
            width: cs.width,
            height: cs.height,
            display: cs.display,
            rect: img.getBoundingClientRect()
        };
    }''')
    print('Claude IMG info:', info)
    browser.close()
