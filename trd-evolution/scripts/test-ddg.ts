import axios from 'axios';

async function testDDG() {
  console.log('=== Test 1: DuckDuckGo IA API ===');
  try {
    const ia = await axios.get('https://api.duckduckgo.com/', {
      params: { q: 'rock progressivo', format: 'json', no_html: '1', no_redirect: '1', t: 'evolution-api' },
      timeout: 5000,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      },
      validateStatus: () => true,
    });
    console.log('IA status:', ia.status);
    console.log('IA AbstractText:', ia.data?.AbstractText?.substring(0, 100) || 'none');
    console.log('IA RelatedTopics:', ia.data?.RelatedTopics?.length || 0);
  } catch (e: any) {
    console.log('IA error:', e.message);
  }

  console.log('\n=== Test 2: DuckDuckGo HTML (POST) ===');
  try {
    const html = await axios.post('https://html.duckduckgo.com/html/', 
      'q=rock+progressivo&b=1&kl=br-pt',
      {
        timeout: 10000,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept': 'text/html,application/xhtml+xml',
          'Content-Type': 'application/x-www-form-urlencoded',
          'Referer': 'https://html.duckduckgo.com/',
          'Origin': 'https://html.duckduckgo.com',
        },
        validateStatus: () => true,
      });
    const htmlStr = String(html.data);
    console.log('HTML status:', html.status, 'len:', htmlStr.length);
    const ra = htmlStr.match(/class="result__a"/g);
    const rs = htmlStr.match(/class="result__snippet"/g);
    console.log('result__a matches:', ra?.length || 0);
    console.log('result__snippet matches:', rs?.length || 0);
    // Check if it's a redirect or challenge page
    if (htmlStr.includes('anomaly') || htmlStr.includes('blocked') || htmlStr.includes('captcha')) {
      console.log('WARNING: Page appears to be a block/captcha page');
    }
    console.log('First 300 chars:', htmlStr.substring(0, 300));
  } catch (e: any) {
    console.log('HTML error:', e.message);
  }

  console.log('\n=== Test 3: DuckDuckGo Lite (GET) ===');
  try {
    const lite = await axios.get('https://lite.duckduckgo.com/lite/', {
      params: { q: 'rock progressivo', kl: 'br-pt' },
      timeout: 10000,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
      },
      validateStatus: () => true,
    });
    const liteStr = String(lite.data);
    console.log('Lite status:', lite.status, 'len:', liteStr.length);
    const rl = liteStr.match(/class="result-link"/g);
    const rsn = liteStr.match(/class="result-snippet"/g);
    console.log('result-link matches:', rl?.length || 0);
    console.log('result-snippet matches:', rsn?.length || 0);
    // Check for links in general
    const allLinks = liteStr.match(/<a[^>]*href="https?:[^"]*"[^>]*>/g);
    console.log('Total <a> links:', allLinks?.length || 0);
    if (allLinks) {
      for (const link of allLinks.slice(0, 5)) {
        console.log('  link:', link.substring(0, 120));
      }
    }
    console.log('First 300 chars:', liteStr.substring(0, 300));
  } catch (e: any) {
    console.log('Lite error:', e.message);
  }

  console.log('\n=== Test 4: DuckDuckGo HTML with GET ===');
  try {
    const htmlGet = await axios.get('https://html.duckduckgo.com/html/', {
      params: { q: 'rock progressivo', kl: 'br-pt' },
      timeout: 10000,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml',
        'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
      },
      validateStatus: () => true,
    });
    const htmlGetStr = String(htmlGet.data);
    console.log('HTML GET status:', htmlGet.status, 'len:', htmlGetStr.length);
    const ra = htmlGetStr.match(/class="result__a"/g);
    console.log('result__a matches:', ra?.length || 0);
    console.log('First 300 chars:', htmlGetStr.substring(0, 300));
  } catch (e: any) {
    console.log('HTML GET error:', e.message);
  }
}

testDDG().catch(console.error);
