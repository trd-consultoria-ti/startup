import axios from 'axios';
import * as fs from 'fs';

async function inspectBing() {
  const r = await axios.get('https://www.bing.com/search', {
    params: { q: 'rock progressivo links', setlang: 'pt-BR', count: 8 },
    timeout: 10000,
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      'Accept': 'text/html,application/xhtml+xml',
      'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
    },
    validateStatus: () => true,
  });

  const html = String(r.data);
  fs.writeFileSync('bing-sample.html', html);
  console.log('Saved to bing-sample.html, len:', html.length);

  // Check b_algo blocks
  const blocks = html.split('<li class="b_algo"').slice(1);
  console.log('b_algo blocks:', blocks.length);

  if (blocks.length > 0) {
    const block = blocks[0];
    // Show first 500 chars of first block
    console.log('\nFirst block (500 chars):');
    console.log(block.substring(0, 500));

    // Try different regex patterns
    const patterns = [
      { name: 'h2>a href', regex: /<h2><a[^>]*href="([^"]+)"[^>]*>(.*?)<\/a><\/h2>/s },
      { name: 'h2 any a', regex: /<h2>.*?<a[^>]*href="([^"]+)"[^>]*>(.*?)<\/a>.*?<\/h2>/s },
      { name: 'any h2', regex: /<h2[^>]*>(.*?)<\/h2>/s },
      { name: 'cite', regex: /<cite[^>]*>(.*?)<\/cite>/s },
      { name: 'b_algo title', regex: /class="b_algo"[^>]*>.*?<a[^>]*href="([^"]+)"/s },
    ];

    for (const p of patterns) {
      const m = block.match(p.regex);
      console.log(`\n${p.name}: ${m ? 'MATCH' : 'no match'}`);
      if (m) console.log('  ', m[1]?.substring(0, 100));
    }
  } else {
    // Check for other result patterns
    const patterns = [
      'b_algo', 'b_caption', 'b_title', 'tilk', 'b_focusTextLarge',
      'sa_uc', 'b_rs', 'b_factrow', 'b_context', 'b_ans',
      'js_b_algo', 'b_algoSlug', 'algoSlug',
    ];
    for (const p of patterns) {
      const matches = html.match(new RegExp(p, 'g'));
      if (matches) console.log(`Pattern "${p}": ${matches.length} matches`);
    }

    // Try to find any <a> with href containing http
    const allLinks = html.match(/<a[^>]*href="https?:\/\/[^"]*"[^>]*>[^<]{5,}<\/a>/g);
    console.log('\nAll <a> links with text:', allLinks?.length || 0);
    if (allLinks) {
      for (const link of allLinks.slice(0, 10)) {
        const href = link.match(/href="([^"]+)"/);
        const text = link.replace(/<[^>]*>/g, '').trim();
        console.log('  ', text.substring(0, 60), '|', href?.[1]?.substring(0, 80));
      }
    }
  }
}

inspectBing().catch(console.error);
