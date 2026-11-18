import os
from playwright.sync_api import sync_playwright

VIEWPORTS = [360, 768, 1024]
os.makedirs("scratch/screenshots", exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch()
    for w in VIEWPORTS:
        page = browser.new_page(viewport={"width": w, "height": 800})
        page.goto("http://localhost:8080/", wait_until="networkidle")
        burger = page.locator("#nav-toggle-btn")
        if burger.is_visible():
            burger.click()
            page.wait_for_timeout(300)
            page.screenshot(path=f"scratch/screenshots/mobile_menu_open_{w}.png")
            print(f"Captured open mobile menu at {w}px")
        page.close()
    browser.close()
