from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto('http://localhost:8080')
    bbox = page.evaluate('''() => {
        return fetch('assets/logo-serasa.svg')
            .then(r => r.text())
            .then(svgText => {
                const div = document.createElement('div');
                div.innerHTML = svgText;
                document.body.appendChild(div);
                const g = div.querySelector('#Artwork');
                const box = g.getBBox();
                return { x: box.x, y: box.y, width: box.width, height: box.height };
            });
    }''')
    print('Serasa artwork bbox:', bbox)
    browser.close()
