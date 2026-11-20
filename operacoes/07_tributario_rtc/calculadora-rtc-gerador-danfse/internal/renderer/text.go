package renderer

import (
	"math"
	"strings"

	"github.com/carlos7ags/folio/document"
	foliofont "github.com/carlos7ags/folio/font"
)

// --- Auxiliares de medição de texto ---

func (r *Renderer) countLines(text string, width, size float64) int {
	lines := 0
	remaining := strings.TrimSpace(text)
	for remaining != "" {
		fitBytes, _, _ := r.regular.PrefixFit(remaining, size, width)
		if fitBytes <= 0 {
			break
		}
		part := strings.TrimSpace(remaining[:fitBytes])
		if fitBytes < len(remaining) {
			if n := strings.LastIndexAny(part, " \n"); n > 0 {
				part = strings.TrimSpace(part[:n])
			}
		}
		if part == "" {
			break
		}
		remaining = strings.TrimSpace(remaining[len(part):])
		lines++
	}
	return lines
}

func (r *Renderer) medirLinhas(text string, width, size float64) int {
	return r.countLines(text, width, size)
}

func (r *Renderer) truncateLines(text string, width, size float64, maxLines int) string {
	remaining := strings.TrimSpace(text)
	var lines []string
	for len(lines) < maxLines && remaining != "" {
		fitBytes, _, _ := r.regular.PrefixFit(remaining, size, width)
		if fitBytes <= 0 {
			break
		}
		part := strings.TrimSpace(remaining[:fitBytes])
		if fitBytes < len(remaining) {
			if n := strings.LastIndexAny(part, " \n"); n > 0 {
				part = strings.TrimSpace(part[:n])
			}
		}
		if part == "" {
			break
		}
		lines = append(lines, part)
		remaining = strings.TrimSpace(remaining[len(part):])
	}
	result := strings.Join(lines, "\n")
	if remaining != "" {
		result = strings.TrimRight(result, " \n") + "..."
	}
	return result
}

func (r *Renderer) ajustarMultiplasLinhas(text string, width, size float64, maxLines int) string {
	return r.truncateLines(text, width, size, maxLines)
}

// --- Auxiliares de renderização de texto ---

func (r *Renderer) text(page *document.Page, value string, x, y, size float64, font *foliofont.Standard, color ...[3]float64) {
	if strings.TrimSpace(value) == "" {
		return
	}
	fill := blackRGB
	if len(color) > 0 {
		fill = color[0]
	}
	page.ContentStream().SetFillColorRGB(fill[0], fill[1], fill[2])
	page.AddText(value, font, size, x, y)
	page.ContentStream().SetFillColorRGB(blackRGB[0], blackRGB[1], blackRGB[2])
}

func (r *Renderer) textTop(page *document.Page, value string, x, topCM, size float64, font *foliofont.Standard, color ...[3]float64) {
	r.text(page, value, x, baselineFromTop(topCM, size, font), size, font, color...)
}

func (r *Renderer) centerText(page *document.Page, value string, x, top, width, size float64, font *foliofont.Standard, color ...[3]float64) {
	w := font.MeasureString(value, size)
	xCenter := x + (width-w)/2
	y := baselineFromTop(top, size, font)
	fill := blackRGB
	if len(color) > 0 {
		fill = color[0]
	}
	page.ContentStream().SetFillColorRGB(fill[0], fill[1], fill[2])
	page.AddText(value, font, size, xCenter, y)
	page.ContentStream().SetFillColorRGB(blackRGB[0], blackRGB[1], blackRGB[2])
}

func (r *Renderer) wrapText(page *document.Page, value string, x, top, width, size float64, font *foliofont.Standard, maxLines int) {
	if maxLines <= 0 {
		return
	}
	lineHeight := size * 1.2
	remaining := strings.TrimSpace(value)
	for line := 0; line < maxLines && remaining != ""; line++ {
		part, consumed := nextTextLine(remaining, font, size, width)
		if part == "" || consumed == 0 {
			break
		}
		if line == maxLines-1 && consumed < len(remaining) {
			part = truncateText(part+ellipsis, font, size, width)
		}
		r.textTop(page, part, x, top+lineHeight*float64(line)/cmPt, size, font)
		remaining = strings.TrimSpace(remaining[consumed:])
	}
}

// --- Auxiliares de campo / rótulo ---

// fallbackTraco retorna "-" quando a string está vazia ou contém apenas
// espaços em branco; caso contrário devolve o valor original intacto.
func fallbackTraco(s string) string {
	if strings.TrimSpace(s) == "" {
		return "-"
	}
	return s
}

func (r *Renderer) desenharCampo(page *document.Page, x, topCM, widthCM float64, rotulo, valor string, sombreado bool) {
	estilo := estiloCampo
	if sombreado {
		estilo = estiloCampoSombreado
	}
	r.desenharCelula(page, x, topCM, widthCM, rotulo, valor, estilo)
}

func (r *Renderer) desenharCampoIdentificacao(page *document.Page, x, topCM, widthCM float64, rotulo, valor string, sombreado bool) {
	estilo := estiloIdentificacao
	if sombreado {
		estilo = estiloIdentificacaoSombreada
	}
	r.desenharCelula(page, x, topCM, widthCM, rotulo, valor, estilo)
}

func (r *Renderer) desenharRotuloValor(page *document.Page, x, topCM, width float64, rotulo, valor string, rotuloSize, valorSize float64) {
	estilo := estiloAcesso
	estilo.tamanhoRotuloPt = rotuloSize
	estilo.tamanhoValorPt = valorSize
	r.desenharCelula(page, x, topCM, width/cmPt, rotulo, valor, estilo)
}

func (r *Renderer) desenharCelula(page *document.Page, x, topCM, widthCM float64, rotulo, valor string, estilo estiloCelula) {
	recuoEsquerdo := cm(estilo.recuoEsquerdoCM)
	recuoDireito := cm(estilo.recuoDireitoCM)
	largura := widthCM*cmPt - recuoEsquerdo - recuoDireito
	if estilo.sombreado {
		page.AddRectFilled(
			x,
			topToPDFY(topCM+estilo.sombreadoTopCM+estilo.sombreadoAlturaCM),
			widthCM*cmPt,
			estilo.sombreadoAlturaCM*cmPt,
			grayHeaderRGB,
		)
	}
	if rotulo != "" {
		r.textTop(page, fit(rotulo, r.bold, estilo.tamanhoRotuloPt, largura), x+recuoEsquerdo, topCM+estilo.offsetRotuloCM, estilo.tamanhoRotuloPt, r.bold)
	}
	r.textTop(page, fit(fallbackTraco(valor), r.regular, estilo.tamanhoValorPt, largura), x+recuoEsquerdo, topCM+estilo.offsetValorCM, estilo.tamanhoValorPt, r.regular)
}

func (r *Renderer) desenharMultiplasLinhas(page *document.Page, x, topCM, width, height float64, rotulo, valor string, size float64) {
	recuo := cm(fieldTextInsetCM)
	if rotulo != "" {
		r.textTop(page, rotulo, x+recuo, topCM, labelSizePt, r.bold)
	}
	r.wrapText(page, valor, x+recuo, topCM+0.30, width-2*recuo, size, r.regular, int(math.Max(1, math.Floor(height/(size*1.2)))))
}

// --- Auxiliares de ajuste / truncamento ---

func fit(value string, font *foliofont.Standard, size, width float64) string {
	if font.MeasureString(value, size) <= width {
		return value
	}
	return truncateText(value, font, size, width)
}

func nextTextLine(text string, font *foliofont.Standard, size, width float64) (string, int) {
	fitBytes, _, _ := font.PrefixFit(text, size, width)
	if fitBytes <= 0 {
		return "", 0
	}
	part := strings.TrimSpace(text[:fitBytes])
	consumed := fitBytes
	if fitBytes < len(text) {
		if lastSpace := strings.LastIndexAny(part, " \n"); lastSpace > 0 {
			part = strings.TrimSpace(part[:lastSpace])
			consumed = len(strings.TrimRight(text[:lastSpace], " \n"))
		}
	}
	return part, consumed
}

func ajustarLinha(value string, font *foliofont.Standard, size, width float64) string {
	return fit(value, font, size, width)
}

// truncateText implementa a semântica de truncamento:
//
//   - se o texto cabe dentro de maxWidth (pt), retorna o original;
//   - caso contrário, busca binariamente o maior prefixo que cabe com "..." anexado;
//   - se apenas a elipse cabe, retorna "" (sem largura para dados).
//
// A medição usa Standard.MeasureString / PrefixFit como primitiva,
// mas o contrato da busca binária é preservado exatamente.
func truncateText(text string, font *foliofont.Standard, size, maxWidth float64) string {
	if text == "" {
		return text
	}
	if font.MeasureString(text, size) <= maxWidth {
		return text
	}

	runes := []rune(text)
	lo, hi, melhor := 0, len(runes)-1, ""
	for lo <= hi {
		meio := (lo + hi) / 2
		cand := strings.TrimRight(string(runes[:meio]), " ") + ellipsis
		w := font.MeasureString(cand, size)
		if w <= maxWidth {
			melhor = cand
			lo = meio + 1
		} else {
			hi = meio - 1
		}
	}
	if melhor == ellipsis {
		return ""
	}
	return melhor
}

// splitText divide o texto em linhas de no máximo width (pt) usando
// comportamento de limite de palavra: quando uma palavra não cabe, a linha
// quebra no último espaço antes do transbordamento. maxLines limita a saída;
// se excedido, a última linha recebe "..." anexado.
func splitText(text string, font *foliofont.Standard, size, width float64, maxLines int) []string {
	if maxLines <= 0 {
		return nil
	}
	linhas := make([]string, 0, maxLines)
	remaining := strings.TrimSpace(text)
	for len(linhas) < maxLines && remaining != "" {
		fitBytes, _, _ := font.PrefixFit(remaining, size, width)
		if fitBytes <= 0 {
			break
		}
		part := strings.TrimSpace(remaining[:fitBytes])
		if fitBytes < len(remaining) {
			if n := strings.LastIndexAny(part, " \n"); n > 0 {
				part = strings.TrimSpace(part[:n])
			}
		}
		if part == "" {
			break
		}
		if len(linhas) == maxLines-1 && len(part) < len(remaining) {
			part = truncateText(part, font, size, width)
		}
		linhas = append(linhas, part)
		remaining = strings.TrimSpace(remaining[len(part):])
	}
	return linhas
}

// limitLines corta o texto nas primeiras maxLines linhas de largura pt,
// marcando o corte com elipse na última linha mantida. Usa a abordagem de
// contar caracteres a partir da saída de SplitText em vez de recompor linhas.
func limitLines(text string, font *foliofont.Standard, width, size float64, maxLines int) string {
	linhas, err := splitTextRaw(text, font, width, size)
	if err != nil || len(linhas) <= maxLines {
		return text
	}
	cabem := 0
	for _, l := range linhas[:maxLines] {
		cabem += len([]rune(l))
	}
	runes := []rune(text)
	if cabem > len(runes) {
		cabem = len(runes)
	}
	cabem -= len(ellipsis)
	if cabem < 0 {
		cabem = 0
	}
	return strings.TrimRight(string(runes[:cabem]), " ") + ellipsis
}

// splitTextRaw divide o texto por largura usando PrefixFit (consciente de
// limite de palavra). Retorna todas as linhas (sem limite maxLines).
func splitTextRaw(text string, font *foliofont.Standard, width, size float64) ([]string, error) {
	if width <= 0 {
		return nil, nil
	}
	linhas := make([]string, 0)
	remaining := strings.TrimSpace(text)
	for remaining != "" {
		fitBytes, _, _ := font.PrefixFit(remaining, size, width)
		if fitBytes <= 0 {
			break
		}
		part := strings.TrimSpace(remaining[:fitBytes])
		if fitBytes < len(remaining) {
			if n := strings.LastIndexAny(part, " \n"); n > 0 {
				part = strings.TrimSpace(part[:n])
			}
		}
		if part == "" {
			break
		}
		linhas = append(linhas, part)
		remaining = strings.TrimSpace(remaining[len(part):])
	}
	return linhas, nil
}

// fitCell trunca o texto para caber dentro de limitX (cm) menos o espaçamento,
// usando a fonte atual. Retorna o texto truncado.
func fitCell(text string, font *foliofont.Standard, size, limitXCM float64) string {
	maxW := (limitXCM-colGutterCM)*cmPt - 0 // sem necessidade de rastreamento de cursor aqui
	return truncateText(text, font, size, maxW)
}

// ajustarInformacoesComplementares ajusta o texto de informações complementares
// para caber em maxLines linhas de largura width e tamanho size. Se o texto couber,
// retorna-o intacto. Se não couber e terminar com linhaTributos, preserva o sufixo
// tributário e ajusta somente o prefixo usando busca binária em runas, construindo
// prefixo + "... | " + linhaTributos. Se não couber prefixo, retorna linhaTributos.
// Se não houver sufixo reconhecível, usa o truncamento existente.
func (r *Renderer) ajustarInformacoesComplementares(texto, linhaTributos string, width, size float64, maxLines int) string {
	// Se o texto cabe nas linhas disponíveis, retorna-o intacto.
	if r.medirLinhas(texto, width, size) <= maxLines {
		return texto
	}

	// Texto transborda — tenta preservar o sufixo tributário.
	if linhaTributos != "" && strings.HasSuffix(texto, linhaTributos) {
		prefixo := strings.TrimSuffix(texto, linhaTributos)
		sufixo := " | " + linhaTributos

		// Busca binária em runas para encontrar o maior prefixo que cabe
		// dentro do limite de linhas.
		runes := []rune(prefixo)
		lo, hi, melhor := 0, len(runes)-1, ""
		for lo <= hi {
			meio := (lo + hi) / 2
			cand := strings.TrimRight(string(runes[:meio]), " ") + ellipsis + sufixo
			if r.medirLinhas(cand, width, size) <= maxLines {
				melhor = cand
				lo = meio + 1
			} else {
				hi = meio - 1
			}
		}
		if melhor != "" {
			return melhor
		}
		// Nenhum prefixo cabe — retorna só a linha tributária (sem pipe).
		return linhaTributos
	}

	// Sem sufixo reconhecível — usa o truncamento existente.
	return r.truncateLines(texto, width, size, maxLines)
}
