from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()

    # Desktop 1200px
    page = browser.new_page(viewport={"width": 1200, "height": 900})
    page.goto("http://localhost:8080/", wait_until="networkidle")

    # Trustbar screenshot
    tb = page.locator(".trustbar")
    tb.screenshot(path="C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/preview_trustbar_desktop.png")

    # Founder card screenshot
    fc = page.locator(".founder-card")
    fc.screenshot(path="C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/preview_founder_desktop.png")

    doc_scroll = page.evaluate("() => document.documentElement.scrollWidth")
    print(f"Desktop 1200: doc_scroll={doc_scroll}")
    page.close()

    # Mobile 375px
    page_m = browser.new_page(viewport={"width": 375, "height": 812})
    page_m.goto("http://localhost:8080/", wait_until="networkidle")

    tb_m = page_m.locator(".trustbar")
    tb_m.screenshot(path="C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/preview_trustbar_mobile.png")

    fc_m = page_m.locator(".founder-card")
    fc_m.screenshot(path="C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/preview_founder_mobile.png")

    doc_scroll_m = page_m.evaluate("() => document.documentElement.scrollWidth")
    print(f"Mobile 375: doc_scroll={doc_scroll_m}")
    page_m.close()

    browser.close()

print("Screenshots captured successfully!")
