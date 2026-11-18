from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1440, "height": 900})
    page.goto("http://localhost:8080", wait_until="networkidle")
    result = page.evaluate('''() => {
        const overflowingElements = [];
        const allElements = document.querySelectorAll('*');
        for (const el of allElements) {
            if (el.scrollWidth > el.clientWidth && 
                !['HTML', 'BODY'].includes(el.tagName) &&
                !el.classList.contains('trustbar__marquee') &&
                !el.classList.contains('trustbar__track') &&
                window.getComputedStyle(el).overflowX !== 'hidden') {
                overflowingElements.push({
                    tag: el.tagName,
                    className: el.className,
                    scrollWidth: el.scrollWidth,
                    clientWidth: el.clientWidth,
                    text: el.innerText ? el.innerText.slice(0, 30) : ''
                });
            }
        }
        return overflowingElements;
    }''')
    print("Overflowing elements at 1440px:", result)
    browser.close()
