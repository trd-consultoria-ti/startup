from playwright.sync_api import sync_playwright

test_css = """
/* REMOVE ANTHROPIC FROM TRUSTBAR */
.trustbar__badge--anthropic {
  display: none !important;
}

/* TRUSTBAR BADGE: LOGO-FIRST HIERARCHY */
.trustbar__badge {
  display: inline-flex;
  align-items: center;
  gap: .75rem;
  padding: .4rem .95rem .4rem .55rem;
  border-radius: 9999px;
  background: #FFFFFF;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all .2s ease;
  white-space: nowrap;
}

[data-theme="dark"] .trustbar__badge,
:root:not([data-theme="light"]) .trustbar__badge {
  background: #FFFFFF;
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.3), 0 1px 3px rgba(0, 0, 0, 0.1);
}

.trustbar__badge:hover {
  border-color: #D97706;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.18);
}

/* LOGO CHIP: PROMINENT & HIGH-VISIBILITY */
.trustbar__logo-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  min-width: 36px;
  padding: 3px 6px;
  border-radius: 9999px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 1px 3px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.trustbar__img {
  height: 24px;
  width: auto;
  max-width: 38px;
  max-height: 24px;
  object-fit: contain;
  display: block;
}

/* MJSP Brasão: extra prominent */
.trustbar__badge--mjsp .trustbar__img {
  width: 26px;
  height: 26px;
  max-width: 26px;
  max-height: 26px;
}
.trustbar__badge--dou .trustbar__img {
  width: 24px;
  height: 24px;
  max-width: 24px;
  max-height: 24px;
}

/* TEXT: ELEGANT, REFINED SECONDARY LABEL */
.trustbar__badge > span:last-child {
  font-size: .8rem;
  font-weight: 600;
  color: #1E293B;
  letter-spacing: .01em;
}
"""

with sync_playwright() as p:
    browser = p.chromium.launch()
    
    # 1. Dark mode
    ctx_dark = browser.new_context(viewport={'width': 1280, 'height': 800})
    page_dark = ctx_dark.new_page()
    page_dark.goto('http://localhost:8080')
    page_dark.evaluate("() => localStorage.setItem('trd-theme', 'dark')")
    page_dark.reload()
    page_dark.wait_for_timeout(1000)
    page_dark.add_style_tag(content=test_css)
    page_dark.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    trustbar_dark = page_dark.locator('.trustbar')
    trustbar_dark.screenshot(path='scratch/test_logo_prominent_dark.png')
    
    mjsp_badge = page_dark.locator('.trustbar__badge--mjsp').first
    mjsp_badge.screenshot(path='scratch/test_mjsp_prominent_zoom.png')

    # Shift to see second set
    page_dark.evaluate("""() => {
        const track = document.querySelector('.trustbar__track');
        track.style.animation = 'none';
        track.style.transform = 'translateX(-700px)';
    }""")
    page_dark.wait_for_timeout(300)
    trustbar_dark.screenshot(path='scratch/test_logo_prominent_dark_shift.png')

    # 2. Light mode
    ctx_light = browser.new_context(viewport={'width': 1280, 'height': 800})
    page_light = ctx_light.new_page()
    page_light.goto('http://localhost:8080')
    page_light.evaluate("() => { localStorage.setItem('trd-theme', 'light'); document.documentElement.setAttribute('data-theme', 'light'); }")
    page_light.reload()
    page_light.wait_for_timeout(1000)
    page_light.add_style_tag(content=test_css)
    page_light.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    trustbar_light = page_light.locator('.trustbar')
    trustbar_light.screenshot(path='scratch/test_logo_prominent_light.png')

    browser.close()

print('Logo prominent test screenshots captured successfully')
