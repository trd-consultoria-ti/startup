package presentation

import (
	"testing"
)

func TestFormatarDocumento_CPF(t *testing.T) {
	got := FormatarDocumento("12345678901")
	want := "123.456.789-01"
	if got != want {
		t.Fatalf("FormatarDocumento(CPF): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDocumentoCNPJ(t *testing.T) {
	got := FormatarDocumento("12345678000195")
	want := "12.345.678/0001-95"
	if got != want {
		t.Fatalf("FormatarDocumento(CNPJ): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDocumentoInvalidLength(t *testing.T) {
	got := FormatarDocumento("12345")
	want := "12345"
	if got != want {
		t.Fatalf("FormatarDocumento(invalid): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDocumentoWhitespace(t *testing.T) {
	got := FormatarDocumento("  12345678901  ")
	want := "123.456.789-01"
	if got != want {
		t.Fatalf("FormatarDocumento(whitespace): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarCEP(t *testing.T) {
	got := FormatarCEP("01001000")
	want := "01.001-000"
	if got != want {
		t.Fatalf("FormatarCEP: esperado %q, obtido %q", want, got)
	}
}

func TestFormatarCEPInvalidLength(t *testing.T) {
	got := FormatarCEP("12345")
	want := "12345"
	if got != want {
		t.Fatalf("FormatarCEP(invalid): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarTelefone10Digits(t *testing.T) {
	got := FormatarTelefone("(11) 1234-5678")
	want := "(11) 1234-5678"
	if got != want {
		t.Fatalf("FormatarTelefone(10): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarTelefone11Digits(t *testing.T) {
	got := FormatarTelefone("11912345678")
	want := "(11) 91234-5678"
	if got != want {
		t.Fatalf("FormatarTelefone(11): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarTelefoneInvalidLength(t *testing.T) {
	got := FormatarTelefone("12345")
	want := "12345"
	if got != want {
		t.Fatalf("FormatarTelefone(invalid): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarMoeda_Positive(t *testing.T) {
	got := FormatarMoeda(1234.56)
	want := "R$ 1.234,56"
	if got != want {
		t.Fatalf("FormatarMoeda(positive): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarMoedaNegative(t *testing.T) {
	got := FormatarMoeda(-1234.56)
	want := "R$ -1.234,56"
	if got != want {
		t.Fatalf("FormatarMoeda(negative): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarMoedaZero(t *testing.T) {
	got := FormatarMoeda(0)
	want := "R$ 0,00"
	if got != want {
		t.Fatalf("FormatarMoeda(zero): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarPercentual(t *testing.T) {
	got := FormatarPercentual(12.345)
	want := "12,35%"
	if got != want {
		t.Fatalf("FormatarPercentual: esperado %q, obtido %q", want, got)
	}
}

func TestFormatarNumeroPercentual(t *testing.T) {
	got := FormatarNumeroPercentual(12.345)
	want := "12,35"
	if got != want {
		t.Fatalf("FormatarNumeroPercentual: esperado %q, obtido %q", want, got)
	}
}

func TestFormatarCodigoTributacaoNacional(t *testing.T) {
	got := FormatarCodigoTributacaoNacional("010101")
	want := "01.01.01"
	if got != want {
		t.Fatalf("FormatarCodigoTributacaoNacional: esperado %q, obtido %q", want, got)
	}
}

func TestFormatarCodigoTributacaoNacionalInvalidLength(t *testing.T) {
	got := FormatarCodigoTributacaoNacional("12345")
	want := "12345"
	if got != want {
		t.Fatalf("FormatarCodigoTributacaoNacional(invalid): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarCodigoTributacaoNacionalWithDots(t *testing.T) {
	got := FormatarCodigoTributacaoNacional("01.01.01")
	want := "01.01.01"
	if got != want {
		t.Fatalf("FormatarCodigoTributacaoNacional(dots): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarNBS(t *testing.T) {
	got := FormatarNBS("110010100")
	want := "1.1001.01.00"
	if got != want {
		t.Fatalf("FormatarNBS: esperado %q, obtido %q", want, got)
	}
}

func TestFormatarNBSInvalidLength(t *testing.T) {
	got := FormatarNBS("12345")
	want := "12345"
	if got != want {
		t.Fatalf("FormatarNBS(invalid): esperado %q, obtido %q", want, got)
	}
}

func TestMapearCStat(t *testing.T) {
	tests := []struct {
		code string
		want string
	}{
		{"100", "NFS-e Regular"},
		{"101", "NFS-e Cancelada"},
		{"102", "NFS-e Substituída"},
		{"103", "NFS-e de Decisão Judicial ou Administrativa"},
		{"104", "NFS-e Emitida em Contingência"},
		{"105", "NFS-e de Ajuste"},
		{"", "-"},
		{"999", "999"},
	}
	for _, tc := range tests {
		got := MapearCStat(tc.code)
		if got != tc.want {
			t.Fatalf("MapearCStat(%q): esperado %q, obtido %q", tc.code, tc.want, got)
		}
	}
}

func TestMapearTipoAmbiente(t *testing.T) {
	if got := MapearTipoAmbiente(1); got != "Produção" {
		t.Fatalf("MapearTipoAmbiente(1): esperado %q, obtido %q", "Produção", got)
	}
	if got := MapearTipoAmbiente(2); got != "Homologação" {
		t.Fatalf("MapearTipoAmbiente(2): esperado %q, obtido %q", "Homologação", got)
	}
	if got := MapearTipoAmbiente(9); got != "-" {
		t.Fatalf("MapearTipoAmbiente(9): esperado %q, obtido %q", "-", got)
	}
}

func TestMapearAmbienteGerador(t *testing.T) {
	if got := MapearAmbienteGerador("1"); got != "Ambiente de Produção" {
		t.Fatalf("MapearAmbienteGerador(1): esperado %q, obtido %q", "Ambiente de Produção", got)
	}
	if got := MapearAmbienteGerador("2"); got != "Ambiente de Homologação" {
		t.Fatalf("MapearAmbienteGerador(2): esperado %q, obtido %q", "Ambiente de Homologação", got)
	}
	if got := MapearAmbienteGerador("9"); got != "-" {
		t.Fatalf("MapearAmbienteGerador(9): esperado %q, obtido %q", "-", got)
	}
}

func TestMapearTipoEmitente(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{1, "Prestador"},
		{2, "Tomador"},
		{3, "Intermediário"},
		{9, "-"},
	}
	for _, tc := range tests {
		got := MapearTipoEmitente(tc.val)
		if got != tc.want {
			t.Fatalf("MapearTipoEmitente(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearFinalidadeNFSe(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{0, "NFS-e regular"},
		{1, "NFS-e de substituição"},
		{2, "NFS-e de ajuste"},
		{9, "-"},
	}
	for _, tc := range tests {
		got := MapearFinalidadeNFSe(tc.val)
		if got != tc.want {
			t.Fatalf("MapearFinalidadeNFSe(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearTributacaoISSQN(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{1, "Operação Tributável"},
		{2, "Imunidade"},
		{3, "Exportação de Serviço"},
		{4, "Não Incidência"},
		{0, "-"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearTributacaoISSQN(tc.val)
		if got != tc.want {
			t.Fatalf("MapearTributacaoISSQN(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearOpcaoSimplesNacional(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{1, "1 - Não Optante"},
		{2, "2 - Optante - MEI"},
		{3, "3 - Optante - ME/EPP"},
		{0, "-"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearOpcaoSimplesNacional(tc.val)
		if got != tc.want {
			t.Fatalf("MapearOpcaoSimplesNacional(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearRegimeEspecialTributacao(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{0, "Nenhum"},
		{1, "Ato Cooperado (Cooperativa)"},
		{2, "Estimativa"},
		{3, "Microempresa Municipal"},
		{4, "Notário ou Registrador"},
		{5, "Profissional Autônomo"},
		{6, "Sociedade de Profissionais"},
		{9, "Outros"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearRegimeEspecialTributacao(tc.val)
		if got != tc.want {
			t.Fatalf("MapearRegimeEspecialTributacao(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearRegimeApuracaoSN(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{1, "1 - Regime de apuração dos tributos federais e municipal pelo SN"},
		{2, "2 - Regime de apuração dos tributos federais pelo SN e ISSQN por fora do SN"},
		{0, "-"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearRegimeApuracaoSN(tc.val)
		if got != tc.want {
			t.Fatalf("MapearRegimeApuracaoSN(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearSuspensaoExigibilidade(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{1, "Exigibilidade Suspensa por Decisão Judicial"},
		{2, "Exigibilidade Suspensa por Processo Administrativo"},
		{0, "-"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearSuspensaoExigibilidade(tc.val)
		if got != tc.want {
			t.Fatalf("MapearSuspensaoExigibilidade(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearTipoBeneficioMunicipal(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{1, "Isenção"},
		{2, "Redução da BC em 'ppBM' %'"},
		{3, "Redução da BC em R$ 'vInfoBM'"},
		{4, "Alíquota Diferenciada de 'aliqDifBM' %"},
		{0, "-"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearTipoBeneficioMunicipal(tc.val)
		if got != tc.want {
			t.Fatalf("MapearTipoBeneficioMunicipal(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearTipoRetencaoISSQN(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{1, "Não Retido"},
		{2, "Retido pelo Tomador"},
		{3, "Retido pelo Intermediário"},
		{0, "-"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearTipoRetencaoISSQN(tc.val)
		if got != tc.want {
			t.Fatalf("MapearTipoRetencaoISSQN(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearTipoRetencaoPISCOFINS(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{0, "PIS/COFINS/CSLL Não Retidos"},
		{1, "PIS/COFINS Retidos"},
		{2, "PIS/COFINS Não Retidos"},
		{3, "PIS/COFINS/CSLL Retidos"},
		{4, "PIS/COFINS Retidos, CSLL Não Retido"},
		{5, "PIS Retido, COFINS/CSLL Não Retido"},
		{6, "COFINS Retido, PIS/CSLL Não Retido"},
		{7, "PIS Não Retido, COFINS/CSLL Retidos"},
		{8, "PIS/COFINS Não Retidos, CSLL Retido"},
		{9, "COFINS Não Retido, PIS/CSLL Retidos"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearTipoRetencaoPISCOFINS(tc.val)
		if got != tc.want {
			t.Fatalf("MapearTipoRetencaoPISCOFINS(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestMapearTipoImunidade(t *testing.T) {
	if got := MapearTipoImunidade(0); got != "-" {
		t.Fatalf("MapearTipoImunidade(0): esperado %q, obtido %q", "-", got)
	}
	if got := MapearTipoImunidade(1); got != "Patrimônio, renda ou serviços, uns dos outros (CF88, Art 150, VI, a)" {
		t.Fatalf("MapearTipoImunidade(1): esperado %q, obtido %q", "Patrimônio...", got)
	}
	if got := MapearTipoImunidade(99); got != "-" {
		t.Fatalf("MapearTipoImunidade(99): esperado %q, obtido %q", "-", got)
	}
}

func TestMapearCodigoNaoNIF(t *testing.T) {
	tests := []struct {
		val  int
		want string
	}{
		{1, "Não informado na nota de origem"},
		{2, "Dispensado do NIF"},
		{3, "Não exigência do NIF"},
		{0, "-"},
		{99, "-"},
	}
	for _, tc := range tests {
		got := MapearCodigoNaoNIF(tc.val)
		if got != tc.want {
			t.Fatalf("MapearCodigoNaoNIF(%d): esperado %q, obtido %q", tc.val, tc.want, got)
		}
	}
}

func TestFormatarCodigoIBGECEP(t *testing.T) {
	tests := []struct {
		ibge string
		cep  string
		want string
	}{
		{"3550308", "01001000", "3550308 / 01.001-000"},
		{"3550308", "", "3550308"},
		{"", "01001000", "01.001-000"},
		{"", "", "-"},
		{"  ", "  ", "-"},
	}
	for _, tc := range tests {
		got := FormatarCodigoIBGECEP(tc.ibge, tc.cep)
		if got != tc.want {
			t.Fatalf("FormatarCodigoIBGECEP(%q, %q): esperado %q, obtido %q", tc.ibge, tc.cep, tc.want, got)
		}
	}
}

func TestTracoSeVazio(t *testing.T) {
	if got := TracoSeVazio(""); got != "-" {
		t.Fatalf("TracoSeVazio(empty): esperado %q, obtido %q", "-", got)
	}
	if got := TracoSeVazio("  "); got != "-" {
		t.Fatalf("TracoSeVazio(whitespace): esperado %q, obtido %q", "-", got)
	}
	if got := TracoSeVazio("São Paulo"); got != "São Paulo" {
		t.Fatalf("TracoSeVazio(value): esperado %q, obtido %q", "São Paulo", got)
	}
}

func TestFormatarValorMonetarioOuTraco(t *testing.T) {
	var nilPtr *float64
	if got := FormatarValorMonetarioOuTraco(nilPtr); got != "-" {
		t.Fatalf("FormatarValorMonetarioOuTraco(nil): esperado %q, obtido %q", "-", got)
	}
	zero := 0.0
	if got := FormatarValorMonetarioOuTraco(&zero); got != "-" {
		t.Fatalf("FormatarValorMonetarioOuTraco(0): esperado %q, obtido %q", "-", got)
	}
	val := 123.45
	if got := FormatarValorMonetarioOuTraco(&val); got != "R$ 123,45" {
		t.Fatalf("FormatarValorMonetarioOuTraco(123.45): esperado %q, obtido %q", "R$ 123,45", got)
	}
}

func TestFormatarValorMonetarioZero(t *testing.T) {
	got := FormatarValorMonetario(0)
	want := "R$ 0,00"
	if got != want {
		t.Fatalf("FormatarValorMonetario(0): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarValorMonetarioNonZero(t *testing.T) {
	got := FormatarValorMonetario(1234.56)
	want := "R$ 1.234,56"
	if got != want {
		t.Fatalf("FormatarValorMonetario(1234.56): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarValorMonetarioNegative(t *testing.T) {
	got := FormatarValorMonetario(-100.00)
	want := "R$ 100,00"
	if got != want {
		t.Fatalf("FormatarValorMonetario(-100): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarNumeroPercentualOuTracoZero(t *testing.T) {
	got := FormatarNumeroPercentualOuTraco(0)
	want := "-"
	if got != want {
		t.Fatalf("FormatarNumeroPercentualOuTraco(0): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarNumeroPercentualOuTracoNonZero(t *testing.T) {
	got := FormatarNumeroPercentualOuTraco(12.345)
	want := "12,35"
	if got != want {
		t.Fatalf("FormatarNumeroPercentualOuTraco(12.345): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarPercentualOuTracoZero(t *testing.T) {
	got := FormatarPercentualOuTraco(0)
	want := "-"
	if got != want {
		t.Fatalf("FormatarPercentualOuTraco(0): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarPercentualOuTracoSmall(t *testing.T) {
	got := FormatarPercentualOuTraco(0.10)
	want := "0,10%"
	if got != want {
		t.Fatalf("FormatarPercentualOuTraco(0.10): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarPercentualOuTracoOne(t *testing.T) {
	got := FormatarPercentualOuTraco(1.00)
	want := "1,00%"
	if got != want {
		t.Fatalf("FormatarPercentualOuTraco(1.00): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarPercentualOuTracoTwoFifty(t *testing.T) {
	got := FormatarPercentualOuTraco(2.50)
	want := "2,50%"
	if got != want {
		t.Fatalf("FormatarPercentualOuTraco(2.50): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarPercentualOuTracoRounding(t *testing.T) {
	got := FormatarPercentualOuTraco(12.345)
	want := "12,35%"
	if got != want {
		t.Fatalf("FormatarPercentualOuTraco(12.345): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDataValid(t *testing.T) {
	got := FormatarData("2026-07-14")
	want := "14/07/2026"
	if got != want {
		t.Fatalf("FormatarData(valid): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDataEmpty(t *testing.T) {
	got := FormatarData("")
	want := "-"
	if got != want {
		t.Fatalf("FormatarData(empty): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDataInvalid(t *testing.T) {
	got := FormatarData("14-07-2026")
	want := "14-07-2026"
	if got != want {
		t.Fatalf("FormatarData(invalid): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDataWhitespace(t *testing.T) {
	got := FormatarData("  2026-07-14  ")
	want := "14/07/2026"
	if got != want {
		t.Fatalf("FormatarData(whitespace): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDataHoraValid(t *testing.T) {
	got := FormatarDataHora("2026-07-14T10:00:00-03:00")
	want := "14/07/2026 10:00:00"
	if got != want {
		t.Fatalf("FormatarDataHora(valid): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDataHoraEmpty(t *testing.T) {
	got := FormatarDataHora("")
	want := "-"
	if got != want {
		t.Fatalf("FormatarDataHora(empty): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDataHoraInvalid(t *testing.T) {
	got := FormatarDataHora("14-07-2026 10:00:00")
	want := "14-07-2026 10:00:00"
	if got != want {
		t.Fatalf("FormatarDataHora(invalid): esperado %q, obtido %q", want, got)
	}
}

func TestFormatarDataHoraShort(t *testing.T) {
	got := FormatarDataHora("2026-07-14")
	want := "14/07/2026"
	if got != want {
		t.Fatalf("FormatarDataHora(short): esperado %q, obtido %q", want, got)
	}
}

func TestMunicipioUF(t *testing.T) {
	tests := []struct {
		mun  string
		uf   string
		want string
	}{
		{"São Paulo", "SP", "São Paulo / SP"},
		{"São Paulo", "", "São Paulo"},
		{"", "SP", "SP"},
		{"", "", "-"},
	}
	for _, tc := range tests {
		got := FormatarMunicipioUF(tc.mun, tc.uf)
		if got != tc.want {
			t.Fatalf("FormatarMunicipioUF(%q, %q): esperado %q, obtido %q", tc.mun, tc.uf, tc.want, got)
		}
	}
}
