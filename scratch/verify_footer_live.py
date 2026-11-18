from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    
    # 1. Desktop (1440px) Light & Dark on index.html
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    footer = page.locator('.footer')
    footer.scroll_into_view_if_needed()
    page.wait_for_timeout(300)
    footer.screenshot(path='scratch/footer_live_desktop_light.png')

    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    footer.screenshot(path='scratch/footer_live_desktop_dark.png')
    page.close()

    # 2. Tablet (768px) Dark
    page_tab = browser.new_page(viewport={'width': 768, 'height': 1024})
    page_tab.goto('http://localhost:8080', wait_until='networkidle')
    page_tab.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    footer_tab = page_tab.locator('.footer')
    footer_tab.scroll_into_view_if_needed()
    page_tab.wait_for_timeout(300)
    footer_tab.screenshot(path='scratch/footer_live_tablet_dark.png')
    page_tab.close()

    # 3. Mobile (375px) Dark
    page_mob = browser.new_page(viewport={'width': 375, 'height': 812})
    page_mob.goto('http://localhost:8080', wait_until='networkidle')
    page_mob.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    footer_mob = page_mob.locator('.footer')
    footer_mob.scroll_into_view_if_needed()
    page_mob.wait_for_timeout(300)
    footer_mob.screenshot(path='scratch/footer_live_mobile_dark.png')
    page_mob.close()

    # 4. Check privacidade.html
    page_priv = browser.new_page(viewport={'width': 1440, 'height': 900})
    page_priv.goto('http://localhost:8080/privacidade.html', wait_until='networkidle')
    page_priv.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    footer_priv = page_priv.locator('.footer')
    footer_priv.scroll_into_view_if_needed()
    page_priv.wait_for_timeout(300)
    footer_priv.screenshot(path='scratch/footer_privacidade_dark.png')
    page_priv.close()

    browser.close()
    print("All live footer verifications completed successfully!")
