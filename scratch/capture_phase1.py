import asyncio
from playwright.async_api import async_playwright

async def capture():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        
        # 1. Desktop Dark
        context_dark = await browser.new_context(viewport={'width': 1280, 'height': 800})
        page = await context_dark.new_page()
        await page.goto('http://localhost:8080')
        await page.evaluate("() => { localStorage.setItem('trd-theme', 'dark'); document.documentElement.setAttribute('data-theme', 'dark'); }")
        await page.reload(wait_until='networkidle')
        await page.screenshot(path='scratch/phase1_desktop_hero_dark.png', clip={'x': 0, 'y': 0, 'width': 1280, 'height': 720})
        
        term = page.locator('.terminal')
        await term.scroll_into_view_if_needed()
        await page.wait_for_timeout(300)
        await term.screenshot(path='scratch/phase1_terminal_dark.png')
        
        # 2. Desktop Light
        context_light = await browser.new_context(viewport={'width': 1280, 'height': 800})
        page_light = await context_light.new_page()
        await page_light.goto('http://localhost:8080')
        await page_light.evaluate("() => { localStorage.setItem('trd-theme', 'light'); document.documentElement.setAttribute('data-theme', 'light'); }")
        await page_light.reload(wait_until='networkidle')
        await page_light.screenshot(path='scratch/phase1_desktop_hero_light.png', clip={'x': 0, 'y': 0, 'width': 1280, 'height': 720})
        
        term_light = page_light.locator('.terminal')
        await term_light.scroll_into_view_if_needed()
        await page_light.wait_for_timeout(300)
        await term_light.screenshot(path='scratch/phase1_terminal_light.png')

        # 3. Mobile Dark (375px)
        context_mobile = await browser.new_context(viewport={'width': 375, 'height': 667})
        page_mobile = await context_mobile.new_page()
        await page_mobile.goto('http://localhost:8080')
        await page_mobile.evaluate("() => { localStorage.setItem('trd-theme', 'dark'); document.documentElement.setAttribute('data-theme', 'dark'); }")
        await page_mobile.reload(wait_until='networkidle')
        await page_mobile.screenshot(path='scratch/phase1_mobile_hero_dark.png', clip={'x': 0, 'y': 0, 'width': 375, 'height': 650})
        
        term_mobile = page_mobile.locator('.terminal')
        await term_mobile.scroll_into_view_if_needed()
        await page_mobile.wait_for_timeout(300)
        await term_mobile.screenshot(path='scratch/phase1_mobile_terminal_dark.png')

        await browser.close()
        print('Captured all Phase 1 preview screenshots successfully!')

asyncio.run(capture())
