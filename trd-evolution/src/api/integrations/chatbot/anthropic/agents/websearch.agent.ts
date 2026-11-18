import { AgentContext, AgentResult, SEARCH_CONFIG } from './agent-types';
import { BaseAgent } from './base.agent';
import axios from 'axios';

interface SearchResult {
  title: string;
  url: string;
  snippet: string;
  source: string;
}

export class WebSearchAgent extends BaseAgent {
  private lastSearchTime = 0;
  private searchCount = 0;
  private searchCountResetTime = Date.now();
  private readonly minIntervalMs = 3000;
  private readonly maxSearchesPerMinute = 10;

  constructor() {
    super({
      type: 'websearch',
      name: 'Web Search Agent',
      capabilities: ['web-search', 'duckduckgo', 'youtube', 'google', 'real-time-info'],
    });
  }

  private async enforceRateLimit(): Promise<void> {
    const now = Date.now();

    if (now - this.searchCountResetTime > 60_000) {
      this.searchCount = 0;
      this.searchCountResetTime = now;
    }

    if (this.searchCount >= this.maxSearchesPerMinute) {
      const waitMs = 60_000 - (now - this.searchCountResetTime);
      this.logger.warn(`[WebSearchAgent] Rate limit reached (${this.maxSearchesPerMinute}/min). Waiting ${Math.round(waitMs / 1000)}s...`);
      await new Promise((resolve) => setTimeout(resolve, waitMs));
      this.searchCount = 0;
      this.searchCountResetTime = Date.now();
    }

    const elapsed = now - this.lastSearchTime;
    if (elapsed < this.minIntervalMs) {
      const wait = this.minIntervalMs - elapsed;
      await new Promise((resolve) => setTimeout(resolve, wait));
    }

    this.lastSearchTime = Date.now();
    this.searchCount++;
  }

  canHandle(context: AgentContext): boolean {
    const searchQuery = this.cleanContent(context.content);
    return searchQuery.length > 3 && this.isSearchWorthy(searchQuery);
  }

  async execute(context: AgentContext): Promise<AgentResult> {
    const startTime = Date.now();
    this.status = 'running';

    await this.enforceRateLimit();

    try {
      const rawQuery = this.cleanContent(context.content);
      const searchQuery = this.extractSearchQuery(rawQuery);

      this.logger.log(
        `[WebSearchAgent] Query: "${searchQuery}" | raw="${rawQuery.substring(0, 60)}"`,
      );
      this.logger.log(
        `[WebSearchAgent] Config | ytApiKey=${!!SEARCH_CONFIG.youtubeApiKey} | googleKey=${!!SEARCH_CONFIG.googleSearchApiKey} | googleCx=${!!SEARCH_CONFIG.googleSearchCx} | maxResults=${SEARCH_CONFIG.maxResults} | timeout=${SEARCH_CONFIG.timeout}`,
      );

      const results: SearchResult[] = [];

      const isVideoQuery = /\b(v[ií]deo|v[ií]deos|youtube|yt|m[uú]sica|music|song|clip|clipe|trailer)\b/i.test(searchQuery);

      if (isVideoQuery) {
        this.logger.log(`[WebSearchAgent] → Starting YouTube search (video query detected)...`);
        const ytResults = await this.searchYouTube(searchQuery);
        this.logger.log(`[WebSearchAgent] ← YouTube returned ${ytResults.length} results`);
        results.push(...ytResults);

        if (results.length < SEARCH_CONFIG.maxResults) {
          this.logger.log(`[WebSearchAgent] → Starting Bing search (have ${results.length}/${SEARCH_CONFIG.maxResults})...`);
          const bingResults = await this.searchBing(searchQuery);
          this.logger.log(`[WebSearchAgent] ← Bing returned ${bingResults.length} results`);
          results.push(...bingResults);
        }

        if (results.length < SEARCH_CONFIG.maxResults) {
          this.logger.log(`[WebSearchAgent] → Starting DuckDuckGo search (have ${results.length}/${SEARCH_CONFIG.maxResults})...`);
          const ddgResults = await this.searchDuckDuckGo(searchQuery);
          this.logger.log(`[WebSearchAgent] ← DuckDuckGo returned ${ddgResults.length} results`);
          results.push(...ddgResults);
        }
      } else {
        this.logger.log(`[WebSearchAgent] → Starting Google scrape search (non-video query)...`);
        const googleResults = await this.searchGoogleScrape(searchQuery);
        this.logger.log(`[WebSearchAgent] ← Google scrape returned ${googleResults.length} results`);
        results.push(...googleResults);

        if (results.length < SEARCH_CONFIG.maxResults) {
          this.logger.log(`[WebSearchAgent] → Starting Bing search (have ${results.length}/${SEARCH_CONFIG.maxResults})...`);
          const bingResults = await this.searchBing(searchQuery);
          this.logger.log(`[WebSearchAgent] ← Bing returned ${bingResults.length} results`);
          results.push(...bingResults);
        }

        if (results.length < SEARCH_CONFIG.maxResults) {
          this.logger.log(`[WebSearchAgent] → Starting DuckDuckGo search (have ${results.length}/${SEARCH_CONFIG.maxResults})...`);
          const ddgResults = await this.searchDuckDuckGo(searchQuery);
          this.logger.log(`[WebSearchAgent] ← DuckDuckGo returned ${ddgResults.length} results`);
          results.push(...ddgResults);
        }

        if (results.length < SEARCH_CONFIG.maxResults) {
          this.logger.log(`[WebSearchAgent] → Starting YouTube search (have ${results.length}/${SEARCH_CONFIG.maxResults})...`);
          const ytResults = await this.searchYouTube(searchQuery);
          this.logger.log(`[WebSearchAgent] ← YouTube returned ${ytResults.length} results`);
          results.push(...ytResults);
        }
      }

      if (results.length < SEARCH_CONFIG.maxResults && SEARCH_CONFIG.googleSearchApiKey && SEARCH_CONFIG.googleSearchCx) {
        this.logger.log(`[WebSearchAgent] → Starting Google CSE search (have ${results.length}/${SEARCH_CONFIG.maxResults})...`);
        const googleResults = await this.searchGoogle(searchQuery);
        this.logger.log(`[WebSearchAgent] ← Google CSE returned ${googleResults.length} results`);
        results.push(...googleResults);
      }

      this.logger.log(`[WebSearchAgent] Total raw results: ${results.length} | sources=[${[...new Set(results.map(r => r.source))].join(',')}]`);

      const deduped = this.dedupeResults(results).slice(0, SEARCH_CONFIG.maxResults);

      // Fetch full content from top non-video results for richer context
      const nonVideoResults = deduped.filter((r) => r.url && !r.url.includes('youtube.com') && !r.url.includes('youtu.be'));
      const pagesToFetch = nonVideoResults.slice(0, 3);

      for (let i = 0; i < pagesToFetch.length; i++) {
        const r = pagesToFetch[i];
        this.logger.log(`[WebSearchAgent] → Fetching page ${i + 1}/${pagesToFetch.length}: ${r.url}`);
        const pageContent = await this.fetchPageContent(r.url);
        if (pageContent) {
          const score = this.dataScore(pageContent);
          this.logger.log(`[WebSearchAgent] ← Page ${i + 1} content: ${pageContent.length} chars | dataScore=${score}`);
          // Put fetched content into the snippet so the LLM sees all pages
          r.snippet = pageContent;
        }
      }

      if (deduped.length === 0) {
        this.logger.warn(`[WebSearchAgent] ✗ No results found for: "${searchQuery}" | allProviders returned 0`);
        return this.success('', undefined, {
          searchQuery,
          resultsFound: 0,
          processingTime: Date.now() - startTime,
        });
      }

      const formatted = this.formatResults(deduped, searchQuery);
      this.logger.log(
        `[WebSearchAgent] ✓ Found ${deduped.length} results | time=${Date.now() - startTime}ms`,
      );

      return this.success(formatted, undefined, {
        searchQuery,
        resultsFound: deduped.length,
        providers: [...new Set(deduped.map((r) => r.source))],
        processingTime: Date.now() - startTime,
      });
    } catch (error: any) {
      return this.failure(error.message || 'Web search agent failed', {
        processingTime: Date.now() - startTime,
      });
    }
  }

  private extractSearchQuery(raw: string): string {
    let query = raw.trim();

    query = query.replace(/^[,;:.!?\-]+/, '').trim();
    query = query.replace(/^(chat|bot|ia|ai|hey|olá|ola|ei)\s*[,;:.!?\-]?\s*/i, '').trim();

    const prefixes = [
      'agora quero que voce', 'agora quero q vc', 'agora quero que vc', 'agora quero vc',
      'agora traga me', 'agora traga', 'agora tragame', 'agora me traga',
      'agora busque', 'agora procure', 'agora pesquise', 'agora me mostre',
      'quero que voce', 'quero q vc', 'quero que vc', 'quero vc',
      'traga me', 'tragame', 'me traga', 'traga',
      'busque a', 'busque o', 'busque', 'buscar', 'busca',
      'pesquise', 'pesquisar', 'pesquisa',
      'procure', 'procurar', 'procura',
      'encontre', 'encontrar',
      'ache', 'achar', 'me mostre', 'mostre',
      'search for', 'search',
      'google',
      'por favor', 'pf',
      'voce pode', 'vc pode', 'pode',
    ];

    let lower = query.toLowerCase();
    let changed = true;
    while (changed) {
      changed = false;
      for (const prefix of prefixes) {
        if (lower.startsWith(prefix + ' ')) {
          query = query.substring(prefix.length + 1);
          lower = query.toLowerCase();
          changed = true;
          break;
        }
      }
    }

    const platformPrefixes = [
      'no youtube', 'na youtube', 'no yt', 'do youtube', 'da youtube',
      'no google', 'na google', 'do google', 'da google',
      'no site', 'na internet', 'na web', 'na net',
      'on youtube', 'on google',
    ];
    for (const pp of platformPrefixes) {
      if (lower.startsWith(pp + ' ')) {
        query = query.substring(pp.length + 1);
        lower = query.toLowerCase();
        break;
      }
    }

    const platformAnywhere = [
      'no youtube', 'na youtube', 'no yt', 'do youtube', 'da youtube',
      'no google', 'na google', 'do google', 'da google',
    ];
    for (const pa of platformAnywhere) {
      if (lower.includes(pa)) {
        query = query.replace(new RegExp(pa, 'gi'), '');
        lower = query.toLowerCase();
        break;
      }
    }

    const fillerWords = ['a mais tocada', 'as mais tocadas', 'o mais tocado', 'os mais tocados',
      'a mais famosa', 'as mais famosas', 'o mais famoso', 'os mais famosos',
      'a melhor', 'o melhor', 'as melhores', 'os melhores',
      'a mais popular', 'o mais popular'];
    for (const fw of fillerWords) {
      if (lower.startsWith(fw + ' ')) {
        query = query.substring(fw.length + 1);
        lower = query.toLowerCase();
        break;
      }
    }

    query = query.replace(/^(da|do|de|das|dos|a|o|as|os)\s+/i, '').trim();

    // Remove question prefixes to optimize search query
    const questionPrefixes = [
      'quais as', 'quais os', 'quais foram', 'quais são', 'quais sao',
      'qual a', 'qual o', 'qual foi', 'qual é', 'qual e',
      'quem é', 'quem e', 'quem foi',
      'o que é', 'o que e', 'o que são', 'o que sao',
      'quando é', 'quando e', 'quando foi',
      'onde está', 'onde esta', 'onde fica',
      'como funciona', 'como faço', 'como faco', 'como é', 'como e',
      'quantos', 'quantas',
    ];
    lower = query.toLowerCase();
    for (const qp of questionPrefixes) {
      if (lower.startsWith(qp + ' ')) {
        query = query.substring(qp.length + 1);
        lower = query.toLowerCase();
        break;
      }
    }

    // Remove filler adjectives
    const fillerAdjectives = [
      'principais', 'melhores', 'piores', 'maiores', 'últimos', 'ultimos',
      'recentes', 'atual', 'atuais', 'importantes', 'famosos', 'famosas',
    ];
    lower = query.toLowerCase();
    for (const fa of fillerAdjectives) {
      if (lower.startsWith(fa + ' ')) {
        query = query.substring(fa.length + 1);
        lower = query.toLowerCase();
        break;
      }
    }

    query = query.replace(/\s+/g, ' ').trim();
    return query;
  }

  private async searchYouTube(query: string): Promise<SearchResult[]> {
    const results: SearchResult[] = [];

    if (SEARCH_CONFIG.youtubeApiKey) {
      this.logger.log(`[WebSearchAgent] YouTube API: searching "${query}"...`);
      try {
        const response = await axios.get('https://www.googleapis.com/youtube/v3/search', {
          params: {
            key: SEARCH_CONFIG.youtubeApiKey,
            q: query,
            part: 'snippet',
            type: 'video',
            maxResults: Math.min(SEARCH_CONFIG.maxResults, 10),
            regionCode: 'BR',
            relevanceLanguage: 'pt',
          },
          timeout: SEARCH_CONFIG.timeout,
        });
        this.logger.log(`[WebSearchAgent] YouTube API: response status=${response.status} | items=${response.data?.items?.length || 0}`);

        for (const item of response.data?.items || []) {
          const videoId = item.id?.videoId;
          if (!videoId) continue;
          results.push({
            title: item.snippet?.title || '',
            url: `https://www.youtube.com/watch?v=${videoId}`,
            snippet: item.snippet?.description || item.snippet?.channelTitle || '',
            source: 'youtube-api',
          });
        }

        if (results.length > 0) {
          this.logger.log(`[WebSearchAgent] YouTube API: ${results.length} results`);
          return results;
        }
      } catch (err: any) {
        this.logger.warn(`[WebSearchAgent] YouTube API failed: ${err.message} | status=${err.response?.status} | data=${JSON.stringify(err.response?.data)?.substring(0, 200)}`);
      }
    } else {
      this.logger.log(`[WebSearchAgent] YouTube API: no API key configured, using scrape fallback`);
    }

    this.logger.log(`[WebSearchAgent] YouTube scrape: fetching html for "${query}"...`);
    try {
      const response = await axios.get('https://www.youtube.com/results', {
        params: { search_query: query, hl: 'pt', gl: 'BR' },
        timeout: SEARCH_CONFIG.timeout,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
          'Cookie': 'CONSENT=YES+1; GPS=1; PREF=hl=pt&gl=BR',
        },
      });

      const html = response.data as string;
      this.logger.log(`[WebSearchAgent] YouTube scrape: HTML received | len=${html.length} | status=${response.status}`);
      this.logger.log(`[WebSearchAgent] YouTube scrape: HTML preview="${html.substring(0, 300).replace(/\n/g, ' ')}"`);

      const videoData = this.parseYouTubeHtml(html);
      this.logger.log(`[WebSearchAgent] YouTube scrape: parsed ${videoData.length} videos from HTML`);

      for (const v of videoData) {
        results.push({
          title: v.title,
          url: `https://www.youtube.com/watch?v=${v.videoId}`,
          snippet: v.channel || '',
          source: 'youtube-scrape',
        });
      }

      if (results.length > 0) {
        this.logger.log(`[WebSearchAgent] YouTube scrape: ${results.length} results | first="${results[0].title}"`);
      } else {
        this.logger.warn(`[WebSearchAgent] YouTube scrape: 0 results after parsing`);
      }
    } catch (err: any) {
      this.logger.warn(`[WebSearchAgent] YouTube scrape failed: ${err.message} | status=${err.response?.status}`);
    }

    return results;
  }

  private parseYouTubeHtml(html: string): { videoId: string; title: string; channel: string }[] {
    const results: { videoId: string; title: string; channel: string }[] = [];

    try {
      const scriptMatch = html.match(/var ytInitialData\s*=\s*({.+?});<\/script>/s) || html.match(/ytInitialData\s*=\s*({.+?})\s*;/s);
      if (!scriptMatch) {
        this.logger.warn(`[WebSearchAgent] YouTube: ytInitialData not found in HTML (len=${html.length})`);
        if (html.includes('consent') || html.includes('CONSENT')) {
          this.logger.warn(`[WebSearchAgent] YouTube: HTML appears to be a consent/redirect page`);
        }
        this.logger.warn(`[WebSearchAgent] YouTube: HTML snippet="${html.substring(0, 500).replace(/\n/g, ' ')}"`);
        return results;
      }

      this.logger.log(`[WebSearchAgent] YouTube: ytInitialData found | matchLen=${scriptMatch[1].length}`);
      const data = JSON.parse(scriptMatch[1]);

      const videoRenderers = this.findAllVideoRenderers(data);
      this.logger.log(`[WebSearchAgent] YouTube: deep scan found ${videoRenderers.length} videoRenderer objects`);

      for (const vr of videoRenderers) {
        const videoId = vr.videoId;
        const title = vr.title?.runs?.[0]?.text || vr.title?.simpleText || '';
        const channel = vr.ownerText?.runs?.[0]?.text || vr.shortBylineText?.runs?.[0]?.text || '';
        if (videoId && title) {
          results.push({ videoId, title, channel });
        }
      }
      this.logger.log(`[WebSearchAgent] YouTube: structured parsing got ${results.length} videos`);
    } catch (err: any) {
      this.logger.warn(`[WebSearchAgent] YouTube: JSON parse failed: ${err.message}`);
    }

    if (results.length === 0) {
      this.logger.log(`[WebSearchAgent] YouTube: trying regex fallback on full HTML...`);
      const videoIdRegex = /"videoId":"([a-zA-Z0-9_-]{11})"/g;
      const titleRegex = /"title":\{"runs":\[\{"text":"([^"]+)"/g;
      const channelRegex = /"ownerText":\{"runs":\[\{"text":"([^"]+)"/g;

      const ids: string[] = [];
      const titles: string[] = [];
      const channels: string[] = [];

      let m;
      while ((m = videoIdRegex.exec(html)) !== null) ids.push(m[1]);
      while ((m = titleRegex.exec(html)) !== null) titles.push(m[1]);
      while ((m = channelRegex.exec(html)) !== null) channels.push(m[1]);

      const seen = new Set<string>();
      for (let i = 0; i < ids.length && results.length < 8; i++) {
        if (seen.has(ids[i])) continue;
        seen.add(ids[i]);
        results.push({ videoId: ids[i], title: titles[i] || '', channel: channels[i] || '' });
      }
      this.logger.log(`[WebSearchAgent] YouTube regex fallback: ids=${ids.length} | titles=${titles.length} | channels=${channels.length} | parsed=${results.length}`);
    }

    return results;
  }

  private findAllVideoRenderers(obj: any): any[] {
    const renderers: any[] = [];
    const seen = new Set<object>();

    const scan = (node: any): void => {
      if (!node || typeof node !== 'object') return;
      if (seen.has(node)) return;
      seen.add(node);

      if (node.videoRenderer && node.videoRenderer.videoId) {
        renderers.push(node.videoRenderer);
      }

      if (Array.isArray(node)) {
        for (const item of node) scan(item);
      } else {
        for (const key of Object.keys(node)) {
          const val = node[key];
          if (val && typeof val === 'object') scan(val);
        }
      }
    };

    scan(obj);
    return renderers;
  }

  private async searchDuckDuckGo(query: string): Promise<SearchResult[]> {
    const results: SearchResult[] = [];

    this.logger.log(`[WebSearchAgent] DuckDuckGo IA: searching "${query}"...`);
    try {
      const iaResponse = await axios.get('https://api.duckduckgo.com/', {
        params: { q: query, format: 'json', no_html: '1', no_redirect: '1', t: 'evolution-api' },
        timeout: 5000,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        },
      });
      this.logger.log(`[WebSearchAgent] DuckDuckGo IA: response status=${iaResponse.status} | Abstract=${!!iaResponse.data?.AbstractText} | Answer=${!!iaResponse.data?.Answer} | RelatedTopics=${iaResponse.data?.RelatedTopics?.length || 0}`);

      if (iaResponse.data?.AbstractText) {
        results.push({
          title: iaResponse.data.Heading || query,
          url: iaResponse.data.AbstractURL || '',
          snippet: iaResponse.data.AbstractText,
          source: 'duckduckgo-ia',
        });
      }
      if (iaResponse.data?.Answer) {
        results.push({
          title: 'DuckDuckGo Answer',
          url: '',
          snippet: iaResponse.data.Answer,
          source: 'duckduckgo-ia',
        });
      }
      if (iaResponse.data?.RelatedTopics?.length) {
        for (const topic of iaResponse.data.RelatedTopics.slice(0, 5)) {
          if (topic?.Text && topic?.FirstURL) {
            results.push({
              title: topic.Text.split(' - ')[0] || topic.Text.substring(0, 60),
              url: topic.FirstURL,
              snippet: topic.Text,
              source: 'duckduckgo-ia',
            });
          }
        }
      }
    } catch (err: any) {
      this.logger.warn(`[WebSearchAgent] DuckDuckGo IA failed: ${err.message}`);
    }

    if (results.length < 3) {
      this.logger.log(`[WebSearchAgent] DuckDuckGo HTML: searching "${query}" (IA returned only ${results.length})...`);
      try {
        const htmlResponse = await axios.post('https://html.duckduckgo.com/html/', {
          q: query,
          b: '1',
          kl: 'br-pt',
        }, {
          timeout: SEARCH_CONFIG.timeout,
          headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
            'Content-Type': 'application/x-www-form-urlencoded',
            'Referer': 'https://html.duckduckgo.com/',
            'Origin': 'https://html.duckduckgo.com',
          },
        });

        const html = htmlResponse.data as string;
        this.logger.log(`[WebSearchAgent] DuckDuckGo HTML: received | len=${html.length} | status=${htmlResponse.status}`);
        if (htmlResponse.status !== 200) {
          this.logger.warn(`[WebSearchAgent] DuckDuckGo HTML: non-200 status, trying lite endpoint...`);
          const liteResponse = await axios.get('https://lite.duckduckgo.com/lite/', {
            params: { q: query, kl: 'br-pt' },
            timeout: SEARCH_CONFIG.timeout,
            headers: {
              'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
              'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
            },
          });
          const liteHtml = liteResponse.data as string;
          this.logger.log(`[WebSearchAgent] DuckDuckGo lite: received | len=${liteHtml.length} | status=${liteResponse.status}`);
          const liteScraped = this.parseDuckDuckGoLiteHtml(liteHtml);
          this.logger.log(`[WebSearchAgent] DuckDuckGo lite: parsed ${liteScraped.length} results`);
          results.push(...liteScraped);
        } else {
          const scraped = this.parseDuckDuckGoHtml(html);
          this.logger.log(`[WebSearchAgent] DuckDuckGo HTML: parsed ${scraped.length} results`);
          results.push(...scraped);
        }
      } catch (err: any) {
        this.logger.warn(`[WebSearchAgent] DuckDuckGo HTML failed: ${err.message}`);
      }
    }

    if (results.length > 0) {
      this.logger.log(`[WebSearchAgent] DuckDuckGo: ${results.length} results`);
    }

    return results;
  }

  private async searchBing(query: string): Promise<SearchResult[]> {
    const results: SearchResult[] = [];

    try {
      this.logger.log(`[WebSearchAgent] Bing: searching "${query}"...`);
      const response = await axios.get('https://www.bing.com/search', {
        params: { q: query, setlang: 'pt-BR', count: SEARCH_CONFIG.maxResults },
        timeout: SEARCH_CONFIG.timeout,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept': 'text/html,application/xhtml+xml',
          'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
        },
      });

      const html = String(response.data);
      this.logger.log(`[WebSearchAgent] Bing: status=${response.status} | len=${html.length}`);

      const blocks = html.split('<li class="b_algo"').slice(1);
      for (const block of blocks.slice(0, SEARCH_CONFIG.maxResults)) {
        const h2Match = block.match(/<h2[^>]*>([\s\S]*?)<\/h2>/);
        const citeMatch = block.match(/<cite[^>]*>([\s\S]*?)<\/cite>/);
        const snippetMatch = block.match(/<p[^>]*>([\s\S]*?)<\/p>/);

        if (h2Match) {
          const title = h2Match[1].replace(/<[^>]*>/g, '').trim();
          let url = '';
          if (citeMatch) {
            url = citeMatch[1].replace(/<[^>]*>/g, '').trim();
            url = url.replace(/\s*›\s*/g, '/').replace(/\s+/g, '');
            if (!url.startsWith('http')) url = 'https://' + url;
          }
          const snippet = snippetMatch ? snippetMatch[1].replace(/<[^>]*>/g, '').replace(/&[#a-zA-Z0-9]+;/g, ' ').trim() : '';

          if (title) {
            results.push({ title, url, snippet, source: 'bing' });
          }
        }
      }

      this.logger.log(`[WebSearchAgent] Bing: parsed ${results.length} results`);
    } catch (err: any) {
      this.logger.warn(`[WebSearchAgent] Bing failed: ${err.message}`);
    }

    return results;
  }

  private parseDuckDuckGoHtml(html: string): SearchResult[] {
    const results: SearchResult[] = [];

    const resultBlocks = html.split(/<div class="result[^"]*"/i).slice(1);

    for (const block of resultBlocks.slice(0, 8)) {
      const titleMatch = block.match(/<a[^>]*class="result__a"[^>]*>(.*?)<\/a>/s);
      const snippetMatch = block.match(/<a[^>]*class="result__snippet"[^>]*>(.*?)<\/a>/s);
      const urlMatch = block.match(/<a[^>]*class="result__a"[^>]*href="([^"]+)"/);

      if (titleMatch) {
        const title = titleMatch[1].replace(/<[^>]*>/g, '').trim();
        let url = urlMatch ? urlMatch[1] : '';
        if (url.startsWith('//')) url = 'https:' + url;
        if (url.includes('duckduckgo.com/l/?uddg=')) {
          const uddg = url.match(/uddg=([^&]+)/);
          if (uddg) url = decodeURIComponent(uddg[1]);
        }
        const snippet = snippetMatch ? snippetMatch[1].replace(/<[^>]*>/g, '').trim() : '';

        if (title) {
          results.push({ title, url, snippet, source: 'duckduckgo-html' });
        }
      }
    }

    if (results.length === 0) {
      const titleRegex = /<a[^>]*class="result__a"[^>]*>(.*?)<\/a>/gs;
      const snippetRegex = /<a[^>]*class="result__snippet"[^>]*>(.*?)<\/a>/gs;

      const titles: string[] = [];
      const snippets: string[] = [];

      let m;
      while ((m = titleRegex.exec(html)) !== null) {
        titles.push(m[1].replace(/<[^>]*>/g, '').trim());
      }
      while ((m = snippetRegex.exec(html)) !== null) {
        snippets.push(m[1].replace(/<[^>]*>/g, '').trim());
      }

      for (let i = 0; i < Math.min(titles.length, 8); i++) {
        results.push({
          title: titles[i],
          url: '',
          snippet: snippets[i] || '',
          source: 'duckduckgo-html',
        });
      }
    }

    return results;
  }

  private parseDuckDuckGoLiteHtml(html: string): SearchResult[] {
    const results: SearchResult[] = [];

    const rows = html.split(/<tr[^>]*class="result[^"]*"/i).slice(1);

    for (const row of rows.slice(0, 8)) {
      const titleMatch = row.match(/<a[^>]*class="result-link"[^>]*>(.*?)<\/a>/s) || row.match(/<a[^>]*>(.*?)<\/a>/s);
      const urlMatch = row.match(/<a[^>]*href="([^"]+)"/);
      const snippetMatch = row.match(/<td[^>]*class="result-snippet"[^>]*>(.*?)<\/td>/s) || row.match(/<a[^>]*class="result-snippet"[^>]*>(.*?)<\/a>/s);

      if (titleMatch) {
        const title = titleMatch[1].replace(/<[^>]*>/g, '').trim();
        let url = urlMatch ? urlMatch[1] : '';
        if (url.startsWith('//')) url = 'https:' + url;
        const snippet = snippetMatch ? snippetMatch[1].replace(/<[^>]*>/g, '').trim() : '';

        if (title) {
          results.push({ title, url, snippet, source: 'duckduckgo-lite' });
        }
      }
    }

    if (results.length === 0) {
      const linkRegex = /<a[^>]*href="(https?:[^"]+)"[^>]*>(.*?)<\/a>/gs;
      let m;
      while ((m = linkRegex.exec(html)) !== null && results.length < 8) {
        const url = m[1];
        const title = m[2].replace(/<[^>]*>/g, '').trim();
        if (title && url && !url.includes('duckduckgo.com')) {
          results.push({ title, url, snippet: '', source: 'duckduckgo-lite' });
        }
      }
    }

    return results;
  }

  private async searchGoogle(query: string): Promise<SearchResult[]> {
    const results: SearchResult[] = [];

    try {
      const response = await axios.get('https://www.googleapis.com/customsearch/v1', {
        params: {
          key: SEARCH_CONFIG.googleSearchApiKey,
          cx: SEARCH_CONFIG.googleSearchCx,
          q: query,
          num: Math.min(SEARCH_CONFIG.maxResults, 10),
        },
        timeout: SEARCH_CONFIG.timeout,
      });

      for (const item of response.data?.items || []) {
        results.push({
          title: item.title || '',
          url: item.link || '',
          snippet: item.snippet || '',
          source: 'google-cse',
        });
      }

      if (results.length > 0) {
        this.logger.log(`[WebSearchAgent] Google CSE: ${results.length} results`);
      }
    } catch (err: any) {
      this.logger.warn(`[WebSearchAgent] Google CSE failed: ${err.message}`);
    }

    return results;
  }

  private async searchGoogleScrape(query: string): Promise<SearchResult[]> {
    const results: SearchResult[] = [];

    try {
      this.logger.log(`[WebSearchAgent] Google scrape: searching "${query}"...`);
      const response = await axios.get('https://www.google.com/search', {
        params: { q: query, hl: 'pt-BR', gl: 'BR', num: Math.min(SEARCH_CONFIG.maxResults, 10) },
        timeout: SEARCH_CONFIG.timeout,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept-Language': 'pt-BR,pt;q=0.9,en;q=0.8',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        },
      });

      this.logger.log(`[WebSearchAgent] Google scrape: status=${response.status} | len=${response.data?.length || 0}`);
      const html = response.data as string;
      if (!html) return results;

      // Parse Google search results from HTML
      // Google uses <div class="g"> containers with <a href> and <h3> for title
      const linkRegex = /<a[^>]+href="\/url\?q=([^&"]+)&[^"]*"[^>]*>[\s\S]*?<h3[^>]*>([\s\S]*?)<\/h3>/gi;
      let match;
      while ((match = linkRegex.exec(html)) !== null && results.length < SEARCH_CONFIG.maxResults) {
        const url = decodeURIComponent(match[1]);
        const titleRaw = match[2].replace(/<[^>]+>/g, '').trim();
        if (url && titleRaw && !url.includes('google.com') && !url.includes('youtube.com')) {
          // Try to extract snippet from nearby text
          const afterIdx = match.index + match[0].length;
          const nearby = html.substring(afterIdx, afterIdx + 500);
          const snippetMatch = nearby.match(/<span[^>]*>([\s\S]{20,300}?)<\/span>/i);
          const snippet = snippetMatch ? snippetMatch[1].replace(/<[^>]+>/g, '').trim() : '';
          results.push({ title: titleRaw, url, snippet, source: 'google-scrape' });
        }
      }

      // Fallback: try simpler regex if the above didn't work
      if (results.length === 0) {
        const simpleRegex = /<a[^>]+href="(https?:\/\/[^"]+)"[^>]*>[\s\S]*?<h3[^>]*>([\s\S]*?)<\/h3>/gi;
        while ((match = simpleRegex.exec(html)) !== null && results.length < SEARCH_CONFIG.maxResults) {
          const url = match[1];
          const titleRaw = match[2].replace(/<[^>]+>/g, '').trim();
          if (url && titleRaw && !url.includes('google.com') && !url.includes('youtube.com') && !url.includes('gstatic.com')) {
            results.push({ title: titleRaw, url, snippet: '', source: 'google-scrape' });
          }
        }
      }

      this.logger.log(`[WebSearchAgent] Google scrape: parsed ${results.length} results`);
    } catch (err: any) {
      this.logger.warn(`[WebSearchAgent] Google scrape failed: ${err.message}`);
    }

    return results;
  }

  private dedupeResults(results: SearchResult[]): SearchResult[] {
    const seen = new Set<string>();
    const deduped: SearchResult[] = [];

    for (const r of results) {
      const key = (r.url || r.title).toLowerCase();
      if (key && !seen.has(key)) {
        seen.add(key);
        deduped.push(r);
      }
    }

    return deduped;
  }

  private formatResults(results: SearchResult[], query: string): string {
    const lines: string[] = [];
    lines.push(`Resultados de busca para: "${query}"`);
    lines.push('');

    for (let i = 0; i < results.length; i++) {
      const r = results[i];
      lines.push(`[${i + 1}] ${r.title}`);
      if (r.url) lines.push(`    Link: ${r.url}`);
      if (r.snippet) {
        const maxLen = r.snippet.length > 500 ? 4000 : 300;
        lines.push(`    ${r.snippet.substring(0, maxLen)}`);
      }
      lines.push('');
    }

    lines.push('Use estas informações para responder à pergunta do usuário de forma precisa e atualizada. Transcreva os dados relevantes (tabelas, listas, números) diretamente da fonte.');

    return lines.join('\n');
  }

  private dataScore(content: string): number {
    if (!content) return 0;
    let score = 0;
    // Count numbers (points, scores, prices, dates)
    const numbers = content.match(/\d+/g);
    if (numbers) score += numbers.length;
    // Count pipe-separated table rows
    const tableRows = content.match(/\|.*\|/g);
    if (tableRows) score += tableRows.length * 5;
    // Count lines with team/player-like patterns
    const lines = content.split('\n').filter((l) => l.trim().length > 10 && l.includes('|'));
    score += lines.length * 3;
    return score;
  }

  private async fetchPageContent(url: string): Promise<string | null> {
    try {
      // Use Jina Reader API to render JavaScript pages and get clean text
      const jinaUrl = `https://r.jina.ai/${url}`;
      this.logger.log(`[WebSearchAgent] Fetching via Jina Reader: ${jinaUrl}`);
      const response = await axios.get(jinaUrl, {
        timeout: SEARCH_CONFIG.timeout + 5000,
        headers: {
          'Accept': 'text/plain',
          'X-Return-Format': 'text',
        },
      });

      const text = response.data as string;
      if (!text || text.length < 100) return null;

      // Clean up the text
      let cleaned = text
        .replace(/\n{3,}/g, '\n\n')
        .replace(/ {2,}/g, ' ')
        .trim();

      // Remove Jina metadata header if present
      cleaned = cleaned.replace(/^Title:[^\n]*\nURL Source:[^\n]*\nMarkdown Content:\n/i, '');

      // Return full content — Jina already extracts clean text
      return cleaned.substring(0, 4000);
    } catch (err: any) {
      this.logger.warn(`[WebSearchAgent] Page fetch failed: ${err.message} | url=${url}`);
      return null;
    }
  }

  private isSearchWorthy(query: string): boolean {
    const searchIndicators = [
      'qual o', 'quem é', 'o que é', 'quando', 'onde', 'como funciona',
      'preço', 'preco', 'cotação', 'cotacao', 'notícia', 'noticia', 'tempo agora', 'clima',
      'hoje', 'agora', 'atual', 'recente', 'último', 'ultimo',
      'what is', 'who is', 'how to', 'latest', 'news', 'price',
      'busque', 'buscar', 'busca', 'pesquise', 'pesquisar', 'pesquisa',
      'procure', 'procurar', 'procura',
      'links', 'link', 'youtube', 'google', 'site',
      'encontre', 'encontrar', 'achar', 'ache',
      'musica', 'música', 'music', 'song', 'video', 'vídeo',
      'melhor', 'pior', 'top', 'mais tocada', 'ranking',
      'traga', 'tragame', 'mostre',
      'jogue', 'jogar', 'calcule', 'calcular',
      'tabela', 'classificação', 'classificacao', 'placar', 'resultado', 'jogo', 'jogos',
      'campeonato', 'brasileirão', 'brasileirao', 'copa', 'libertadores', 'champions',
      'dólar', 'dolar', 'euro', 'bitcoin', 'cripto',
      'previsão', 'previsao', 'temperatura', 'chuva', 'forecast',
      'manchete', 'última hora', 'ultima hora',
      'lançamento', 'lancamento', 'lançou', 'lancou', 'estreiou',
      'eleição', 'eleicao', 'datafolha', 'censo',
      'covid', 'casos', 'óbitos', 'obitos', 'vacina',
      'ao vivo', 'aovivo', 'live', 'transmissão', 'transmissao',
      'onde assistir', 'que horas',
    ];
    const lower = query.toLowerCase();
    return searchIndicators.some((ind) => lower.includes(ind));
  }
}
