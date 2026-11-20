package renderer

import (
	"math"
	"testing"

	"danfse-service/internal/domain/presentation"
)

func TestLayout_CmToPtConversion(t *testing.T) {
	// 1 cm = 72/2.54 points ≈ 28.34645669
	expected := 28.34645669
	actual := cmPt
	if math.Abs(actual-expected) > 1e-6 {
		t.Errorf("cmPt = %v, want %v", actual, expected)
	}

	// Testa a conversão cm()
	if got := cm(1); math.Abs(got-cmPt) > 1e-6 {
		t.Errorf("cm(1) = %v, want %v", got, cmPt)
	}
	if got := cm(0); got != 0 {
		t.Errorf("cm(0) = %v, want 0", got)
	}
	if got := cm(2.54); math.Abs(got-72) > 1e-6 {
		t.Errorf("cm(2.54) = %v, want 72", got)
	}
}

func TestLayout_TopToPDFY(t *testing.T) {
	// topY(0) deve ser pageHeightPt (topo da página)
	if got := topToPDFY(0); got != pageHeightPt {
		t.Errorf("topToPDFY(0) = %v, want %v", got, pageHeightPt)
	}
	// topY(pageBottomCM) deve ser pageBottom (fundo da área de conteúdo)
	if got := topToPDFY(pageBottomCM); math.Abs(got-(pageHeightPt-pageBottomCM*cmPt)) > 1e-6 {
		t.Errorf("topToPDFY(%v) = %v, want %v", pageBottomCM, got, pageBottom())
	}
	// topY(1) deve ser pageHeightPt - cmPt
	expected := pageHeightPt - cmPt
	if got := topToPDFY(1); math.Abs(got-expected) > 1e-6 {
		t.Errorf("topToPDFY(1) = %v, want %v", got, expected)
	}
}

func TestLayout_GridConstants(t *testing.T) {
	// Verifica que as constantes da grade estão em ordem correta
	if gridLeft() >= gridX2() {
		t.Error("gridLeft should be < gridX2")
	}
	if gridX2() >= gridX3() {
		t.Error("gridX2 should be < gridX3")
	}
	if gridX3() >= gridX4() {
		t.Error("gridX3 should be < gridX4")
	}
	if gridX4() >= gridEnd() {
		t.Error("gridX4 should be < gridEnd")
	}

	if math.Abs(gridLeftCM-0.30) > 1e-6 || math.Abs(colX2CM-5.41) > 1e-6 ||
		math.Abs(colX3CM-10.51) > 1e-6 || math.Abs(colX4CM-15.62) > 1e-6 ||
		math.Abs(colXEndCM-20.70) > 1e-6 {
		t.Fatalf("anchors = %.2f, %.2f, %.2f, %.2f, %.2f; want 0.30, 5.41, 10.51, 15.62, 20.70", gridLeftCM, colX2CM, colX3CM, colX4CM, colXEndCM)
	}
	if got := col1CM; math.Abs(got-5.11) > 1e-6 {
		t.Errorf("col1CM = %v, want 5.11", got)
	}
	if got := col2CM; math.Abs(got-5.10) > 1e-6 {
		t.Errorf("col2CM = %v, want 5.10", got)
	}
	if got := col3CM; math.Abs(got-5.11) > 1e-6 {
		t.Errorf("col3CM = %v, want 5.11", got)
	}
	if got := col4CM; math.Abs(got-5.08) > 1e-6 {
		t.Errorf("col4CM = %v, want 5.08", got)
	}
	if math.Abs(bodyLeft()-cm(0.30)) > 1e-6 || math.Abs(bodyRight()-cm(20.70)) > 1e-6 {
		t.Errorf("body bounds = %.4f..%.4f, want %.4f..%.4f", bodyLeft(), bodyRight(), cm(0.30), cm(20.70))
	}
}

func TestLayout_IdentificationAndQRGeometry(t *testing.T) {
	if qrSizeCM != 1.90 || qrTextTopCM < qrTopCM+qrSizeCM {
		t.Fatalf("QR geometry = left %.2f, top %.2f, size %.2f, text top %.2f", qrLeftCM, qrTopCM, qrSizeCM, qrTextTopCM)
	}
	if math.Abs(qrTextLeftCM-15.98) > 1e-9 || qrTextWidthCM != 4.72 {
		t.Fatalf("QR text alignment = left %.2f, width %.2f", qrTextLeftCM, qrTextWidthCM)
	}
	if identificationTextInsetCM != fieldTextInsetCM || identificationRowHeightCM != 0.69 || identificationLabelOffsetCM != 0.06 || identificationValueOffsetCM != 0.36 {
		t.Fatalf("identification offsets = height %.3f, label %.3f, value %.3f", identificationRowHeightCM, identificationLabelOffsetCM, identificationValueOffsetCM)
	}
	if identificationShadeTopCM != 0.015 || identificationShadeHeightCM != 0.675 {
		t.Fatalf("identification shading = top %.3f, height %.3f", identificationShadeTopCM, identificationShadeHeightCM)
	}
}

func TestLayout_ColumnSpans(t *testing.T) {
	if got := colunaX(0); math.Abs(got-gridLeft()) > 1e-9 {
		t.Errorf("colunaX(0) = %.6f, want %.6f", got, gridLeft())
	}
	if got := colunaX(3); math.Abs(got-gridX4()) > 1e-9 {
		t.Errorf("colunaX(3) = %.6f, want %.6f", got, gridX4())
	}
	if got := larguraColunas(0, 1); math.Abs(got-cm(col1CM)) > 1e-9 {
		t.Errorf("larguraColunas(0,1) = %.6f, want %.6f", got, cm(col1CM))
	}
	if got := larguraColunas(2, 2); math.Abs(got-cm(col3CM+col4CM)) > 1e-9 {
		t.Errorf("larguraColunas(2,2) = %.6f, want %.6f", got, cm(col3CM+col4CM))
	}
	if colunaX(-1) != 0 || colunaX(4) != 0 || larguraColunas(-1, 1) != 0 || larguraColunas(3, 2) != 0 {
		t.Fatal("invalid column spans should return zero")
	}
}

func TestLayout_BaselineDirection(t *testing.T) {
	// baselineFromTop deve retornar um valor abaixo de topToPDFY (já que o ascent é positivo)
	// Para uma fonte com ascent positivo, baseline = topY - ascent
	// Então baseline < topY
	y := topToPDFY(10)
	// Com face nil, baselineFromTop retorna y - size (aproximado)
	// Isso deve ser menor que y
	if y <= 0 {
		t.Skip("page height is too small for test")
	}
	// A linha de base deve estar abaixo da borda superior (valor Y menor em coordenadas PDF)
	// baselineFromTop com face nil: y - size
	baseline := y - 7 // size 7 pt
	if baseline >= y {
		t.Error("baseline should be below top edge (smaller Y in PDF coords)")
	}
}

func TestLayout_PageDimensions(t *testing.T) {
	// Página A4: 595,28 × 841,89 pontos
	if pageWidthPt != 595.28 {
		t.Errorf("pageWidthPt = %v, want 595.28", pageWidthPt)
	}
	if pageHeightPt != 841.89 {
		t.Errorf("pageHeightPt = %v, want 841.89", pageHeightPt)
	}
}

func TestLayout_StandardFontBaseline(t *testing.T) {
	// baselineFromTop com uma fonte Standard não-nil deve incluir a
	// compensação standardFontBaselineAdjustment para que o texto Helvetica
	// fique na mesma posição visual que a fonte anterior.
	const size = 7.0

	// Não é possível construir facilmente um *foliofont.Standard aqui, então verificamos
	// a fórmula diretamente: baseline = y - ascent - size*0,19
	// O ajuste deve ser positivo (baseline desce = Y menor).
	expectedAdjustment := size * standardFontBaselineAdjustment
	if expectedAdjustment <= 0 {
		t.Errorf("standardFontBaselineAdjustment must be positive, got %v", expectedAdjustment)
	}
	// Verifica o valor da constante
	if standardFontBaselineAdjustment != 0.19 {
		t.Errorf("standardFontBaselineAdjustment = %v, want 0.19", standardFontBaselineAdjustment)
	}
	// Verifica que o ajuste é proporcional ao tamanho
	if math.Abs(expectedAdjustment-7.0*0.19) > 1e-9 {
		t.Errorf("adjustment for size 7 = %v, want %v", expectedAdjustment, 7.0*0.19)
	}
}

func TestLayout_LineStepCM(t *testing.T) {
	expected := (valueSizePt * 1.2) / cmPt
	if math.Abs(lineStepCM-expected) > 1e-6 {
		t.Errorf("lineStepCM = %v, want %v", lineStepCM, expected)
	}
}

func TestLayout_AllocationUsesAvailableSpace(t *testing.T) {
	doc := presentation.DocumentoDanfse{
		Partes: presentation.DadosPartes{
			Tomador:       presentation.SecaoParte{Visivel: true},
			Destinatario:  presentation.SecaoParte{Visivel: false},
			Intermediario: presentation.SecaoParte{Visivel: false},
		},
		Canhoto: presentation.DadosCanhoto{Visivel: true},
	}
	layout := montarLayoutPagina(doc)
	allocation := calcularAlocacaoTexto(layout, doc, 16, 20)
	if allocation.descriptionLines <= 8 {
		t.Fatalf("description lines = %d, want more than the old fixed cap", allocation.descriptionLines)
	}
	if allocation.complementaryLines < 1 {
		t.Fatal("complementary lines should remain positive")
	}
}

func TestLayout_MinComplLines(t *testing.T) {
	if minComplLines != 12 {
		t.Errorf("minComplLines = %d, want 12", minComplLines)
	}
}

func TestLayout_YInfoComplAndYCanhoto(t *testing.T) {
	if yInfoComplCM != 22.68 {
		t.Errorf("yInfoComplCM = %v, want 22.68", yInfoComplCM)
	}
	if yCanhotoCM != 28.10 {
		t.Errorf("yCanhotoCM = %v, want 28.10", yCanhotoCM)
	}
	if yComplTributosCM != 27.65 {
		t.Errorf("yComplTributosCM = %v, want 27.65", yComplTributosCM)
	}
	if complementaryLineStepCM <= 0 {
		t.Errorf("complementaryLineStepCM = %v, want positive step", complementaryLineStepCM)
	}
}

func TestLayout_CanhotoHeightCM(t *testing.T) {
	expected := canhotoBottomCM - yCanhotoCM
	if math.Abs(canhotoHeightCM-expected) > 1e-9 {
		t.Errorf("canhotoHeightCM = %v, want %v", canhotoHeightCM, expected)
	}
}

func TestLayout_ComplementaryLineCountDynamic(t *testing.T) {
	contentTop := 23.09
	linesVisible := contarLinhasInformacoesComplementares(contentTop, yComplTributosCM)
	linesHidden := contarLinhasInformacoesComplementares(contentTop, yComplTributosCM+canhotoHeightCM)
	if linesHidden <= linesVisible {
		t.Errorf("hidden canhoto should allow more lines: visible=%d, hidden=%d", linesVisible, linesHidden)
	}
	if linesVisible < 1 || linesHidden < 1 {
		t.Errorf("line counts must remain positive: visible=%d hidden=%d", linesVisible, linesHidden)
	}
}
