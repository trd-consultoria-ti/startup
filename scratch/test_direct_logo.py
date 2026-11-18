from playwright.sync_api import sync_playwright

test_css = """
/* REMOVE ANTHROPIC FROM TRUSTBAR */
.trustbar__badge--anthropic {
  display: none !important;
}

/* TRUSTBAR BADGE: ELEGANT WHITE CAPSULE WITH PROMINENT LOGO */
.trustbar__badge {
  display: inline-flex;
  align-items: center;
  gap: .85rem;
  padding: .5rem 1.15rem .5rem .85rem;
  border-radius: 9999px;
  background: #FFFFFF;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all .2s ease;
  white-space: nowrap;
}

[data-theme="dark"] .trustbar__badge,
:root:not([data-theme="light"]) .trustbar__badge {
  background: #FFFFFF;
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35), 0 1px 3px rgba(0, 0, 0, 0.1);
}

.trustbar__badge:hover {
  border-color: #D97706;
  transform: translateY(-2px);
  box-shadow: 0 6px 22px rgba(0, 0, 0, 0.2);
}

/* LOGO CHIP: SEAMLESS FRAME THAT LETS LOGOS SHINE */
.trustbar__logo-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  width: auto;
  min-width: 32px;
  padding: 0;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  flex-shrink: 0;
}

.trustbar__img {
  height: 28px !important;
  width: auto !important;
  max-width: 80px !important;
  max-height: 28px !important;
  object-fit: contain !important;
  display: block !important;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.06));
}

/* Brasões / Selos 1:1 */
.trustbar__badge--dou .trustbar__img,
.trustbar__badge--mjsp .trustbar__img,
.trustbar__badge--animalworld .trustbar__img {
  height: 30px !important;
  width: 30px !important;
  max-width: 30px !important;
  max-height: 30px !important;
}

/* Horizontal Marks (Serasa, CVM, INEP, ALERGS, AlertaSeg) */
.trustbar__badge--serasa .trustbar__img {
  height: 22px !important;
  max-width: 70px !important;
}
.trustbar__badge--cvm .trustbar__img {
  height: 20px !important;
  max-width: 65px !important;
}
.trustbar__badge--inep .trustbar__img {
  height: 20px !important;
  max-width: 55px !important;
}
.trustbar__badge--alergs .trustbar__img {
  height: 22px !important;
  max-width: 55px !important;
}
.trustbar__badge--alertaseg .trustbar__img {
  height: 22px !important;
  max-width: 55px !important;
}

/* TEXT: SUBTLE, HIGH-END SECONDARY CORPORATIVE LABEL */
.trustbar__badge > span:last-child {
  font-size: .8125rem;
  font-weight: 600;
  color: #334155;
  letter-spacing: .005em;
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
    trustbar_dark.screenshot(path='scratch/test_direct_logo_dark.png')
    
    mjsp_badge = page_dark.locator('.trustbar__badge--mjsp').first
    mjsp_badge.screenshot(path='scratch/test_direct_mjsp_zoom.png')

    serasa_badge = page_dark.locator('.trustbar__badge--serasa').first
    serasa_badge.screenshot(path='scratch/test_direct_serasa_zoom.png')

    # Shift to see second half
    page_dark.evaluate("""() => {
        const track = document.querySelector('.trustbar__track');
        track.style.animation = 'none';
        track.style.transform = 'translateX(-700px)';
    }""")
    page_dark.wait_for_timeout(300)
    trustbar_dark.screenshot(path='scratch/test_direct_logo_dark_shift.png')

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
    trustbar_light.screenshot(path='scratch/test_direct_logo_light.png')

    browser.close()

print('Direct logo test screenshots captured successfully')
