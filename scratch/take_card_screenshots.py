from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1280, 'height': 1000})
    page.goto('http://localhost:8080')
    page.wait_for_timeout(1000)

    # 1. Trustbar
    page.locator('.trustbar').screenshot(path='scratch/screen_trustbar.png')

    # 2. Sistemas section (specifically the two showcase cards)
    page.locator('#sistemas .grid--2').screenshot(path='scratch/screen_sistemas_grid2.png')

    # 3. Cases section (the 9 cards)
    page.locator('#cases .grid--3').screenshot(path='scratch/screen_cases_grid3.png')

    # 4. Light theme versions
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'light')")
    page.wait_for_timeout(500)
    page.locator('.trustbar').screenshot(path='scratch/screen_trustbar_light.png')
    page.locator('#sistemas .grid--2').screenshot(path='scratch/screen_sistemas_grid2_light.png')
    page.locator('#cases .grid--3').screenshot(path='scratch/screen_cases_grid3_light.png')

    browser.close()

print('Screenshots captured successfully!')
