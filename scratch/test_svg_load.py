from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    resp = page.goto('http://localhost:8080/assets/logo-claude.svg')
    print('Status:', resp.status)
    print('Content type:', resp.headers.get('content-type'))
    body = page.content()
    print('Page content length:', len(body))
    print('Content snippet:', body[:200])
    browser.close()
