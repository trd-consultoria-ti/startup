from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 375, "height": 812})
    page.goto("https://trdconsultoria.com.br/", wait_until="networkidle")

    doc_scroll = page.evaluate("() => document.documentElement.scrollWidth")
    body_scroll = page.evaluate("() => document.body.scrollWidth")
    inner_w = page.evaluate("() => window.innerWidth")

    user_scrollable = page.evaluate("""() => {
        const bad = [];
        document.querySelectorAll('*').forEach(el => {
            const ox = getComputedStyle(el).overflowX;
            if ((ox === 'auto' || ox === 'scroll') && el.scrollWidth > el.clientWidth + 2) {
                bad.push({
                    tag: el.tagName,
                    className: el.className,
                    scrollWidth: el.scrollWidth,
                    clientWidth: el.clientWidth
                });
            }
        });
        return bad;
    }""")

    print(f"LIVE PRODUCTION TEST: doc_scroll={doc_scroll}, inner_w={inner_w}, user_scrollable={len(user_scrollable)}")
    page.close()
    browser.close()
