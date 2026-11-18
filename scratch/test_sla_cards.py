from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 375, "height": 812})
    page.goto("http://localhost:8080/", wait_until="networkidle")

    # Add data-labels and inject card CSS
    page.evaluate("""() => {
        const rows = document.querySelectorAll('.sla-table tbody tr');
        const headers = ['Severidade', 'Definição Operacional', 'MTTA (Primeira Resposta)', 'Canal de Atendimento', 'Plano Contemplado'];
        rows.forEach(tr => {
            const tds = tr.querySelectorAll('td');
            tds.forEach((td, i) => {
                td.setAttribute('data-label', headers[i]);
            });
        });

        const style = document.createElement('style');
        style.innerHTML = `
            @media (max-width: 768px) {
                .table-responsive {
                    overflow-x: visible !important;
                    border: none !important;
                    background: transparent !important;
                }
                .sla-table,
                .sla-table thead,
                .sla-table tbody,
                .sla-table th,
                .sla-table td,
                .sla-table tr {
                    display: block !important;
                    min-width: 0 !important;
                    width: 100% !important;
                }
                .sla-table thead {
                    display: none !important;
                }
                .sla-table tr {
                    background: var(--surface) !important;
                    border: 1px solid var(--border) !important;
                    border-radius: var(--radius-card) !important;
                    padding: 1.25rem !important;
                    margin-bottom: 1rem !important;
                }
                .sla-table td {
                    padding: .6rem 0 !important;
                    border-bottom: 1px solid var(--border-hairline) !important;
                    font-size: .875rem !important;
                }
                .sla-table td:last-child {
                    border-bottom: none !important;
                }
                .sla-table td::before {
                    content: attr(data-label) ": ";
                    font-weight: 700 !important;
                    font-size: .75rem !important;
                    text-transform: uppercase !important;
                    letter-spacing: .05em !important;
                    color: var(--accent) !important;
                    display: block !important;
                    margin-bottom: .2rem !important;
                }
            }
        `;
        document.head.appendChild(style);
    }""")

    wrap = page.locator(".sla-matrix-wrap")
    wrap.screenshot(path="C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/sla_cards_mobile.png")
    
    scroll_w = page.evaluate("() => document.querySelector('.sla-matrix-wrap').scrollWidth")
    client_w = page.evaluate("() => document.querySelector('.sla-matrix-wrap').clientWidth")
    print(f"SLA Matrix Wrap: client={client_w}, scroll={scroll_w}")

    page.close()
    browser.close()
