from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1280, 'height': 900})
    page.goto('http://localhost:8080')
    
    # Check if there is any inherited word-break
    res = page.evaluate('''() => {
        let el = document.querySelector('.founder-card p.small');
        let chain = [];
        while(el) {
            let s = window.getComputedStyle(el);
            chain.push({
                tag: el.tagName,
                cls: el.className,
                wordBreak: s.wordBreak,
                overflowWrap: s.overflowWrap,
                hyphens: s.hyphens
            });
            el = el.parentElement;
        }
        return chain;
    }''')
    for c in res:
        print(c)
    browser.close()
