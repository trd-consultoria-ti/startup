from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Pause marquee animation for clean screenshots
    page.evaluate("() => { document.querySelector('.trustbar__track').style.animationPlayState = 'paused'; }")

    # 1. Light Mode Full Trustbar
    page.locator('.trustbar').screenshot(path='scratch/trustbar_fixed_light.png')
    
    # 2. Dark Mode Full Trustbar
    page.evaluate("() => { document.documentElement.setAttribute('data-theme', 'dark'); }")
    page.locator('.trustbar').screenshot(path='scratch/trustbar_fixed_dark.png')

    # Zoomed screenshot of first few badges
    # We clip to trustbar area
    tb_box = page.locator('.trustbar').bounding_box()
    page.screenshot(
        path='scratch/trustbar_badges_zoom_dark.png',
        clip={'x': 0, 'y': tb_box['y'], 'width': 1200, 'height': tb_box['height']}
    )
    
    # Check cushions in dark mode
    styles_dark = page.evaluate('''() => {
        const tm = document.querySelector('.trustbar__marquee');
        const badge = document.querySelector('.trustbar__badge');
        const tmRect = tm.getBoundingClientRect();
        const bRect = badge.getBoundingClientRect();
        return {
            marquee_height: tmRect.height,
            badge_height: bRect.height,
            bottom_cushion: tmRect.bottom - bRect.bottom,
            top_cushion: bRect.top - tmRect.top
        };
    }''')
    print('Dark mode cushions:', styles_dark)
    browser.close()
