package renderer

import (
	"math"

	"danfse-service/internal/domain/presentation"
	foliofont "github.com/carlos7ags/folio/font"
)

// Dimensões da página PDF (pontos, origem inferior-esquerda).
const (
	pageWidthPt  = 595.28
	pageHeightPt = 841.89
)

// cmPt é o fator de conversão exato: 1 cm = 72/2,54 pontos.
const cmPt = 72.0 / 2.54

// Margens da página (cm).
const (
	pageLeftCM   = 0.15
	pageTopCM    = 0.15
	pageRightCM  = 20.85
	pageBottomCM = 29.55
)

// Limites das colunas da grade (cm) — bordas direitas de cada zona de coluna.
const (
	gridLeftCM       = 0.30
	colX2CM          = 5.41
	colX3CM          = 10.51
	colX4CM          = 15.62
	colXEndCM        = 20.70
	colXQRStart      = 17.48 // borda esquerda do código QR (cm)
	qrLeftCM         = 17.39
	qrTopCM          = 1.58
	qrSizeCM         = 1.90
	qrTextTopCM      = 3.53
	qrTextStepCM     = 0.21
	qrTextWidthCM    = 4.72
	accessKeyTopCM   = 1.48
	accessKeyWidthCM = 15.30
	nfseFieldsTopCM  = 2.27
	dpsFieldsTopCM   = 2.96
)

const qrTextLeftCM = qrLeftCM + (qrSizeCM-qrTextWidthCM)/2

const (
	fieldTextInsetCM              = 0.08
	identificationTextInsetCM     = fieldTextInsetCM
	identificationRowHeightCM     = 0.69
	identificationLabelOffsetCM   = 0.06
	identificationValueOffsetCM   = 0.36
	identificationShadeTopCM      = 0.015
	identificationShadeHeightCM   = 0.675
	sectionTitleOffsetCM          = 0.07
	partyRowStepCM                = 0.64
	municipalRowStepCM            = 0.65
	federalRowStepCM              = 0.65
	ibsRowStepCM                  = 0.64
	ibsSecondRowOffsetCM          = 1.29
	ibsThirdRowOffsetCM           = 1.94
	totalsRowStepCM               = 0.69
	serviceTaxDescriptionOffsetCM = 0.65
	serviceDescriptionOffsetCM    = 1.05
	complementaryContentOffsetCM  = 0.41
	fieldShadeTopCM               = 0.015
	fieldShadeHeightCM            = partyRowStepCM - fieldShadeTopCM
)

// Larguras das colunas (cm) — derivadas dos limites da grade.
const (
	col1CM = colX2CM - gridLeftCM
	col2CM = colX3CM - colX2CM
	col3CM = colX4CM - colX3CM
	col4CM = colXEndCM - colX4CM
)

// Passo de linha (cm) — deslocamento vertical por linha de descrição usado pelo
// layout fixo de página única. Derivado de valueSizePt × 1,2 de espaçamento entre linhas.
const lineStepCM = (valueSizePt * 1.2) / cmPt

// Espaço para reticências entre o fim do texto e a borda da coluna (cm).
const colGutterCM = 0.15

// Sequência de caracteres de reticências para truncamento.
const ellipsis = "..."

// Orçamento de informações complementares: linhas mínimas preservadas mesmo quando a
// descrição cresce.
const minComplLines = 12

// yInfoCompl e yCanhoto delimitam o bloco de informações complementares na grade
// sem distúrbio: o conteúdo começa em 22,68 cm e o canhoto é fixo em
// 28,10 cm (DANFSe de página única — canhoto nunca se desloca).
const (
	yInfoComplCM = 22.68
	yCanhotoCM   = 28.10
	folgaInfoCM  = 0.10 // folga entre a última linha de informação e o canhoto
)

// Limites da seção complementar e alturas das seções (cm).
const (
	yComplTributosCM             = 27.65
	normalSectionHeightCM        = 0.63
	complementarySectionHeightCM = 0.39
	totalSectionHeightCM         = 0.67
	canhotoBottomCM              = 28.77
	canhotoHeightCM              = canhotoBottomCM - yCanhotoCM // 0.67
)

// limiteInferiorBlocoCM retorna a coordenada y (cm) que delimita o espaço
// disponível para o bloco de informações complementares: yCanhotoCM quando o
// canhoto está visível, canhotoBottomCM quando oculto.
func limiteInferiorBlocoCM(visivelCanhoto bool) float64 {
	if visivelCanhoto {
		return yCanhotoCM
	}
	return canhotoBottomCM
}

// Altura do bloco suprimido de parte (cm) — bloco compacto de uma linha.
const suppressedPartyBlockHeightCM = 0.40

// Passo de linha para o bloco de informações complementares: fonte de 6,5 pt × 1,2
// de espaçamento entre linhas, convertido para centímetros.
const complementaryLineStepCM = (6.5 * 1.2) / cmPt

// --- Auxiliares de conversão ---

// topToPDFY converte uma coordenada top-em-centímetros para um Y PDF (pontos,
// origem inferior-esquerda).  Y PDF = pageHeightPt − topCM × cmPt.
func topToPDFY(topCM float64) float64 {
	return pageHeightPt - topCM*cmPt
}

// cm converte centímetros para pontos PDF.
func cm(v float64) float64 {
	return v * cmPt
}

// baselineFromTop retorna a linha de base Y PDF para texto posicionado em topCM com
// o tamanho de fonte dado.  Usa o Ascent da fonte Standard escalado para o
// tamanho da fonte, que é a forma correta de calcular a linha de base — NÃO
// topY − size.
func baselineFromTop(topCM float64, size float64, ef *foliofont.Standard) float64 {
	y := topToPDFY(topCM)
	if ef == nil {
		return y - size // fallback: aproximado
	}
	return y - ef.Ascent(size) - size*standardFontBaselineAdjustment
}

// --- Auxiliares da grade (cm → pt) ---

func gridLeft() float64  { return gridLeftCM * cmPt }
func gridX2() float64    { return colX2CM * cmPt }
func gridX3() float64    { return colX3CM * cmPt }
func gridX4() float64    { return colX4CM * cmPt }
func gridEnd() float64   { return colXEndCM * cmPt }
func gridXQR() float64   { return colXQRStart * cmPt }
func bodyLeft() float64  { return gridLeftCM * cmPt }
func bodyRight() float64 { return colXEndCM * cmPt }

func colunaX(indice int) float64 {
	limites := [...]float64{gridLeftCM, colX2CM, colX3CM, colX4CM}
	if indice < 0 || indice >= len(limites) {
		return 0
	}
	return limites[indice] * cmPt
}

func larguraColunas(inicio, quantidade int) float64 {
	if inicio < 0 || quantidade <= 0 || inicio+quantidade > 4 {
		return 0
	}
	limites := [...]float64{gridLeftCM, colX2CM, colX3CM, colX4CM, colXEndCM}
	return (limites[inicio+quantidade] - limites[inicio]) * cmPt
}
func pageLeft() float64   { return pageLeftCM * cmPt }
func pageTop() float64    { return pageTopCM * cmPt }
func pageRight() float64  { return pageRightCM * cmPt }
func pageBottom() float64 { return pageBottomCM * cmPt }

// addLine desenha uma linha horizontal em topCM de gridLeft a gridEnd.
func addLineH(page interface {
	AddLine(x1, y1, x2, y2, w float64, c [3]float64)
},
	topCM float64, w float64, c [3]float64) {
	page.AddLine(bodyLeft(), topToPDFY(topCM), bodyRight(), topToPDFY(topCM), w, c)
}

// addLineV desenha uma linha vertical em xCM de topCM a bottomCM.
func addLineV(page interface {
	AddLine(x1, y1, x2, y2, w float64, c [3]float64)
},
	xCM, topCM, bottomCM float64, w float64, c [3]float64) {
	page.AddLine(xCM*cmPt, topToPDFY(topCM), xCM*cmPt, topToPDFY(bottomCM), w, c)
}

// Constantes de altura do layout (cm).
const (
	suppressedMunicipalBlockHeightCM = 0.32
	partyBlockHeightCM               = 1.94
	municipalBlockHeightCM           = 2.59
)

// Constantes de âncora nomeadas do layout (cm) — bordas superiores de cada seção.
const (
	tomadorTopCM       = 6.92
	destinatarioTopCM  = 8.86
	intermediarioTopCM = 10.80
	serviceTopCM       = 12.74
	municipalTopCM     = 14.43
	federalTopCM       = 17.02
	ibsTopCM           = 18.32
	totalsTopCM        = 20.90
	complementaryTopCM = 22.27
)

// descriptionContentOffsetCM é o deslocamento vertical a partir do topo do bloco de serviço
// até a área de conteúdo da descrição.
const descriptionContentOffsetCM = 1.35

// pageLayout armazena as posições top calculadas para cada seção do layout
// de página única do DANFSe.
type pageLayout struct {
	alturaTomador       float64
	alturaDestinatario  float64
	alturaIntermediario float64
	serviceTop          float64
	municipalTop        float64
	federalTop          float64
	ibsTop              float64
	totalsTop           float64
	complementaryTop    float64
}

type textAllocation struct {
	descriptionLines   int
	complementaryLines int
	shiftCM            float64
}

func montarLayoutPagina(doc presentation.DocumentoDanfse) pageLayout {
	l := pageLayout{
		alturaTomador:       tomadorTopCM,
		alturaDestinatario:  destinatarioTopCM,
		alturaIntermediario: intermediarioTopCM,
		serviceTop:          serviceTopCM,
		municipalTop:        municipalTopCM,
		federalTop:          federalTopCM,
		ibsTop:              ibsTopCM,
		totalsTop:           totalsTopCM,
		complementaryTop:    complementaryTopCM,
	}
	partyHeight := func(visivel bool) float64 {
		if visivel {
			return partyBlockHeightCM
		}
		return suppressedPartyBlockHeightCM
	}
	alturaTomador := partyHeight(doc.Partes.Tomador.Visivel)
	alturaDestinatario := partyHeight(doc.Partes.Destinatario.Visivel)
	alturaIntermediario := partyHeight(doc.Partes.Intermediario.Visivel)
	l.alturaDestinatario = l.alturaTomador + alturaTomador
	l.alturaIntermediario = l.alturaDestinatario + alturaDestinatario
	l.serviceTop = l.alturaIntermediario + alturaIntermediario
	l.municipalTop = l.serviceTop + (municipalTopCM - serviceTopCM)
	municipalHeight := municipalBlockHeightCM
	if !doc.Tributacoes.Municipal.Visivel {
		municipalHeight = suppressedMunicipalBlockHeightCM
	}
	l.federalTop = l.municipalTop + municipalHeight
	l.ibsTop = l.federalTop + (ibsTopCM - federalTopCM)
	l.totalsTop = l.ibsTop + (totalsTopCM - ibsTopCM)
	l.complementaryTop = l.totalsTop + (complementaryTopCM - totalsTopCM)
	return l
}

// contarLinhasInformacoesComplementares retorna o número de linhas de informações complementares que
// cabem entre contentTopCM e a linha de totais de tributos (taxLineTopCM), com um
// mínimo de 1.
func contarLinhasInformacoesComplementares(contentTopCM, taxLineTopCM float64) int {
	lines := int(math.Floor((taxLineTopCM-contentTopCM-folgaInfoCM)/complementaryLineStepCM + 1e-9))
	if lines < 1 {
		return 1
	}
	return lines
}

func calcularAlocacaoTexto(layout pageLayout, doc presentation.DocumentoDanfse, descricaoLinhas, complementarLinhas int) textAllocation {
	if descricaoLinhas < 1 {
		descricaoLinhas = 1
	}
	if complementarLinhas < 1 {
		complementarLinhas = 1
	}
	reservaComplementarLinhas := complementarLinhas
	if reservaComplementarLinhas > minComplLines {
		reservaComplementarLinhas = minComplLines
	}
	taxLineTop := limiteInferiorBlocoCM(doc.Canhoto.Visivel)
	baseComplementaryContentTop := layout.complementaryTop + complementaryContentOffsetCM
	availableComplementaryCM := taxLineTop - baseComplementaryContentTop - folgaInfoCM
	if availableComplementaryCM < 0 {
		availableComplementaryCM = 0
	}

	// Preserve all requested lines when both texts fit. When they do not, the
	// service description gets the available space first and complementary
	// information retains at least one visible line.
	extraDescriptionCM := float64(descricaoLinhas-1) * lineStepCM
	requestedComplementaryCM := float64(reservaComplementarLinhas) * complementaryLineStepCM
	if extraDescriptionCM+requestedComplementaryCM > availableComplementaryCM {
		maxExtraCM := availableComplementaryCM - requestedComplementaryCM
		if maxExtraCM < 0 {
			maxExtraCM = 0
		}
		maxDescricaoLinhas := 1 + int(math.Floor(maxExtraCM/lineStepCM+1e-9))
		if descricaoLinhas > maxDescricaoLinhas {
			descricaoLinhas = maxDescricaoLinhas
		}
	}
	if descricaoLinhas < 1 {
		descricaoLinhas = 1
	}

	shift := float64(descricaoLinhas-1) * lineStepCM
	contentTop := baseComplementaryContentTop + shift
	linhasComplementares := contarLinhasInformacoesComplementares(contentTop, taxLineTop)
	return textAllocation{
		descriptionLines:   descricaoLinhas,
		complementaryLines: linhasComplementares,
		shiftCM:            shift,
	}
}
