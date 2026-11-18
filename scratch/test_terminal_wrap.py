from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 375, "height": 812})
    page.goto("http://localhost:8080/", wait_until="networkidle")

    terminal = page.locator(".terminal")
    terminal.screenshot(path="scratch/screenshots/terminal_before.png")

    # Now apply pre-wrap to terminal
    page.evaluate("""() => {
        const style = document.createElement('style');
        style.innerHTML = `
            @media (max-width: 768px) {
                .terminal__body {
                    white-space: pre-wrap !important;
                    word-break: break-word !important;
                    overflow-x: hidden !important;
                }
                .code-line--dim {
                    padding-left: 0.5rem !important;
                }
            }
        `;
        document.head.appendChild(style);
    }""")
    terminal.screenshot(path="scratch/screenshots/terminal_after_wrap.png")

    t_scroll = page.evaluate("() => document.querySelector('.terminal__body').scrollWidth")
    t_client = page.evaluate("() => document.querySelector('.terminal__body').clientWidth")
    print(f"Terminal after wrap: client={t_client}, scroll={t_scroll}")

    page.close()
    browser.close()
