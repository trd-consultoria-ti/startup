from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Click #contato
    print("Clicking #contato...")
    page.locator('.nav__links a[href="#contato"]').click()
    
    # Check scrollY over time
    for i in range(15):
        page.wait_for_timeout(300)
        y = page.evaluate('window.scrollY')
        rect_contato = page.evaluate('document.querySelector("#contato").getBoundingClientRect().top')
        active = page.evaluate('Array.from(document.querySelectorAll(".nav__links a.is-active")).map(a => a.getAttribute("href"))')
        print(f"Time {(i+1)*300}ms: scrollY={y:.0f}px, contato.top={rect_contato:.0f}px, active={active}")
        
    browser.close()
