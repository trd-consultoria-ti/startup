# Operações 06 — Identidade Visual, Design Tokens & Spartan UI
### TRD Consultoria e Serviços de TI LTDA
*Governança de Marca, Colorimetria Industrial, Padrão Spartan UI e Curadoria Frontend Cooperativo B2B*

---

## 1. Visão Geral da Identidade Visual

A identidade visual e os padrões de interface da **TRD Consultoria** materializam o posicionamento fiduciário da empresa: **solidez patrimonial, sobriedade, precisão matemática e princípio Zero-Noise**. 

Nossos padrões visuais foram construídos para atender clientes de alta exigência (cooperativas de crédito, mesas financeiras e órgãos corporativos), assegurando total conformidade com as normas internacionais de acessibilidade **WCAG 2.2 Nível AAA**.

---

## 2. Catálogo de Manuais, Normas e Tokens

| Artefato | Formato | Papel e Conteúdo |
| :--- | :---: | :--- |
| **[Manual de Identidade Visual TRD](MANUAL_DE_IDENTIDADE_VISUAL_TRD.md)** | `.md` | Manual master de governança da marca: proporções áureas da Keystone Isométrica, tipografia Neo-Grotesque, fórmulas de Cool Rich Black e tabela Pantone oficial. |
| **[Padrão Visual Spartan Sistemas TRD](PADRAO_VISUAL_SPARTAN_SISTEMAS_TRD.md)** | `.md` | Diretriz técnica de engenharia de UI corporativa utilizando Spartan UI (`@spartan-ng/ui-helm`) sobre Tailwind CSS para todas as aplicações web da TRD. |
| **[Curadoria Frontend & Design System Cooperativo B2B](CURATORIA_FRONTEND_DESIGN_SYSTEM_COOPERATIVO.md)** | `.md` | Norma técnica aprofundada de frontend fiduciário: especificação de tokens Dual-Theme (Modo Claro "Solidez da Terra" e Modo Escuro "Elegância Noturna Corporativa" sem `#000000`), benchmarks mundiais (*Tremor*, *Shadcn Fintech*), arquitetura Angular 19 (Signals e OnPush) e os 5 vetos técnicos do Cético. |
| **[tokens.json](tokens.json) & [tokens.css](tokens.css)** | `.json` · `.css` | Dicionário canônico de design tokens no padrão W3C e variáveis CSS (`:root` e `.dark`) prontas para importação. |
| **[tailwind.config.js](tailwind.config.js)** | `.js` | Arquivo de configuração oficial do Tailwind CSS estendendo a paleta cromática e famílias tipográficas da TRD. |
| **[LogoTRD.tsx](LogoTRD.tsx)** | `.tsx` | Componente React / Next.js otimizado para renderização vetorial SVG do logotipo oficial com suporte nativo a temas claro e escuro. |
| **[assinatura_email.html](assinatura_email.html)** | `.html` | Template executivo de assinatura de e-mail corporativo em HTML compatível com clientes de e-mail de desktop e mobile. |
| **[papel_timbrado_template.md](papel_timbrado_template.md)** | `.md` | Template padrão de papel timbrado com cabeçalho institucional para emissão de laudos técnicos e propostas comerciais. |

---

## 3. Paleta Cromática Institucional

```
  OBSIDIAN NAVY (Primária)  TITANIUM SLATE (Secundária)  NEURAL CYAN (Acento IA)  EMERALD QUANTUM (SLA)
       #080D1A                   #1E293B                   #0EA5E9                 #10B981
  C:65 M:50 Y:30 K:100       C:50 M:40 Y:25 K:80       C:70 M:15 Y:0 K:0       C:75 M:0 Y:60 K:0
```

---

## 4. Recursos Vetoriais e Ativos

Os arquivos vetoriais originais em SVG e imagens de alta resolução encontram-se organizados na pasta raiz em [`assets/`](../../assets/).
