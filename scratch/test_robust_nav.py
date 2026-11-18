from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Inject our new bulletproof click & scrollspy script
    page.evaluate('''() => {
      const allNavLinks = Array.from(document.querySelectorAll('.nav__links a[href^="#"], .nav__mobile a[href^="#"]'));
      const targetMap = [];
      let isClickScrolling = false;
      let clickTargetHref = null;
      let scrollEndTimer = null;

      // Map targets
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

        // If at top in Hero
        if (scrollY < 200) {
          setActiveLink(null);
          return;
        }

        // If at bottom of page
        if (windowHeight + scrollY >= documentHeight - 60) {
          const lastTarget = targetMap[targetMap.length - 1];
          if (lastTarget) {
            setActiveLink(lastTarget.id);
            return;
          }
        }

        // Section closest to header top
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

      // Click handler
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

              // Close mobile if open
              const navMobile = document.getElementById('nav-mobile');
              const burgerBtn = document.getElementById('nav-toggle-btn');
              if (navMobile && navMobile.classList.contains('is-open')) {
                navMobile.classList.remove('is-open');
                if (burgerBtn) burgerBtn.setAttribute('aria-expanded', 'false');
              }
            }
          }
        });
      });

      // Handle scroll
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

      window.initTestNavSpy = { setActiveLink, updateActiveOnScroll };
    }''')

    links = [
        ("Sistemas Sob Medida", "#sistemas"),
        ("Diagnóstico", "#solucoes"),
        ("Experiência", "#cases"),
        ("Planos & Preços", "#planos"),
        ("Equipe & Governança", "#sobre"),
        ("Contato", "#contato")
    ]
    
    print("\nVERIFYING ALL CLICKS WITH NEW ENGINE:")
    all_success = True
    for label, target in links:
        link_locator = page.locator(f'.nav__links a[href="{target}"]')
        link_locator.click()
        
        # Wait until smooth scrolling completes
        page.wait_for_timeout(1800)
        
        active_links = page.evaluate('''() => {
            return Array.from(document.querySelectorAll('.nav__links a.is-active'))
                .map(a => a.getAttribute('href'));
        }''')
        
        is_target_active = (active_links == [target])
        if not is_target_active:
            all_success = False
        print(f"[{'PASS' if is_target_active else 'FAIL'}] Click '{label}' ({target}) -> Active: {active_links}")
        
    print(f"\nALL CLICKS PASSED: {all_success}")
    browser.close()
