from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1280, 'height': 900})
    page.goto('http://localhost:8080')
    
    # Let's inspect the text of .founder-card p
    lines = page.evaluate('''() => {
        const p = document.querySelector('.founder-card p.small');
        // Let's get the range / client rects of text nodes
        const text = p.innerText;
        return text;
    }''')
    print('Founder text:', repr(lines))
    
    # Let's test at different widths to see line breaks
    for w in range(320, 1400, 50):
        page.set_viewport_size({'width': w, 'height': 800})
        # Check if any line in .founder-card p starts with 'M,'
        breaks = page.evaluate('''() => {
            const p = document.querySelector('.founder-card p.small');
            // Get all text rects
            const range = document.createRange();
            const walker = document.createTreeWalker(p, NodeFilter.SHOW_TEXT, null, false);
            let n;
            let words = [];
            while(n = walker.nextNode()) {
                const text = n.textContent;
                for(let i=0; i<text.length; i++) {
                    range.setStart(n, i);
                    range.setEnd(n, i+1);
                    const rect = range.getBoundingClientRect();
                    words.push({ char: text[i], top: Math.round(rect.top), left: Math.round(rect.left) });
                }
            }
            // Group by top
            let lines = {};
            words.forEach(w => {
                if(!lines[w.top]) lines[w.top] = '';
                lines[w.top] += w.char;
            });
            return Object.values(lines);
        }''')
        for l in breaks:
            if 'ALERGS' in l:
                print(f'Width {w}px line: {repr(l)}')
    
    browser.close()
