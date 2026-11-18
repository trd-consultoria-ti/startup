from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1200, "height": 800})
    page.goto("http://localhost:8080/", wait_until="networkidle")
    # toggle theme or set data-theme="dark"
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    header = page.locator("header.nav")
    header.screenshot(path="C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/header_dark_1200.png")
    page.close()
    browser.close()
print("Dark 1200 captured")
