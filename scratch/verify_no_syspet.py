from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # 1. Trustbar Animal World text
    tb_text = page.locator('.trustbar__badge--animalworld span:last-child').first.inner_text().strip()
    print("Trustbar Animal World text:", tb_text)
    
    # 2. Sistemas Animal World title
    sistemas_title = page.locator('#sistemas h3:has-text("Animal World")').inner_text().strip()
    print("Sistemas title:", sistemas_title)
    
    # 3. Cases Animal World title
    cases_title = page.locator('#cases h3:has-text("Animal World")').inner_text().strip()
    print("Cases title:", cases_title)

    # 4. Check badge dimensions
    tb_badge = page.locator('.trustbar__badge--animalworld').first.bounding_box()
    print("Trustbar Animal World badge size:", f"{tb_badge['width']:.2f}px x {tb_badge['height']:.2f}px")
    
    browser.close()
