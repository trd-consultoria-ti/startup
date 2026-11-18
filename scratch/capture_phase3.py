import asyncio
from playwright.async_api import async_playwright

async def capture():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        
        # 1. Desktop Dark - Plans Section with Filter & Border-Beam
        page = await browser.new_page(viewport={'width': 1280, 'height': 850})
        await page.goto('http://localhost:8080')
        await page.evaluate("() => { localStorage.setItem('trd-theme', 'dark'); document.documentElement.setAttribute('data-theme', 'dark'); }")
        await page.reload(wait_until='networkidle')
        
        # Scroll to #planos
        planos_el = page.locator('#planos')
        await planos_el.scroll_into_view_if_needed()
        await page.wait_for_timeout(500)
        
        # Capture plans filter and the creation cards with Hub Pro border beam
        await page.screenshot(path='scratch/phase3_plans_filter_and_beam_dark.png', clip={'x': 100, 'y': 250, 'width': 1080, 'height': 650})
        
        # Test Spotlight by hovering over a card
        card = page.locator('#bloco-criacao .card--beam')
        box = await card.bounding_box()
        if box:
            await page.mouse.move(box['x'] + box['width'] / 2, box['y'] + box['height'] / 2)
            await page.wait_for_timeout(300)
            await card.screenshot(path='scratch/phase3_spotlight_card_beam_zoom.png')
            
        # Test filtering: click "Sustentação & SLA Contratual"
        btn_sust = page.locator('.plans-filter__btn[data-filter="sustentacao"]')
        await btn_sust.click()
        await page.wait_for_timeout(400)
        await page.screenshot(path='scratch/phase3_plans_filter_sustentacao_active.png', clip={'x': 100, 'y': 200, 'width': 1080, 'height': 650})

        # 2. Mobile Dark (375px)
        page_mobile = await browser.new_page(viewport={'width': 375, 'height': 667})
        await page_mobile.goto('http://localhost:8080')
        await page_mobile.evaluate("() => { localStorage.setItem('trd-theme', 'dark'); document.documentElement.setAttribute('data-theme', 'dark'); }")
        await page_mobile.reload(wait_until='networkidle')
        
        planos_mobile = page_mobile.locator('#planos')
        await planos_mobile.scroll_into_view_if_needed()
        await page_mobile.wait_for_timeout(500)
        await page_mobile.screenshot(path='scratch/phase3_mobile_plans_filter_dark.png', clip={'x': 0, 'y': 150, 'width': 375, 'height': 500})

        await browser.close()
        print('Captured all Phase 3 preview screenshots successfully!')

asyncio.run(capture())
