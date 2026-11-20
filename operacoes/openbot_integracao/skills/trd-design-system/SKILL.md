---
name: trd-design-system
description: Implementação fiel e aplicação dos padrões visuais do Brandbook da TRD Consultoria em Angular 19 e Tailwind CSS. Define paleta cromática Obsidian Navy (#0b0f19), Deep Slate (#111827), Neural Cyan (#06b6d4) e Electric Cyan (#22d3ee), tipografia JetBrains Mono/Inter, componentes de UI executivos, acessibilidade WCAG AA e dark mode corporativo.
license: Proprietary-TRD
metadata:
  author: TRD Consultoria e Serviços de TI LTDA
  version: "1.0.0"
  cluster: "Fábrica / UX / Front"
---

# TRD Design System — Manual de Identidade Visual & UI

Esta skill governa a aplicação visual dos tokens cromáticos, componentes Tailwind CSS e padrões de interface em todos os projetos da TRD Consultoria.

## 1. Paleta Cromática Institucional (Tokens)
- **Background Primário (Obsidian Navy):** `#0b0f19` (bg-background)
- **Card / Surface (Deep Slate):** `#111827` (bg-card / surface)
- **Borda Suave (Border Slate):** `#1e293b` (border-slate-800)
- **Acento Primário (Neural Cyan):** `#06b6d4` (cyan-500)
- **Hover / Brilho (Electric Cyan):** `#22d3ee` (cyan-400)
- **Texto Primário:** `#f8fafc` (slate-50)
- **Texto Secundário / Muted:** `#94a3b8` (slate-400)

## 2. Tipografia
- **Títulos e Interface Executiva:** `Inter`, `-apple-system`, `sans-serif` (tracking-tight, font-semibold).
- **Códigos, Hashes, Logs e Métricas Financeiras:** `JetBrains Mono`, `Consolas`, `monospace`.

## 3. Diretrizes de Componentes
- Botões primários: Fundo Neural Cyan com texto escuro Obsidian, cantos arredondados (`rounded-lg`), transição suave e hover Electric Cyan.
- Tabelas de dados: Linhas zebradas discretas com hover sutil, cabeçalhos em maiúsculas tracking-wider e badges de status com cores semânticas (Verde: pago/ativo, Amarelo: pendente, Vermelho: atraso/erro).
