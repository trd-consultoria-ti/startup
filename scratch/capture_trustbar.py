from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    # Dark mode
    context_dark = browser.new_context(viewport={'width': 1280, 'height': 800})
    page_dark = context_dark.new_page()
    page_dark.goto('http://localhost:8080')
    page_dark.evaluate("() => localStorage.setItem('trd-theme', 'dark')")
    page_dark.reload()
    page_dark.wait_for_timeout(1000)
    page_dark.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    trustbar_dark = page_dark.locator('.trustbar')
    trustbar_dark.screenshot(path='scratch/trustbar_current_dark.png')
    
    # Also capture cases section in dark mode
    cases_dark = page_dark.locator('#cases')
    cases_dark.screenshot(path='scratch/cases_current_dark.png')

    # Light mode
    context_light = browser.new_context(viewport={'width': 1280, 'height': 800})
    page_light = context_light.new_page()
    page_light.goto('http://localhost:8080')
    page_light.evaluate("() => { localStorage.setItem('trd-theme', 'light'); document.documentElement.setAttribute('data-theme', 'light'); }")
    page_light.reload()
    page_light.wait_for_timeout(1000)
    page_light.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    trustbar_light = page_light.locator('.trustbar')
    trustbar_light.screenshot(path='scratch/trustbar_current_light.png')

    cases_light = page_light.locator('#cases')
    cases_light.screenshot(path='scratch/cases_current_light.png')

    browser.close()

print('Screenshots captured successfully')
