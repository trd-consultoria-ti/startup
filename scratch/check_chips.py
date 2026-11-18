from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # 1. Capture #sistemas cards
    sistemas = page.locator('#sistemas')
    sistemas.screenshot(path='scratch/chips_sistemas_current.png')
    
    # 2. Capture #cases cards
    cases = page.locator('#cases')
    cases.screenshot(path='scratch/chips_cases_current.png')
    
    # 3. Capture dark mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    sistemas.screenshot(path='scratch/chips_sistemas_dark.png')
    cases.screenshot(path='scratch/chips_cases_dark.png')
    
    browser.close()
