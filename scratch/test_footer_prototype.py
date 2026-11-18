from playwright.sync_api import sync_playwright

footer_html = '''
  <footer class="footer">
    <div class="container">
      <div class="footer__grid">
        <!-- Coluna 1: Marca & Identidade -->
        <div class="footer__brand-col">
          <a href="#" class="nav__brand" style="display:inline-block; margin-bottom:.5rem;">
            TRD<span class="nav__brand-dot">.</span>
          </a>
          <p class="footer__tagline">
            Engenharia de software de missão crítica, sistemas corporativos sob encomenda e governança técnica de alta disponibilidade para ecossistemas regulados.
          </p>
          <div class="footer__badges">
            <span class="footer__badge"><span class="footer__dot-live"></span> Brasília – DF</span>
            <span class="footer__badge">CNPJ 43.065.207/0001-57</span>
            <span class="footer__badge">Ativa desde 10/08/2021</span>
          </div>
        </div>

        <!-- Coluna 2: Soluções -->
        <div class="footer__col">
          <h4 class="footer__col-title">Soluções</h4>
          <ul class="footer__nav">
            <li><a href="#sistemas">Sistemas Sob Medida</a></li>
            <li><a href="#sistemas">Hubs de Gestão &amp; ERPs</a></li>
            <li><a href="#consultoria">Sustentação N3 &amp; SLA</a></li>
            <li><a href="#cases">Arquitetura de Microsserviços</a></li>
            <li><a href="#cases">IA Aplicada &amp; LLM Gateway</a></li>
            <li><a href="#planos">Planos &amp; Modelos</a></li>
          </ul>
        </div>

        <!-- Coluna 3: Cases & Clientes -->
        <div class="footer__col">
          <h4 class="footer__col-title">Cases &amp; Clientes</h4>
          <ul class="footer__nav">
            <li><a href="#cases">Imprensa Nacional (DOU)</a></li>
            <li><a href="#cases">INEP · ENADE (MEC)</a></li>
            <li><a href="#cases">Ministério da Justiça (MJSP)</a></li>
            <li><a href="#cases">Serasa Experian (eConsig)</a></li>
            <li><a href="#cases">CVM · Mercado de Capitais</a></li>
            <li><a href="#cases">ALERGS · Poder Legislativo</a></li>
          </ul>
        </div>

        <!-- Coluna 4: Contato Executivo -->
        <div class="footer__col footer__contact-col">
          <h4 class="footer__col-title">Contato Executivo</h4>
          <ul class="footer__contact-list">
            <li>
              <span class="footer__contact-label">WhatsApp Direto</span>
              <a href="https://wa.me/5561999526374" target="_blank" rel="noopener" class="footer__contact-link">
                (61) 99952-6374
              </a>
            </li>
            <li>
              <span class="footer__contact-label">E-mail Corporativo</span>
              <a href="mailto:contato@trdconsultoria.com.br" class="footer__contact-link">
                contato@trdconsultoria.com.br
              </a>
            </li>
            <li>
              <span class="footer__contact-label">Atendimento</span>
              <span class="footer__contact-text">Seg a Sex · 08h às 19h</span>
            </li>
            <li>
              <span class="footer__contact-label">Plantão Crítico</span>
              <span class="footer__contact-text">24/7 para clientes em sustentação N3</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Barra Inferior de Copyright & Compliance -->
      <div class="footer__bottom">
        <div class="footer__legal">
          <span>&copy; 2026 TRD Consultoria e Serviços de TI LTDA. Todos os direitos reservados.</span>
        </div>
        <div class="footer__links">
          <a href="privacidade.html">Privacidade &amp; LGPD</a>
          <span class="footer__sep" aria-hidden="true">&middot;</span>
          <span>Sede em Brasília/DF</span>
          <span class="footer__sep" aria-hidden="true">&middot;</span>
          <span class="footer__status-badge"><span class="footer__dot-live"></span> Operação 99.9% Uptime</span>
        </div>
      </div>
    </div>
  </footer>
'''

footer_css = '''
  .footer {
    border-top: 1px solid var(--border);
    padding-block: 4.5rem 2.5rem;
    font-size: .875rem;
    color: var(--text-2);
    background: var(--footer-bg);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    position: relative;
  }
  .footer__grid {
    display: grid;
    grid-template-columns: 2.2fr 1.2fr 1.3fr 1.5fr;
    gap: 3rem;
    margin-bottom: 3.5rem;
  }
  @media (max-width: 1024px) {
    .footer__grid {
      grid-template-columns: 1.8fr 1fr 1fr 1.2fr;
      gap: 2.25rem;
    }
  }
  @media (max-width: 768px) {
    .footer {
      padding-block: 3.5rem 2rem;
    }
    .footer__grid {
      grid-template-columns: 1fr 1fr;
      gap: 2.25rem 1.5rem;
      margin-bottom: 2.5rem;
    }
    .footer__brand-col {
      grid-column: 1 / -1;
    }
    .footer__contact-col {
      grid-column: 1 / -1;
    }
    .footer__bottom {
      flex-direction: column;
      align-items: flex-start;
      gap: .75rem;
    }
  }
  @media (max-width: 420px) {
    .footer__grid {
      grid-template-columns: 1fr 1fr;
      gap: 2rem 1rem;
    }
    .footer__badges {
      flex-direction: column;
      align-items: flex-start;
      gap: .4rem;
    }
  }
  .footer__brand-col {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  }
  .footer__tagline {
    font-size: .875rem;
    color: var(--text-3);
    line-height: 1.6;
    max-width: 25rem;
    margin-bottom: 1.25rem;
  }
  .footer__badges {
    display: flex;
    flex-wrap: wrap;
    gap: .5rem;
  }
  .footer__badge {
    display: inline-flex;
    align-items: center;
    gap: .4rem;
    padding: .3rem .65rem;
    font-size: .75rem;
    font-weight: 500;
    color: var(--text-2);
    background: var(--surface-alt);
    border: 1px solid var(--border);
    border-radius: 6px;
    white-space: nowrap;
  }
  .footer__dot-live {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #10B981;
    box-shadow: 0 0 8px rgba(16, 185, 129, 0.7);
    display: inline-block;
  }
  .footer__col-title {
    font-size: .8125rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: .08em;
    color: var(--text);
    margin-bottom: 1.15rem;
  }
  .footer__nav {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: .65rem;
  }
  .footer__nav a {
    color: var(--text-3);
    text-decoration: none;
    font-size: .875rem;
    transition: color .15s ease, transform .15s ease;
    display: inline-block;
  }
  .footer__nav a:hover {
    color: var(--accent);
    transform: translateX(2px);
  }
  .footer__contact-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: .75rem;
  }
  .footer__contact-label {
    display: block;
    font-size: .6875rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: .06em;
    color: var(--text-3);
    margin-bottom: .15rem;
  }
  .footer__contact-link {
    color: var(--text);
    font-weight: 600;
    text-decoration: none;
    font-size: .875rem;
    transition: color .15s ease;
  }
  .footer__contact-link:hover {
    color: var(--accent);
  }
  .footer__contact-text {
    font-size: .8125rem;
    color: var(--text-2);
  }
  .footer__bottom {
    border-top: 1px solid var(--border);
    padding-top: 1.75rem;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    font-size: .8125rem;
    color: var(--text-3);
  }
  .footer__legal {
    line-height: 1.5;
  }
  .footer__links {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: .65rem;
  }
  .footer__links a {
    color: var(--text-3);
    text-decoration: none;
    transition: color .15s ease;
  }
  .footer__links a:hover {
    color: var(--accent);
    text-decoration: underline;
  }
  .footer__sep {
    color: var(--border-strong);
  }
  .footer__status-badge {
    display: inline-flex;
    align-items: center;
    gap: .4rem;
    color: var(--text-2);
    font-weight: 500;
  }
'''

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 900})
    page.goto('http://localhost:8080', wait_until='networkidle')
    
    # Replace footer in page
    page.evaluate(f'''() => {{
        const oldFooter = document.querySelector('footer');
        if (oldFooter) {{
            oldFooter.outerHTML = `{footer_html}`;
        }}
    }}''')
    page.add_style_tag(content=footer_css)
    
    # Scroll to footer
    footer = page.locator('.footer')
    footer.scroll_into_view_if_needed()
    page.wait_for_timeout(300)
    
    # 1. Desktop Light & Dark
    footer.screenshot(path='scratch/footer_pro_desktop_light.png')
    page.evaluate("() => document.documentElement.setAttribute('data-theme', 'dark')")
    footer.screenshot(path='scratch/footer_pro_desktop_dark.png')
    
    # 2. Tablet (768px)
    page.set_viewport_size({'width': 768, 'height': 1024})
    footer.scroll_into_view_if_needed()
    footer.screenshot(path='scratch/footer_pro_tablet_dark.png')

    # 3. Mobile Dark (375px)
    page.set_viewport_size({'width': 375, 'height': 812})
    footer.scroll_into_view_if_needed()
    footer.screenshot(path='scratch/footer_pro_mobile_dark.png')
    
    box_mob = footer.bounding_box()
    print(f"Mobile Footer Bounding Box: {box_mob['width']:.1f}px x {box_mob['height']:.1f}px")
    
    browser.close()
    print("All professional footer screenshots taken!")
