from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Switch to dark theme
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    
    page.locator('.trustbar').screenshot(path='scratch/trustbar_dark_check.png')
    
    # Let's check marquee bounding rect vs badge bounding rect in dark mode
    styles = page.evaluate('''() => {
        const tb = document.querySelector('.trustbar');
        const tm = document.querySelector('.trustbar__marquee');
        const track = document.querySelector('.trustbar__track');
        const badge = document.querySelector('.trustbar__badge');
        return {
            marquee_rect: tm.getBoundingClientRect(),
            track_rect: track.getBoundingClientRect(),
            badge_rect: badge.getBoundingClientRect(),
            marquee_scrollHeight: tm.scrollHeight,
            marquee_clientHeight: tm.clientHeight,
            track_scrollHeight: track.scrollHeight,
            track_clientHeight: track.clientHeight,
        };
    }''')
    print('Dark mode rects:', styles)
    browser.close()
