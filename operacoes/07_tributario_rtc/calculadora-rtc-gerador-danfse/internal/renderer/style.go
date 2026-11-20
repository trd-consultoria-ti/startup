package renderer

// Tamanhos de fonte (pontos).
const (
	labelSizePt = 6.0
	valueSizePt = 7.0
	titleSizePt = 7.0
)

type estiloCelula struct {
	tamanhoRotuloPt   float64
	tamanhoValorPt    float64
	recuoEsquerdoCM   float64
	recuoDireitoCM    float64
	offsetRotuloCM    float64
	offsetValorCM     float64
	alturaCM          float64
	sombreado         bool
	sombreadoTopCM    float64
	sombreadoAlturaCM float64
}

var (
	estiloCampo = estiloCelula{
		tamanhoRotuloPt: labelSizePt,
		tamanhoValorPt:  valueSizePt,
		recuoEsquerdoCM: fieldTextInsetCM,
		recuoDireitoCM:  fieldTextInsetCM,
		offsetValorCM:   0.30,
	}
	estiloAcesso = estiloCelula{
		tamanhoRotuloPt: valueSizePt,
		tamanhoValorPt:  valueSizePt,
		recuoEsquerdoCM: fieldTextInsetCM,
		recuoDireitoCM:  fieldTextInsetCM,
		offsetValorCM:   0.30,
	}
	estiloIdentificacao = estiloCelula{
		tamanhoRotuloPt: titleSizePt,
		tamanhoValorPt:  valueSizePt,
		recuoEsquerdoCM: identificationTextInsetCM,
		recuoDireitoCM:  identificationTextInsetCM,
		offsetRotuloCM:  identificationLabelOffsetCM,
		offsetValorCM:   identificationValueOffsetCM,
		alturaCM:        identificationRowHeightCM,
	}
	estiloIdentificacaoSombreada = estiloCelula{
		tamanhoRotuloPt:   titleSizePt,
		tamanhoValorPt:    valueSizePt,
		recuoEsquerdoCM:   identificationTextInsetCM,
		recuoDireitoCM:    identificationTextInsetCM,
		offsetRotuloCM:    identificationLabelOffsetCM,
		offsetValorCM:     identificationValueOffsetCM,
		alturaCM:          identificationRowHeightCM,
		sombreado:         true,
		sombreadoTopCM:    identificationShadeTopCM,
		sombreadoAlturaCM: identificationShadeHeightCM,
	}
	estiloCampoSombreado = estiloCelula{
		tamanhoRotuloPt:   labelSizePt,
		tamanhoValorPt:    valueSizePt,
		recuoEsquerdoCM:   fieldTextInsetCM,
		recuoDireitoCM:    fieldTextInsetCM,
		offsetValorCM:     0.30,
		sombreado:         true,
		sombreadoTopCM:    fieldShadeTopCM,
		sombreadoAlturaCM: fieldShadeHeightCM,
	}
)

// standardFontBaselineAdjustment compensa o deslocamento para cima observado
// quando Helvetica substituiu a fonte anterior.  É uma fração do tamanho da fonte
// aplicada à linha de base para que o topo visual do texto se alinhe
// com o leiaute original.
const standardFontBaselineAdjustment = 0.19

// Valores RGB nomeados ([3]float64).
var (
	blackRGB      = [3]float64{0, 0, 0}
	grayHeaderRGB = [3]float64{0.9490196078, 0.9490196078, 0.9490196078}
	redRGB        = [3]float64{1, 0, 0}
)
