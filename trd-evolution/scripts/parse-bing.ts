import * as fs from 'fs';

const html = fs.readFileSync('bing-sample.html', 'utf8');
const blocks = html.split('<li class="b_algo"').slice(1);

for (let i = 0; i < Math.min(5, blocks.length); i++) {
  const b = blocks[i];
  const h2 = b.match(/<h2[^>]*>([\s\S]*?)<\/h2>/);
  const cite = b.match(/<cite[^>]*>([\s\S]*?)<\/cite>/);
  const p = b.match(/<p[^>]*>([\s\S]*?)<\/p>/);
  const a = b.match(/<a[^>]*href="([^"]+)"/);

  console.log(`--- Block ${i} ---`);
  console.log('h2:', h2 ? h2[1].replace(/<[^>]*>/g, '').trim().substring(0, 100) : 'none');
  console.log('cite:', cite ? cite[1].replace(/<[^>]*>/g, '').trim().substring(0, 100) : 'none');
  console.log('p:', p ? p[1].replace(/<[^>]*>/g, '').trim().substring(0, 100) : 'none');
  console.log('first href:', a ? a[1].substring(0, 100) : 'none');
  console.log();
}
