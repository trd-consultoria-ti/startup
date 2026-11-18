from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Standardize all chips in the DOM
    page.evaluate('''() => {
        const chips = document.querySelectorAll('.card__logo-chip');
        chips.forEach(chip => {
            chip.className = 'card__logo-chip';
            const img = chip.querySelector('img');
            if (img) {
                img.className = 'card__brand-logo';
                img.removeAttribute('width');
                img.removeAttribute('height');
            }
        });
    }''')
    
    # Inject prototype styling
    page.add_style_tag(content='''
        .card__logo-chip {
            display: inline-flex !important;
            align-items: center !important;
            justify-content: center !important;
            width: 68px !important;
            height: 38px !important;
            min-width: 68px !important;
            max-width: 68px !important;
            flex: 0 0 68px !important;
            padding: 4px 8px !important;
            border-radius: 10px !important;
            background: #FFFFFF !important;
            border: 1px solid rgba(226, 232, 240, 0.95) !important;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04) !important;
            box-sizing: border-box !important;
            flex-shrink: 0 !important;
            transition: all .2s ease !important;
        }
        [data-theme="dark"] .card__logo-chip,
        :root:not([data-theme="light"]) .card__logo-chip {
            background: #FFFFFF !important;
            border: 1px solid rgba(255, 255, 255, 0.25) !important;
            box-shadow: 0 4px 14px rgba(0, 0, 0, 0.35), 0 1px 2px rgba(0, 0, 0, 0.1) !important;
        }
        .card:hover .card__logo-chip {
            border-color: #D97706 !important;
            transform: translateY(-2px) !important;
            box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25) !important;
        }
        .card__brand-logo {
            max-width: 100% !important;
            max-height: 26px !important;
            width: auto !important;
            height: auto !important;
            object-fit: contain !important;
            display: block !important;
            filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.06)) !important;
        }
        /* Selos 1:1 (DOU, MJSP, Animal World, Claude) */
        .card--dou .card__brand-logo,
        .card--mjsp .card__brand-logo,
        .card--animalworld .card__brand-logo,
        .card--anthropic .card__brand-logo {
            max-height: 28px !important;
            max-width: 28px !important;
        }
        /* No card de sistemas, animal world */
        .card:has(img[src*="logo-animal-world"]) .card__brand-logo {
            max-height: 28px !important;
            max-width: 28px !important;
        }
        .card:has(img[src*="logo-imprensa-nacional"]) .card__brand-logo {
            max-height: 28px !important;
            max-width: 28px !important;
        }
        .card:has(img[src*="logo-mjsp"]) .card__brand-logo {
            max-height: 28px !important;
            max-width: 28px !important;
        }
        .card:has(img[src*="logo-claude"]) .card__brand-logo {
            max-height: 26px !important;
            max-width: 26px !important;
        }
    ''')
    
    # 1. Capture dark mode screenshots
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    page.locator('#sistemas').screenshot(path='scratch/test_std_chips_sistemas_dark.png')
    page.locator('#cases').screenshot(path='scratch/test_std_chips_cases_dark.png')
    
    # 2. Capture light mode screenshots
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'light')")
    page.locator('#sistemas').screenshot(path='scratch/test_std_chips_sistemas_light.png')
    page.locator('#cases').screenshot(path='scratch/test_std_chips_cases_light.png')

    # Inspect all chips
    chips_data = page.evaluate('''() => {
        const els = Array.from(document.querySelectorAll('.card__logo-chip'));
        return els.map((el, i) => {
            const rect = el.getBoundingClientRect();
            const img = el.querySelector('img');
            const cardTitle = el.closest('.card') ? el.closest('.card').querySelector('h3').innerText.trim() : 'N/A';
            return {
                index: i,
                title: cardTitle,
                chip: `${rect.width.toFixed(1)}x${rect.height.toFixed(1)}px`,
                img: img ? `${img.getBoundingClientRect().width.toFixed(1)}x${img.getBoundingClientRect().height.toFixed(1)}px` : 'N/A'
            };
        });
    }''')
    print("STANDARDIZED CHIPS DATA:")
    for c in chips_data:
        print(f"[{c['index']}] {c['title']}: chip={c['chip']}, img={c['img']}")
        
    browser.close()
