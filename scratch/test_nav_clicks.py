from playwright.sync_api import sync_playwright
import time

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    links = [
        ("Sistemas Sob Medida", "#sistemas"),
        ("Diagnóstico", "#solucoes"),
        ("Experiência", "#cases"),
        ("Planos & Preços", "#planos"),
        ("Equipe & Governança", "#sobre"),
        ("Contato", "#contato")
    ]
    
    print("TESTING CLICKS ON NAVBAR LINKS:")
    for label, target in links:
        link_locator = page.locator(f'.nav__links a[href="{target}"]')
        link_locator.click()
        
        # Wait for scroll animation to settle
        page.wait_for_timeout(1000)
        
        # Check active class across all links
        active_links = page.evaluate('''() => {
            return Array.from(document.querySelectorAll('.nav__links a.is-active'))
                .map(a => ({ text: a.innerText.trim(), href: a.getAttribute('href') }));
        }''')
        
        current_scroll_y = page.evaluate('window.scrollY')
        target_el_top = page.evaluate(f'''() => {{
            const el = document.querySelector("{target}");
            return el ? el.getBoundingClientRect().top : 'NOT FOUND';
        }}''')
        
        is_target_active = any(a['href'] == target for a in active_links)
        print(f"Clicked '{label}' ({target}):")
        print(f"  Target active? {is_target_active}")
        print(f"  Active links currently: {active_links}")
        print(f"  Target element relative top: {target_el_top}px, scrollY: {current_scroll_y}px")
        
        # Take screenshot of navbar after each click
        page.locator('.nav').screenshot(path=f'scratch/nav_after_click_{target[1:]}.png')
        
    browser.close()
