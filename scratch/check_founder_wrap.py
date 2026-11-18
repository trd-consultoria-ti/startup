from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1280, 'height': 900})
    page.goto('http://localhost:8080')
    page.wait_for_timeout(500)

    # Let's check how the paragraph lines wrap
    p_text = page.locator('.founder-card p.small').first
    box = p_text.bounding_box()
    print('Bounding box of p.small:', box)

    # Let's take screenshot of founder card at 1280, 1024, 768, 412, 375
    for w in [1280, 1024, 900, 768, 600, 412, 375]:
        page.set_viewport_size({'width': w, 'height': 800})
        page.wait_for_timeout(200)
        page.locator('.founder-card').screenshot(path=f'scratch/founder_{w}.png')

    browser.close()

print('Saved founder screenshots!')
