import json
from playwright.sync_api import sync_playwright

VIEWPORTS = [
    {"name": "iPhone_5_320", "width": 320, "height": 568},
    {"name": "Android_360", "width": 360, "height": 740},
    {"name": "iPhone_SE_375", "width": 375, "height": 667},
    {"name": "iPhone_14_390", "width": 390, "height": 844},
    {"name": "Android_412", "width": 412, "height": 915},
    {"name": "iPhone_Max_428", "width": 428, "height": 926},
    {"name": "iPad_Mini_768", "width": 768, "height": 1024},
    {"name": "iPad_Air_820", "width": 820, "height": 1180},
    {"name": "iPad_Pro_1024", "width": 1024, "height": 1366},
]

results = []

with sync_playwright() as p:
    browser = p.chromium.launch()
    for vp in VIEWPORTS:
        page = browser.new_page(viewport={"width": vp["width"], "height": vp["height"]})
        page.goto("http://localhost:8080/", wait_until="networkidle")

        scroll_width = page.evaluate("() => document.documentElement.scrollWidth")
        inner_width = vp["width"]
        has_overflow = scroll_width > inner_width

        links_vis = page.locator(".nav__links").is_visible()
        burger_vis = page.locator("#nav-toggle-btn").is_visible()

        # Open drawer and test
        burger = page.locator("#nav-toggle-btn")
        burger.click()
        page.wait_for_timeout(200)
        drawer_vis = page.locator("#nav-mobile").is_visible()
        drawer_scroll_width = page.evaluate("() => document.documentElement.scrollWidth")
        drawer_overflow = drawer_scroll_width > inner_width

        page.screenshot(path=f"scratch/screenshots/mobile_model_{vp['name']}.png")

        res = {
            "name": vp["name"],
            "width": vp["width"],
            "has_overflow_closed": has_overflow,
            "has_overflow_open": drawer_overflow,
            "links_horizontal_presented": links_vis,
            "burger_visible": burger_vis,
            "drawer_works": drawer_vis
        }
        results.append(res)
        print(f"[{vp['name']}] Width: {vp['width']} | Panel presented: {links_vis} | Overflow: {has_overflow} | Drawer OK: {drawer_vis}")
        page.close()
    browser.close()

with open("scratch/mobile_model_results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2)

print("Finished mobile model validation.")
