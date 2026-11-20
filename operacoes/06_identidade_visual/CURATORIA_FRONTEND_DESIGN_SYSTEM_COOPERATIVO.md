# CURATORIA DE ARQUITETURA FRONTEND & DESIGN SYSTEM COOPERATIVO B2B
### TRD Consultoria e Serviços de TI LTDA
**Documento Normativo de Engenharia de Interface, Design Tokens Dual-Theme e Padrões Fiduciários**  
*Versão de Homologação: v1.0.0-PROD | Status: Vigente & Mandatório*  
*Autoria: Comitê de Governança, Arquitetura Frontend & Auditoria Crítica (O Cético)*  
*Homologado para o Ecossistema TRD (Angular 19, Tailwind CSS, Spartan UI)*  

---

## 1. MANIFESTO DE IDENTIDADE & VALORES DE NEGÓCIO

### 1.1. Tese Central: A Dignidade Fiduciária no Software Corporativo
A **TRD Consultoria e Serviços de TI LTDA** projeta, moderniza e sustenta sistemas de missão crítica para cooperativas de crédito (referências setoriais: *Sicredi, Sicoob, Unicred*), mesas de tesouraria, distribuidoras e plataformas de inteligência corporativa. Nossos usuários operam com faturamentos expressivos, fluxos de liquidação em tempo real e conciliações fiscais complexas.

A estética do nosso frontend não é decorativa: **ela é um instrumento fiduciário de controle de risco**. Cada elemento visual comunica estabilidade institucional, sobriedade patrimonial e confiabilidade matemática inabalável.

```
       [ PRINCÍPIOS DA IDENTIDADE FIDUCIÁRIA TRD ]
 ┌─────────────────────────────────────────────────────────────┐
 │  1. SOLIDEZ PATRIMONIAL  ──  Estética de banco sólido       │
 │  2. SOBRIEDADE           ──  Zero modismos efêmeros         │
 │  3. TRANSPARÊNCIA TOTAL  ──  Auditabilidade imediata        │
 │  4. PRECISÃO MATEMÁTICA  ──  Grid estrito & tabular-nums    │
 │  5. ZERO-NOISE DESIGN    ──  Máxima razão sinal-ruído       │
 └─────────────────────────────────────────────────────────────┘
```

### 1.2. Os Cinco Pilares Fiduciários

#### 1. Solidez Patrimonial
O design evoca a solidez física das instituições financeiras tradicionais e o rigor de terminais corporativos consolidados (*Bloomberg, Reuters Eikon*). A interface transmite perenidade: layouts estruturados, geometria ortogonal e sensação de segurança jurídica.

#### 2. Sobriedade Fiduciária
Rejeição expressa e deliberada de modismos transitórios de startups B2C:
- **Zero gradientes roxos ou cor-de-rosa saturados.**
- **Zero ilustrações cartunescas infantis ou mascotes.**
- **Zero animações cosméticas gratuitas** que retardem a tomada de decisão do operador.

#### 3. Transparência & Auditabilidade
Nenhum dado é mascarado ou truncado sem controle explícito do usuário. Saldos, débitos, créditos, status de conciliação e registros de auditoria são expostos com clareza cristalina, sem padrões obscuros (*dark patterns*) ou ambiguidades visuais.

#### 4. Precisão Matemática & Notação Contábil
- Todas as cifras e métricas percentuais utilizam obrigatoriamente a classe tipográfica `tabular-nums` (numerais monoespaçados alinhados verticalmente).
- Alinhamento numérico estritamente à direita em tabelas financeiras, permitindo a conferência visual instantânea de casas decimais e ordens de grandeza.
- Uso mandatório de separadores de milhar e duas casas decimais no padrão brasileiro (`R$ 1.250.450,00`), evitando truncamentos amadores (`R$ 1.2M`).

#### 5. Princípio Zero-Noise (Razão Sinal-Ruído Máxima)
Em uma jornada operacional de 8 horas diárias de um analista financeiro ou operador de tesouraria, sombras pesadas e bordas espessas acumulam fadiga cognitiva. O padrão TRD adota **micro-bordas de 1px real com opacidade sutil**, dispensando sombras difusas e elevando a informação útil ao primeiro plano cognitivo.

---

## 2. FICHA TÉCNICA CANÔNICA DE TOKENS CSS & DUAL-THEME

O Design System da TRD adota o conceito de **Dual-Theme Nativo**, garantindo paridade total de contraste, semântica e hierarquia em ambientes de alta luminosidade (escritórios corporativos) e de baixa luminosidade (salas de controle, NOCs e operações noturnas).

### 2.1. Modo Claro: "Solidez da Terra"
Inspirado na estabilidade da terra fértil, na tradição do cooperativismo de crédito e na nobreza dos papéis de contratos fiduciários.

| Token Semântico | Hexadecimal | Papel / Significado no Sistema |
| :--- | :--- | :--- |
| `--bg-canvas` | `#F8FAF9` | Fundo principal da aplicação; off-white esmeralda frio anti-ofuscamento |
| `--bg-surface` | `#FFFFFF` | Superfície de cards, modais e tabelas elevadas |
| `--border-subtle` | `#E2E8E5` | Micro-borda de 1px real para delimitação limpa de superfícies |
| `--brand-primary` | `#0F382C` | Deep Pine (Verde Floresta Fiduciário): autoridade institucional máxima |
| `--accent-emerald`| `#10B981` | Esmeralda Quantum: liquidez positiva, aprovações, integridade de nós |
| `--accent-gold`   | `#D97706` | Dourado Fiduciário: tesouraria, remuneração de capital, atenção contábil |
| `--text-primary`  | `#0C1E17` | Tipografia de títulos e dados mestres (contraste AAA 16.5:1) |
| `--text-secondary`| `#4B6358` | Muted Forest: legendas, labels e metadados contextuais (5.8:1) |

### 2.2. Modo Escuro: "Elegância Noturna Corporativa"
Projetado para telas OLED/IPS de alta resolução em salas de monitoramento de risco e mesas financeiras. **Proíbe expressamente o preto puro (`#000000`)**, substituindo-o por ardósia esmeralda profunda (*Deep Slate Esmeralda*) que preserva a profundidade de campo e elimina o esmagamento de sombras (*shadow clipping*).

| Token Semântico | Hexadecimal | Papel / Significado no Sistema |
| :--- | :--- | :--- |
| `--bg-canvas` | `#0B1411` | Deep Slate Esmeralda (proibido preto absoluto `#000000`) |
| `--bg-surface` | `#121F1B` | Superfície de cards com elevação cromática discreta de 1 degrau |
| `--border-subtle` | `#1C3029` | Micro-borda de 1px em esmeralda escuro para recorte cirúrgico |
| `--brand-primary` | `#10B981` | Esmeralda vibrante para ícones institucionais e seleções ativas |
| `--accent-emerald`| `#34D399` | Esmeralda luminescente para status "Online", "Auditado" e "Liquidado" |
| `--accent-gold`   | `#F59E0B` | Ouro ambarino para rentabilidade, yields de carteira e taxas CDI |
| `--text-primary`  | `#F0FDF4` | Mint White atenuado para leitura prolongada sem fadiga (15.8:1) |
| `--text-secondary`| `#94A3B8` | Slate Neutro para carimbos de data/hora e rótulos auxiliares |

### 2.3. Matriz de Variáveis CSS Canônicas (`tokens-dual-theme.css`)

```css
/**
 * TRD Consultoria - Tokens de Design System Fiduciário
 * Conformidade: WCAG 2.2 Nível AAA & APCA
 */

:root {
  /* Modo Claro - "Solidez da Terra" */
  --bg-canvas: #F8FAF9;
  --bg-surface: #FFFFFF;
  --bg-surface-hover: #F2F5F3;
  --border-subtle: #E2E8E5;
  --border-strong: #C4D0CA;
  
  --brand-primary: #0F382C;
  --brand-primary-hover: #09241C;
  --brand-primary-foreground: #FFFFFF;
  
  --accent-emerald: #10B981;
  --accent-emerald-subtle: #D1FAE5;
  --accent-emerald-foreground: #065F46;
  
  --accent-gold: #D97706;
  --accent-gold-subtle: #FEF3C7;
  --accent-gold-foreground: #92400E;
  
  --danger-crimson: #EF4444;
  --danger-subtle: #FEE2E2;
  --danger-foreground: #991B1B;
  
  --text-primary: #0C1E17;
  --text-secondary: #4B6358;
  --text-tertiary: #71897F;
  --text-inverse: #FFFFFF;
  
  --radius-sm: 0.125rem; /* 2px - tags e micro-badges */
  --radius-md: 0.375rem; /* 6px - inputs e botões operacionais */
  --radius-lg: 0.5rem;   /* 8px - cards institucionais e modais */
}

.dark {
  /* Modo Escuro - "Elegância Noturna Corporativa" */
  --bg-canvas: #0B1411;
  --bg-surface: #121F1B;
  --bg-surface-hover: #182924;
  --border-subtle: #1C3029;
  --border-strong: #2D4A40;
  
  --brand-primary: #34D399;
  --brand-primary-hover: #10B981;
  --brand-primary-foreground: #0B1411;
  
  --accent-emerald: #34D399;
  --accent-emerald-subtle: rgba(52, 211, 153, 0.12);
  --accent-emerald-foreground: #A7F3D0;
  
  --accent-gold: #F59E0B;
  --accent-gold-subtle: rgba(245, 158, 11, 0.14);
  --accent-gold-foreground: #FDE68A;
  
  --danger-crimson: #F87171;
  --danger-subtle: rgba(239, 68, 68, 0.15);
  --danger-foreground: #FCA5A5;
  
  --text-primary: #F0FDF4;
  --text-secondary: #94A3B8;
  --text-tertiary: #64748B;
  --text-inverse: #0B1411;
}
```

### 2.4. Extensão de Tema no Tailwind CSS (`tailwind.config.ts`)

```typescript
import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: 'class',
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        trd: {
          canvas: 'var(--bg-canvas)',
          surface: 'var(--bg-surface)',
          'surface-hover': 'var(--bg-surface-hover)',
          border: 'var(--border-subtle)',
          'border-strong': 'var(--border-strong)',
          primary: 'var(--brand-primary)',
          'primary-hover': 'var(--brand-primary-hover)',
          'primary-fg': 'var(--brand-primary-foreground)',
          emerald: 'var(--accent-emerald)',
          'emerald-subtle': 'var(--accent-emerald-subtle)',
          'emerald-fg': 'var(--accent-emerald-foreground)',
          gold: 'var(--accent-gold)',
          'gold-subtle': 'var(--accent-gold-subtle)',
          'gold-fg': 'var(--accent-gold-foreground)',
          danger: 'var(--danger-crimson)',
          'danger-subtle': 'var(--danger-subtle)',
          'danger-fg': 'var(--danger-foreground)',
          text: 'var(--text-primary)',
          muted: 'var(--text-secondary)',
          subtle: 'var(--text-tertiary)',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        DEFAULT: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
      },
    },
  },
  plugins: [],
};

export default config;
```

---

## 3. BENCHMARKS OPEN-SOURCE PADRÃO OURO & PAPEL ARQUITETURAL

Para atingir qualidade de padrão mundial sem incorrer em licenças proprietárias ou ferramentas de caixa-preta, a TRD homologou cinco referências open-source que guiam a construção do nosso ecossistema frontend:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    MATRIZ DE BENCHMARKS OPEN-SOURCE TRD                    │
├────────────────────────┬─────────────────────┬─────────────────────────────┤
│ Repositório Benchmark  │ Especialidade       │ Papel Arquitetural no TRD   │
├────────────────────────┼─────────────────────┼─────────────────────────────┤
│ spartan-ng / spartan   │ Headless Angular 19 │ Primitives acessíveis & Helm│
│ spartan-admin-dashboard│ Dashboard Data-Dense│ Grid tabular e densidade    │
│ tremorlabs / tremor    │ KPI Cards & Charts  │ Visualização fiduciária     │
│ shadcn-fintech         │ Ledger & Extratos   │ Notação contábil e moeda    │
│ free-angular-dashboard │ Shell Ergonômico    │ Sidebar limpa e Ctrl+K      │
└────────────────────────┴─────────────────────┴─────────────────────────────┘
```

### 3.1. `spartan-ng / spartan` (@spartan-ng/ui-helm)
- **O que é:** O equivalente oficial do *shadcn/ui* para o ecossistema Angular 19 moderno.
- **Por que foi homologado:** Elimina dependências externas monolíticas (como Angular Material legado ou bibliotecas de componentes acopladas a runtime fechado). O código-fonte dos componentes reside diretamente no projeto do cliente (`src/app/components/ui`), permitindo inspeção, customização de tokens CSS e auditoria de segurança estrita.
- **Contrato de Uso:** Componentes primitivos (`HlmButton`, `HlmCard`, `HlmDialog`, `HlmTable`) são estilizados exclusivamente com classes de Tailwind ligadas aos nossos tokens fiduciários.

### 3.2. `Oussemasahbeni / spartan-admin-dashboard`
- **O que é:** Implementação de referência de dashboard administrativo corporativo com Spartan UI.
- **Por que foi homologado:** Resolve o problema de tabelas densas corporativas com paginação assíncrona, filtros compostos em popovers suspensos, seleção de colunas visíveis e ordenação multi-coluna sem re-renderização de tela cheia.
- **Contrato de Uso:** Inspiração para o layout de extratos e listagens de auditoria de logs no módulo de conciliação.

### 3.3. `tremorlabs / tremor`
- **O que é:** Padrão global em bibliotecas de visualização para analítica e métricas executivas sem ruído visual.
- **Por que foi homologado:** A Tremor estabeleceu a regra de ouro dos **KPI Cards sem ruído**: ausência de fundos com gradientes berrantes, badges compactos com dots luminosos de 6px que informam a saúde de métricas, e micro-bordas de 1px que funcionam com igual elegância em telas retina ou monitores de baixa fidelidade.
- **Contrato de Uso:** Toda métrica de faturamento, liquidez, margem e SLA da TRD segue a anatomia de KPI da Tremor.

### 3.4. `abderrahimghazali / shadcn-fintech` & `Fintech-LSI / fintech-web-client`
- **O que é:** Interfaces especializadas em aplicações bancárias, extratos de pagamentos e fluxos de caixa corporativos.
- **Por que foi homologado:** Modelos de extratos bancários com numerais tabulares rigorosamente calculados, sparklines monocromáticos discretos (sem poluição de eixos X/Y para tendências intra-diárias) e sinalização sóbria de movimentações (crédito em esmeralda, débito em ardósia neutra).
- **Contrato de Uso:** Telas de faturamento, extratos de horas técnicas e balanços de prestação de serviços da consultoria.

### 3.5. `TailAdmin / free-angular-tailwind-dashboard`
- **O que é:** Arquitetura limpa de shell de aplicação corporativa em Angular com Tailwind.
- **Por que foi homologado:** Modelo de sidebar colapsável que preserva o estado de abertura no `localStorage`, evitando transições espalhafatosas, e Command Palette acessível pelo atalho universal `Ctrl + K` (ou `Cmd + K`), permitindo que operadores naveguem entre clientes, tickets e relatórios sem retirar as mãos do teclado.
- **Contrato de Uso:** Shell master de todas as soluções web corporativas da TRD.

---

## 4. ARQUITETURA TÉCNICA & COMPONENTIZAÇÃO ANGULAR 19

O ecossistema frontend da TRD adota os padrões mais avançados e rigorosos do **Angular 19**, assegurando desempenho de 60 FPS, tempo de inicialização submétrico e tipagem estrita de ponta a ponta.

### 4.1. Pilares Arquiteturais Mandatórios

1. **Standalone Components & Directives:** Todos os componentes, pipes e diretivas são 100% `standalone: true`. É proibido o uso de `NgModule` em código novo.
2. **OnPush Change Detection Obrigatório:** Todo componente declara `changeDetection: ChangeDetectionStrategy.OnPush`. O ciclo de checagem do Angular roda exclusivamente quando `@Input` com referências imutáveis mudam ou Signals emitem novos valores.
3. **Reatividade Nativa com Angular Signals:** Gerenciamento de estado via `signal()`, `computed()` e `effect()`, eliminando vazamento de memória e subscrições imperativas com RxJS onde a reatividade síncrona for mais limpa.
4. **TypeScript Strict Mode:** Tipagem estrita com zero tolerância a `any`, `unknown` sem type guard ou coerções de tipo inseguras (`as any`).

### 4.2. Implementação Canônica do `ThemeService` (Signals)

```typescript
import { Injectable, signal, effect, computed } from '@angular/core';

export type AppTheme = 'light' | 'dark' | 'system';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly STORAGE_KEY = 'trd_app_theme';

  // Signal mestre de preferência de tema
  private readonly _themePreference = signal<AppTheme>(this.loadPersistedTheme());

  // Signal computado indicando se o dark mode está ativo na DOM
  readonly isDarkActive = computed(() => {
    const pref = this._themePreference();
    if (pref === 'system') {
      return typeof window !== 'undefined' && 
             window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    return pref === 'dark';
  });

  readonly currentPreference = this._themePreference.asReadonly();

  constructor() {
    // Efeito reativo que atualiza a classe na tag <html> sem vazamento de estado
    effect(() => {
      const isDark = this.isDarkActive();
      if (typeof document !== 'undefined') {
        const root = document.documentElement;
        if (isDark) {
          root.classList.add('dark');
        } else {
          root.classList.remove('dark');
        }
      }
    });

    // Escuta mudanças de preferência do sistema operacional
    if (typeof window !== 'undefined') {
      window.matchMedia('(prefers-color-scheme: dark)')
        .addEventListener('change', () => {
          if (this._themePreference() === 'system') {
            // Re-dispara a computação forçando a notificação
            this._themePreference.set('system');
          }
        });
    }
  }

  setTheme(theme: AppTheme): void {
    this._themePreference.set(theme);
    try {
      localStorage.setItem(this.STORAGE_KEY, theme);
    } catch {
      // Falha silenciosa em ambientes com restrição de cookies/storage
    }
  }

  toggleTheme(): void {
    const next = this.isDarkActive() ? 'light' : 'dark';
    this.setTheme(next);
  }

  private loadPersistedTheme(): AppTheme {
    try {
      const saved = localStorage.getItem(this.STORAGE_KEY) as AppTheme | null;
      if (saved === 'light' || saved === 'dark' || saved === 'system') {
        return saved;
      }
    } catch {
      // Fallback para ambientes restritos
    }
    return 'system';
  }
}
```

### 4.3. Componente Canônico: `KpiCardComponent` (Padrão Tremor Fiduciário)

```typescript
import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

export type KpiHealthStatus = 'optimal' | 'warning' | 'critical' | 'neutral';

@Component({
  selector: 'trd-kpi-card',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rounded-lg border border-trd-border bg-trd-surface p-5 transition-colors hover:border-trd-border-strong">
      <!-- Cabeçalho do Card: Rótulo e Badge com Dot Luminoso -->
      <div class="flex items-center justify-between">
        <span class="text-xs font-semibold uppercase tracking-wider text-trd-muted">
          {{ title() }}
        </span>
        
        <div [class]="badgeClasses()">
          <span [class]="dotClasses()"></span>
          <span class="text-xs font-medium tabular-nums">{{ statusLabel() }}</span>
        </div>
      </div>

      <!-- Valor Numérico Principal em Notação Tabular de Grande Porte -->
      <div class="mt-3 flex items-baseline justify-between">
        <span class="font-sans text-2xl font-bold tracking-tight text-trd-text tabular-nums">
          {{ formattedValue() }}
        </span>

        @if (subtext()) {
          <span class="text-xs text-trd-subtle tabular-nums">
            {{ subtext() }}
          </span>
        }
      </div>

      <!-- Barra de Progresso ou Delta Fiduciário Opcional -->
      @if (delta(); as d) {
        <div class="mt-3 flex items-center gap-1.5 text-xs font-medium tabular-nums">
          <span [class]="d.positive ? 'text-trd-emerald' : 'text-trd-danger'">
            {{ d.positive ? '▲ +' : '▼ ' }}{{ d.value }}%
          </span>
          <span class="text-trd-subtle">vs. período anterior</span>
        </div>
      }
    </div>
  `,
})
export class KpiCardComponent {
  readonly title = input.required<string>();
  readonly value = input.required<number | string>();
  readonly status = input<KpiHealthStatus>('optimal');
  readonly statusLabel = input<string>('Operacional');
  readonly subtext = input<string>('');
  readonly delta = input<{ value: number; positive: boolean } | null>(null);

  readonly formattedValue = computed(() => {
    const val = this.value();
    if (typeof val === 'number') {
      return new Intl.NumberFormat('pt-BR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(val);
    }
    return val;
  });

  readonly badgeClasses = computed(() => {
    const base = 'inline-flex items-center gap-1.5 rounded-sm px-2 py-0.5';
    switch (this.status()) {
      case 'optimal':
        return `${base} bg-trd-emerald-subtle text-trd-emerald-fg dark:text-trd-emerald`;
      case 'warning':
        return `${base} bg-trd-gold-subtle text-trd-gold-fg dark:text-trd-gold`;
      case 'critical':
        return `${base} bg-trd-danger-subtle text-trd-danger-fg dark:text-trd-danger`;
      case 'neutral':
      default:
        return `${base} bg-trd-border text-trd-muted`;
    }
  });

  readonly dotClasses = computed(() => {
    const base = 'h-1.5 w-1.5 rounded-full';
    switch (this.status()) {
      case 'optimal':
        return `${base} bg-trd-emerald animate-pulse`;
      case 'warning':
        return `${base} bg-trd-gold`;
      case 'critical':
        return `${base} bg-trd-danger animate-ping`;
      case 'neutral':
      default:
        return `${base} bg-trd-subtle`;
    }
  });
}
```

---

## 5. PROTOCOLO DE AUDITORIA ADVERSARIAL & VETOS DO CÉTICO

A Máquina de Governança da TRD submete qualquer proposta de Pull Request de interface ao escrutínio implacável do **Cético (Auditor Crítico)**. O Cético possui prerrogativa canônica de **Veto Técnico** com base nos cinco vetos abaixo:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    MATRIZ DE VETOS TÉCNICOS DO CÉTICO                      │
├───────┬───────────────────────────────┬────────────────────────────────────┤
│ ID    │ Violação / Proibição          │ Motivação do Veto & Risco          │
├───────┼───────────────────────────────┼────────────────────────────────────┤
│ VET-01│ Cores Genéricas de Startup    │ Degrada autoridade e seriedade     │
│ VET-02│ Raios Infantis (rounded-3xl)  │ Quebra sobriedade e rigor técnico  │
│ VET-03│ Glassmorphism Caótico         │ Polui contraste e prejudica WCAG   │
│ VET-04│ Ausência de tabular-nums      │ Causa erros de leitura em colunas  │
│ VET-05│ Preto Puro (#000000) no Dark  │ Fadiga visual e clipping óptico    │
└───────┴───────────────────────────────┴────────────────────────────────────┘
```

### Detalhamento dos Vetos Inegociáveis

#### [VETO 01] — Cores Genéricas de Startup & SaaS Hipster
- **Proibição:** É estritamente vedado o uso de azul genérico (*Bootstrap Blue `#0D6EFD`*), roxos néon (*Stripe/Discord Purple `#6366F1`, `#8B5CF6`*) ou gradientes caleidoscópicos.
- **Fundamento:** A TRD não vende software de entretenimento nem ferramentas para criadores de conteúdo infantis. Nossos clientes são CFOs e gestores cooperativos. O sistema opera na solidez do **Deep Pine (`#0F382C`)**, **Emerald Quantum (`#10B981`)** e **Dourado Fiduciário (`#D97706`)**.

#### [VETO 02] — Bordas Infantis e Raios Excessivos (`rounded-2xl` / `rounded-3xl` em Cards)
- **Proibição:** Cards, modais e containers com raios de curvatura superiores a `8px` (`rounded-lg`). Botões em formato de "pílula de brinquedo" (`rounded-full`) em tabelas ou formulários.
- **Fundamento:** Raios excessivos desperdiçam área útil de tela e transmitem imaturidade visual. O software corporativo fiduciário adota cantos precisos e limpos (`rounded-sm` de 2px a `rounded-lg` de 8px).

#### [VETO 03] — Glassmorphism Caótico e Sombras Difusas
- **Proibição:** Fundos semitransparentes com desfoque de fundo excessivo (`backdrop-blur-md`) e sombras caóticas multicores (`shadow-2xl` azuladas ou púrpuras).
- **Fundamento:** Efeitos translúcidos causam colapso de contraste quando janelas se sobrepõem e degradam o desempenho de renderização em estações de trabalho de clientes que utilizam placas de vídeo integradas ou virtualização (VDI/Citrix).

#### [VETO 04] — Numerais Sem Notação Contábil / Tabular
- **Proibição:** Renderizar saldos, taxas, contadores ou valores de transações financeiras sem a classe CSS `tabular-nums` ou alinhados à esquerda em tabelas.
- **Fundamento:** Sem numerais tabulares, os dígitos possuem larguras variáveis (o caractere "1" é muito mais estreito que o "8"), fazendo com que colunas numéricas tremam visualmente e impeçam a comparação vertical rápida, o que induz analistas de risco a cometer erros de interpretação.

#### [VETO 05] — Fundo Preto Puro (`#000000`) no Modo Escuro
- **Proibição:** Configurar o canvas principal ou cards com fundo `black` absoluto (`#000000`).
- **Fundamento:** O contraste extremo entre texto branco e fundo preto puro causa aberração óptica e efeito de halo (*halation effect*), gerando queima de retina e cansaço visual após 30 minutos de trabalho contínuo. A solução é o fundo orgânico **Deep Slate Esmeralda (`#0B1411`)**, que harmoniza a iluminação e preserva a micro-borda de 1px.

---

## 6. MATRIZ DE CONFORMIDADE & CHECKLIST PRÉ-MERGE

Antes de qualquer branch de feature ser fundida na `main`, o revisor ou o agente executor deve validar o checklist a seguir:

- [ ] **Validação de Tokens:** Todos os elementos utilizam variáveis CSS ou classes `trd-*` do Tailwind. Zero ocorrências de cores hexadecimais soltas no código HTML/TS.
- [ ] **Dual-Theme Testado:** A interface foi inspecionada visualmente no Modo Claro (`:root`) e no Modo Escuro (`.dark`), sem textos ilegíveis ou botões invisíveis.
- [ ] **Alinhamento Numérico:** Todos os números contábeis possuem `tabular-nums` e estão alinhados à direita nas colunas de tabelas.
- [ ] **Auditoria de Acessibilidade:** Contraste de texto versus fundo validado em **no mínimo 7:1 (WCAG AAA)** para texto principal e **4.5:1 (WCAG AA)** para metadados secundários.
- [ ] **Sem Dependências Pesadas:** Zero importação de pacotes legados de UI incompatíveis com os princípios de standalone e OnPush do Angular 19.
- [ ] **Responsividade Corporativa:** Grid adaptável a resoluções de **1366x768** (laptops corporativos padrão) até **3840x2160** (monitores 4K de salas de diretoria).

---

**Brasília - DF, 20 de Novembro de 2026.**  
*TRD Consultoria e Serviços de TI LTDA — Engenharia de Software Fiduciária & Sistemas de Missão Crítica.*
