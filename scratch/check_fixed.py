from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # 1. Light mode
    page.locator('.trustbar').screenshot(path='scratch/trustbar_fixed_light.png')
    
    # Check rects
    styles_light = page.evaluate('''() => {
        const tb = document.querySelector('.trustbar');
        const tm = document.querySelector('.trustbar__marquee');
        const track = document.querySelector('.trustbar__track');
        const badge = document.querySelector('.trustbar__badge');
        const tmRect = tm.getBoundingClientRect();
        const bRect = badge.getBoundingClientRect();
        return {
            marquee_height: tmRect.height,
            marquee_top: tmRect.top,
            marquee_bottom: tmRect.bottom,
            badge_height: bRect.height,
            badge_top: bRect.top,
            badge_bottom: bRect.bottom,
            bottom_cushion: tmRect.bottom - bRect.bottom,
            top_cushion: bRect.top - tmRect.top
        };
    }''')
    print('Light mode cushions:', styles_light)

    # 2. Dark mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page.locator('.trustbar').screenshot(path='scratch/trustbar_fixed_dark.png')
    
    # Zoom into badges
    page.locator('.trustbar__track').screenshot(path='scratch/trustbar_fixed_track_dark.png')

    styles_dark = page.evaluate('''() => {
        const tb = document.querySelector('.trustbar');
        const tm = document.querySelector('.trustbar__marquee');
        const track = document.querySelector('.trustbar__track');
        const badge = document.querySelector('.trustbar__badge');
        const tmRect = tm.getBoundingClientRect();
        const bRect = badge.getBoundingClientRect();
        return {
            marquee_height: tmRect.height,
            marquee_top: tmRect.top,
            marquee_bottom: tmRect.bottom,
            badge_height: bRect.height,
            badge_top: bRect.top,
            badge_bottom: bRect.bottom,
            bottom_cushion: tmRect.bottom - bRect.bottom,
            top_cushion: bRect.top - tmRect.top
        };
    }''')
    print('Dark mode cushions:', styles_dark)

    browser.close()
