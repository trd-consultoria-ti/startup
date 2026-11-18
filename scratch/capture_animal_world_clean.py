from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    
    # 1. Animal World card in Sistemas
    card_sistemas = page.locator('#sistemas .card:has(img[src*="logo-animal-world"])')
    card_sistemas.screenshot(path='scratch/animal_world_sistemas_clean.png')
    
    # 2. Animal World card in Cases
    card_cases = page.locator('#cases .card--animalworld')
    card_cases.screenshot(path='scratch/animal_world_cases_clean.png')
    
    browser.close()
