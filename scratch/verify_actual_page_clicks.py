from playwright.sync_api import sync_playwright

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
    
    print("TESTING ACTUAL SITE/INDEX.HTML (NO INJECTIONS):")
    results = []
    for label, target in links:
        link_locator = page.locator(f'.nav__links a[href="{target}"]')
        link_locator.click()
        
        # Immediate check (100ms)
        page.wait_for_timeout(100)
        active_immediate = page.evaluate('Array.from(document.querySelectorAll(".nav__links a.is-active")).map(a => a.getAttribute("href"))')
        
        # Settled check (1800ms)
        page.wait_for_timeout(1800)
        active_settled = page.evaluate('Array.from(document.querySelectorAll(".nav__links a.is-active")).map(a => a.getAttribute("href"))')
        
        # Take screenshot of navbar
        page.locator('.nav').screenshot(path=f'scratch/nav_live_click_{target[1:]}.png')
        
        passed = (active_immediate == [target] and active_settled == [target])
        results.append((label, target, passed, active_immediate, active_settled))
        print(f"[{'PASS' if passed else 'FAIL'}] '{label}' ({target}): immediate={active_immediate}, settled={active_settled}")
        
    all_passed = all(r[2] for r in results)
    print(f"\nALL CLICKS PASSED LIVE: {all_passed}")
    browser.close()
