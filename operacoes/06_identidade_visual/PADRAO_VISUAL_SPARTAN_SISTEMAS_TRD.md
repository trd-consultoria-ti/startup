# Padrão Visual Oficial — Spartan UI nos Sistemas da TRD Consultoria

> **Documento Normativo Institucional**  
> **Status:** HOMOLOGADO E OBRIGATÓRIO EM TODAS AS ENTREGAS DE SOFTWARE DA TRD  
> **Referência Oficial Externa:** [spartan-ng/spartan (GitHub)](https://github.com/spartan-ng/spartan) · [spartan.ng](https://spartan.ng/)  
> **Data de Homologação:** 20 de Novembro de 2026  
> **Aprovado por:** Thiago Duarte (Founder & CEO)

---

## 1. Diretriz Soberana de Identidade Visual para Sistemas TRD

A partir desta data, fica formalmente instituído que **todo e qualquer sistema de software concebido, desenvolvido ou sustentado pela TRD Consultoria e Serviços de TI LTDA** (seja para uso próprio interno, SaaS proprietário ou soluções contratadas por clientes B2B) adotará **estritamente o Spartan UI** como seu padrão ouro de interface e experiência do usuário (UI/UX).

Ficam banidos de todos os projetos TRD o uso de frameworks engessados legados (estilo Bootstrap, Angular Material clássico ou PrimeNG monolítico), bem como elementos visuais infantis, emojis no chassi ou paletas sem contraste fiduciário.

---

## 2. As Duas Camadas Arquiteturais Oficiais

Seguindo a especificação do repositório oficial [`spartan-ng/spartan`](https://github.com/spartan-ng/spartan):

```
┌────────────────────────────────────────────────────────────────────────┐
│             ARQUITETURA DE DESIGN SYSTEM — SPARTAN UI (TRD)            │
├───────────────────────────────────┬────────────────────────────────────┤
│ 1. Camada UX (@spartan-ng/brain)  │ 2. Camada UI (helm)                │
│ Primitivas Headless & Comportamento│ Componentes Estilizados (Tailwind) │
├───────────────────────────────────┼────────────────────────────────────┤
│ • Baseada no Angular CDK          │ • Filosofia "Copy-and-Own"         │
│ • Acessibilidade WAI-ARIA nativa  │ • Código em `src/app/ui/`          │
│ • Navegação total por teclado     │ • Micro-bordas de 1px (Zinc)       │
│ • Focus Trap e restauração        │ • Sombras mínimas (`shadow-2xs`)   │
│ • Reatividade nativa com Signals  │ • Variantes CVA previsíveis        │
└───────────────────────────────────┴────────────────────────────────────┘
```

---

## 3. Paleta Canônica Institucional: Spartan Zinc

A identidade de cores dos sistemas TRD é governada exclusivamente pela escala **Zinc Monocromática**, garantindo elegância atemporal, legibilidade contábil e sobriedade de terminal executivo:

| Token CSS | Modo Claro (Light Mode) | Modo Escuro (Dark Mode) | Função Semântica |
| :--- | :--- | :--- | :--- |
| `--background` | `#ffffff` (Branco puro) | `#09090b` (`zinc-950` estrito) | Fundo estrutural da tela |
| `--foreground` | `#09090b` (`zinc-950`) | `#fafafa` (`zinc-50`) | Texto primário e títulos |
| `--card` | `#ffffff` | `#09090b` / `#101012` | Superfícies e painéis de dados |
| `--primary` | `#18181b` (`zinc-900`) | `#fafafa` (`zinc-50`) | Botões primários e chamadas de ação |
| `--primary-foreground` | `#fafafa` (`zinc-50`) | `#18181b` (`zinc-900`) | Texto em botões primários |
| `--muted` | `#f4f4f5` (`zinc-100`) | `#27272a` (`zinc-800`) | Fundo de abas, cabeçalhos de tabela |
| `--muted-foreground` | `#71717a` (`zinc-500`) | `#a1a1aa` (`zinc-400`) | Rótulos secundários e metadados |
| `--border` / `--input` | `#e4e4e7` (`zinc-200`) | `#27272a` (`zinc-800`) | Micro-bordas de 1px |
| `--ring` | `#18181b` (`zinc-900`) | `#d4d4d8` (`zinc-300`) | Anel de foco acessível de teclado |

> **Veto Pétreo:** Fica terminantemente proibido o uso de tons verdes, oliva ou coloridos no fundo ou nas bordas estruturais da aplicação. Cores semânticas (como verde para sucesso ou vermelho para erro) restringem-se a badges e indicadores de status pontuais.

---

## 4. Iconografia Corporativa Obrigatória

- **Biblioteca Exclusiva:** **Lucide Icons** (`lucide-angular` / `@ng-icons/lucide`).
- **Traço:** Estritamente **1.5px** de espessura (linear / outline only).
- **Dimensões:** **16px × 16px** (contido e microscópico).
- **Cor:** 100% monocromático (`text-zinc-500` / `text-zinc-400` em repouso, ascendendo para `text-zinc-950` / `text-zinc-50` no hover ou seleção).
- **Banimento:** Fica estritamente vetado o uso de emojis (📦, 🏢, 📊, 🛡️) ou ilustrações 3D coloridas em menus e cabeçalhos.

---

## 5. Navegação & Sidebar Oficial (*Icon Rail*)

Todo sistema desenvolvido pela TRD deve disponibilizar uma barra lateral de navegação com as seguintes especificações:
1. **Modo Expandido:** Largura contida de `240px` (`w-60`).
2. **Modo Retraído (*Icon Rail*):** Largura de `56px` (`w-14`), com ícones centralizados e textos esvanecidos sem quebra de layout.
3. **Física da Transição:** `transition: width 180ms cubic-bezier(0.4, 0, 0.2, 1);` (sem atrasos e sem rebote).
4. **Tooltips Flutuantes:** No modo colapsado, o hover em qualquer ícone dispara um micro-balão flutuante instantâneo (`hlm-tooltip`).
5. **Atalho de Teclado Global:** **`Ctrl + B`** para colapsar/expandir e **`Ctrl + M`** para alternar Claro/Escuro.

---

## 6. Governança e Auditoria

Qualquer Pull Request de frontend nos repositórios da TRD Consultoria que viole esta norma visual será sumariamente vetado pelo **Cético** e pelo **QA Gate** com base neste documento.
