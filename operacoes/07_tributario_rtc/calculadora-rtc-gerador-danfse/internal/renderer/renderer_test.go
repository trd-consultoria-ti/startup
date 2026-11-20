package renderer

import (
	"context"
	"math"
	"os"
	"os/exec"
	"strings"
	"testing"

	"danfse-service/internal/domain/models"
	"danfse-service/internal/domain/presentation"
	"danfse-service/internal/service"
)

func TestRenderer_ProducesA4PDF(t *testing.T) {
	renderer, err := NewRenderer()
	if err != nil {
		t.Fatal(err)
	}

	nota := models.NFSe{}
	nota.MarcaDagua = "CANCELADA"
	nota.InfNFSe.Id = "NFS123456"
	nota.InfNFSe.NNFSe = "1"
	nota.InfNFSe.AmbGer = "1"
	nota.InfNFSe.Emit.EnderNac.XMun = "São Paulo"
	nota.InfNFSe.Emit.EnderNac.UF = "SP"
	nota.InfNFSe.DPS.InfDPS.TpAmb = 1
	nota.InfNFSe.DPS.InfDPS.TpEmit = 1
	nota.InfNFSe.DPS.InfDPS.Serv.CServ.XDescServ = "Serviço de teste"
	nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.UF = "SP"
	nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.Pais = "Brasil"
	nota.InfNFSe.DPS.InfDPS.Valores.VServPrest.VServ = 100
	nota.InfNFSe.Valores.VLiq = 100
	nota.ExibicaoDescServ = "Serviço de teste"
	nota.ExibicaoCompl = "Informações complementares"
	nota.ExibicaoLinhaTributos = "Totais aproximados dos Tributos: -"

	presenter := service.NovoServicoApresentacaoDanfse()
	doc := presenter.Apresentar(nota)

	pdf, err := renderer.Renderizar(context.Background(), doc)
	if err != nil {
		t.Fatal(err)
	}
	if !strings.HasPrefix(string(pdf), "%PDF-") {
		t.Fatalf("PDF inválido: %q", pdf[:minPDF(len(pdf), 8)])
	}

	if _, err := exec.LookPath("pdfinfo"); err != nil {
		t.Skip("pdfinfo não instalado")
	}
	tmp, err := os.CreateTemp(t.TempDir(), "direct-*.pdf")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := tmp.Write(pdf); err != nil {
		t.Fatal(err)
	}
	if err := tmp.Close(); err != nil {
		t.Fatal(err)
	}
	out, err := exec.Command("pdfinfo", tmp.Name()).Output()
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(out), "Pages:           1") {
		t.Fatalf("renderer direto não produziu uma página: %s", out)
	}
}

func TestRenderer_PageLayoutSuppressesOptionalBlocks(t *testing.T) {
	doc := presentation.DocumentoDanfse{
		Tributacoes: struct {
			Municipal presentation.DadosTributacaoMunicipal
			Federal   presentation.DadosTributacaoFederal
			IBSCBS    presentation.DadosTributacaoIBSCBS
		}{Municipal: presentation.DadosTributacaoMunicipal{Visivel: true}},
	}
	layout := montarLayoutPagina(doc)
	tomDiff := layout.alturaDestinatario - layout.alturaTomador
	if math.Abs(tomDiff-suppressedPartyBlockHeightCM) > 1e-9 {
		t.Fatalf("tomador suprimido: altura %.2f, want %.2f", tomDiff, suppressedPartyBlockHeightCM)
	}
	destDiff := layout.alturaIntermediario - layout.alturaDestinatario
	if math.Abs(destDiff-suppressedPartyBlockHeightCM) > 1e-9 {
		t.Fatalf("destinatário suprimido: altura %.2f, want %.2f", destDiff, suppressedPartyBlockHeightCM)
	}
	intDiff := layout.serviceTop - layout.alturaIntermediario
	if math.Abs(intDiff-suppressedPartyBlockHeightCM) > 1e-9 {
		t.Fatalf("intermediário suprimido: altura %.2f, want %.2f", intDiff, suppressedPartyBlockHeightCM)
	}
	// Com ISSQN visível, federalTop usa a altura completa do bloco municipal.
	munDiff := layout.federalTop - layout.municipalTop
	if math.Abs(munDiff-municipalBlockHeightCM) > 1e-9 {
		t.Fatalf("municipal sempre visível: altura %.2f, want %.2f", munDiff, municipalBlockHeightCM)
	}
}

// nil resolver = no municipality enrichment (non-fatal)

func minPDF(a, b int) int {
	if a < b {
		return a
	}
	return b
}

func TestRenderer_ComplementaryTaxLineTop(t *testing.T) {
	// Canhoto visível → linha de tributos em yComplTributosCM
	docVisible := presentation.DocumentoDanfse{Canhoto: presentation.DadosCanhoto{Visivel: true}}
	topVisible := topoLinhaTotaisTributos(docVisible)
	if math.Abs(topVisible-yComplTributosCM) > 1e-9 {
		t.Errorf("visible receipt: tax line top = %.4f, want %.4f", topVisible, yComplTributosCM)
	}

	// Canhoto oculto → linha de tributos deslocada para baixo em canhotoHeightCM
	docHidden := presentation.DocumentoDanfse{Canhoto: presentation.DadosCanhoto{Visivel: false}}
	topHidden := topoLinhaTotaisTributos(docHidden)
	expectedHidden := yComplTributosCM + canhotoHeightCM
	if math.Abs(topHidden-expectedHidden) > 1e-9 {
		t.Errorf("hidden receipt: tax line top = %.4f, want %.4f", topHidden, expectedHidden)
	}
}

// --- Testes de regressão: fallbackTraco e IBS/CBS vazio ---

func TestRenderer_FallbackTraco(t *testing.T) {
	tests := []struct {
		input    string
		expected string
	}{
		{"", "-"},
		{"   ", "-"},
		{"\t\n", "-"},
		{"-", "-"},
		{"- / -", "- / -"},
		{"SP", "SP"},
		{"  SP  ", "  SP  "},
		{"100,00", "100,00"},
	}
	for _, tc := range tests {
		got := fallbackTraco(tc.input)
		if got != tc.expected {
			t.Errorf("fallbackTraco(%q) = %q, want %q", tc.input, got, tc.expected)
		}
	}
}

func TestRenderer_IBSCBSEmptyFields(t *testing.T) {
	// Constrói um DocumentoDanfse mínimo com IBS/CBS visível e campos vazios.
	doc := presentation.DocumentoDanfse{
		Cabecalho: presentation.DadosCabecalho{
			Municipio:        "São Paulo",
			UF:               "SP",
			AmbienteGerador:  "Emissor de teste",
			TipoAmbiente:     "Homologação",
			ExibicaoDescServ: "Serviço de teste",
		},
		Acesso: presentation.DadosAcesso{
			ChaveAcesso:         "35260800000000000000000000000000001234567890",
			CodigoQR:            "https://exemplo/qr?token=abc",
			NumeroNFSe:          "1",
			Competencia:         "08/2025",
			DataHoraEmissaoNFSe: "2025-08-01T10:00:00",
			NumeroDPS:           "1",
			SerieDPS:            "1",
			DataHoraEmissaoDPS:  "2025-08-01T09:00:00",
		},
		Partes: presentation.DadosPartes{
			Emitente: presentation.SecaoParte{
				Titulo:             "EMITENTE",
				CNPJCPFNIF:         "12345678000199",
				InscricaoMunicipal: "1234567",
				Nome:               "PRESTADOR DE TESTE LTDA",
				MunicipioUF:        "São Paulo/SP",
			},
			Tomador: presentation.SecaoParte{
				Titulo:      "TOMADOR",
				CNPJCPFNIF:  "99887766000155",
				Nome:        "TOMADOR DE TESTE LTDA",
				MunicipioUF: "São Paulo/SP",
				Visivel:     true,
			},
			Destinatario: presentation.SecaoParte{
				Titulo:      "DESTINATÁRIO",
				CNPJCPFNIF:  "99887766000155",
				Nome:        "TOMADOR DE TESTE LTDA",
				MunicipioUF: "São Paulo/SP",
				Visivel:     true,
			},
		},
		Servico: presentation.DadosServico{
			CodigoNBS:        "999999",
			LocalPrestacaoUF: "BR",
			DescServ:         "Serviço de teste para validação de placeholders",
		},
		Tributacoes: struct {
			Municipal presentation.DadosTributacaoMunicipal
			Federal   presentation.DadosTributacaoFederal
			IBSCBS    presentation.DadosTributacaoIBSCBS
		}{
			Municipal: presentation.DadosTributacaoMunicipal{
				Visivel: true,
			},
			Federal: presentation.DadosTributacaoFederal{
				Visivel: true,
			},
			IBSCBS: presentation.DadosTributacaoIBSCBS{
				Visivel: true,
				// Todos os campos intencionalmente vazios para testar fallback "-"
			},
		},
		Totais: presentation.DadosTotais{
			ValorOperacaoServico: "100,00",
			ValorLiquido:         "100,00",
		},
		InformacoesComplementares: presentation.DadosInformacoesComplementares{
			Conteudo:      "",
			LinhaTributos: "Totais aproximados dos Tributos: -",
		},
		Canhoto: presentation.DadosCanhoto{
			Visivel: false,
		},
	}

	renderer, err := NewRenderer()
	if err != nil {
		t.Fatal(err)
	}

	pdf, err := renderer.Renderizar(context.Background(), doc)
	if err != nil {
		t.Fatal(err)
	}

	// Validação básica do PDF
	assertPDFBasic(t, pdf)
	pdfPath := writePDFToTempDir(t, "ibs_cbs_empty.pdf", pdf)
	assertPDFPageCount(t, pdfPath)

	// Quando pdftotext estiver disponível, verificar que o cabeçalho
	// "TRIBUTAÇÃO IBS / CBS" aparece e que há pelo menos um "-" no texto.
	if _, err := exec.LookPath("pdftotext"); err != nil {
		t.Skip("pdftotext indisponível — pulando verificação textual")
	}
	text := pdfText(t, pdfPath)

	if !strings.Contains(text, "TRIBUTAÇÃO IBS / CBS") {
		t.Error("PDF não contém o cabeçalho 'TRIBUTAÇÃO IBS / CBS'")
	}

	// Não contamos hífens globalmente; apenas verificamos que pelo menos
	// alguns rótulos/valores "-" estão presentes na seção IBS/CBS.
	// A seção IBS/CBS tem muitos campos vazios; basta confirmar que há
	// mais de um "-" no texto extraído.
	hyphenCount := strings.Count(text, "-")
	if hyphenCount < 2 {
		t.Errorf("esperado pelo menos 2 hífens (placeholders) no texto extraído, got %d", hyphenCount)
	}
}

// --- Testes unitarios para combinarConteudoComplementar ---

func TestRenderer_CombinarConteudoComplementarVazioRetornaTributos(t *testing.T) {
	linhaTributos := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 100,00; Estaduais: R$ 50,00; Municipais: R$ 25,00;"
	got := combinarConteudoComplementar("", linhaTributos)
	if got != linhaTributos {
		t.Fatalf("conteudo vazio deveria retornar apenas tributos, obteve: %q", got)
	}
}

func TestRenderer_CombinarConteudoComplementarJaContemTributosNaoDuplica(t *testing.T) {
	linhaTributos := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 100,00; Estaduais: R$ 50,00; Municipais: R$ 25,00;"
	conteudo := "Informacao complementar | Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 100,00; Estaduais: R$ 50,00; Municipais: R$ 25,00;"
	got := combinarConteudoComplementar(conteudo, linhaTributos)
	if got != conteudo {
		t.Fatalf("conteudo ja com tributos nao deveria duplicar, obteve: %q", got)
	}
}

func TestRenderer_CombinarConteudoComplementarSemTributosAnexaSeparador(t *testing.T) {
	linhaTributos := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 100,00; Estaduais: R$ 50,00; Municipais: R$ 25,00;"
	conteudo := "Informacao complementar"
	got := combinarConteudoComplementar(conteudo, linhaTributos)
	expected := conteudo + " | " + linhaTributos
	if got != expected {
		t.Fatalf("conteudo sem tributos deveria anexar ' | ' + tributos, obteve: %q, esperado: %q", got, expected)
	}
}

func TestRenderer_PisCofinsHidden_ExibirPisCofinsFalse(t *testing.T) {
	doc := presentation.DocumentoDanfse{
		Cabecalho: presentation.DadosCabecalho{
			Municipio:        "São Paulo",
			UF:               "SP",
			AmbienteGerador:  "Emissor de teste",
			TipoAmbiente:     "Homologação",
			ExibicaoDescServ: "Serviço de teste",
		},
		Acesso: presentation.DadosAcesso{
			ChaveAcesso:         "35260800000000000000000000000000001234567890",
			CodigoQR:            "https://exemplo/qr?token=abc",
			NumeroNFSe:          "1",
			Competencia:         "01/2027",
			DataHoraEmissaoNFSe: "2027-01-15T10:00:00",
			NumeroDPS:           "1",
			SerieDPS:            "1",
			DataHoraEmissaoDPS:  "2027-01-15T09:00:00",
		},
		Partes: presentation.DadosPartes{
			Emitente: presentation.SecaoParte{
				Titulo:             "EMITENTE",
				CNPJCPFNIF:         "12345678000199",
				InscricaoMunicipal: "1234567",
				Nome:               "PRESTADOR DE TESTE LTDA",
				MunicipioUF:        "São Paulo/SP",
			},
			Tomador: presentation.SecaoParte{
				Titulo:      "TOMADOR",
				CNPJCPFNIF:  "99887766000155",
				Nome:        "TOMADOR DE TESTE LTDA",
				MunicipioUF: "São Paulo/SP",
				Visivel:     true,
			},
			Destinatario: presentation.SecaoParte{Visivel: false},
		},
		Servico: presentation.DadosServico{
			CodigoNBS:        "999999",
			LocalPrestacaoUF: "BR",
			DescServ:         "Serviço de teste",
		},
		Tributacoes: struct {
			Municipal presentation.DadosTributacaoMunicipal
			Federal   presentation.DadosTributacaoFederal
			IBSCBS    presentation.DadosTributacaoIBSCBS
		}{
			Municipal: presentation.DadosTributacaoMunicipal{Visivel: true},
			Federal: presentation.DadosTributacaoFederal{
				Visivel:          true,
				ExibirPisCofins:  false,
				IRRF:             "R$ 50,00",
				ContribPrevidRet: "R$ 100,00",
				ContribSocRet:    "R$ 60,00",
				PISDebito:        "R$ 0,00",
				COFINSDebito:     "R$ 0,00",
				DescContribRet:   "Retido na Fonte",
			},
			IBSCBS: presentation.DadosTributacaoIBSCBS{Visivel: true},
		},
		Totais: presentation.DadosTotais{
			ValorOperacaoServico: "100,00",
			ValorLiquido:         "100,00",
		},
		InformacoesComplementares: presentation.DadosInformacoesComplementares{
			Conteudo:      "",
			LinhaTributos: "Totais aproximados dos Tributos: -",
		},
		Canhoto: presentation.DadosCanhoto{Visivel: false},
	}

	renderer, err := NewRenderer()
	if err != nil {
		t.Fatal(err)
	}

	pdf, err := renderer.Renderizar(context.Background(), doc)
	if err != nil {
		t.Fatal(err)
	}

	assertPDFBasic(t, pdf)
	pdfPath := writePDFToTempDir(t, "pis_cofins_hidden.pdf", pdf)
	assertPDFPageCount(t, pdfPath)

	// Quando ExibirPisCofins=false, os rótulos "PIS - Débito" e "COFINS - Débito"
	// NÃO devem aparecer no texto extraído do PDF.
	if _, err := exec.LookPath("pdftotext"); err != nil {
		t.Skip("pdftotext indisponível — pulando verificação textual")
	}
	text := pdfText(t, pdfPath)

	if strings.Contains(text, "PIS - Débito") {
		t.Error("PDF contém 'PIS - Débito' quando ExibirPisCofins=false")
	}
	if strings.Contains(text, "COFINS - Débito") {
		t.Error("PDF contém 'COFINS - Débito' quando ExibirPisCofins=false")
	}
}
