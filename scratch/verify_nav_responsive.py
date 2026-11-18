import json
import os
from playwright.sync_api import sync_playwright

VIEWPORTS = [
    {"name": "mobile_360", "width": 360, "height": 740},
    {"name": "mobile_414", "width": 414, "height": 896},
    {"name": "tablet_768", "width": 768, "height": 1024},
    {"name": "laptop_1024", "width": 1024, "height": 768},
    {"name": "laptop_1100", "width": 1100, "height": 800},
    {"name": "laptop_1199", "width": 1199, "height": 800},
    {"name": "laptop_1200", "width": 1200, "height": 800},
    {"name": "laptop_1280", "width": 1280, "height": 800},
    {"name": "laptop_1366", "width": 1366, "height": 768},
    {"name": "desktop_1440", "width": 1440, "height": 900},
    {"name": "desktop_1920", "width": 1920, "height": 1080},
]

os.makedirs("scratch/screenshots", exist_ok=True)

results = []

with sync_playwright() as p:
    browser = p.chromium.launch()
    for vp in VIEWPORTS:
        page = browser.new_page(viewport={"width": vp["width"], "height": vp["height"]})
        page.goto("http://localhost:8080/", wait_until="networkidle")

        scroll_width = page.evaluate("() => document.documentElement.scrollWidth")
        inner_width = vp["width"]
        has_overflow = scroll_width > inner_width

        burger_visible = page.locator("#nav-toggle-btn").is_visible()
        links_visible = page.locator(".nav__links").is_visible()

        link_data = []
        if links_visible:
            links = page.locator(".nav__links a").all()
            for l in links:
                box = l.bounding_box()
                txt = l.inner_text().strip()
                link_data.append({"text": txt, "width": box["width"] if box else 0, "height": box["height"] if box else 0})

        cta_data = None
        cta = page.locator(".nav__actions .nav__cta")
        if cta.is_visible():
            c_box = cta.bounding_box()
            cta_txt = cta.inner_text().strip()
            cta_data = {"text": cta_txt, "width": c_box["width"] if c_box else 0, "height": c_box["height"] if c_box else 0}

        header = page.locator("header.nav")
        ss_path = f"scratch/screenshots/header_{vp['name']}.png"
        header.screenshot(path=ss_path)

        res = {
            "viewport": vp["name"],
            "width": vp["width"],
            "has_overflow": has_overflow,
            "scroll_width": scroll_width,
            "burger_visible": burger_visible,
            "links_visible": links_visible,
            "links": link_data,
            "cta_data": cta_data,
            "screenshot": ss_path
        }
        results.append(res)
        print(f"[{vp['name']}] Overflow: {has_overflow} ({scroll_width}/{inner_width}) | Burger: {burger_visible} | Links: {links_visible}")

        page.close()

    browser.close()

with open("scratch/results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2)

print("\nDone. Results saved to scratch/results.json")
