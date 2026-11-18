from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    
    # Variant 1: Clean Executive White (Zero Orange)
    page.add_style_tag(content='''
        .nav__links a.is-active {
            color: #FFFFFF !important;
            font-weight: 600 !important;
        }
        .nav__links a:hover {
            color: #FFFFFF !important;
        }
        .nav__links a::after {
            background: #FFFFFF !important;
            box-shadow: 0 1px 3px rgba(255, 255, 255, 0.25) !important;
            height: 2px !important;
        }
    ''')
    page.locator('.nav').screenshot(path='scratch/nav_v1_white_underline.png')
    
    # Variant 2: Clean Satin Pill (No Underline, Zero Orange)
    page.add_style_tag(content='''
        .nav__links a::after {
            display: none !important;
        }
        .nav__links a.is-active {
            color: #FFFFFF !important;
            font-weight: 600 !important;
            background: rgba(255, 255, 255, 0.08) !important;
            border-radius: 6px !important;
            padding-inline: .65rem !important;
        }
    ''')
    page.locator('.nav').screenshot(path='scratch/nav_v2_satin_pill.png')

    # Variant 3: No persistent active state (Neutral clean links, zero orange)
    page.add_style_tag(content='''
        .nav__links a.is-active {
            color: var(--text-2) !important;
            font-weight: 500 !important;
            background: transparent !important;
            padding-inline: .25rem !important;
        }
        .nav__links a:hover {
            color: #FFFFFF !important;
        }
    ''')
    page.locator('.nav').screenshot(path='scratch/nav_v3_neutral.png')
    
    browser.close()
    print("Screenshots of all variants taken!")
