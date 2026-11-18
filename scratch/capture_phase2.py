import asyncio
from playwright.async_api import async_playwright

async def capture():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        
        # 1. Desktop Dark - Scrolled to 45% (showing reading bar, scrollspy on cases, and exec dock)
        context_dark = await browser.new_context(viewport={'width': 1280, 'height': 800})
        page = await context_dark.new_page()
        await page.goto('http://localhost:8080')
        await page.evaluate("() => { localStorage.setItem('trd-theme', 'dark'); document.documentElement.setAttribute('data-theme', 'dark'); }")
        await page.reload(wait_until='networkidle')
        
        # Scroll to #cases
        cases_el = page.locator('#cases')
        await cases_el.scroll_into_view_if_needed()
        await page.wait_for_timeout(600)
        
        # Capture full viewport showing sticky nav with progress bar + active link, and exec dock at bottom right
        await page.screenshot(path='scratch/phase2_desktop_scrolled_dock_dark.png')
        
        # Capture zoom on header showing progress bar and active link
        await page.screenshot(path='scratch/phase2_header_progress_scrollspy.png', clip={'x': 0, 'y': 0, 'width': 1280, 'height': 120})
        
        # Capture zoom on the floating executive dock
        dock = page.locator('#exec-dock')
        await dock.screenshot(path='scratch/phase2_exec_dock_zoom.png')

        # 2. Mobile Dark (375px) - Scrolled to #cases
        context_mobile = await browser.new_context(viewport={'width': 375, 'height': 667})
        page_mobile = await context_mobile.new_page()
        await page_mobile.goto('http://localhost:8080')
        await page_mobile.evaluate("() => { localStorage.setItem('trd-theme', 'dark'); document.documentElement.setAttribute('data-theme', 'dark'); }")
        await page_mobile.reload(wait_until='networkidle')
        
        cases_mobile = page_mobile.locator('#cases')
        await cases_mobile.scroll_into_view_if_needed()
        await page_mobile.wait_for_timeout(600)
        await page_mobile.screenshot(path='scratch/phase2_mobile_scrolled_dock_dark.png')

        await browser.close()
        print('Captured all Phase 2 preview screenshots successfully!')

asyncio.run(capture())
