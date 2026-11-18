from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 375, 'height': 812})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Inject new engine
    page.evaluate('''() => {
      const allNavLinks = Array.from(document.querySelectorAll('.nav__links a[href^="#"], .nav__mobile a[href^="#"]'));
      const targetMap = [];
      let isClickScrolling = false;
      let clickTargetHref = null;
      let scrollEndTimer = null;

      allNavLinks.forEach(function(link) {
        const href = link.getAttribute('href');
        if (href && href.startsWith('#') && href.length > 1) {
          const targetEl = document.querySelector(href);
          if (targetEl && !targetMap.some(function(t) { return t.id === href; })) {
            targetMap.push({ id: href, el: targetEl });
          }
        }
      });

      function setActiveLink(activeHref) {
        allNavLinks.forEach(function(link) {
          const isTarget = link.getAttribute('href') === activeHref;
          if (isTarget) {
            link.classList.add('is-active');
            link.setAttribute('aria-current', 'page');
          } else {
            link.classList.remove('is-active');
            link.removeAttribute('aria-current');
          }
        });
      }

      function updateActiveOnScroll() {
        if (isClickScrolling) return;

        const scrollY = window.scrollY || window.pageYOffset;
        const windowHeight = window.innerHeight;
        const documentHeight = document.documentElement.scrollHeight;

        if (scrollY < 200) {
          setActiveLink(null);
          return;
        }

        if (windowHeight + scrollY >= documentHeight - 60) {
          const lastTarget = targetMap[targetMap.length - 1];
          if (lastTarget) {
            setActiveLink(lastTarget.id);
            return;
          }
        }

        const headerOffset = 130;
        let currentActive = null;

        for (let i = 0; i < targetMap.length; i++) {
          const rect = targetMap[i].el.getBoundingClientRect();
          if (rect.top <= headerOffset) {
            currentActive = targetMap[i].id;
          }
        }

        if (currentActive) {
          setActiveLink(currentActive);
        }
      }

      allNavLinks.forEach(function(link) {
        link.addEventListener('click', function(e) {
          const href = link.getAttribute('href');
          if (href && href.startsWith('#')) {
            const targetEl = document.querySelector(href);
            if (targetEl) {
              isClickScrolling = true;
              clickTargetHref = href;
              setActiveLink(href);

              clearTimeout(scrollEndTimer);
              scrollEndTimer = setTimeout(function() {
                isClickScrolling = false;
                clickTargetHref = null;
                updateActiveOnScroll();
              }, 1800);

              const navMobile = document.getElementById('nav-mobile');
              const burgerBtn = document.getElementById('nav-toggle-btn');
              if (navMobile && navMobile.classList.contains('is-open')) {
                navMobile.classList.remove('is-open');
                if (burgerBtn) {
                  burgerBtn.setAttribute('aria-expanded', 'false');
                  const iconMenu = burgerBtn.querySelector('.nav__icon-menu');
                  const iconClose = burgerBtn.querySelector('.nav__icon-close');
                  if (iconMenu) iconMenu.style.display = 'block';
                  if (iconClose) iconClose.style.display = 'none';
                }
              }
            }
          }
        });
      });

      window.addEventListener('scroll', function() {
        if (isClickScrolling) {
          setActiveLink(clickTargetHref);
          clearTimeout(scrollEndTimer);
          scrollEndTimer = setTimeout(function() {
            isClickScrolling = false;
            clickTargetHref = null;
            updateActiveOnScroll();
          }, 200);
          return;
        }

        updateActiveOnScroll();
      }, { passive: true });

      if ('onscrollend' in window) {
        window.addEventListener('scrollend', function() {
          isClickScrolling = false;
          clickTargetHref = null;
          updateActiveOnScroll();
        }, { passive: true });
      }
    }''')

    # Test Mobile Menu Open and Click
    print("Testing Mobile Drawer Click:")
    burger = page.locator('#nav-toggle-btn')
    burger.click()
    page.wait_for_timeout(300)
    
    is_open = page.evaluate('document.getElementById("nav-mobile").classList.contains("is-open")')
    print("Mobile menu opened?", is_open)
    
    # Click Planos & Preços in mobile menu
    page.locator('#nav-mobile a[href="#planos"]').click()
    page.wait_for_timeout(1500)
    
    is_closed_after_click = not page.evaluate('document.getElementById("nav-mobile").classList.contains("is-open")')
    active_in_mobile = page.evaluate('document.querySelector("#nav-mobile a.is-active") ? document.querySelector("#nav-mobile a.is-active").getAttribute("href") : "NONE"')
    print("Mobile menu closed after click?", is_closed_after_click)
    print("Active link in mobile:", active_in_mobile)
    
    browser.close()
