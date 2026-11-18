from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    tb = page.locator('.trustbar').bounding_box()
    tm = page.locator('.trustbar__marquee').bounding_box()
    tt = page.locator('.trustbar__track').bounding_box()
    badge = page.locator('.trustbar__badge').first.bounding_box()
    print('trustbar:', tb)
    print('marquee:', tm)
    print('track:', tt)
    print('badge:', badge)
    
    styles = page.evaluate('''() => {
        const tb = document.querySelector('.trustbar');
        const tm = document.querySelector('.trustbar__marquee');
        const badge = document.querySelector('.trustbar__badge');
        const csTb = window.getComputedStyle(tb);
        const csTm = window.getComputedStyle(tm);
        const csB = window.getComputedStyle(badge);
        return {
            tb_height: csTb.height,
            tb_padding: csTb.padding,
            tb_overflow: csTb.overflow,
            tm_height: csTm.height,
            tm_padding: csTm.padding,
            tm_overflow: csTm.overflow,
            b_height: csB.height,
            b_box_shadow: csB.boxShadow,
            tb_bottom: tb.getBoundingClientRect().bottom,
            tm_bottom: tm.getBoundingClientRect().bottom,
            badge_bottom: badge.getBoundingClientRect().bottom
        };
    }''')
    print('Computed styles & rects:', styles)
    
    # Take screenshot of the trustbar
    page.locator('.trustbar').screenshot(path='scratch/trustbar_current.png')
    browser.close()
