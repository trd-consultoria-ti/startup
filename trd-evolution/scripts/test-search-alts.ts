import axios from 'axios';

async function testAlternatives() {
  console.log('=== Test 1: Bing HTML ===');
  try {
    const r = await axios.get('https://www.bing.com/search', {
      params: { q: 'rock progressivo links', setlang: 'pt-BR' },
      timeout: 10000,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml',
        'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
      },
      validateStatus: () => true,
    });
    const html = String(r.data);
    console.log('Bing status:', r.status, 'len:', html.length);
    const bResults = html.match(/<li class="b_algo"/g);
    console.log('b_algo matches:', bResults?.length || 0);
    if (bResults && bResults.length > 0) {
      const blocks = html.split('<li class="b_algo"').slice(1);
      for (const block of blocks.slice(0, 3)) {
        const titleMatch = block.match(/<h2><a[^>]*href="([^"]+)"[^>]*>(.*?)<\/a><\/h2>/s);
        if (titleMatch) {
          const url = titleMatch[1];
          const title = titleMatch[2].replace(/<[^>]*>/g, '').trim();
          console.log('  Result:', title.substring(0, 80), '|', url.substring(0, 80));
        }
      }
    }
  } catch (e: any) {
    console.log('Bing error:', e.message);
  }

  console.log('\n=== Test 2: DuckDuckGo with cookie jar ===');
  try {
    const session = axios.create({
      timeout: 10000,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml',
        'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
      },
      validateStatus: () => true,
    });

    // First get the main page to obtain cookies
    const home = await session.get('https://duckduckgo.com/');
    console.log('Home status:', home.status);
    const setCookies = home.headers['set-cookie'];
    console.log('Cookies:', setCookies?.length || 0);

    // Now try lite with cookies
    const lite = await session.get('https://lite.duckduckgo.com/lite/', {
      params: { q: 'rock progressivo', kl: 'br-pt' },
    });
    const liteStr = String(lite.data);
    console.log('Lite status:', lite.status, 'len:', liteStr.length);
    const rl = liteStr.match(/class="result-link"/g);
    console.log('result-link matches:', rl?.length || 0);
    if (liteStr.includes('bot') || liteStr.includes('challenge')) {
      console.log('Still blocked');
    }
  } catch (e: any) {
    console.log('DDG cookie error:', e.message);
  }

  console.log('\n=== Test 3: Google search (no API key, just HTML) ===');
  try {
    const r = await axios.get('https://www.google.com/search', {
      params: { q: 'rock progressivo links', hl: 'pt-BR', num: 5 },
      timeout: 10000,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml',
        'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
      },
      validateStatus: () => true,
    });
    const html = String(r.data);
    console.log('Google status:', r.status, 'len:', html.length);
    const gResults = html.match(/<div class="g"/g);
    console.log('div.g matches:', gResults?.length || 0);
    const h3Matches = html.match(/<h3[^>]*>(.*?)<\/h3>/gs);
    console.log('h3 matches:', h3Matches?.length || 0);
    if (h3Matches) {
      for (const h3 of h3Matches.slice(0, 3)) {
        console.log('  Title:', h3.replace(/<[^>]*>/g, '').trim().substring(0, 80));
      }
    }
  } catch (e: any) {
    console.log('Google error:', e.message);
  }

  console.log('\n=== Test 4: SearXNG public instance ===');
  try {
    const r = await axios.get('https://searx.be/search', {
      params: { q: 'rock progressivo', format: 'json', language: 'pt-BR' },
      timeout: 10000,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Accept': 'application/json',
      },
      validateStatus: () => true,
    });
    console.log('SearXNG status:', r.status);
    if (r.status === 200 && r.data?.results) {
      console.log('SearXNG results:', r.data.results.length);
      for (const result of r.data.results.slice(0, 3)) {
        console.log('  ', result.title?.substring(0, 80), '|', result.url?.substring(0, 80));
      }
    } else {
      console.log('SearXNG response:', JSON.stringify(r.data).substring(0, 200));
    }
  } catch (e: any) {
    console.log('SearXNG error:', e.message);
  }
}

testAlternatives().catch(console.error);
