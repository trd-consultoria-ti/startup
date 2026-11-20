package presentation

import (
	"fmt"
	"math"
	"regexp"
	"strings"
)

// ---- Tabelas de mapeamento de apresentação ----

var (
	mapaNaoNIF = map[int]string{
		1: "Não informado na nota de origem",
		2: "Dispensado do NIF",
		3: "Não exigência do NIF",
	}

	mapaTributacaoISSQN = map[int]string{
		1: "Operação Tributável",
		2: "Imunidade",
		3: "Exportação de Serviço",
		4: "Não Incidência",
	}

	mapaOpcaoSimplesNacional = map[int]string{
		1: "1 - Não Optante",
		2: "2 - Optante - MEI",
		3: "3 - Optante - ME/EPP",
	}

	mapaSuspensaoExigibilidade = map[int]string{
		1: "Exigibilidade Suspensa por Decisão Judicial",
		2: "Exigibilidade Suspensa por Processo Administrativo",
	}

	mapaTipoBeneficioMunicipal = map[int]string{
		1: "Isenção",
		2: "Redução da BC em 'ppBM' %'",
		3: "Redução da BC em R$ 'vInfoBM'",
		4: "Alíquota Diferenciada de 'aliqDifBM' %",
	}

	mapaTipoRetencaoISSQN = map[int]string{
		1: "Não Retido",
		2: "Retido pelo Tomador",
		3: "Retido pelo Intermediário",
	}

	mapaTipoRetencaoPISCOFINS = map[int]string{
		0: "PIS/COFINS/CSLL Não Retidos",
		1: "PIS/COFINS Retidos",
		2: "PIS/COFINS Não Retidos",
		3: "PIS/COFINS/CSLL Retidos",
		4: "PIS/COFINS Retidos, CSLL Não Retido",
		5: "PIS Retido, COFINS/CSLL Não Retido",
		6: "COFINS Retido, PIS/CSLL Não Retido",
		7: "PIS Não Retido, COFINS/CSLL Retidos",
		8: "PIS/COFINS Não Retidos, CSLL Retido",
		9: "COFINS Não Retido, PIS/CSLL Retidos",
	}

	mapaTipoImunidade = map[int]string{
		0: "",
		1: "Patrimônio, renda ou serviços, uns dos outros (CF88, Art 150, VI, a)",
		2: "Templos de qualquer culto (CF88, Art 150, VI, b)",
		3: "Patrimônio, renda ou serviços dos partidos políticos, inclusive suas fundações, das entidades sindicais dos trabalhadores, das instituições de educação e de assistência social, sem fins lucrativos, atendidos os requisitos da lei (CF88, Art 150, VI, c)",
		4: "Livros, jornais, periódicos e o papel destinado a sua impressão (CF88, Art 150, VI, d)",
		5: "Fonogramas e videofonogramas musicais produzidos no Brasil contendo obras musicais ou literomusicais de autores brasileiros e/ou obras em geral interpretadas por artistas brasileiros bem como os suportes materiais ou arquivos digitais que os contenham, salvo na etapa de replicação industrial de mídias ópticas de leitura a laser.   (CF88, Art 150, VI, e)",
	}

	mapaRegimeEspecialTributacao = map[int]string{
		0: "Nenhum",
		1: "Ato Cooperado (Cooperativa)",
		2: "Estimativa",
		3: "Microempresa Municipal",
		4: "Notário ou Registrador",
		5: "Profissional Autônomo",
		6: "Sociedade de Profissionais",
		9: "Outros",
	}

	mapaRegimeApuracaoSN = map[int]string{
		1: "1 - Regime de apuração dos tributos federais e municipal pelo SN",
		2: "2 - Regime de apuração dos tributos federais pelo SN e ISSQN por fora do SN",
	}

	mapaTipoEmitente = map[int]string{
		1: "Prestador",
		2: "Tomador",
		3: "Intermediário",
	}

	mapaCStat = map[string]string{
		"100": "NFS-e Regular",
		"101": "NFS-e Cancelada",
		"102": "NFS-e Substituída",
		"103": "NFS-e de Decisão Judicial ou Administrativa",
		"104": "NFS-e Emitida em Contingência",
		"105": "NFS-e de Ajuste",
	}

	mapaFinalidadeNFSe = map[int]string{
		0: "NFS-e regular",
		1: "NFS-e de substituição",
		2: "NFS-e de ajuste",
	}

	mapaAmbienteGerador = map[string]string{
		"1": "Ambiente de Produção",
		"2": "Ambiente de Homologação",
	}

	mapaTipoAmbiente = map[int]string{
		1: "Produção",
		2: "Homologação",
	}
)

// ---- Funções de formatação pura ----

// FormatarDocumento formats a CNPJ (14 digits) or CPF (11 digits) string with
// standard Brazilian masking. Returns the original string when length doesn't
// match either expected size.
func FormatarDocumento(s string) string {
	s = strings.TrimSpace(s)
	if len(s) == 11 {
		return fmt.Sprintf("%s.%s.%s-%s", s[0:3], s[3:6], s[6:9], s[9:11])
	}
	if len(s) == 14 {
		return fmt.Sprintf("%s.%s.%s/%s-%s", s[0:2], s[2:5], s[5:8], s[8:12], s[12:14])
	}
	return s
}

// FormatarCEP formats an 8-digit CEP as "XX.XXX-XXX".
func FormatarCEP(s string) string {
	if len(s) == 8 {
		return fmt.Sprintf("%s.%s-%s", s[0:2], s[2:5], s[5:8])
	}
	return s
}

// FormatarTelefone formats a phone number string. 10-digit → (XX) XXXX-XXXX,
// 11-digit → (XX) XXXXX-XXXX. Returns the original string otherwise.
func FormatarTelefone(s string) string {
	reClean := regexp.MustCompile(`[^\d]`)
	s = reClean.ReplaceAllString(s, "")
	if len(s) == 10 {
		re := regexp.MustCompile(`^(\d{2})(\d{4})(\d{4})$`)
		return re.ReplaceAllString(s, "($1) $2-$3")
	}
	if len(s) == 11 {
		re := regexp.MustCompile(`^(\d{2})(\d{5})(\d{4})$`)
		return re.ReplaceAllString(s, "($1) $2-$3")
	}
	return s
}

// FormatarMoeda formats a float as Brazilian currency "R$ X.XXX,XX".
func FormatarMoeda(v float64) string {
	abs := math.Abs(v)
	str := fmt.Sprintf("%.2f", abs)
	parts := strings.Split(str, ".")
	intPart := parts[0]
	decPart := parts[1]
	var intFormatted []byte
	count := 0
	for i := len(intPart) - 1; i >= 0; i-- {
		if count > 0 && count%3 == 0 {
			intFormatted = append([]byte{'.'}, intFormatted...)
		}
		intFormatted = append([]byte{intPart[i]}, intFormatted...)
		count++
	}
	prefix := "R$ "
	if v < 0 {
		prefix = "R$ -"
	}
	return prefix + string(intFormatted) + "," + decPart
}

// FormatarPercentual formats a float as Brazilian percentage "XX,XX%".
func FormatarPercentual(v float64) string {
	str := fmt.Sprintf("%.2f", v)
	return strings.Replace(str, ".", ",", 1) + "%"
}

// FormatarNumeroPercentual formats a float as Brazilian percentage number "XX,XX"
// (without the % sign).
func FormatarNumeroPercentual(v float64) string {
	return strings.Replace(fmt.Sprintf("%.2f", v), ".", ",", 1)
}

// FormatarNumeroPercentualOuTraco formats a float as Brazilian percentage number
// "XX,XX" (without the % sign). Returns "-" when the value is zero.
func FormatarNumeroPercentualOuTraco(v float64) string {
	if v == 0 {
		return "-"
	}
	return FormatarNumeroPercentual(v)
}

// FormatarPercentualOuTraco formats a float as Brazilian percentage "XX,XX%"
// with the % sign. Returns "-" when the value is zero.
func FormatarPercentualOuTraco(v float64) string {
	if v == 0 {
		return "-"
	}
	return FormatarPercentual(v)
}

// FormatarValorMonetario formats a float as Brazilian currency "R$ X.XXX,XX", always
// including zero (R$ 0,00). This mirrors the original renderer's `money(v)`
// helper based on FormatarMoeda.
func FormatarValorMonetario(v float64) string {
	return "R$ " + formatarMoedaAgrupada(v)
}

// FormatarData formats a date string from ISO "YYYY-MM-DD" to Brazilian "DD/MM/YYYY".
// Empty input returns "-". Invalid shapes are returned as-is.
func FormatarData(s string) string {
	s = strings.TrimSpace(s)
	if s == "" {
		return "-"
	}
	if len(s) == 10 {
		year := s[0:4]
		month := s[5:7]
		day := s[8:10]
		if isDigits(year) && isDigits(month) && isDigits(day) && s[4] == '-' && s[7] == '-' {
			return day + "/" + month + "/" + year
		}
	}
	return s
}

// FormatarDataHora formats a datetime string from ISO "YYYY-MM-DDTHH:MM:SS" to
// Brazilian "DD/MM/YYYY HH:MM:SS". Empty input returns "-". Invalid shapes are
// returned as-is.
func FormatarDataHora(s string) string {
	s = strings.TrimSpace(s)
	if s == "" {
		return "-"
	}
	if len(s) == 10 {
		return FormatarData(s)
	}
	if len(s) < 19 || s[10] != 'T' {
		return s
	}
	date := FormatarData(s[0:10])
	if date == s[0:10] {
		return s
	}
	if isDigits(s[11:13]) && s[13] == ':' && isDigits(s[14:16]) && s[16] == ':' && isDigits(s[17:19]) {
		return date + " " + s[11:19]
	}
	return s
}

func isDigits(s string) bool {
	if s == "" {
		return false
	}
	for _, r := range s {
		if r < '0' || r > '9' {
			return false
		}
	}
	return true
}

// FormatarCodigoTributacaoNacional formats a 6-digit national tax code as "XX.XX.XX".
func FormatarCodigoTributacaoNacional(s string) string {
	s = fmt.Sprintf("%v", s)
	s = strings.ReplaceAll(s, ".", "")
	s = strings.TrimSpace(s)
	if len(s) != 6 {
		return s
	}
	return s[0:2] + "." + s[2:4] + "." + s[4:6]
}

// FormatarNBS masks a 9-digit NBS code as "X.XXXX.XX.XXX".
func FormatarNBS(content string) string {
	var numeros []rune
	for _, r := range content {
		if r >= '0' && r <= '9' {
			numeros = append(numeros, r)
		}
	}
	s := string(numeros)
	if len(s) != 9 {
		return content
	}
	return s[:1] + "." + s[1:5] + "." + s[5:7] + "." + s[7:]
}

// FormatarMunicipioUF returns the municipality name and UF combined.
func FormatarMunicipioUF(municipio, uf string) string {
	municipio = strings.TrimSpace(municipio)
	uf = strings.TrimSpace(uf)
	if municipio == "" && uf == "" {
		return "-"
	}
	if municipio != "" && uf != "" {
		return municipio + " / " + uf
	}
	if municipio != "" {
		return municipio
	}
	return uf
}

// MapearCStat returns the human-readable description for a cStat code.
func MapearCStat(s string) string {
	s = strings.TrimSpace(s)
	if s == "" {
		return "-"
	}
	if v, ok := mapaCStat[s]; ok {
		return v
	}
	return s
}

// MapearTipoAmbiente returns the human-readable description for tpAmb.
func MapearTipoAmbiente(i int) string {
	if v, ok := mapaTipoAmbiente[i]; ok {
		return v
	}
	return "-"
}

// MapearAmbienteGerador returns the human-readable description for ambGer.
func MapearAmbienteGerador(s string) string {
	if v, ok := mapaAmbienteGerador[s]; ok {
		return v
	}
	return "-"
}

// MapearTipoEmitente returns the human-readable description for tpEmit.
func MapearTipoEmitente(i int) string {
	if v, ok := mapaTipoEmitente[i]; ok {
		return v
	}
	return "-"
}

// MapearFinalidadeNFSe returns the human-readable description for finalidade (FinNFSe).
func MapearFinalidadeNFSe(i int) string {
	if v, ok := mapaFinalidadeNFSe[i]; ok {
		return v
	}
	return "-"
}

// MapearTributacaoISSQN returns the human-readable description for tribISSQN.
func MapearTributacaoISSQN(i int) string {
	if v, ok := mapaTributacaoISSQN[i]; ok {
		return v
	}
	return "-"
}

// MapearOpcaoSimplesNacional returns the human-readable description for opSimpNac.
func MapearOpcaoSimplesNacional(i int) string {
	if v, ok := mapaOpcaoSimplesNacional[i]; ok {
		return v
	}
	return "-"
}

// MapearRegimeEspecialTributacao returns the human-readable description for regEspTrib.
func MapearRegimeEspecialTributacao(i int) string {
	if v, ok := mapaRegimeEspecialTributacao[i]; ok {
		return v
	}
	return "-"
}

// MapearRegimeApuracaoSN returns the human-readable description for regApTribSN.
func MapearRegimeApuracaoSN(i int) string {
	if v, ok := mapaRegimeApuracaoSN[i]; ok {
		return v
	}
	return "-"
}

// MapearSuspensaoExigibilidade returns the human-readable description for tpSusp.
func MapearSuspensaoExigibilidade(i int) string {
	if v, ok := mapaSuspensaoExigibilidade[i]; ok {
		return v
	}
	return "-"
}

// MapearTipoBeneficioMunicipal returns the human-readable description for tpBM.
func MapearTipoBeneficioMunicipal(i int) string {
	if v, ok := mapaTipoBeneficioMunicipal[i]; ok {
		return v
	}
	return "-"
}

// MapearTipoRetencaoISSQN returns the human-readable description for tpRetISSQN.
func MapearTipoRetencaoISSQN(i int) string {
	if v, ok := mapaTipoRetencaoISSQN[i]; ok {
		return v
	}
	return "-"
}

// MapearTipoRetencaoPISCOFINS returns the human-readable description for tpRetPisCofins.
func MapearTipoRetencaoPISCOFINS(i int) string {
	if v, ok := mapaTipoRetencaoPISCOFINS[i]; ok {
		return v
	}
	return "-"
}

// MapearTipoImunidade returns the human-readable description for tpImunidade.
func MapearTipoImunidade(i int) string {
	if v, ok := mapaTipoImunidade[i]; ok {
		if v == "" {
			return "-"
		}
		return v
	}
	return "-"
}

// MapearCodigoNaoNIF returns the human-readable description for cNaoNIF.
func MapearCodigoNaoNIF(i int) string {
	if v, ok := mapaNaoNIF[i]; ok {
		return v
	}
	return "-"
}

// FormatarCodigoIBGECEP combines IBGE code and CEP with formatting.
func FormatarCodigoIBGECEP(ibge, cep string) string {
	ibge = strings.TrimSpace(ibge)
	cep = strings.TrimSpace(cep)
	if ibge == "" && cep == "" {
		return "-"
	}
	if cep != "" && len(cep) == 8 {
		cep = fmt.Sprintf("%s.%s-%s", cep[0:2], cep[2:5], cep[5:8])
	}
	if ibge != "" && cep != "" {
		return ibge + " / " + cep
	}
	if ibge != "" {
		return ibge
	}
	return cep
}

// TracoSeVazio returns "-" when the string is empty or whitespace, otherwise the string.
func TracoSeVazio(s string) string {
	s = strings.TrimSpace(s)
	if s == "" {
		return "-"
	}
	return s
}

// FormatarValorMonetarioOuTraco returns "-" for nil pointers or zero, otherwise "R$ X.XXX,XX".
func FormatarValorMonetarioOuTraco(value interface{}) string {
	var v *float64
	switch typed := value.(type) {
	case *float64:
		v = typed
	case float64:
		v = &typed
	case float32:
		converted := float64(typed)
		v = &converted
	case int:
		converted := float64(typed)
		v = &converted
	default:
		return "-"
	}
	if v == nil || *v == 0 {
		return "-"
	}
	return "R$ " + formatarMoedaAgrupada(*v)
}

// formatarMoedaAgrupada formats the absolute value with thousand separator "."
// and decimal separator "," (without "R$" prefix).
func formatarMoedaAgrupada(v float64) string {
	absVal := math.Abs(v)
	str := fmt.Sprintf("%.2f", absVal)
	parts := strings.Split(str, ".")
	intPart := parts[0]
	decPart := parts[1]
	var intPartFormatted []byte
	count := 0
	for i := len(intPart) - 1; i >= 0; i-- {
		if count > 0 && count%3 == 0 {
			intPartFormatted = append([]byte{'.'}, intPartFormatted...)
		}
		intPartFormatted = append([]byte{intPart[i]}, intPartFormatted...)
		count++
	}
	return string(intPartFormatted) + "," + decPart
}

// PrimeiroNaoVazio returns the first non-empty (after trimming) string, or "-" if
// all are empty.
func PrimeiroNaoVazio(values ...string) string {
	for _, v := range values {
		if strings.TrimSpace(v) != "" {
			return v
		}
	}
	return "-"
}

// MontarEndereco joins non-empty address parts with ", ".
func MontarEndereco(parts ...string) string {
	values := make([]string, 0, len(parts))
	for _, part := range parts {
		if strings.TrimSpace(part) != "" {
			values = append(values, strings.TrimSpace(part))
		}
	}
	if len(values) == 0 {
		return "-"
	}
	return strings.Join(values, ", ")
}
