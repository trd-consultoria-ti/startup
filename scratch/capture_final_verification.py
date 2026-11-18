from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    
    # 1. Desktop Dark
    ctx_dark = browser.new_context(viewport={'width': 1280, 'height': 800})
    p_dark = ctx_dark.new_page()
    p_dark.goto('http://localhost:8080')
    p_dark.evaluate("() => localStorage.setItem('trd-theme', 'dark')")
    p_dark.reload()
    p_dark.wait_for_timeout(1000)
    p_dark.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    trustbar_dark = p_dark.locator('.trustbar')
    trustbar_dark.screenshot(path='scratch/trustbar_final_dark.png')
    
    mjsp_badge = p_dark.locator('.trustbar__badge--mjsp').first
    mjsp_badge.screenshot(path='scratch/mjsp_badge_final_zoom.png')
    
    cases_dark = p_dark.locator('#cases')
    cases_dark.screenshot(path='scratch/cases_final_dark.png')
    
    # 2. Desktop Light
    ctx_light = browser.new_context(viewport={'width': 1280, 'height': 800})
    p_light = ctx_light.new_page()
    p_light.goto('http://localhost:8080')
    p_light.evaluate("() => { localStorage.setItem('trd-theme', 'light'); document.documentElement.setAttribute('data-theme', 'light'); }")
    p_light.reload()
    p_light.wait_for_timeout(1000)
    p_light.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    trustbar_light = p_light.locator('.trustbar')
    trustbar_light.screenshot(path='scratch/trustbar_final_light.png')
    
    cases_light = p_light.locator('#cases')
    cases_light.screenshot(path='scratch/cases_final_light.png')

    # 3. Mobile Dark (375px)
    ctx_mob = browser.new_context(viewport={'width': 375, 'height': 812})
    p_mob = ctx_mob.new_page()
    p_mob.goto('http://localhost:8080')
    p_mob.evaluate("() => localStorage.setItem('trd-theme', 'dark')")
    p_mob.reload()
    p_mob.wait_for_timeout(1000)
    trustbar_mob = p_mob.locator('.trustbar')
    trustbar_mob.screenshot(path='scratch/mobile_trustbar_dark.png')

    browser.close()

print('All final screenshots captured successfully')
