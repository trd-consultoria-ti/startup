import json
from playwright.sync_api import sync_playwright

VIEWPORTS = [
    {"name": "iPhone_SE", "width": 375, "height": 667},
    {"name": "iPhone_14", "width": 390, "height": 844},
    {"name": "Android_360", "width": 360, "height": 800},
    {"name": "Android_412", "width": 412, "height": 915},
    {"name": "iPad_Mini", "width": 768, "height": 1024},
    {"name": "iPad_Pro", "width": 1024, "height": 1366},
]

with sync_playwright() as p:
    browser = p.chromium.launch()
    for vp in VIEWPORTS:
        page = browser.new_page(viewport={"width": vp["width"], "height": vp["height"]})
        try:
            page.goto("https://trdconsultoria.com.br/", wait_until="networkidle", timeout=15000)
            scroll_width = page.evaluate("() => document.documentElement.scrollWidth")
            inner_width = vp["width"]
            overflow = scroll_width > inner_width
            
            # Find overflowing elements
            overflowing_elements = page.evaluate("""() => {
                const docWidth = document.documentElement.clientWidth;
                const elems = document.querySelectorAll('*');
                const bad = [];
                elems.forEach(el => {
                    const rect = el.getBoundingClientRect();
                    if (rect.right > docWidth + 1 || rect.left < -1) {
                        bad.push({
                            tag: el.tagName,
                            id: el.id,
                            className: el.className,
                            right: rect.right,
                            docWidth: docWidth
                        });
                    }
                });
                return bad.slice(0, 10);
            }""")
            
            print(f"[{vp['name']}] Width: {inner_width} | ScrollWidth: {scroll_width} | Overflow: {overflow}")
            if overflowing_elements:
                print(f"  Bad elements: {overflowing_elements}")
            
            # Check nav__links visibility
            links_vis = page.locator(".nav__links").is_visible()
            burger_vis = page.locator("#nav-toggle-btn").is_visible()
            print(f"  Links vis: {links_vis} | Burger vis: {burger_vis}")
            
            page.screenshot(path=f"scratch/screenshots/prod_{vp['name']}.png")
        except Exception as e:
            print(f"Error on {vp['name']}: {e}")
        finally:
            page.close()
    browser.close()
