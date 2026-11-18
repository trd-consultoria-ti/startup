from playwright.sync_api import sync_playwright

test_css = """
/* RESET NEON BORDERS ON TRUSTBAR BADGES */
.trustbar__badge--dou,
.trustbar__badge--inep,
.trustbar__badge--mjsp,
.trustbar__badge--serasa,
.trustbar__badge--cvm,
.trustbar__badge--alergs,
.trustbar__badge--alertaseg,
.trustbar__badge--animalworld,
.trustbar__badge--anthropic {
  background: #FFFFFF !important;
  border-color: rgba(226, 232, 240, 0.9) !important;
  color: #0F172A !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04) !important;
}

.trustbar__badge {
  background: #FFFFFF !important;
  color: #0F172A !important;
  border: 1px solid rgba(226, 232, 240, 0.9) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04) !important;
  padding: .45rem 1rem !important;
  font-weight: 600 !important;
  letter-spacing: .01em !important;
}

.trustbar__badge:hover {
  border-color: #D97706 !important;
  color: #0F172A !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.14) !important;
}

.trustbar__logo-chip {
  background: #F8FAFC !important;
  border: 1px solid #E2E8F0 !important;
  box-shadow: none !important;
  width: 28px !important;
  height: 28px !important;
  border-radius: 7px !important;
  padding: 3px !important;
}

/* MJSP BADGE REFINEMENT */
.trustbar__badge--mjsp .trustbar__logo-chip {
  background: #FFFFFF !important;
  border: 1px solid #E2E8F0 !important;
}
.trustbar__badge--mjsp .trustbar__img {
  width: 22px !important;
  height: 22px !important;
  max-width: 22px !important;
  object-fit: contain !important;
}

/* RESET NEON BORDERS ON CASE CARDS */
.card--dou,
.card--inep,
.card--mjsp,
.card--serasa,
.card--cvm,
.card--alergs,
.card--alertaseg,
.card--animalworld,
.card--anthropic {
  border-top: 1px solid var(--border) !important;
  background: var(--surface) !important;
}
"""

with sync_playwright() as p:
    browser = p.chromium.launch()
    
    # Dark mode test
    context_dark = browser.new_context(viewport={'width': 1280, 'height': 800})
    page_dark = context_dark.new_page()
    page_dark.goto('http://localhost:8080')
    page_dark.evaluate("() => localStorage.setItem('trd-theme', 'dark')")
    page_dark.reload()
    page_dark.wait_for_timeout(1000)
    page_dark.add_style_tag(content=test_css)
    page_dark.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    trustbar_dark = page_dark.locator('.trustbar')
    trustbar_dark.screenshot(path='scratch/test_trustbar_white_dark.png')
    
    mjsp_badge = page_dark.locator('.trustbar__badge--mjsp').first
    mjsp_badge.screenshot(path='scratch/test_mjsp_badge_zoom.png')

    cases_dark = page_dark.locator('#cases')
    cases_dark.screenshot(path='scratch/test_cases_clean_dark.png')

    # Light mode test
    context_light = browser.new_context(viewport={'width': 1280, 'height': 800})
    page_light = context_light.new_page()
    page_light.goto('http://localhost:8080')
    page_light.evaluate("() => { localStorage.setItem('trd-theme', 'light'); document.documentElement.setAttribute('data-theme', 'light'); }")
    page_light.reload()
    page_light.wait_for_timeout(1000)
    page_light.add_style_tag(content=test_css)
    page_light.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")

    trustbar_light = page_light.locator('.trustbar')
    trustbar_light.screenshot(path='scratch/test_trustbar_white_light.png')

    cases_light = page_light.locator('#cases')
    cases_light.screenshot(path='scratch/test_cases_clean_light.png')

    browser.close()

print('Test screenshots generated')
