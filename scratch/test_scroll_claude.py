from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Scroll card 10 into view
    card = page.locator('.card--anthropic')
    card.scroll_into_view_if_needed()
    page.wait_for_timeout(500)
    
    img = card.locator('img')
    box = img.bounding_box()
    print('Claude IMG bounding box after scroll:', box)
    browser.close()
