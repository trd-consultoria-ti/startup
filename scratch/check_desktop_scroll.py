from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1024, "height": 850})
    page.goto("http://localhost:8080/", wait_until="networkidle")

    bad = page.evaluate("""() => {
        const res = [];
        document.querySelectorAll('*').forEach(el => {
            const ox = getComputedStyle(el).overflowX;
            if ((ox === 'auto' || ox === 'scroll') && el.scrollWidth > el.clientWidth + 2) {
                res.push({
                    tag: el.tagName,
                    className: el.className,
                    id: el.id,
                    scrollWidth: el.scrollWidth,
                    clientWidth: el.clientWidth
                });
            }
        });
        return res;
    }""")
    print("Bad elements at 1024px:", bad)
    page.close()
    browser.close()
