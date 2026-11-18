import asyncio
from playwright.async_api import async_playwright

async def capture():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        
        # Desktop Dark - Scrolled to 45%
        context_dark = await browser.new_context(viewport={'width': 1280, 'height': 800})
        page = await context_dark.new_page()
        await page.goto('http://localhost:8080')
        await page.evaluate("() => { localStorage.setItem('trd-theme', 'dark'); document.documentElement.setAttribute('data-theme', 'dark'); }")
        await page.reload(wait_until='networkidle')
        
        # Scroll to #cases
        cases_el = page.locator('#cases')
        await cases_el.scroll_into_view_if_needed()
        await page.wait_for_timeout(500)
        
        # Capture nav
        nav = page.locator('.nav')
        await nav.screenshot(path='scratch/phase2_nav_scrolled_dark.png')
        
        # Capture dock
        dock = page.locator('#exec-dock')
        await dock.screenshot(path='scratch/phase2_exec_dock_zoom.png')

        await browser.close()
        print('Captured phase2_nav_scrolled_dark successfully!')

asyncio.run(capture())
