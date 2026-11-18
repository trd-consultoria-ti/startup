import os
from playwright.sync_api import sync_playwright

artifact_dir = r"C:\Users\Thiago Duarte\.gemini\antigravity-cli\brain\6bbf3b1b-3f2a-4754-9903-a79b7c5a2507"

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1280, 'height': 900})
    page.goto('http://localhost:8080')
    page.wait_for_timeout(500)

    # Screenshot founder card
    card = page.locator('.founder-card')
    card.screenshot(path=os.path.join(artifact_dir, 'founder_card_fixed_typography.png'))

    # Light version
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'light')")
    page.wait_for_timeout(300)
    card.screenshot(path=os.path.join(artifact_dir, 'founder_card_fixed_typography_light.png'))

    # Mobile 375px
    mobile_page = browser.new_page(viewport={'width': 375, 'height': 812})
    mobile_page.goto('http://localhost:8080')
    mobile_page.wait_for_timeout(500)
    mobile_card = mobile_page.locator('.founder-card')
    mobile_card.screenshot(path=os.path.join(artifact_dir, 'founder_card_fixed_typography_mobile.png'))

    browser.close()

print('Founder card fixed screenshots saved!')
