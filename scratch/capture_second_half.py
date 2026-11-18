from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    context = browser.new_context(viewport={'width': 1280, 'height': 800})
    page = context.new_page()
    page.goto('http://localhost:8080')
    page.evaluate("() => localStorage.setItem('trd-theme', 'dark')")
    page.reload()
    page.wait_for_timeout(1000)
    
    with open('scratch/test_white_badges.py', 'r', encoding='utf-8') as f:
        code = f.read()
    import re
    m = re.search(r'test_css = """(.*?)"""', code, re.DOTALL)
    test_css = m.group(1) if m else ""
    page.add_style_tag(content=test_css)
    
    # translate track directly
    page.evaluate("""() => {
        const track = document.querySelector('.trustbar__track');
        track.style.animation = 'none';
        track.style.transform = 'translateX(-1100px)';
    }""")
    page.wait_for_timeout(300)
    
    trustbar = page.locator('.trustbar')
    trustbar.screenshot(path='scratch/test_trustbar_second_half.png')
    
    browser.close()

print('Second half captured')
