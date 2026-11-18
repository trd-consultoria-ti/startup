from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 375, "height": 812})
    page.goto("http://localhost:8080/", wait_until="networkidle")

    # Find all elements that have internal horizontal scroll
    scrollable_elements = page.evaluate("""() => {
        const els = document.querySelectorAll('*');
        const res = [];
        els.forEach(el => {
            if (el.scrollWidth > el.clientWidth + 2) {
                const overflowX = getComputedStyle(el).overflowX;
                res.push({
                    tag: el.tagName,
                    class: el.className,
                    id: el.id,
                    clientWidth: el.clientWidth,
                    scrollWidth: el.scrollWidth,
                    overflowX: overflowX
                });
            }
        });
        return res;
    }""")

    print("Elements with internal horizontal scroll (scrollWidth > clientWidth):")
    for item in scrollable_elements:
        print(f" - <{item['tag']} class='{item['class']}' id='{item['id']}'>: clientWidth={item['clientWidth']}, scrollWidth={item['scrollWidth']}, overflowX={item['overflowX']}")

    page.close()
    browser.close()
