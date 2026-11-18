from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1200, "height": 900})
    page.goto("http://localhost:8080/", wait_until="networkidle")

    # Dark mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    fc_dark = page.locator(".founder-card")
    fc_dark.screenshot(path="C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/preview_founder_glasses_dark.png")

    # Light mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'light')")
    fc_light = page.locator(".founder-card")
    fc_light.screenshot(path="C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/preview_founder_glasses_light.png")

    page.close()
    browser.close()

print("Both glasses previews captured!")
