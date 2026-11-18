import os
from playwright.sync_api import sync_playwright

artifact_dir = r"C:\Users\Thiago Duarte\.gemini\antigravity-cli\brain\6bbf3b1b-3f2a-4754-9903-a79b7c5a2507"

with sync_playwright() as p:
    browser = p.chromium.launch()

    # Desktop Dark
    page = browser.new_page(viewport={'width': 1280, 'height': 900})
    page.goto('http://localhost:8080')
    page.wait_for_timeout(1000)

    # 1. Trustbar marquee in dark mode
    page.locator('.trustbar').screenshot(path=os.path.join(artifact_dir, 'trustbar_marquee_kimi_dark.png'))

    # 2. Sistemas section
    page.locator('#sistemas .grid--2').screenshot(path=os.path.join(artifact_dir, 'sistemas_cards_dark.png'))

    # 3. Cases section (Animal World & AlertaSeg row)
    page.locator('#cases .grid--3').screenshot(path=os.path.join(artifact_dir, 'cases_cards_dark.png'))

    # Desktop Light
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'light')")
    page.wait_for_timeout(500)
    page.locator('.trustbar').screenshot(path=os.path.join(artifact_dir, 'trustbar_marquee_kimi_light.png'))
    page.locator('#sistemas .grid--2').screenshot(path=os.path.join(artifact_dir, 'sistemas_cards_light.png'))
    page.close()

    # Mobile 375px
    mobile_page = browser.new_page(viewport={'width': 375, 'height': 812})
    mobile_page.goto('http://localhost:8080')
    mobile_page.wait_for_timeout(1000)
    mobile_page.locator('.trustbar').screenshot(path=os.path.join(artifact_dir, 'trustbar_marquee_kimi_mobile.png'))
    mobile_page.locator('#sistemas .grid--2').screenshot(path=os.path.join(artifact_dir, 'sistemas_cards_mobile.png'))
    mobile_page.close()

    browser.close()

print('All artifact screenshots captured successfully!')
