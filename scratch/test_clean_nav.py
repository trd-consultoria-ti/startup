from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Inject clean executive styling (zero orange)
    page.add_style_tag(content='''
        .nav__links a:hover {
            color: var(--text) !important;
        }
        .nav__links a.is-active {
            color: var(--text) !important;
            font-weight: 600 !important;
        }
        .nav__links a::after {
            background: var(--text) !important;
            box-shadow: 0 1px 3px rgba(255, 255, 255, 0.2) !important;
        }
        [data-theme="light"] .nav__links a::after {
            box-shadow: 0 1px 2px rgba(15, 23, 42, 0.15) !important;
        }
    ''')
    
    # Dark Mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page.locator('.nav__links a[href="#sistemas"]').click()
    page.wait_for_timeout(300)
    page.locator('.nav').screenshot(path='scratch/nav_clean_dark_sistemas.png')
    
    # Light Mode
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'light')")
    page.locator('.nav__links a[href="#sistemas"]').click()
    page.wait_for_timeout(300)
    page.locator('.nav').screenshot(path='scratch/nav_clean_light_sistemas.png')
    
    browser.close()
    print("Clean nav screenshots taken successfully!")
