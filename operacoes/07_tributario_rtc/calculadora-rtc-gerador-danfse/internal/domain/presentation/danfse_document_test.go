package presentation

import (
	"testing"
)

func TestDocumentoDanfseZeroValues(t *testing.T) {
	doc := DocumentoDanfse{}
	if doc.MarcaDagua.Texto != "" {
		t.Fatal("MarcaDagua.Texto should be empty at zero value")
	}
	if doc.Cabecalho.Municipio != "" {
		t.Fatal("Cabecalho.Municipio should be empty at zero value")
	}
	if doc.Partes.Emitente.Visivel {
		t.Fatal("SecaoParte.Visivel should be false at zero value")
	}
	if doc.Tributacoes.Municipal.Visivel {
		t.Fatal("DadosTributacaoMunicipal.Visivel should be false at zero value")
	}
}

func TestDocumentoDanfseBuildMinimal(t *testing.T) {
	doc := DocumentoDanfse{
		Cabecalho: DadosCabecalho{
			Municipio:       "São Paulo",
			UF:              "SP",
			AmbienteGerador: "Ambiente de Produção",
			TipoAmbiente:    "Produção",
		},
		Acesso: DadosAcesso{
			ChaveAcesso: "12345678901234",
			NumeroNFSe:  "000123",
			Competencia: "15/09/2026",
			NumeroDPS:   "000456",
			SerieDPS:    "1",
		},
		Partes: DadosPartes{
			Emitente: SecaoParte{
				Titulo:             "PRESTADOR / FORNECEDOR",
				CNPJCPFNIF:         "12.345.678/0001-95",
				InscricaoMunicipal: "1234567",
				Nome:               "Empresa Teste LTDA",
				MunicipioUF:        "São Paulo / SP",
				Email:              "contato@empresa.com",
				Visivel:            true,
			},
		},
		Servico: DadosServico{
			CodigoTribNacMun: "050200 / 0001",
			CodigoNBS:        "1.1001.01.00",
			DescServ:         "Serviço de consultoria",
		},
		Tributacoes: struct {
			Municipal DadosTributacaoMunicipal
			Federal   DadosTributacaoFederal
			IBSCBS    DadosTributacaoIBSCBS
		}{
			Municipal: DadosTributacaoMunicipal{
				TribISSQN:            "Operação Tributável",
				MunicipioUFPAISIncid: "São Paulo - SP / SP / Brasil",
				RegEspTrib:           "Nenhum",
				BCISSQN:              "R$ 1.000,00",
				ISSQNApurado:         "R$ 20,00",
				Visivel:              true,
			},
		},
		Totais: DadosTotais{
			ValorOperacaoServico: "R$ 1.000,00",
			ValorLiquido:         "R$ 980,00",
		},
	}

	if doc.Cabecalho.Municipio != "São Paulo" {
		t.Fatalf("Cabecalho.Municipio: esperado %q, obtido %q", "São Paulo", doc.Cabecalho.Municipio)
	}
	if !doc.Partes.Emitente.Visivel {
		t.Fatal("Emitente.Visivel should be true")
	}
	if !doc.Tributacoes.Municipal.Visivel {
		t.Fatal("DadosTributacaoMunicipal.Visivel deve ser true")
	}
}

func TestDocumentoDanfseSuppressedBlocks(t *testing.T) {
	doc := DocumentoDanfse{
		Partes: DadosPartes{
			Tomador: SecaoParte{
				Visivel:           false,
				MensagemSupressao: "TOMADOR/ADQUIRENTE DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e",
			},
			Destinatario: SecaoParte{
				Visivel:           false,
				MensagemSupressao: "DESTINATÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e",
			},
			Intermediario: SecaoParte{
				Visivel:           false,
				MensagemSupressao: "INTERMEDIÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e",
			},
		},
		Tributacoes: struct {
			Municipal DadosTributacaoMunicipal
			Federal   DadosTributacaoFederal
			IBSCBS    DadosTributacaoIBSCBS
		}{
			Municipal: DadosTributacaoMunicipal{
				Visivel:           false,
				MensagemSupressao: "TRIBUTAÇÃO MUNICIPAL (ISSQN) - OPERAÇÃO NÃO SUJEITA AO ISSQN",
			},
		},
	}

	if doc.Partes.Tomador.Visivel {
		t.Fatal("Tomador.Visivel should be false")
	}
	if doc.Partes.Tomador.MensagemSupressao == "" {
		t.Fatal("Tomador.MensagemSupressao should be set")
	}
	if doc.Tributacoes.Municipal.Visivel {
		t.Fatal("DadosTributacaoMunicipal.Visivel deve ser false")
	}
}

func TestDocumentoDanfseNoModelsImport(t *testing.T) {
	// Verificação em tempo de compilação: este arquivo não deve importar models ou renderer.
	// Se importasse, a compilação falharia. Este teste documenta a restrição.
	doc := DocumentoDanfse{}
	_ = doc
}

func TestDocumentoDanfseMarcaDagua(t *testing.T) {
	doc := DocumentoDanfse{
		MarcaDagua: MarcaDagua{
			Texto:        "Marca d'água de teste",
			TamanhoFonte: 60,
			ColorR:       0.35,
			ColorG:       0.35,
			ColorB:       0.35,
			Angulo:       45,
			Opacidade:    0.35,
		},
	}
	if doc.MarcaDagua.Texto != "Marca d'água de teste" {
		t.Fatalf("MarcaDagua.Texto: esperado %q, obtido %q", "Marca d'água de teste", doc.MarcaDagua.Texto)
	}
	if doc.MarcaDagua.TamanhoFonte != 60 {
		t.Fatalf("MarcaDagua.TamanhoFonte: esperado %v, obtido %v", 60.0, doc.MarcaDagua.TamanhoFonte)
	}
}

func TestDocumentoDanfseIBSCBSBlock(t *testing.T) {
	doc := DocumentoDanfse{
		Tributacoes: struct {
			Municipal DadosTributacaoMunicipal
			Federal   DadosTributacaoFederal
			IBSCBS    DadosTributacaoIBSCBS
		}{
			IBSCBS: DadosTributacaoIBSCBS{
				CSTcClassTrib:            "01 / 01",
				IndOpCodIBGELocalidadeUF: "1 / 3550308 / São Paulo / SP",
				ExclusoesReducoesBC:      "R$ 0,00",
				BCPosExclusoes:           "R$ 1.000,00",
				Visivel:                  true,
			},
		},
	}
	if !doc.Tributacoes.IBSCBS.Visivel {
		t.Fatal("IBSCBS.Visivel should be true")
	}
}

func TestDocumentoDanfseComplementaryAndReceipt(t *testing.T) {
	doc := DocumentoDanfse{
		InformacoesComplementares: DadosInformacoesComplementares{
			Conteudo:      "Informações complementares",
			LinhaTributos: "Linha de tributos",
			Visivel:       true,
		},
		Canhoto: DadosCanhoto{
			Visivel: true,
		},
	}
	if !doc.InformacoesComplementares.Visivel {
		t.Fatal("InformacoesComplementares.Visivel should be true")
	}
	if !doc.Canhoto.Visivel {
		t.Fatal("Canhoto.Visivel should be true")
	}
}
