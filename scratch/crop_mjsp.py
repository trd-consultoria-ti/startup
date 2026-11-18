from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    context = browser.new_context(viewport={'width': 1280, 'height': 800})
    page = context.new_page()
    page.goto('http://localhost:8080')
    page.wait_for_timeout(1000)
    page.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    # Capture specifically the MJSP badge
    mjsp_badge = page.locator('.trustbar__badge--mjsp').first
    mjsp_badge.screenshot(path='scratch/mjsp_badge_current.png')
    
    # Also capture the whole trustbar track
    track = page.locator('.trustbar__track')
    track.screenshot(path='scratch/trustbar_track_zoom.png')
    
    browser.close()

print('MJSP badge captured')
