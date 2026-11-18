import json
from playwright.sync_api import sync_playwright

VIEWPORTS = [320, 360, 375, 390, 412, 428, 768, 820, 1024, 1200, 1440, 1920]

all_passed = True
results = []

with sync_playwright() as p:
    browser = p.chromium.launch()
    for w in VIEWPORTS:
        page = browser.new_page(viewport={"width": w, "height": 850})
        page.goto("http://localhost:8080/", wait_until="networkidle")

        doc_scroll = page.evaluate("() => document.documentElement.scrollWidth")
        body_scroll = page.evaluate("() => document.body.scrollWidth")
        inner_w = page.evaluate("() => window.innerWidth")

        # Check for elements with actual user-scrollable overflow-x
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

        page_overflow = doc_scroll > inner_w or body_scroll > inner_w
        has_bad_scroll = len(user_scrollable) > 0

        status = "PASS" if not page_overflow and not has_bad_scroll else "FAIL"
        if status == "FAIL":
            all_passed = False

        res = {
            "width": w,
            "page_overflow": page_overflow,
            "doc_scroll": doc_scroll,
            "inner_width": inner_w,
            "user_scrollable_elements": user_scrollable,
            "status": status
        }
        results.append(res)
        print(f"[{w}px] Status: {status} | Page Overflow: {page_overflow} | Scrollable Elements: {len(user_scrollable)}")

        page.close()
    browser.close()

print(f"\nALL PASSED: {all_passed}")
