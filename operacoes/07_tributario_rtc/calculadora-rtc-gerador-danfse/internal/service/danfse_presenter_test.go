package service

import (
	"strings"
	"testing"

	"danfse-service/internal/domain/models"
	"danfse-service/internal/domain/presentation"
)

func TestApresentadorDanfse_MarcaDagua(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{MarcaDagua: "CANCELADA"}
	documento := p.Apresentar(nota)

	if documento.MarcaDagua.Texto != "CANCELADA" {
		t.Fatalf("marcaDagua texto: esperado %q, obteve %q", "CANCELADA", documento.MarcaDagua.Texto)
	}
	if documento.MarcaDagua.TamanhoFonte != 60 {
		t.Fatalf("marcaDagua tamanhoFonte: esperado 60, obteve %f", documento.MarcaDagua.TamanhoFonte)
	}
	if documento.MarcaDagua.ColorR != 0.35 || documento.MarcaDagua.ColorG != 0.35 || documento.MarcaDagua.ColorB != 0.35 {
		t.Fatalf("marcaDagua color: esperado 0.35/0.35/0.35, obteve %f/%f/%f",
			documento.MarcaDagua.ColorR, documento.MarcaDagua.ColorG, documento.MarcaDagua.ColorB)
	}
	if documento.MarcaDagua.Angulo != 45 {
		t.Fatalf("marcaDagua angulo: esperado 45, obteve %f", documento.MarcaDagua.Angulo)
	}
	if documento.MarcaDagua.Opacidade != 0.35 {
		t.Fatalf("marcaDagua opacidade: esperado 0.35, obteve %f", documento.MarcaDagua.Opacidade)
	}
}

func TestApresentadorDanfse_MarcaDaguaVazio(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{MarcaDagua: ""}
	documento := p.Apresentar(nota)

	if documento.MarcaDagua.Texto != "" {
		t.Fatalf("marcaDagua texto vazio esperado, obteve %q", documento.MarcaDagua.Texto)
	}
}

func TestApresentadorDanfse_VisibilidadePartes(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()

	// NFSe sem tomador, destinatário ou intermediário
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Partes.Tomador.Visivel {
		t.Fatal("tomador deveria estar suprimido")
	}
	if documento.Partes.Destinatario.Visivel {
		t.Fatal("destinatário deveria estar suprimido")
	}
	if documento.Partes.Intermediario.Visivel {
		t.Fatal("intermediário deveria estar suprimido")
	}
}

func TestApresentadorDanfse_DestinatarioEhTomador(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()

	cnpj := "12345678000195"
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Toma: &models.Tomador{CNPJ: cnpj, XNome: "Tomador"},
					IBSCBS: &models.IBSCBSDPS{
						Dest: &models.Destinatario{CNPJ: cnpj, XNome: "Mesmo CNPJ"},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Partes.Destinatario.Visivel {
		t.Fatal("destinatário deveria estar suprimido quando igual ao tomador")
	}
	if !strings.Contains(documento.Partes.Destinatario.MensagemSupressao, "PRÓPRIO TOMADOR") {
		t.Fatalf("mensagem de supressão inesperada: %q", documento.Partes.Destinatario.MensagemSupressao)
	}
}

func TestApresentadorDanfse_DestinatarioDiferente(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()

	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Toma: &models.Tomador{CNPJ: "11111111000111", XNome: "Tomador"},
					IBSCBS: &models.IBSCBSDPS{
						Dest: &models.Destinatario{CNPJ: "22222222000222", XNome: "Destinatário"},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !documento.Partes.Destinatario.Visivel {
		t.Fatal("destinatário deveria estar visível quando diferente do tomador")
	}
}

func TestApresentadorDanfse_TribMunAusente(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: nil,
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	// A tributação municipal sem dados deve ser suprimida conforme a NT-008.
	if documento.Tributacoes.Municipal.Visivel {
		t.Fatal("tributação municipal deveria ser suprimida com TribMun nil")
	}
	if documento.Tributacoes.Municipal.MensagemSupressao != "TRIBUTAÇÃO MUNICIPAL (ISSQN) - OPERAÇÃO NÃO SUJEITA AO ISSQN" {
		t.Fatalf("mensagem de supressão ISSQN inesperada: %q", documento.Tributacoes.Municipal.MensagemSupressao)
	}
	if !documento.Tributacoes.Federal.Visivel {
		t.Fatal("tributação federal deveria estar visível mesmo com dados ausentes")
	}
	if !documento.Tributacoes.IBSCBS.Visivel {
		t.Fatal("IBS/CBS deveria estar visível mesmo com dados ausentes")
	}

	// Placeholders principais para cada seção
	if documento.Tributacoes.Municipal.TribISSQN != "-" {
		t.Fatalf("Municipal.TribISSQN: esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.TribISSQN)
	}
	if documento.Tributacoes.Federal.IRRF != "-" {
		t.Fatalf("Federal.IRRF: esperado %q, obteve %q", "-", documento.Tributacoes.Federal.IRRF)
	}
	if documento.Tributacoes.IBSCBS.CSTcClassTrib != "-" {
		t.Fatalf("IBSCBS.CSTcClassTrib: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.CSTcClassTrib)
	}
}

func TestApresentadorDanfse_TribMunVisivel(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0},
								BM:          models.BM{VRedBCBM: 100},
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !documento.Tributacoes.Municipal.Visivel {
		t.Fatal("tributação municipal deveria estar visível")
	}
	if documento.Tributacoes.Municipal.TribISSQN != "Operação Tributável" {
		t.Fatalf("tribISSQN label: esperado %q, obteve %q", "Operação Tributável", documento.Tributacoes.Municipal.TribISSQN)
	}
}

func TestApresentadorDanfse_LabelsFallbacks(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0},
								BM:          models.BM{VRedBCBM: 100},
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	// Verificar que labels usam os mapeamentos corretos
	if documento.Tributacoes.Municipal.TribISSQN != "Operação Tributável" {
		t.Errorf("tribISSQN: esperado %q, obteve %q", "Operação Tributável", documento.Tributacoes.Municipal.TribISSQN)
	}
	if documento.Tributacoes.Municipal.RetISSQN != "Não Retido" {
		t.Errorf("retISSQN: esperado %q, obteve %q", "Não Retido", documento.Tributacoes.Municipal.RetISSQN)
	}
	if documento.Tributacoes.Municipal.BeneficioMunicipal != "-" {
		t.Errorf("beneficioMunicipal (tpBM=0): esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.BeneficioMunicipal)
	}
}

func TestApresentadorDanfse_DadosCabecalho(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		MarcaDagua: "TESTE",
		InfNFSe: models.InfNFSe{
			AmbGer: "1",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					TpAmb: 1,
				},
			},
			Emit: models.Emitente{
				EnderNac: models.Endereco{XMun: "São Paulo", UF: "SP"},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Cabecalho.Municipio != "São Paulo" {
		t.Fatalf("header municipio: esperado %q, obteve %q", "São Paulo", documento.Cabecalho.Municipio)
	}
	if documento.Cabecalho.UF != "SP" {
		t.Fatalf("header uf: esperado %q, obteve %q", "SP", documento.Cabecalho.UF)
	}
	if documento.Cabecalho.AmbienteGerador != "Ambiente de Produção" {
		t.Fatalf("header ambienteGerador: esperado %q, obteve %q", "Ambiente de Produção", documento.Cabecalho.AmbienteGerador)
	}
	if documento.Cabecalho.TipoAmbiente != "Produção" {
		t.Fatalf("header tipoAmbiente: esperado %q, obteve %q", "Produção", documento.Cabecalho.TipoAmbiente)
	}
}

func TestApresentadorDanfse_DadosAcesso(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Id:     "NFS123456789",
			Qrcode: "https://qrcode.test",
			NNFSe:  "42",
			DhProc: "2026-07-14T10:00:00-03:00",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					NDPS:    "10",
					Serie:   "S1",
					DhEmi:   "2026-07-14T09:00:00-03:00",
					DCompet: "2026-07-14",
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Acesso.ChaveAcesso != "123456789" {
		t.Fatalf("chaveAcesso: esperado %q, obteve %q", "123456789", documento.Acesso.ChaveAcesso)
	}
	if documento.Acesso.NumeroNFSe != "42" {
		t.Fatalf("numeroNFSe: esperado %q, obteve %q", "42", documento.Acesso.NumeroNFSe)
	}
	if documento.Acesso.NumeroDPS != "10" {
		t.Fatalf("numeroDPS: esperado %q, obteve %q", "10", documento.Acesso.NumeroDPS)
	}
}

func TestApresentadorDanfse_DadosServico(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoDescServ: "Serviço de Teste",
		InfNFSe: models.InfNFSe{
			XTribNac: "Transporte de cargas",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Serv: models.Servico{
						CServ: models.CServ{
							CTribNac: "010101",
							CTribMun: "001",
							CNBS:     "123456789",
						},
						LocPrest: models.LocPrest{
							UF:             "SP",
							CPaisPrestacao: "BR",
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Servico.DescServ != "Serviço de Teste" {
		t.Fatalf("descServ: esperado %q, obteve %q", "Serviço de Teste", documento.Servico.DescServ)
	}
	if documento.Servico.CodigoTribNacMun != "01.01.01 / 001" {
		t.Fatalf("codigoTribNacMun: esperado %q, obteve %q", "01.01.01 / 001", documento.Servico.CodigoTribNacMun)
	}
	if documento.Servico.CodigoNBS != "1.2345.67.89" {
		t.Fatalf("codigoNBS: esperado %q, obteve %q", "1.2345.67.89", documento.Servico.CodigoNBS)
	}
	if documento.Servico.LocalPrestacaoUF != "- / SP / BR" {
		t.Fatalf("localPrestacaoUF: esperado %q, obteve %q", "- / SP / BR", documento.Servico.LocalPrestacaoUF)
	}
}

func TestApresentadorDanfse_CodigosTributacaoNacionalEMunicipal(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	testes := []struct {
		nacional  string
		municipal string
		expected  string
	}{
		{nacional: "010101", municipal: "001", expected: "01.01.01 / 001"},
		{nacional: "010101", expected: "01.01.01"},
		{municipal: "001", expected: "001"},
		{expected: ""},
	}

	for _, tc := range testes {
		nota := models.NFSe{InfNFSe: models.InfNFSe{DPS: models.DPS{InfDPS: models.InfDPS{
			Serv: models.Servico{CServ: models.CServ{CTribNac: tc.nacional, CTribMun: tc.municipal}},
		}}}}
		documento := p.Apresentar(nota)
		if documento.Servico.CodigoTribNacMun != tc.expected {
			t.Errorf("cTribNac=%q cTribMun=%q: esperado %q, obteve %q", tc.nacional, tc.municipal, tc.expected, documento.Servico.CodigoTribNacMun)
		}
	}
}

func TestApresentadorDanfse_DadosInformacoesComplementares(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoCompl:         "Info complementar",
		ExibicaoLinhaTributos: "Linha tributária",
	}
	documento := p.Apresentar(nota)

	if documento.InformacoesComplementares.Conteudo != "Info complementar" {
		t.Fatalf("complementary conteudo: esperado %q, obteve %q", "Info complementar", documento.InformacoesComplementares.Conteudo)
	}
	if documento.InformacoesComplementares.LinhaTributos != "Linha tributária" {
		t.Fatalf("complementary linhaTributos: esperado %q, obteve %q", "Linha tributária", documento.InformacoesComplementares.LinhaTributos)
	}
}

func TestApresentadorDanfse_CanhotoVisivel(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{}
	documento := p.Apresentar(nota)

	if !documento.Canhoto.Visivel {
		t.Fatal("canhoto deveria estar visível")
	}
}

func TestApresentadorDanfse_EmitenteParte(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Emit: models.Emitente{
				CNPJ:  "12345678000195",
				IM:    "123456",
				Fone:  "11999998888",
				XNome: "Empresa Teste",
				EnderNac: models.Endereco{
					XMun:    "São Paulo",
					UF:      "SP",
					CMun:    "3550308",
					CEP:     "01001000",
					XLgr:    "Rua Teste",
					Nro:     "123",
					XBairro: "Centro",
				},
				Email: "teste@empresa.com",
			},
		},
	}
	documento := p.Apresentar(nota)

	if !documento.Partes.Emitente.Visivel {
		t.Fatal("emitente deveria estar visível")
	}
	if documento.Partes.Emitente.CNPJCPFNIF != "12.345.678/0001-95" {
		t.Fatalf("emitente CNPJ formatado: esperado %q, obteve %q", "12.345.678/0001-95", documento.Partes.Emitente.CNPJCPFNIF)
	}
	if documento.Partes.Emitente.Nome != "Empresa Teste" {
		t.Fatalf("emitente nome: esperado %q, obteve %q", "Empresa Teste", documento.Partes.Emitente.Nome)
	}
	if documento.Partes.Emitente.MunicipioUF != "São Paulo / SP" {
		t.Fatalf("emitente municipioUF: esperado %q, obteve %q", "São Paulo / SP", documento.Partes.Emitente.MunicipioUF)
	}
}

func TestApresentadorDanfse_TomadorParte(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Toma: &models.Tomador{
						CPF:   "12345678901",
						XNome: "Tomador Teste",
						IM:    "987654",
						Fone:  "11988887777",
						Email: "tomador@teste.com",
						End: &models.EnderecoTomador{
							XLgr:    "Av. Tomador",
							Nro:     "456",
							XBairro: "Jardim",
							EndNac: &models.EndNac{
								XMun: "Rio de Janeiro",
								UF:   "RJ",
								CMun: "3304557",
								CEP:  "20000000",
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !documento.Partes.Tomador.Visivel {
		t.Fatal("tomador deveria estar visível")
	}
	if documento.Partes.Tomador.CNPJCPFNIF != "123.456.789-01" {
		t.Fatalf("tomador CPF formatado: esperado %q, obteve %q", "123.456.789-01", documento.Partes.Tomador.CNPJCPFNIF)
	}
	if documento.Partes.Tomador.MunicipioUF != "Rio de Janeiro / RJ" {
		t.Fatalf("tomador municipioUF: esperado %q, obteve %q", "Rio de Janeiro / RJ", documento.Partes.Tomador.MunicipioUF)
	}
}

func TestApresentadorDanfse_IntermediarioParte(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Interm: &models.Interm{
						CNPJ:  "98765432000199",
						XNome: "Intermediário Ltda",
						IM:    "111222",
						Fone:  "21977776666",
						Email: "inter@medio.com",
						End: models.EnderecoDPS{
							XLgr:    "Rua Inter",
							Nro:     "789",
							XBairro: "Centro",
							EndNac: &models.Endereco{
								XMun: "Belo Horizonte",
								UF:   "MG",
								CMun: "3106200",
								CEP:  "30000000",
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !documento.Partes.Intermediario.Visivel {
		t.Fatal("intermediário deveria estar visível")
	}
	if documento.Partes.Intermediario.CNPJCPFNIF != "98.765.432/0001-99" {
		t.Fatalf("intermediario CNPJ formatado: esperado %q, obteve %q", "98.765.432/0001-99", documento.Partes.Intermediario.CNPJCPFNIF)
	}
}

func TestApresentadorDanfse_DadosTotais(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Valores: models.Valores{
				VLiq:      900,
				VTotalRet: 100,
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						VServPrest: models.VServPrest{VServ: 1000},
						VDescCondIncond: models.VDescCondIncond{
							VDescIncond: 50,
							VDescCond:   50,
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Totais.ValorOperacaoServico != "R$ 1.000,00" {
		t.Fatalf("valorOperacao: esperado %q, obteve %q", "R$ 1.000,00", documento.Totais.ValorOperacaoServico)
	}
	if documento.Totais.ValorLiquido != "R$ 900,00" {
		t.Fatalf("valorLiquido: esperado %q, obteve %q", "R$ 900,00", documento.Totais.ValorLiquido)
	}
	if documento.Totais.TotalIBSCBS != "-" {
		t.Fatalf("totalIBSCBS: esperado %q, obteve %q", "-", documento.Totais.TotalIBSCBS)
	}
}

func TestApresentadorDanfse_IBSCBS(t *testing.T) {
	testes := []struct {
		name  string
		nota  models.NFSe
		check func(t *testing.T, documento presentation.DocumentoDanfse)
	}{
		{
			name: "Nulo",
			nota: models.NFSe{
				InfNFSe: models.InfNFSe{
					IBSCBS: nil,
				},
			},
			check: func(t *testing.T, documento presentation.DocumentoDanfse) {
				if !documento.Tributacoes.IBSCBS.Visivel {
					t.Fatal("IBS/CBS deveria estar visível mesmo quando nil")
				}
				if documento.Tributacoes.IBSCBS.CSTcClassTrib != "-" {
					t.Fatalf("CSTcClassTrib: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.CSTcClassTrib)
				}
				if documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF != "-" {
					t.Fatalf("IndOpCodIBGELocalidadeUF: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF)
				}
				if documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS != "-" {
					t.Fatalf("RedAliqIBSRedAliqCBS: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS)
				}
				if documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun != "-" {
					t.Fatalf("AliqIBSUFIBSMun: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun)
				}
			},
		},
		{
			name: "ComposicoesVazias",
			nota: models.NFSe{
				InfNFSe: models.InfNFSe{
					IBSCBS: &models.IBSCBS{},
				},
			},
			check: func(t *testing.T, documento presentation.DocumentoDanfse) {
				if !documento.Tributacoes.IBSCBS.Visivel {
					t.Fatal("IBS/CBS deveria estar visível quando IBSCBS não é nil")
				}
				if documento.Tributacoes.IBSCBS.CSTcClassTrib != "-" {
					t.Fatalf("CSTcClassTrib vazio: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.CSTcClassTrib)
				}
				if documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF != "-" {
					t.Fatalf("IndOpCodIBGELocalidadeUF vazio: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF)
				}
				if documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS != "-" {
					t.Fatalf("RedAliqIBSRedAliqCBS vazio: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS)
				}
				if documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun != "-" {
					t.Fatalf("AliqIBSUFIBSMun vazio: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun)
				}
			},
		},
		{
			name: "ComposicoesParciais",
			nota: models.NFSe{
				InfNFSe: models.InfNFSe{
					IBSCBS: &models.IBSCBS{
						CLocalidadeIncid: "12345",
						// XLocalidadeIncid e UF ficam vazios
						Valores: models.ValoresIBSCBSResp{
							VBC: 1000,
							UF: models.AliqIBSUF{
								PIBSUF:      ptrFloatRef(2.5),
								PAliqEfetUF: ptrFloatRef(2.5),
							},
							Mun: models.AliqIBSMun{
								PIBSMun:      ptrFloatRef(2.5),
								PAliqEfetMun: ptrFloatRef(2.5),
							},
							Fed: models.AliqCBS{},
						},
						TotCIBS: models.TotCIBS{
							GIBS: models.GIBSTot{VIBSTot: 50},
							GCBS: models.GCBSTot{VCBS: 100},
						},
					},
				},
			},
			check: func(t *testing.T, documento presentation.DocumentoDanfse) {
				if !documento.Tributacoes.IBSCBS.Visivel {
					t.Fatal("IBS/CBS deveria estar visível")
				}
				// CSTcClassTrib: dps.IBSCBS é nil → sem informação composta
				if documento.Tributacoes.IBSCBS.CSTcClassTrib != "-" {
					t.Fatalf("CSTcClassTrib parcial: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.CSTcClassTrib)
				}
				// IndOpCodIBGELocalidadeUF: cIndOp="-", localidade="12345", nomeLocalidade="-", uf="-"
				if documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF != "- / 12345 / - / -" {
					t.Fatalf("IndOpCodIBGELocalidadeUF parcial: esperado %q, obteve %q", "- / 12345 / - / -", documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF)
				}
				// RedAliqIBSRedAliqCBS: UF.PRedAliqUF="-", Mun.PRedAliqMun="-", Fed.PRedAliqCBS="-"
				if documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS != "-" {
					t.Fatalf("RedAliqIBSRedAliqCBS parcial: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS)
				}
				// AliqIBSUFIBSMun: "2,50% / 2,50%"
				if documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun != "2,50% / 2,50%" {
					t.Fatalf("AliqIBSUFIBSMun parcial: esperado %q, obteve %q", "2,50% / 2,50%", documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun)
				}
			},
		},
		{
			name: "DpsNaoNil_NfseNil",
			nota: models.NFSe{
				InfNFSe: models.InfNFSe{
					IBSCBS: nil, // NFSe IBSCBS é nil
					DPS: models.DPS{
						InfDPS: models.InfDPS{
							IBSCBS: &models.IBSCBSDPS{
								CIndOp: "1",
								Valores: models.ValoresIBSCBS{
									Trib: models.TribIBSCBS{
										GIBSCBS: models.GIBSCBS{
											CST:        "01",
											CClassTrib: "001",
										},
									},
								},
							},
						},
					},
				},
			},
			check: func(t *testing.T, documento presentation.DocumentoDanfse) {
				// Deve retornar Visivel true com placeholders; DPS não deve vazar quando NFSe.IBSCBS é nil
				if !documento.Tributacoes.IBSCBS.Visivel {
					t.Fatal("IBS/CBS deveria estar visível mesmo quando NFSe IBSCBS é nil")
				}
				if documento.Tributacoes.IBSCBS.CSTcClassTrib != "-" {
					t.Fatalf("CSTcClassTrib: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.CSTcClassTrib)
				}
				if documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF != "-" {
					t.Fatalf("IndOpCodIBGELocalidadeUF: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF)
				}
				if documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS != "-" {
					t.Fatalf("RedAliqIBSRedAliqCBS: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS)
				}
				if documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun != "-" {
					t.Fatalf("AliqIBSUFIBSMun: esperado %q, obteve %q", "-", documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun)
				}
			},
		},
		{
			name: "ComposicoesPreenchidas",
			nota: models.NFSe{
				InfNFSe: models.InfNFSe{
					IBSCBS: &models.IBSCBS{
						CLocalidadeIncid: "3550308",
						XLocalidadeIncid: "São Paulo",
						UF:               "SP",
						Valores: models.ValoresIBSCBSResp{
							VBC: 5000,
							UF: models.AliqIBSUF{
								PIBSUF:      ptrFloatRef(1.5),
								PAliqEfetUF: ptrFloatRef(1.5),
								PRedAliqUF:  ptrFloatRef(0.5),
							},
							Mun: models.AliqIBSMun{
								PIBSMun:      ptrFloatRef(2.5),
								PAliqEfetMun: ptrFloatRef(2.5),
								PRedAliqMun:  ptrFloatRef(1.0),
							},
							Fed: models.AliqCBS{
								PCBS:         ptrFloatRef(2.5),
								PAliqEfetCBS: ptrFloatRef(2.5),
								PRedAliqCBS:  ptrFloatRef(0.25),
							},
						},
						TotCIBS: models.TotCIBS{
							GIBS: models.GIBSTot{
								VIBSTot: 200,
								GIBSMunTot: models.GIBSMunTot{
									VIBSMun: ptrFloatRef(100),
								},
								GIBSUFTot: models.GIBSUFTot{
									VIBSUF: ptrFloatRef(100),
								},
							},
							GCBS: models.GCBSTot{VCBS: 300},
						},
					},
					DPS: models.DPS{
						InfDPS: models.InfDPS{
							IBSCBS: &models.IBSCBSDPS{
								CIndOp: "0",
								Valores: models.ValoresIBSCBS{
									Trib: models.TribIBSCBS{
										GIBSCBS: models.GIBSCBS{
											CST:        "01",
											CClassTrib: "001",
										},
									},
								},
							},
						},
					},
				},
			},
			check: func(t *testing.T, documento presentation.DocumentoDanfse) {
				if !documento.Tributacoes.IBSCBS.Visivel {
					t.Fatal("IBS/CBS deveria estar visível")
				}
				if documento.Tributacoes.IBSCBS.CSTcClassTrib != "01 / 001" {
					t.Fatalf("CSTcClassTrib preenchido: esperado %q, obteve %q", "01 / 001", documento.Tributacoes.IBSCBS.CSTcClassTrib)
				}
				if documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF != "0 / 3550308 / São Paulo / SP" {
					t.Fatalf("IndOpCodIBGELocalidadeUF preenchido: esperado %q, obteve %q", "0 / 3550308 / São Paulo / SP", documento.Tributacoes.IBSCBS.IndOpCodIBGELocalidadeUF)
				}
				if documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS != "0,50% / 1,00% / 0,25%" {
					t.Fatalf("RedAliqIBSRedAliqCBS preenchido: esperado %q, obteve %q", "0,50% / 1,00% / 0,25%", documento.Tributacoes.IBSCBS.RedAliqIBSRedAliqCBS)
				}
				if documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun != "1,50% / 2,50%" {
					t.Fatalf("AliqIBSUFIBSMun preenchido: esperado %q, obteve %q", "1,50% / 2,50%", documento.Tributacoes.IBSCBS.AliqIBSUFIBSMun)
				}
			},
		},
	}

	for _, tc := range testes {
		t.Run(tc.name, func(t *testing.T) {
			p := NovoServicoApresentacaoDanfse()
			documento := p.Apresentar(tc.nota)
			tc.check(t, documento)
		})
	}
}

func TestApresentadorDanfse_TipoDocumentoDanfse(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{}
	documento := p.Apresentar(nota)

	// Verificar que o tipo retornado é presentation.DocumentoDanfse
	var _ presentation.DocumentoDanfse = documento
}

func TestApresentadorDanfse_CamposMonetariosZero(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	// Set TpBM > 0 para habilitar exibirOpcionais para que o bloco de substituição não
	// substitua valores formatados por "-".
	tpBM := 1
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Valores: models.Valores{
				TpBM:      tpBM,
				VBC:       0,
				VISSQN:    0,
				VTotalRet: 0,
				VLiq:      0,
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0},
								BM:          models.BM{VRedBCBM: 0},
							},
						},
						VServPrest: models.VServPrest{VServ: 0},
						VDedRed:    models.VDedRed{VDR: 0},
						VDescCondIncond: models.VDescCondIncond{
							VDescIncond: 0,
							VDescCond:   0,
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	// Campos opcionais devem mostrar "-" quando zero
	if documento.Tributacoes.Municipal.CalculoBM != "-" {
		t.Fatalf("municipal CalculoBM zero: esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.CalculoBM)
	}
	if documento.Tributacoes.Municipal.TotalDeducoes != "-" {
		t.Fatalf("municipal TotalDeducoes zero: esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.TotalDeducoes)
	}
	if documento.Tributacoes.Municipal.DescontoIncond != "-" {
		t.Fatalf("municipal DescontoIncond zero: esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.DescontoIncond)
	}
	if documento.Tributacoes.Municipal.BCISSQN != "-" {
		t.Fatalf("municipal BCISSQN zero: esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.BCISSQN)
	}
	if documento.Tributacoes.Municipal.ISSQNApurado != "-" {
		t.Fatalf("municipal ISSQNApurado zero: esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.ISSQNApurado)
	}
	// Campos obrigatórios devem mostrar "R$ 0,00"
	if documento.Totais.ValorOperacaoServico != "R$ 0,00" {
		t.Fatalf("totals ValorOperacaoServico zero: esperado %q, obteve %q", "R$ 0,00", documento.Totais.ValorOperacaoServico)
	}
	if documento.Totais.DescontoIncond != "-" {
		t.Fatalf("totals DescontoIncond zero: esperado %q, obteve %q", "-", documento.Totais.DescontoIncond)
	}
	if documento.Totais.DescontoCond != "-" {
		t.Fatalf("totals DescontoCond zero: esperado %q, obteve %q", "-", documento.Totais.DescontoCond)
	}
	if documento.Totais.TotalRetencoes != "-" {
		t.Fatalf("totals TotalRetencoes zero: esperado %q, obteve %q", "-", documento.Totais.TotalRetencoes)
	}
	if documento.Totais.ValorLiquido != "R$ 0,00" {
		t.Fatalf("totals ValorLiquido zero: esperado %q, obteve %q", "R$ 0,00", documento.Totais.ValorLiquido)
	}
	// IBSCBS é nil neste teste, então ValorLiquidoIBSCBS deve ser "-"
	if documento.Totais.ValorLiquidoIBSCBS != "-" {
		t.Fatalf("totals ValorLiquidoIBSCBS nil IBSCBS: esperado %q, obteve %q", "-", documento.Totais.ValorLiquidoIBSCBS)
	}
}

func TestApresentadorDanfse_CamposPercentualZero(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	zeroAliq := 0.0
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Valores: models.Valores{
				PAliqAplic: &zeroAliq,
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0},
								BM:          models.BM{VRedBCBM: 0},
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	// AliqAplic deve mostrar "-" quando zero (comportamento original do renderer `percent`)
	if documento.Tributacoes.Municipal.AliqAplic != "-" {
		t.Fatalf("municipal AliqAplic zero: esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.AliqAplic)
	}
}

func TestApresentadorDanfse_CamposMonetariosNaoZero(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			IBSCBS: &models.IBSCBS{},
			Valores: models.Valores{
				VBC:       1500.50,
				VISSQN:    75.03,
				VTotalRet: 100.00,
				VLiq:      1400.47,
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0},
								BM:          models.BM{VRedBCBM: 500.00},
							},
						},
						VServPrest: models.VServPrest{VServ: 1500.50},
						VDedRed:    models.VDedRed{VDR: 100.00},
						VDescCondIncond: models.VDescCondIncond{
							VDescIncond: 50.00,
							VDescCond:   25.00,
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Tributacoes.Municipal.CalculoBM != "R$ 500,00" {
		t.Fatalf("municipal CalculoBM: esperado %q, obteve %q", "R$ 500,00", documento.Tributacoes.Municipal.CalculoBM)
	}
	if documento.Tributacoes.Municipal.TotalDeducoes != "R$ 100,00" {
		t.Fatalf("municipal TotalDeducoes: esperado %q, obteve %q", "R$ 100,00", documento.Tributacoes.Municipal.TotalDeducoes)
	}
	if documento.Tributacoes.Municipal.DescontoIncond != "R$ 50,00" {
		t.Fatalf("municipal DescontoIncond: esperado %q, obteve %q", "R$ 50,00", documento.Tributacoes.Municipal.DescontoIncond)
	}
	if documento.Tributacoes.Municipal.BCISSQN != "R$ 1.500,50" {
		t.Fatalf("municipal BCISSQN: esperado %q, obteve %q", "R$ 1.500,50", documento.Tributacoes.Municipal.BCISSQN)
	}
	if documento.Tributacoes.Municipal.ISSQNApurado != "R$ 75,03" {
		t.Fatalf("municipal ISSQNApurado: esperado %q, obteve %q", "R$ 75,03", documento.Tributacoes.Municipal.ISSQNApurado)
	}
	if documento.Totais.ValorOperacaoServico != "R$ 1.500,50" {
		t.Fatalf("totals ValorOperacaoServico: esperado %q, obteve %q", "R$ 1.500,50", documento.Totais.ValorOperacaoServico)
	}
	if documento.Totais.DescontoIncond != "R$ 50,00" {
		t.Fatalf("totals DescontoIncond: esperado %q, obteve %q", "R$ 50,00", documento.Totais.DescontoIncond)
	}
	if documento.Totais.DescontoCond != "R$ 25,00" {
		t.Fatalf("totals DescontoCond: esperado %q, obteve %q", "R$ 25,00", documento.Totais.DescontoCond)
	}
	if documento.Totais.TotalRetencoes != "R$ 100,00" {
		t.Fatalf("totals TotalRetencoes: esperado %q, obteve %q", "R$ 100,00", documento.Totais.TotalRetencoes)
	}
	if documento.Totais.ValorLiquido != "R$ 1.400,47" {
		t.Fatalf("totals ValorLiquido: esperado %q, obteve %q", "R$ 1.400,47", documento.Totais.ValorLiquido)
	}
	// IBSCBS é não-nil mas vazio (VIBSTot=0, VCBS=0, VTotNF=0) → ambos "-"
	if documento.Totais.TotalIBSCBS != "-" {
		t.Fatalf("totals TotalIBSCBS IBSCBS vazio: esperado %q, obteve %q", "-", documento.Totais.TotalIBSCBS)
	}
	if documento.Totais.ValorLiquidoIBSCBS != "-" {
		t.Fatalf("totals ValorLiquidoIBSCBS IBSCBS vazio: esperado %q, obteve %q", "-", documento.Totais.ValorLiquidoIBSCBS)
	}
}

func TestApresentadorDanfse_EmitenteCamposOpcionaisVazios(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Emit: models.Emitente{
				CNPJ:  "12345678000195",
				IM:    "",
				Fone:  "",
				XNome: "",
				EnderNac: models.Endereco{
					XMun:    "São Paulo",
					UF:      "SP",
					CMun:    "3550308",
					CEP:     "01001000",
					XLgr:    "Rua Teste",
					Nro:     "123",
					XBairro: "Centro",
				},
				Email: "",
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Partes.Emitente.InscricaoMunicipal != "-" {
		t.Fatalf("emitente IM vazio: esperado %q, obteve %q", "-", documento.Partes.Emitente.InscricaoMunicipal)
	}
	if documento.Partes.Emitente.Telefone != "-" {
		t.Fatalf("emitente telefone vazio: esperado %q, obteve %q", "-", documento.Partes.Emitente.Telefone)
	}
	if documento.Partes.Emitente.Nome != "-" {
		t.Fatalf("emitente nome vazio: esperado %q, obteve %q", "-", documento.Partes.Emitente.Nome)
	}
	if documento.Partes.Emitente.Email != "-" {
		t.Fatalf("emitente email vazio: esperado %q, obteve %q", "-", documento.Partes.Emitente.Email)
	}
}

func TestApresentadorDanfse_ServicoComposicoesVazias(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoDescServ: "Serviço de Teste",
		InfNFSe: models.InfNFSe{
			XTribNac: "",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Serv: models.Servico{
						CServ: models.CServ{
							CTribNac: "",
							CTribMun: "",
							CNBS:     "",
						},
						LocPrest: models.LocPrest{
							UF:             "",
							CPaisPrestacao: "",
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Servico.CodigoTribNacMun != "" {
		t.Fatalf("codigoTribNacMun vazio: esperado vazio, obteve %q", documento.Servico.CodigoTribNacMun)
	}
	if documento.Servico.CodigoNBS != "-" {
		t.Fatalf("codigoNBS vazio: esperado %q, obteve %q", "-", documento.Servico.CodigoNBS)
	}
	if documento.Servico.LocalPrestacaoUF != "- / - / BR" {
		t.Fatalf("localPrestacaoUF vazio: esperado %q, obteve %q", "- / - / BR", documento.Servico.LocalPrestacaoUF)
	}
	if documento.Servico.XTribNac != "-" {
		t.Fatalf("xTribNac vazio: esperado %q, obteve %q", "-", documento.Servico.XTribNac)
	}
}

func TestApresentadorDanfse_TribMunNProcessoSuspVazio(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0, NProcesso: ""},
								BM:          models.BM{VRedBCBM: 0},
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Tributacoes.Municipal.NProcessoSusp != "-" {
		t.Fatalf("nProcessoSusp vazio: esperado %q, obteve %q", "-", documento.Tributacoes.Municipal.NProcessoSusp)
	}
}

func TestApresentadorDanfse_OpcionaisMonetariosNaoZero(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Valores: models.Valores{
				TpBM:      1,
				VBC:       2000.00,
				VISSQN:    100.00,
				VTotalRet: 150.00,
				VLiq:      1850.00,
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0},
								BM:          models.BM{VRedBCBM: 1000.00},
							},
						},
						VServPrest: models.VServPrest{VServ: 2000.00},
						VDedRed:    models.VDedRed{VDR: 200.00},
						VDescCondIncond: models.VDescCondIncond{
							VDescIncond: 100.00,
							VDescCond:   50.00,
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Tributacoes.Municipal.CalculoBM != "R$ 1.000,00" {
		t.Fatalf("municipal CalculoBM: esperado %q, obteve %q", "R$ 1.000,00", documento.Tributacoes.Municipal.CalculoBM)
	}
	if documento.Tributacoes.Municipal.TotalDeducoes != "R$ 200,00" {
		t.Fatalf("municipal TotalDeducoes: esperado %q, obteve %q", "R$ 200,00", documento.Tributacoes.Municipal.TotalDeducoes)
	}
	if documento.Tributacoes.Municipal.DescontoIncond != "R$ 100,00" {
		t.Fatalf("municipal DescontoIncond: esperado %q, obteve %q", "R$ 100,00", documento.Tributacoes.Municipal.DescontoIncond)
	}
	if documento.Tributacoes.Municipal.BCISSQN != "R$ 2.000,00" {
		t.Fatalf("municipal BCISSQN: esperado %q, obteve %q", "R$ 2.000,00", documento.Tributacoes.Municipal.BCISSQN)
	}
	if documento.Tributacoes.Municipal.ISSQNApurado != "R$ 100,00" {
		t.Fatalf("municipal ISSQNApurado: esperado %q, obteve %q", "R$ 100,00", documento.Tributacoes.Municipal.ISSQNApurado)
	}
	if documento.Totais.DescontoIncond != "R$ 100,00" {
		t.Fatalf("totals DescontoIncond: esperado %q, obteve %q", "R$ 100,00", documento.Totais.DescontoIncond)
	}
	if documento.Totais.DescontoCond != "R$ 50,00" {
		t.Fatalf("totals DescontoCond: esperado %q, obteve %q", "R$ 50,00", documento.Totais.DescontoCond)
	}
	if documento.Totais.TotalRetencoes != "R$ 150,00" {
		t.Fatalf("totals TotalRetencoes: esperado %q, obteve %q", "R$ 150,00", documento.Totais.TotalRetencoes)
	}
	if documento.Totais.ValorLiquido != "R$ 1.850,00" {
		t.Fatalf("totals ValorLiquido: esperado %q, obteve %q", "R$ 1.850,00", documento.Totais.ValorLiquido)
	}
}

func TestApresentadorDanfse_ValorLiquidoIBSCBS_NilIBSCBS(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			IBSCBS: nil,
			Valores: models.Valores{
				VLiq: 14500.50,
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						VServPrest: models.VServPrest{VServ: 14500.50},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Totais.ValorLiquidoIBSCBS != "-" {
		t.Fatalf("ValorLiquidoIBSCBS com IBSCBS nil: esperado %q, obteve %q", "-", documento.Totais.ValorLiquidoIBSCBS)
	}
	if documento.Totais.ValorOperacaoServico != "R$ 14.500,50" {
		t.Fatalf("ValorOperacaoServico preservado: esperado %q, obteve %q", "R$ 14.500,50", documento.Totais.ValorOperacaoServico)
	}
	if documento.Totais.ValorLiquido != "R$ 14.500,50" {
		t.Fatalf("ValorLiquido preservado: esperado %q, obteve %q", "R$ 14.500,50", documento.Totais.ValorLiquido)
	}
}

func TestApresentadorDanfse_TributacaoFederalSemPisCofins(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribFed: &models.TribFed{},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !documento.Tributacoes.Federal.Visivel {
		t.Fatal("tributação federal deveria estar visível")
	}
	if documento.Tributacoes.Federal.IRRF != "-" || documento.Tributacoes.Federal.ContribPrevidRet != "-" || documento.Tributacoes.Federal.ContribSocRet != "-" {
		t.Fatalf("retenções federais vazias: esperado todos como %q, obtido IRRF=%q CP=%q CSLL=%q", "-", documento.Tributacoes.Federal.IRRF, documento.Tributacoes.Federal.ContribPrevidRet, documento.Tributacoes.Federal.ContribSocRet)
	}
	if documento.Tributacoes.Federal.PISDebito != "-" || documento.Tributacoes.Federal.COFINSDebito != "-" || documento.Tributacoes.Federal.DescContribRet != "-" {
		t.Fatalf("PIS/COFINS ausentes: esperado todos como %q, obtido PIS=%q COFINS=%q desc=%q", "-", documento.Tributacoes.Federal.PISDebito, documento.Tributacoes.Federal.COFINSDebito, documento.Tributacoes.Federal.DescContribRet)
	}
}

func TestApresentadorDanfse_TomadorSemEndereco(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Toma: &models.Tomador{
						CNPJ:  "12345678000195",
						XNome: "Tomador Sem Endereço",
						IM:    "123456",
						Fone:  "11999998888",
						Email: "tomador@teste.com",
						End:   nil,
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !documento.Partes.Tomador.Visivel {
		t.Fatal("tomador deveria estar visível")
	}
	if documento.Partes.Tomador.MunicipioUF != "-" {
		t.Fatalf("tomador municipioUF sem endereco: esperado %q, obteve %q", "-", documento.Partes.Tomador.MunicipioUF)
	}
	if documento.Partes.Tomador.CodigoIBGECEP != "-" {
		t.Fatalf("tomador ibgeCep sem endereco: esperado %q, obteve %q", "-", documento.Partes.Tomador.CodigoIBGECEP)
	}
	if documento.Partes.Tomador.Endereco != "-" {
		t.Fatalf("tomador endereco sem endereco: esperado %q, obteve %q", "-", documento.Partes.Tomador.Endereco)
	}
}

func TestApresentadorDanfse_AcessoCamposVazios(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Id:     "",
			Qrcode: "https://qrcode.test",
			NNFSe:  "",
			DhProc: "",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					NDPS:    "",
					Serie:   "",
					DhEmi:   "",
					DCompet: "",
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Acesso.ChaveAcesso != "-" {
		t.Fatalf("chaveAcesso vazio: esperado %q, obteve %q", "-", documento.Acesso.ChaveAcesso)
	}
	if documento.Acesso.NumeroNFSe != "-" {
		t.Fatalf("numeroNFSe vazio: esperado %q, obteve %q", "-", documento.Acesso.NumeroNFSe)
	}
	if documento.Acesso.Competencia != "-" {
		t.Fatalf("competencia vazio: esperado %q, obteve %q", "-", documento.Acesso.Competencia)
	}
	if documento.Acesso.DataHoraEmissaoNFSe != "-" {
		t.Fatalf("dataHoraEmissaoNFSe vazio: esperado %q, obteve %q", "-", documento.Acesso.DataHoraEmissaoNFSe)
	}
	if documento.Acesso.NumeroDPS != "-" {
		t.Fatalf("numeroDPS vazio: esperado %q, obteve %q", "-", documento.Acesso.NumeroDPS)
	}
	if documento.Acesso.SerieDPS != "-" {
		t.Fatalf("serieDPS vazio: esperado %q, obteve %q", "-", documento.Acesso.SerieDPS)
	}
	if documento.Acesso.DataHoraEmissaoDPS != "-" {
		t.Fatalf("dataHoraEmissaoDPS vazio: esperado %q, obteve %q", "-", documento.Acesso.DataHoraEmissaoDPS)
	}
	// CodigoQR não é normalizado (campo de barcode)
	if documento.Acesso.CodigoQR != "https://qrcode.test" {
		t.Fatalf("codigoQR preservado: esperado %q, obteve %q", "https://qrcode.test", documento.Acesso.CodigoQR)
	}
}

func ptrFloatRef(v float64) *float64 {
	return &v
}

func TestApresentadorDanfse_AcessoDatasFormatadas(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Id:     "NFS123456789",
			Qrcode: "https://qrcode.test",
			NNFSe:  "42",
			DhProc: "2026-07-14T10:00:00-03:00",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					NDPS:    "10",
					Serie:   "S1",
					DhEmi:   "2026-07-14T09:00:00-03:00",
					DCompet: "2026-07-14",
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Acesso.Competencia != "14/07/2026" {
		t.Fatalf("competencia formatada: esperado %q, obteve %q", "14/07/2026", documento.Acesso.Competencia)
	}
	if documento.Acesso.DataHoraEmissaoNFSe != "14/07/2026 10:00:00" {
		t.Fatalf("dataHoraEmissaoNFSe formatada: esperado %q, obteve %q", "14/07/2026 10:00:00", documento.Acesso.DataHoraEmissaoNFSe)
	}
	if documento.Acesso.DataHoraEmissaoDPS != "14/07/2026 09:00:00" {
		t.Fatalf("dataHoraEmissaoDPS formatada: esperado %q, obteve %q", "14/07/2026 09:00:00", documento.Acesso.DataHoraEmissaoDPS)
	}
}

func TestApresentadorDanfse_AcessoDatasVazias(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					DCompet: "",
					DhEmi:   "",
				},
			},
			DhProc: "",
		},
	}
	documento := p.Apresentar(nota)

	if documento.Acesso.Competencia != "-" {
		t.Fatalf("competencia vazia: esperado %q, obteve %q", "-", documento.Acesso.Competencia)
	}
	if documento.Acesso.DataHoraEmissaoNFSe != "-" {
		t.Fatalf("dataHoraEmissaoNFSe vazia: esperado %q, obteve %q", "-", documento.Acesso.DataHoraEmissaoNFSe)
	}
	if documento.Acesso.DataHoraEmissaoDPS != "-" {
		t.Fatalf("dataHoraEmissaoDPS vazia: esperado %q, obteve %q", "-", documento.Acesso.DataHoraEmissaoDPS)
	}
}

func TestApresentadorDanfse_ServicoPaisBR(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoDescServ: "Serviço",
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Serv: models.Servico{
						LocPrest: models.LocPrest{
							UF:             "SP",
							CPaisPrestacao: "BR",
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !strings.Contains(documento.Servico.LocalPrestacaoUF, "BR") {
		t.Fatalf("localPrestacaoUF deveria conter BR, obteve %q", documento.Servico.LocalPrestacaoUF)
	}
}

func TestApresentadorDanfse_ServicoPaisUS(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoDescServ: "Serviço",
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Serv: models.Servico{
						LocPrest: models.LocPrest{
							UF:             "",
							CPaisPrestacao: "us",
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !strings.Contains(documento.Servico.LocalPrestacaoUF, "US") {
		t.Fatalf("localPrestacaoUF deveria conter US (uppercase), obteve %q", documento.Servico.LocalPrestacaoUF)
	}
}

func TestApresentadorDanfse_ServicoPaisFallback(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoDescServ: "Serviço",
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Serv: models.Servico{
						LocPrest: models.LocPrest{
							UF:             "SP",
							CPaisPrestacao: "",
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !strings.Contains(documento.Servico.LocalPrestacaoUF, "BR") {
		t.Fatalf("localPrestacaoUF deveria conter BR (fallback), obteve %q", documento.Servico.LocalPrestacaoUF)
	}
}

func TestApresentadorDanfse_ServicoCTribNacFormatado(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoDescServ: "Serviço",
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Serv: models.Servico{
						CServ: models.CServ{
							CTribNac: "010101",
							CTribMun: "001",
						},
						LocPrest: models.LocPrest{
							UF:             "SP",
							CPaisPrestacao: "BR",
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Servico.CodigoTribNacMun != "01.01.01 / 001" {
		t.Fatalf("codigoTribNacMun formatado: esperado %q, obteve %q", "01.01.01 / 001", documento.Servico.CodigoTribNacMun)
	}
}

func TestApresentadorDanfse_ServicoXTribMunPrioridade(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoDescServ: "Serviço",
		InfNFSe: models.InfNFSe{
			XTribMun: "Tributação municipal",
			XTribNac: "Tributação nacional",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Serv: models.Servico{
						LocPrest: models.LocPrest{
							UF:             "SP",
							CPaisPrestacao: "BR",
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Servico.XTribNac != "Tributação municipal" {
		t.Fatalf("xTribNac deveria usar xTribMun (prioridade): esperado %q, obteve %q", "Tributação municipal", documento.Servico.XTribNac)
	}
}

func TestApresentadorDanfse_ServicoXTribNacFallback(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		ExibicaoDescServ: "Serviço",
		InfNFSe: models.InfNFSe{
			XTribNac: "Tributação nacional",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Serv: models.Servico{
						LocPrest: models.LocPrest{
							UF:             "SP",
							CPaisPrestacao: "BR",
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Servico.XTribNac != "Tributação nacional" {
		t.Fatalf("xTribNac fallback: esperado %q, obteve %q", "Tributação nacional", documento.Servico.XTribNac)
	}
}

func TestApresentadorDanfse_TribMunPaisResult(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			XLocIncid: "São Paulo",
			XUFIncid:  "SP",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0},
								BM:          models.BM{VRedBCBM: 0},
								CPaisResult: "BR",
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !strings.Contains(documento.Tributacoes.Municipal.MunicipioUFPAISIncid, "BR") {
		t.Fatalf("MunicipioUFPAISIncid deveria conter BR, obteve %q", documento.Tributacoes.Municipal.MunicipioUFPAISIncid)
	}
}

func TestApresentadorDanfse_TribMunPaisResultVazioFallback(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			XLocIncid: "São Paulo",
			XUFIncid:  "SP",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribMun: &models.TribMun{
								TribISSQN:   1,
								TpRetISSQN:  1,
								TpImunidade: 0,
								ExigSusp:    models.ExigSusp{TpSusp: 0},
								BM:          models.BM{VRedBCBM: 0},
								CPaisResult: "",
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if !strings.Contains(documento.Tributacoes.Municipal.MunicipioUFPAISIncid, "BR") {
		t.Fatalf("MunicipioUFPAISIncid deveria conter BR (fallback), obteve %q", documento.Tributacoes.Municipal.MunicipioUFPAISIncid)
	}
}

func TestApresentadorDanfse_TotalIBSCBS_Populado(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			IBSCBS: &models.IBSCBS{
				TotCIBS: models.TotCIBS{
					GIBS:   models.GIBSTot{VIBSTot: 150},
					GCBS:   models.GCBSTot{VCBS: 250},
					VTotNF: 1200,
				},
			},
			Valores: models.Valores{
				VLiq: 900,
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						VServPrest: models.VServPrest{VServ: 1000},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	// TotalIBSCBS = VIBSTot(150) + VCBS(250) = 400
	if documento.Totais.TotalIBSCBS != "R$ 400,00" {
		t.Fatalf("TotalIBSCBS populado: esperado %q, obteve %q", "R$ 400,00", documento.Totais.TotalIBSCBS)
	}
	// ValorLiquidoIBSCBS = VTotNF(1200)
	if documento.Totais.ValorLiquidoIBSCBS != "R$ 1.200,00" {
		t.Fatalf("ValorLiquidoIBSCBS populado: esperado %q, obteve %q", "R$ 1.200,00", documento.Totais.ValorLiquidoIBSCBS)
	}
	// ValorLiquido e ValorOperacaoServico permanecem inalterados
	if documento.Totais.ValorLiquido != "R$ 900,00" {
		t.Fatalf("ValorLiquido preservado: esperado %q, obteve %q", "R$ 900,00", documento.Totais.ValorLiquido)
	}
	if documento.Totais.ValorOperacaoServico != "R$ 1.000,00" {
		t.Fatalf("ValorOperacaoServico preservado: esperado %q, obteve %q", "R$ 1.000,00", documento.Totais.ValorOperacaoServico)
	}
}

func TestApresentadorDanfse_TotalIBSCBS_ApenasIBS(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			IBSCBS: &models.IBSCBS{
				TotCIBS: models.TotCIBS{
					GIBS:   models.GIBSTot{VIBSTot: 300},
					GCBS:   models.GCBSTot{VCBS: 0},
					VTotNF: 500,
				},
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						VServPrest: models.VServPrest{VServ: 1000},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Totais.TotalIBSCBS != "R$ 300,00" {
		t.Fatalf("TotalIBSCBS apenas IBS: esperado %q, obteve %q", "R$ 300,00", documento.Totais.TotalIBSCBS)
	}
	if documento.Totais.ValorLiquidoIBSCBS != "R$ 500,00" {
		t.Fatalf("ValorLiquidoIBSCBS com VTotNF: esperado %q, obteve %q", "R$ 500,00", documento.Totais.ValorLiquidoIBSCBS)
	}
}

func TestApresentadorDanfse_TotalIBSCBS_ApenasCBS(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			IBSCBS: &models.IBSCBS{
				TotCIBS: models.TotCIBS{
					GIBS:   models.GIBSTot{VIBSTot: 0},
					GCBS:   models.GCBSTot{VCBS: 175},
					VTotNF: 825,
				},
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						VServPrest: models.VServPrest{VServ: 1000},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Totais.TotalIBSCBS != "R$ 175,00" {
		t.Fatalf("TotalIBSCBS apenas CBS: esperado %q, obteve %q", "R$ 175,00", documento.Totais.TotalIBSCBS)
	}
	if documento.Totais.ValorLiquidoIBSCBS != "R$ 825,00" {
		t.Fatalf("ValorLiquidoIBSCBS com VTotNF: esperado %q, obteve %q", "R$ 825,00", documento.Totais.ValorLiquidoIBSCBS)
	}
}

func TestApresentadorDanfse_TotalIBSCBS_VTotNFZero(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			IBSCBS: &models.IBSCBS{
				TotCIBS: models.TotCIBS{
					GIBS:   models.GIBSTot{VIBSTot: 100},
					GCBS:   models.GCBSTot{VCBS: 200},
					VTotNF: 0,
				},
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						VServPrest: models.VServPrest{VServ: 1000},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Totais.TotalIBSCBS != "R$ 300,00" {
		t.Fatalf("TotalIBSCBS populado: esperado %q, obteve %q", "R$ 300,00", documento.Totais.TotalIBSCBS)
	}
	if documento.Totais.ValorLiquidoIBSCBS != "-" {
		t.Fatalf("ValorLiquidoIBSCBS VTotNF zero: esperado %q, obteve %q", "-", documento.Totais.ValorLiquidoIBSCBS)
	}
}

func TestApresentadorDanfse_AcessoTipoEmitenteSituacao(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			CStat: "100",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					TpEmit: 1,
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Acesso.TipoEmitente != "Prestador" {
		t.Fatalf("tipoEmitente: esperado %q, obteve %q", "Prestador", documento.Acesso.TipoEmitente)
	}
	if documento.Acesso.Situacao != "NFS-e Regular" {
		t.Fatalf("situacao: esperado %q, obteve %q", "NFS-e Regular", documento.Acesso.Situacao)
	}
}

func TestApresentadorDanfse_AcessoFinalidadeExplicitoZero(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	finNFSe := 0
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			CStat: "100",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					TpEmit: 1,
					IBSCBS: &models.IBSCBSDPS{
						FinNFSe: &finNFSe,
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Acesso.Finalidade != "NFS-e regular" {
		t.Fatalf("finalidade explicito 0: esperado %q, obteve %q", "NFS-e regular", documento.Acesso.Finalidade)
	}
}

func TestApresentadorDanfse_AcessoFinalidadeAusente(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			CStat: "100",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					TpEmit: 1,
					IBSCBS: &models.IBSCBSDPS{
						FinNFSe: nil,
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Acesso.Finalidade != "-" {
		t.Fatalf("finalidade ausente: esperado %q, obteve %q", "-", documento.Acesso.Finalidade)
	}
}

func TestApresentadorDanfse_AcessoFinalidadeIBSCBSNil(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			CStat: "100",
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					TpEmit: 1,
					IBSCBS: nil,
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Acesso.Finalidade != "-" {
		t.Fatalf("finalidade IBSCBS nil: esperado %q, obteve %q", "-", documento.Acesso.Finalidade)
	}
}

func TestApresentadorDanfse_EmitenteSimplesNacionalRegimeApuracao(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Emit: models.Emitente{
				CNPJ:  "12345678000195",
				XNome: "Empresa Teste",
				EnderNac: models.Endereco{
					XMun: "São Paulo", UF: "SP", CMun: "3550308", CEP: "01001000",
					XLgr: "Rua Teste", Nro: "123", XBairro: "Centro",
				},
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					TpEmit: 1,
					Prest: &models.Prestador{
						RegTrib: models.RegTrib{
							OpSimpNac:   2,
							RegApTribSN: 1,
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Partes.Emitente.SimplesNacional != "2 - Optante - MEI" {
		t.Fatalf("simplesNacional emitente: esperado %q, obteve %q", "2 - Optante - MEI", documento.Partes.Emitente.SimplesNacional)
	}
	if documento.Partes.Emitente.RegimeApuracaoSN != "1 - Regime de apuração dos tributos federais e municipal pelo SN" {
		t.Fatalf("regimeApuracaoSN emitente: esperado %q, obteve %q", "1 - Regime de apuração dos tributos federais e municipal pelo SN", documento.Partes.Emitente.RegimeApuracaoSN)
	}
}

func TestApresentadorDanfse_EmitenteNilPrestFallback(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Emit: models.Emitente{
				CNPJ:  "12345678000195",
				XNome: "Empresa Teste",
				EnderNac: models.Endereco{
					XMun: "São Paulo", UF: "SP", CMun: "3550308", CEP: "01001000",
					XLgr: "Rua Teste", Nro: "123", XBairro: "Centro",
				},
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					TpEmit: 1,
					Prest:  nil,
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Partes.Emitente.SimplesNacional != "-" {
		t.Fatalf("simplesNacional nil Prest: esperado %q, obteve %q", "-", documento.Partes.Emitente.SimplesNacional)
	}
	if documento.Partes.Emitente.RegimeApuracaoSN != "-" {
		t.Fatalf("regimeApuracaoSN nil Prest: esperado %q, obteve %q", "-", documento.Partes.Emitente.RegimeApuracaoSN)
	}
}

func TestApresentadorDanfse_PartesNaoEmitenteSemSimplesNacional(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			Emit: models.Emitente{
				CNPJ:  "12345678000195",
				XNome: "Empresa Teste",
				EnderNac: models.Endereco{
					XMun: "São Paulo", UF: "SP", CMun: "3550308", CEP: "01001000",
					XLgr: "Rua Teste", Nro: "123", XBairro: "Centro",
				},
			},
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					TpEmit: 1,
					Prest: &models.Prestador{
						RegTrib: models.RegTrib{
							OpSimpNac:   2,
							RegApTribSN: 1,
						},
					},
					Toma: &models.Tomador{
						CNPJ:  "98765432000199",
						XNome: "Tomador Teste",
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	// Emitente deve ter os campos populados
	if documento.Partes.Emitente.SimplesNacional != "2 - Optante - MEI" {
		t.Fatalf("emitente simplesNacional: esperado %q, obteve %q", "2 - Optante - MEI", documento.Partes.Emitente.SimplesNacional)
	}
	// Tomador NAO deve ter os campos populados (deve ser zero value "-")
	if documento.Partes.Tomador.SimplesNacional != "" {
		t.Fatalf("tomador simplesNacional: esperado vazio, obteve %q", documento.Partes.Tomador.SimplesNacional)
	}
	if documento.Partes.Tomador.RegimeApuracaoSN != "" {
		t.Fatalf("tomador regimeApuracaoSN: esperado vazio, obteve %q", documento.Partes.Tomador.RegimeApuracaoSN)
	}
}

// --- NT-008 v1.02: tpRetPisCofins=1 rules ---

func TestApresentadorDanfse_TpRetPisCofins1_SomaContribSocRet(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					DCompet: "2025-06-15",
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribFed: &models.TribFed{
								VRetIRRF: 50,
								VRetCP:   100,
								VRetCSLL: 30,
								PisCofins: &models.PisCofins{
									VPis:           10,
									VCofins:        20,
									TpRetPisCofins: 1,
								},
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	// ContribSocRet = vRetCSLL(30) + vPis(10) + vCofins(20) = 60
	if documento.Tributacoes.Federal.ContribSocRet != "R$ 60,00" {
		t.Fatalf("ContribSocRet com tpRet=1: esperado %q, obteve %q", "R$ 60,00", documento.Tributacoes.Federal.ContribSocRet)
	}
}

func TestApresentadorDanfse_TpRetPisCofins1_PisCofinsZero(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					DCompet: "2025-06-15",
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribFed: &models.TribFed{
								PisCofins: &models.PisCofins{
									VPis:           10,
									VCofins:        20,
									TpRetPisCofins: 1,
								},
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	if documento.Tributacoes.Federal.PISDebito != "R$ 0,00" {
		t.Fatalf("PISDebito com tpRet=1: esperado %q, obteve %q", "R$ 0,00", documento.Tributacoes.Federal.PISDebito)
	}
	if documento.Tributacoes.Federal.COFINSDebito != "R$ 0,00" {
		t.Fatalf("COFINSDebito com tpRet=1: esperado %q, obteve %q", "R$ 0,00", documento.Tributacoes.Federal.COFINSDebito)
	}
}

func TestApresentadorDanfse_TpRetPisCofinsNao1_PreservaValores(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()
	nota := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					DCompet: "2025-06-15",
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribFed: &models.TribFed{
								PisCofins: &models.PisCofins{
									VPis:           10,
									VCofins:        20,
									TpRetPisCofins: 0,
								},
							},
						},
					},
				},
			},
		},
	}
	documento := p.Apresentar(nota)

	// tpRetPisCofins != 1 → valores originais preservados
	if documento.Tributacoes.Federal.PISDebito != "R$ 10,00" {
		t.Fatalf("PISDebito tpRet=0: esperado %q, obteve %q", "R$ 10,00", documento.Tributacoes.Federal.PISDebito)
	}
	if documento.Tributacoes.Federal.COFINSDebito != "R$ 20,00" {
		t.Fatalf("COFINSDebito tpRet=0: esperado %q, obteve %q", "R$ 20,00", documento.Tributacoes.Federal.COFINSDebito)
	}
	// ContribSocRet não deve ser alterada (usa vRetCSLL original)
	if documento.Tributacoes.Federal.ContribSocRet != "-" {
		t.Fatalf("ContribSocRet tpRet=0 sem vRetCSLL: esperado %q, obteve %q", "-", documento.Tributacoes.Federal.ContribSocRet)
	}
}

func TestApresentadorDanfse_ExibirPisCofins_Boundary2026(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()

	// 2026-12-31 → deve exibir
	nota2026 := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					DCompet: "2026-12-31",
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribFed: &models.TribFed{
								PisCofins: &models.PisCofins{VPis: 5, VCofins: 10, TpRetPisCofins: 0},
							},
						},
					},
				},
			},
		},
	}
	doc2026 := p.Apresentar(nota2026)
	if !doc2026.Tributacoes.Federal.ExibirPisCofins {
		t.Fatal("ExibirPisCofins deve ser true para 2026-12-31")
	}
	if doc2026.Tributacoes.Federal.PISDebito != "R$ 5,00" {
		t.Fatalf("PISDebito 2026-12-31: esperado %q, obteve %q", "R$ 5,00", doc2026.Tributacoes.Federal.PISDebito)
	}

	// 2027-01-01 → não deve exibir
	nota2027 := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					DCompet: "2027-01-01",
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribFed: &models.TribFed{
								PisCofins: &models.PisCofins{VPis: 5, VCofins: 10, TpRetPisCofins: 0},
							},
						},
					},
				},
			},
		},
	}
	doc2027 := p.Apresentar(nota2027)
	if doc2027.Tributacoes.Federal.ExibirPisCofins {
		t.Fatal("ExibirPisCofins deve ser false para 2027-01-01")
	}
}

func TestApresentadorDanfse_ExibirPisCofins_EmptyInvalid(t *testing.T) {
	p := NovoServicoApresentacaoDanfse()

	// dCompet vazio → deve exibir (compatibilidade)
	notaVazio := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					DCompet: "",
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribFed: &models.TribFed{
								PisCofins: &models.PisCofins{VPis: 5, VCofins: 10, TpRetPisCofins: 0},
							},
						},
					},
				},
			},
		},
	}
	docVazio := p.Apresentar(notaVazio)
	if !docVazio.Tributacoes.Federal.ExibirPisCofins {
		t.Fatal("ExibirPisCofins deve ser true para dCompet vazio")
	}

	// dCompet inválido → deve exibir (compatibilidade)
	notaInvalida := models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					DCompet: "data-invalida",
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TribFed: &models.TribFed{
								PisCofins: &models.PisCofins{VPis: 5, VCofins: 10, TpRetPisCofins: 0},
							},
						},
					},
				},
			},
		},
	}
	docInvalida := p.Apresentar(notaInvalida)
	if !docInvalida.Tributacoes.Federal.ExibirPisCofins {
		t.Fatal("ExibirPisCofins deve ser true para dCompet inválido")
	}
}
