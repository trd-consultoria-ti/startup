from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Let's inspect the current dimensions of all .card__logo-chip
    chips = page.evaluate('''() => {
        const els = Array.from(document.querySelectorAll('.card__logo-chip'));
        return els.map((el, i) => {
            const rect = el.getBoundingClientRect();
            const img = el.querySelector('img');
            const cardTitle = el.closest('.card') ? el.closest('.card').querySelector('h3').innerText.trim() : 'N/A';
            return {
                index: i,
                title: cardTitle,
                className: el.className,
                imgSrc: img ? img.getAttribute('src') : '',
                chipWidth: rect.width,
                chipHeight: rect.height,
                imgWidth: img ? img.getBoundingClientRect().width : 0,
                imgHeight: img ? img.getBoundingClientRect().height : 0
            };
        });
    }''')
    print("CURRENT CHIP DIMENSIONS:")
    for c in chips:
        print(f"[{c['index']}] {c['title']}: chip={c['chipWidth']:.1f}x{c['chipHeight']:.1f}px (class='{c['className']}'), img={c['imgWidth']:.1f}x{c['imgHeight']:.1f}px")
        
    browser.close()
