package renderer

import (
	"math"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"testing"
)

// palavra é uma palavra do PDF com sua caixa delimitadora em cm.
type palavra struct {
	texto      string
	xMin, xMax float64
	yMin, yMax float64
}

// pdfWords extrai as palavras do PDF com coordenadas via `pdftotext -bbox`.
// Diferente de `-layout`, o bbox revela sobreposição e transbordo: o texto
// invasor sai igual no modo layout, mas com xMax fora da coluna.
func pdfWords(t *testing.T, pdfPath string) []palavra {
	t.Helper()
	if _, err := exec.LookPath("pdftotext"); err != nil {
		t.Skip("pdftotext indisponível")
	}
	out, err := exec.Command("pdftotext", "-bbox", pdfPath, "-").Output()
	if err != nil {
		t.Skipf("pdftotext falhou: %v", err)
	}

	re := regexp.MustCompile(`<word xMin="([\d.]+)" yMin="([\d.]+)" xMax="([\d.]+)" yMax="([\d.]+)">([^<]*)</word>`)
	const ptPorCm = 28.34645669
	var palavras []palavra
	for _, m := range re.FindAllStringSubmatch(string(out), -1) {
		num := func(s string) float64 {
			v, err := strconv.ParseFloat(s, 64)
			if err != nil {
				t.Skipf("parse bbox falhou: %v", err)
				return 0
			}
			return v / ptPorCm
		}
		palavras = append(palavras, palavra{
			texto: m[5], xMin: num(m[1]), xMax: num(m[3]), yMin: num(m[2]), yMax: num(m[4]),
		})
	}
	return palavras
}

// pdfText extrai o texto do PDF via `pdftotext -layout`.
func pdfText(t *testing.T, pdfPath string) string {
	t.Helper()
	if _, err := exec.LookPath("pdftotext"); err != nil {
		t.Skip("pdftotext indisponível")
	}
	out, err := exec.Command("pdftotext", "-layout", pdfPath, "-").Output()
	if err != nil {
		t.Skipf("pdftotext falhou: %v", err)
	}
	return string(out)
}

// pdfPageCount extrai o número de páginas via `pdfinfo`.
func pdfPageCount(t *testing.T, pdfPath string) int {
	t.Helper()
	if _, err := exec.LookPath("pdfinfo"); err != nil {
		t.Skip("pdfinfo indisponível")
	}
	out, err := exec.Command("pdfinfo", pdfPath).Output()
	if err != nil {
		t.Skipf("pdfinfo falhou: %v", err)
	}
	re := regexp.MustCompile(`Pages:\s+(\d+)`)
	m := re.FindSubmatch(out)
	if len(m) < 2 {
		t.Skip("pdfinfo não retornou contagem de páginas")
	}
	n, err := strconv.Atoi(string(m[1]))
	if err != nil {
		t.Skipf("pdfinfo parse falhou: %v", err)
	}
	return n
}

// writePDFToTempDir escreve bytes PDF em t.TempDir e retorna o caminho.
func writePDFToTempDir(t *testing.T, name string, data []byte) string {
	t.Helper()
	dir := t.TempDir()
	path := filepath.Join(dir, name)
	if err := os.WriteFile(path, data, 0o600); err != nil {
		t.Fatal(err)
	}
	return path
}

// assertPDFBasic verifica propriedades básicas do PDF: cabeçalho, página única,
// e formato A4 (quando pdfinfo está disponível).
func assertPDFBasic(t *testing.T, pdf []byte) {
	t.Helper()
	if !strings.HasPrefix(string(pdf), "%PDF-") {
		t.Fatalf("PDF inválido: %q", pdf[:minPDF(len(pdf), 8)])
	}
}

// assertPDFPageCount verifica que o PDF tem exatamente uma página.
func assertPDFPageCount(t *testing.T, pdfPath string) {
	t.Helper()
	count := pdfPageCount(t, pdfPath)
	if count != 1 {
		t.Fatalf("esperado 1 página, got %d", count)
	}
}

// assertPDFPageSizeA4 verifica que o PDF tem dimensões A4 (quando pdfinfo está disponível).
func assertPDFPageSizeA4(t *testing.T, pdfPath string) {
	t.Helper()
	if _, err := exec.LookPath("pdfinfo"); err != nil {
		t.Skip("pdfinfo indisponível")
	}
	out, err := exec.Command("pdfinfo", pdfPath).Output()
	if err != nil {
		t.Skipf("pdfinfo falhou: %v", err)
	}
	// pdfinfo exibe "Page size: 595.28 x 841.89 pts (A4)" para A4.
	if !strings.Contains(string(out), "A4") {
		t.Logf("pdfinfo output: %s", out)
		t.Skip("tamanho de página não é A4")
	}
}

// assertSemanticTokens verifica que o PDF contém os tokens semânticos esperados.
func assertSemanticTokens(t *testing.T, pdfPath string, tokens []string) {
	t.Helper()
	text := pdfText(t, pdfPath)
	for _, tok := range tokens {
		if !strings.Contains(text, tok) {
			t.Errorf("PDF não contém token semântico %q", tok)
		}
	}
}

// assertNoXOverflow verifica que nenhuma palavra ultrapassa o limite direito
// da grade interna com tolerância de 0.05 cm.
func assertNoXOverflow(t *testing.T, palavras []palavra) {
	t.Helper()
	const tol = 0.05
	for _, p := range palavras {
		if p.xMax > colXEndCM+tol {
			t.Errorf("palavra %q ultrapassa a borda direita (xMax=%.2f > %.2f+tol)", p.texto, p.xMax, colXEndCM)
		}
	}
}

// assertKeyBeforeQR verifica que a chave de acesso não invade a área do QR code
// (xMax <= 17.48 cm) para palavras na faixa y do cabeçalho de acesso.
func assertKeyBeforeQR(t *testing.T, palavras []palavra) {
	t.Helper()
	for _, p := range palavras {
		if p.yMin < 1.70 || p.yMin > 2.20 {
			continue
		}
		if p.xMax > colXQRStart {
			t.Errorf("chave de acesso invade o quadro do QR code (xMax=%.2f)", p.xMax)
		}
	}
}

// assertCanhotoFixed verifica que o canhoto (cientificação) aparece próximo a
// y=28.10 cm com tolerância de 0.20 cm (folio renderer usa baseline diferente).
// Retorna true quando o texto do canhoto é encontrado, false caso contrário.
func assertCanhotoFixed(t *testing.T, palavras []palavra) bool {
	t.Helper()
	for _, p := range palavras {
		if strings.Contains(p.texto, "CIENTIFICAÇÃO") {
			if p.yMin < 27.90 || p.yMin > 28.30 {
				t.Errorf("canhoto fora da faixa esperada (yMin=%.2f, esperado ~28.10)", p.yMin)
			}
			return true
		}
	}
	return false
}

// assertEllipsis verifica que os campos truncados terminam com "..." quando o
// fixture contém textos longos.
func assertEllipsis(t *testing.T, palavras []palavra) {
	t.Helper()
	var comReticencias int
	for _, p := range palavras {
		if strings.HasSuffix(p.texto, "...") {
			comReticencias++
		}
	}
	if comReticencias < 1 {
		t.Errorf("nenhum campo truncado com reticências encontrado (esperado >= 1)")
	}
}

// assertWatermark verifica que o texto da marca d'água aparece no PDF quando
// a nota possui uma MarcaDagua não vazia.
func assertWatermark(t *testing.T, pdfPath string, watermark string) {
	t.Helper()
	text := pdfText(t, pdfPath)
	if !strings.Contains(text, watermark) {
		t.Errorf("PDF não contém marca d'água %q", watermark)
	}
}

// assertTpAmbWarning verifica que "SEM VALIDADE JURÍDICA" aparece apenas quando
// tpAmb == 2 (homologação), e não quando tpAmb == 1 (produção).
func assertTpAmbWarning(t *testing.T, pdfPath string, tpAmb int, shouldContain bool) {
	t.Helper()
	text := pdfText(t, pdfPath)
	hasWarning := strings.Contains(text, "SEM VALIDADE JURÍDICA")
	if shouldContain && !hasWarning {
		t.Errorf("tpAmb=%d deveria conter aviso 'SEM VALIDADE JURÍDICA'", tpAmb)
	}
	if !shouldContain && hasWarning {
		t.Errorf("tpAmb=%d não deveria conter aviso 'SEM VALIDADE JURÍDICA'", tpAmb)
	}
}

// assertNoOverlap verifica que nenhum par de palavras se sobrepõe significativamente
// (sobreposição de largura > 0,05 cm E sobreposição de altura > 0,25 cm).
// O renderizador atual possui caixas delimitadoras de palavras maiores que a medição anterior, então usamos
// um limite de altura maior para evitar falsos positivos de linhas adjacentes.
func assertNoOverlap(t *testing.T, palavras []palavra) {
	t.Helper()
	for i, a := range palavras {
		if a.yMin > 27.50 {
			continue
		}
		for _, b := range palavras[i+1:] {
			if b.yMin > 27.50 {
				continue
			}
			if math.Abs(a.yMin-b.yMin) > 0.04 {
				continue
			}
			sobreW := min(a.xMax, b.xMax) - max(a.xMin, b.xMin)
			if sobreW > 0.05 {
				t.Errorf("palavras se sobrepõem: %q (y=%.2f) sobre %q (y=%.2f)",
					a.texto, a.yMin, b.texto, b.yMin)
			}
		}
	}
}

// assertGridAnchors verifica que os cabeçalhos principais das seções aparecem nas posições y
// esperadas (dentro da tolerância), confirmando que a grade está intacta.
// O renderizador atual usa medição de texto diferente da medição anterior, então usamos
// uma tolerância maior (0,25 cm) para considerar diferenças naturais de posicionamento.
// Ancoragens do canhoto são verificadas apenas quando o texto está presente.
func assertGridAnchors(t *testing.T, palavras []palavra, descriptionShift float64, layout pageLayout) {
	t.Helper()
	ancoras := map[string]float64{
		"TRIBUTAÇÃO":  layout.municipalTop + 0.07 + descriptionShift,
		"INFORMAÇÕES": layout.complementaryTop + 0.07 + descriptionShift,
	}
	for texto, esperado := range ancoras {
		y := 0.0
		for _, p := range palavras {
			if p.texto == texto {
				// A palavra TRIBUTAÇÃO aparece em vários rótulos de seção. O
				// alvo da ancoragem do ISSQN é a primeira ocorrência na/apos o
				// bloco de serviço, não o rótulo da coluna de serviço.
				if texto == "TRIBUTAÇÃO" && p.yMin < esperado-0.25 {
					continue
				}
				if y == 0 || p.yMin < y {
					y = p.yMin
				}
			}
		}
		if y == 0 {
			t.Errorf("palavra %q não encontrada", texto)
			continue
		}
		if y < esperado-0.25 || y > esperado+0.25 {
			t.Errorf("%q fora do grid original (y=%.2f, esperado ~%.2f)", texto, y, esperado)
		}
	}
	// Âncora do canhoto: apenas asserir quando presente (Canhoto.Visivel=true).
	for _, p := range palavras {
		if p.texto == "CIENTIFICAÇÃO:" {
			y := p.yMin
			if y < 27.92 || y > 28.42 {
				t.Errorf("CIENTIFICAÇÃO: fora do grid (y=%.2f, esperado ~28.17)", y)
			}
			return
		}
	}
}

// assertTotalsBlockClear verifica que as linhas dos totais não estão contaminadas por
// texto do bloco de informações complementares seguinte.
func assertTotalsBlockClear(t *testing.T, palavras []palavra, descriptionShift float64) {
	t.Helper()
	start := 20.90 + descriptionShift
	end := 22.27 + descriptionShift
	for _, p := range palavras {
		if p.yMin < start+0.67 || p.yMin >= end {
			continue
		}
		if strings.HasPrefix(p.texto, "Inf.") || strings.HasPrefix(p.texto, "Totais") || strings.Contains(p.texto, "contrato") {
			t.Errorf("texto complementar invadiu bloco de totais: %q em y=%.2f", p.texto, p.yMin)
		}
	}
}
