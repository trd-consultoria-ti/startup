from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Check current card dimensions across all cards
    badges = page.evaluate('''() => {
        const elements = Array.from(document.querySelectorAll('.trustbar__badge'));
        return elements.slice(0, 8).map((el, i) => {
            const rect = el.getBoundingClientRect();
            return {
                index: i,
                text: el.querySelector('span:last-child').innerText.trim(),
                width: rect.width,
                height: rect.height
            };
        });
    }''')
    print("CURRENT BADGE SIZES:")
    for b in badges:
        print(f"[{b['index']}] {b['width']:.2f}px x {b['height']:.2f}px - '{b['text']}'")
        
    browser.close()
