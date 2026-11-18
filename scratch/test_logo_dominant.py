from playwright.sync_api import sync_playwright

test_css = """
/* TRUSTBAR BADGE: LOGO DOMINANT (NOME MENOR, LOGO MAIOR - SEMPRE NOME < LOGO) */
.trustbar__track {
  gap: 1.25rem !important;
  align-items: stretch !important;
}

.trustbar__badge {
  display: inline-flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  gap: .5rem !important;
  padding: .85rem 1.15rem .65rem !important;
  min-width: 145px !important;
  max-width: 175px !important;
  border-radius: 14px !important;
  background: #FFFFFF !important;
  border: 1px solid rgba(226, 232, 240, 0.95) !important;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.04) !important;
  transition: all .2s ease !important;
  text-align: center !important;
  white-space: normal !important;
}

[data-theme="dark"] .trustbar__badge,
:root:not([data-theme="light"]) .trustbar__badge {
  background: #FFFFFF !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35), 0 1px 3px rgba(0, 0, 0, 0.1) !important;
}

.trustbar__badge:hover {
  border-color: #D97706 !important;
  transform: translateY(-3px) !important;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.22) !important;
}

/* LOGO CHIP: LARGE HERO CONTAINER */
.trustbar__logo-chip {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  height: 48px !important;
  width: 100% !important;
  padding: 0 !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

/* LOGOS: SIGNIFICANTLY LARGER & CRISP */
.trustbar__img {
  object-fit: contain !important;
  display: block !important;
  filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.08)) !important;
}

/* Brasões / Selos 1:1 (44px) */
.trustbar__badge--dou .trustbar__img,
.trustbar__badge--mjsp .trustbar__img,
.trustbar__badge--animalworld .trustbar__img {
  height: 44px !important;
  width: 44px !important;
  max-width: 44px !important;
  max-height: 44px !important;
}

/* Horizontal Marks (30px-34px height, up to 90px width) */
.trustbar__badge--serasa .trustbar__img {
  height: 30px !important;
  max-height: 30px !important;
  width: auto !important;
  max-width: 95px !important;
}
.trustbar__badge--cvm .trustbar__img {
  height: 28px !important;
  max-height: 28px !important;
  width: auto !important;
  max-width: 85px !important;
}
.trustbar__badge--inep .trustbar__img {
  height: 28px !important;
  max-height: 28px !important;
  width: auto !important;
  max-width: 75px !important;
}
.trustbar__badge--alergs .trustbar__img {
  height: 30px !important;
  max-height: 30px !important;
  width: auto !important;
  max-width: 75px !important;
}
.trustbar__badge--alertaseg .trustbar__img {
  height: 30px !important;
  max-height: 30px !important;
  width: auto !important;
  max-width: 75px !important;
}

/* NOME: SUBTLE, COMPACT MICRO-TYPOGRAPHY (SEMPRE NOME < LOGO) */
.trustbar__badge > span:last-child {
  font-size: .6875rem !important; /* 11px */
  font-weight: 600 !important;
  color: #64748B !important;
  line-height: 1.2 !important;
  letter-spacing: .015em !important;
  text-align: center !important;
  display: block !important;
  max-width: 140px !important;
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
    trustbar_dark.screenshot(path='scratch/test_dominant_dark.png')

    mjsp_badge = page_dark.locator('.trustbar__badge--mjsp').first
    mjsp_badge.screenshot(path='scratch/test_dominant_mjsp_zoom.png')

    serasa_badge = page_dark.locator('.trustbar__badge--serasa').first
    serasa_badge.screenshot(path='scratch/test_dominant_serasa_zoom.png')

    # Shift to see second set
    page_dark.evaluate("""() => {
        const track = document.querySelector('.trustbar__track');
        track.style.animation = 'none';
        track.style.transform = 'translateX(-650px)';
    }""")
    page_dark.wait_for_timeout(300)
    trustbar_dark.screenshot(path='scratch/test_dominant_dark_shift.png')

    # Light mode
    ctx_light = browser.new_context(viewport={'width': 1280, 'height': 800})
    page_light = ctx_light.new_page()
    page_light.goto('http://localhost:8080')
    page_light.evaluate("() => { localStorage.setItem('trd-theme', 'light'); document.documentElement.setAttribute('data-theme', 'light'); }")
    page_light.reload()
    page_light.wait_for_timeout(1000)
    page_light.add_style_tag(content=test_css)
    page_light.evaluate("() => document.querySelector('.trustbar__track').style.animationPlayState = 'paused'")
    
    trustbar_light = page_light.locator('.trustbar')
    trustbar_light.screenshot(path='scratch/test_dominant_light.png')

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
    trustbar_mob.screenshot(path='scratch/test_dominant_mobile.png')

    browser.close()

print('Logo dominant screenshots captured successfully')
