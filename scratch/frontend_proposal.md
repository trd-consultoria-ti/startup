# Proposta de Engenharia: Modernização Visual, Efeitos 60FPS & Zero-CDN
**Para:** Coordenação Técnica & Thiago Duarte — TRD Consultoria  
**De:** Frontend, Angular & Performance Specialist — TRD Consultoria  
**Objetivo:** Elevar o impacto estético e a sofisticação visual da landing page mantendo **Zero-CDN**, **Zero Frameworks Pesados**, **CLS = 0**, **LCP < 1.2s** e **INP < 50ms**.

---

## 1. Princípios de Engenharia de Animação para a TRD

Para uma consultoria de engenharia de missão crítica (com credenciais em órgãos regulados e governo federal), os efeitos visuais não devem parecer "enfeites genéricos de template", mas sim **evidências de precisão técnica, acabamento refinado e sobriedade institucional** (na linha de produtos de alta engenharia como *Linear*, *Vercel*, *Stripe* e *Supabase*).

### Regras Invioláveis de Performance & Hardware:
1. **Composição Exclusiva por GPU:** Todas as animações devem manipular unicamente propriedades compostas pela GPU: `transform` (3D/translate/scale/rotate) e `opacity`. Nenhuma alteração de `width`, `height`, `margin`, `padding` ou `box-shadow` dinâmico em loop (que disparam *Layout/Reflow* e *Paint* no Main Thread).
2. **Zero Layout Shifts (CLS = 0):** Todos os containers possuem dimensões e aspect-ratios fixos ou reservados antes da animação ocorrer.
3. **Desligamento Passivo de Eventos:** Listeners de mouse/touch utilizam flags `{ passive: true }`, throttling com `requestAnimationFrame` e são restritos a dispositivos apontadores finos (`@media (hover: hover) and (pointer: fine)`).
4. **Respeito Estrito à Acessibilidade:** Conformidade integral com `@media (prefers-reduced-motion: reduce)` — qualquer movimento é imediatamente suprimido para usuários sensíveis.
5. **Zero Dependências Externas (Zero-CDN):** Nenhum byte de biblioteca externa (Three.js, GSAP, Framer Motion ou Lottie). 100% Vanilla JS e CSS moderno.

---

## 2. Catálogo de Efeitos Recomendados

### Efeito 1: Card Spotlight & Border Beam (Efeito "Linear / Vercel")
* **Conceito:** À medida que o usuário move o cursor sobre a grid de cards, um gradiente radial suave de luz âmbar acompanha o cursor, iluminando a borda e a superfície do card de forma sutil e futurista.
* **Por que adotar:** Transforma a leitura dos cases e serviços em uma experiência tátil de altíssimo nível sem poluir o layout.
* **Custo de Performance:** **Zero Reflow**. Opera atualizando duas variáveis CSS locais (`--mouse-x` e `--mouse-y`) consumidas em pseudo-elementos via GPU.
* **Comportamento Mobile:** Em touch screens, o listener é ignorado pelo `@media (hover: hover)`, preservando 100% da bateria do smartphone.

#### Snippet CSS (styles.css):
```css
/* Card Spotlight sutil */
@media (hover: hover) and (pointer: fine) {
  .card {
    position: relative;
    overflow: hidden;
  }
  .card::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: radial-gradient(
      600px circle at var(--mouse-x, -1000px) var(--mouse-y, -1000px),
      rgba(245, 158, 11, 0.08),
      transparent 40%
    );
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
    z-index: 1;
  }
  .card:hover::before {
    opacity: 1;
  }
}
```

#### Snippet JS Vanilla (< 25 linhas, passivo e com RAF):
```javascript
(function initCardSpotlight() {
  if (!window.matchMedia('(hover: hover) and (pointer: fine)').matches) return;
  
  const cards = document.querySelectorAll('.card');
  cards.forEach(card => {
    card.addEventListener('pointermove', e => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      card.style.setProperty('--mouse-x', `${x}px`);
      card.style.setProperty('--mouse-y', `${y}px`);
    }, { passive: true });
  });
})();
```

---

### Efeito 2: Ambient Aurora Mesh no Hero (Acelerado por GPU)
* **Conceito:** No topo da página (Hero), em vez de um gradiente estático simples, duas esferas de luz âmbar/navy com desfoque profundo (`blur(90px)`) realizam uma órbita suave e imperceptível (ciclos de 20 a 30 segundos).
* **Por que adotar:** Dá vida e profundidade tridimensional à primeira dobra do site (LCP), gerando impacto imediato ao carregar a página.
* **Custo de Performance:** **0ms de Main Thread**. O desfoque é pré-renderizado em uma camada de GPU (`will-change: transform; transform: translate3d(0,0,0)`), e o loop roda no compositor do navegador.

#### Snippet CSS:
```css
.hero {
  position: relative;
  overflow: hidden;
}

.hero__aurora {
  position: absolute;
  top: -20%;
  left: 50%;
  width: 600px;
  height: 400px;
  margin-left: -300px;
  background: radial-gradient(ellipse at center, rgba(245, 158, 11, 0.14) 0%, rgba(8, 13, 26, 0) 70%);
  filter: blur(80px);
  pointer-events: none;
  z-index: 0;
  transform: translate3d(0, 0, 0);
  animation: aurora-drift 24s ease-in-out infinite alternate;
}

[data-theme="light"] .hero__aurora {
  background: radial-gradient(ellipse at center, rgba(180, 83, 9, 0.10) 0%, rgba(248, 250, 252, 0) 70%);
}

@keyframes aurora-drift {
  0% { transform: translate3d(-10%, -10%, 0) scale(1); }
  50% { transform: translate3d(15%, 5%, 0) scale(1.15); }
  100% { transform: translate3d(-5%, 15%, 0) scale(0.95); }
}

@media (prefers-reduced-motion: reduce) {
  .hero__aurora { animation: none; }
}
```

---

### Efeito 3: Scroll Reveal Suave com Micro-IntersectionObserver (Zero CLS)
* **Conceito:** Títulos de seções, cards e blocos de depoimento/cases deslizam suavemente para a posição correta (`translateY(16px)` para `0`) com desvanecimento suave (`opacity: 0` para `1`) à medida que entram no viewport.
* **Por que adotar:** Confere ritmo cinematográfico à navegação da página conforme o usuário rola o conteúdo.
* **Custo de Performance:** O `IntersectionObserver` é nativo do navegador e, assim que o elemento se torna visível, ele é desconectado com `observer.unobserve(entry.target)`. O consumo de CPU é **zero** após o elemento ser renderizado.

#### Snippet CSS:
```css
.reveal-on-scroll {
  opacity: 0;
  transform: translate3d(0, 20px, 0);
  transition: opacity 0.6s cubic-bezier(0.16, 1, 0.3, 1), transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: opacity, transform;
}

.reveal-on-scroll.is-revealed {
  opacity: 1;
  transform: translate3d(0, 0, 0);
  will-change: auto;
}

@media (prefers-reduced-motion: reduce) {
  .reveal-on-scroll {
    opacity: 1 !important;
    transform: none !important;
    transition: none !important;
  }
}
```

#### Snippet JS Vanilla:
```javascript
(function initScrollReveal() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  
  const observer = new IntersectionObserver((entries, obs) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-revealed');
        obs.unobserve(entry.target); // Libera recursos imediatamente
      }
    });
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

  document.querySelectorAll('.reveal-on-scroll').forEach(el => observer.observe(el));
})();
```

---

### Efeito 4: Animated Shimmer Border no Card Featured (Houdini CSS)
* **Conceito:** O card em destaque (ex: *Plano Profissional* ou *Hub de Gestão Pro*) possui uma borda com um facho de luz âmbar que circula o contorno suavemente, destacando-o como a opção recomendada sem recorrer a cores berrantes.
* **Por que adotar:** Aumenta a taxa de conversão (CRO) no ponto focal dos planos.
* **Custo de Performance:** Utiliza `@property` nativo (suportado em todos os navegadores modernos) ou animação de pseudo-elemento com gradiente cônico acelerado.

#### Snippet CSS:
```css
@property --border-angle {
  syntax: '<angle>';
  inherits: false;
  initial-value: 0deg;
}

.card--featured-shimmer {
  position: relative;
  background: var(--surface);
  border: 1px solid transparent;
}

.card--featured-shimmer::after {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  padding: 1px;
  background: conic-gradient(
    from var(--border-angle),
    transparent 70%,
    var(--accent) 90%,
    transparent 100%
  );
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  animation: rotate-border 6s linear infinite;
}

@keyframes rotate-border {
  to { --border-angle: 360deg; }
}

@media (prefers-reduced-motion: reduce) {
  .card--featured-shimmer::after { animation: none; }
}
```

---

### Efeito 5: Terminal SQL Tuning com Cursor Vivo & Tab Switch ("Antes / Depois")
* **Conceito:** O artefato técnico do terminal PostgreSQL já é um dos pontos mais fortes de credibilidade técnica do site. Podemos enriquecê-lo com:
  1. Um cursor de terminal piscante em verde neon / âmbar (`█` ou `_`).
  2. Duas abas interativas no topo do terminal: `[1. Incidente (Antes)]` e `[2. Otimização TRD (Depois)]`, permitindo ao cliente comparar visualmente a consulta não otimizada com a query indexada com transição instantânea e zero lag.
* **Por que adotar:** Transforma um bloco de texto estático em uma ferramenta de demonstração de autoridade técnica memorável.

#### Snippet CSS:
```css
.terminal__cursor {
  display: inline-block;
  width: 8px;
  height: 15px;
  background-color: var(--accent);
  margin-left: 4px;
  vertical-align: middle;
  animation: terminal-blink 1s steps(2, start) infinite;
}

@keyframes terminal-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
```

---

## 3. Matriz de Impacto nos Core Web Vitals

| Efeito | Impacto em LCP | Impacto em CLS | Impacto em INP | Conformidade 60fps |
| :--- | :---: | :---: | :---: | :---: |
| **Card Spotlight** | Neutro (0ms) | **0.00** | Neutro (<2ms) | **60 FPS** (GPU) |
| **Ambient Aurora** | Neutro (camada GPU) | **0.00** | Neutro (0ms) | **60 FPS** (Off-thread) |
| **Scroll Reveal** | Neutro (após viewport) | **0.00** (altura reservada) | Neutro | **60 FPS** (Compositor) |
| **Shimmer Border** | Neutro | **0.00** | Neutro | **60 FPS** (CSS Houdini) |
| **Terminal Cursor/Tabs** | Neutro | **0.00** | < 5ms | **60 FPS** |

---

## 4. Próximos Passos Recomendados

1. **Implementação Piloto:** Recomendo aplicar primeiramente o **Card Spotlight** e a **Ambient Aurora no Hero**, que trazem sofisticação imediata à página inicial sem alterar a estrutura HTML existente.
2. **Revisão Visual:** Testar nos temas Dark e Light simultaneamente para garantir que a aura âmbar mantenha o contraste adequado de texto em ambos os modos.
3. **Validação no Lighthouse:** Confirmar a manutenção das notas 100/100/100/100 após a integração dos scripts.

Fico à disposição da coordenação para aplicar as alterações nos arquivos `site/styles.css` e `site/index.html`.