from playwright.sync_api import sync_playwright

test_css = """
.trustbar__track {
  gap: 1rem !important;
  align-items: stretch !important;
}

/* VERTICAL LOCKUP: LOGO TOP, TEXT BOTTOM */
.trustbar__badge {
  display: inline-flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  gap: .45rem !important;
  padding: .75rem 1.15rem .65rem !important;
  min-width: 140px !important;
  max-width: 180px !important;
  border-radius: 12px !important;
  background: #FFFFFF !important;
  border: 1px solid rgba(226, 232, 240, 0.95) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.04) !important;
  text-align: center !important;
  white-space: normal !important;
}

[data-theme="dark"] .trustbar__badge,
:root:not([data-theme="light"]) .trustbar__badge {
  background: #FFFFFF !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.35), 0 1px 3px rgba(0, 0, 0, 0.1) !important;
}

.trustbar__badge:hover {
  border-color: #D97706 !important;
  transform: translateY(-3px) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.22) !important;
}

.trustbar__logo-chip {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  height: 36px !important;
  width: 100% !important;
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
}

.trustbar__img {
  height: 32px !important;
  max-height: 32px !important;
  max-width: 80px !important;
  object-fit: contain !important;
}

.trustbar__badge--dou .trustbar__img,
.trustbar__badge--mjsp .trustbar__img,
.trustbar__badge--animalworld .trustbar__img {
  height: 34px !important;
  width: 34px !important;
  max-width: 34px !important;
  max-height: 34px !important;
}

/* TEXT: CENTERED UNDERNEATH */
.trustbar__badge > span:last-child {
  font-size: .75rem !important;
  font-weight: 600 !important;
  color: #334155 !important;
  line-height: 1.25 !important;
  text-align: center !important;
  display: block !important;
}
"""

with sync_playwright() as p:
    browser = p.chromium.launch()
    
    # Dark mode
    ctx_dark = browser.new_context(viewport={'width': 1280, 'height': 800})
    page_dark = ctx_dark.new_page()
    page_dark.goto('http://localhost:8080')
    page_dark.evaluate("() => localStorage.setItem('trd-theme', 'dark')")
    page_dark.reload()
    page_dark.wait_for_timeout(1000)
    page_dark.add_style_tag(content=test_css)
    page_dark.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    trustbar_dark = page_dark.locator('.trustbar')
    trustbar_dark.screenshot(path='scratch/test_vertical_lockup_dark.png')

    mjsp_badge = page_dark.locator('.trustbar__badge--mjsp').first
    mjsp_badge.screenshot(path='scratch/test_vertical_mjsp_zoom.png')

    # Mobile Dark (375px)
    ctx_mob = browser.new_context(viewport={'width': 375, 'height': 812})
    page_mob = ctx_mob.new_page()
    page_mob.goto('http://localhost:8080')
    page_mob.evaluate("() => localStorage.setItem('trd-theme', 'dark')")
    page_mob.reload()
    page_mob.wait_for_timeout(1000)
    page_mob.add_style_tag(content=test_css)
    page_mob.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    trustbar_mob = page_mob.locator('.trustbar')
    trustbar_mob.screenshot(path='scratch/test_vertical_mobile_dark.png')

    browser.close()

print('Vertical lockup screenshots captured')
