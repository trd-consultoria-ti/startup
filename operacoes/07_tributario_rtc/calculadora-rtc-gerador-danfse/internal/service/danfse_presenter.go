package service

import (
	"strings"
	"time"

	"danfse-service/internal/domain/models"
	"danfse-service/internal/domain/presentation"
)

// ServicoApresentacaoDanfse é um apresentador puro sem I/O, banco de dados, XML
// ou dependências Folio. Recebe um models.NFSe totalmente enriquecido e retorna
// um presentation.DocumentoDanfse pronto para renderização.
type ServicoApresentacaoDanfse struct{}

func NovoServicoApresentacaoDanfse() *ServicoApresentacaoDanfse {
	return &ServicoApresentacaoDanfse{}
}

func (p *ServicoApresentacaoDanfse) Apresentar(nota models.NFSe) presentation.DocumentoDanfse {
	documento := presentation.DocumentoDanfse{}

	// ---- Marca d'água ----
	documento.MarcaDagua = p.montarMarcaDagua(nota)

	// ---- Cabeçalho ----
	documento.Cabecalho = p.montarCabecalho(nota)

	// ---- Acesso ----
	documento.Acesso = p.montarAcesso(nota)

	// ---- Partes ----
	documento.Partes = p.montarPartes(nota)

	// ---- Serviço ----
	documento.Servico = p.montarServico(nota)

	// ---- Tributação ----
	documento.Tributacoes.Municipal = p.montarTributacaoMunicipal(nota)
	documento.Tributacoes.Federal = p.montarTributacaoFederal(nota)
	documento.Tributacoes.IBSCBS = p.montarTributacaoIBSCBS(nota)

	// ---- Totais ----
	documento.Totais = p.montarTotais(nota)

	// ---- Informações complementares ----
	documento.InformacoesComplementares = p.montarInformacoesComplementares(nota)

	// ---- Canhoto ----
	documento.Canhoto = presentation.DadosCanhoto{Visivel: true}

	return documento
}

// ---- Auxiliares ----

func (p *ServicoApresentacaoDanfse) montarMarcaDagua(nota models.NFSe) presentation.MarcaDagua {
	return presentation.MarcaDagua{
		Texto:        nota.MarcaDagua,
		TamanhoFonte: 60,
		ColorR:       0.35,
		ColorG:       0.35,
		ColorB:       0.35,
		Angulo:       45,
		Opacidade:    0.35,
	}
}

func (p *ServicoApresentacaoDanfse) montarCabecalho(nota models.NFSe) presentation.DadosCabecalho {
	return presentation.DadosCabecalho{
		MarcaDagua:            nota.MarcaDagua,
		ExibicaoDescServ:      nota.ExibicaoDescServ,
		ExibicaoCompl:         nota.ExibicaoCompl,
		ExibicaoLinhaTributos: nota.ExibicaoLinhaTributos,
		Municipio:             nota.InfNFSe.Emit.EnderNac.XMun,
		UF:                    nota.InfNFSe.Emit.EnderNac.UF,
		AmbienteGerador:       presentation.MapearAmbienteGerador(nota.InfNFSe.AmbGer),
		TipoAmbiente:          presentation.MapearTipoAmbiente(nota.InfNFSe.DPS.InfDPS.TpAmb),
	}
}

func (p *ServicoApresentacaoDanfse) montarAcesso(nota models.NFSe) presentation.DadosAcesso {
	dps := nota.InfNFSe.DPS.InfDPS
	tipoEmitente := presentation.MapearTipoEmitente(dps.TpEmit)
	situacao := presentation.MapearCStat(nota.InfNFSe.CStat)
	finalidade := "-"
	if dps.IBSCBS != nil && dps.IBSCBS.FinNFSe != nil {
		finalidade = presentation.MapearFinalidadeNFSe(*dps.IBSCBS.FinNFSe)
	}
	return presentation.DadosAcesso{
		ChaveAcesso:         presentation.TracoSeVazio(strings.ReplaceAll(nota.InfNFSe.Id, "NFS", "")),
		CodigoQR:            nota.InfNFSe.Qrcode,
		NumeroNFSe:          presentation.TracoSeVazio(nota.InfNFSe.NNFSe),
		Competencia:         presentation.FormatarData(dps.DCompet),
		DataHoraEmissaoNFSe: presentation.FormatarDataHora(nota.InfNFSe.DhProc),
		NumeroDPS:           presentation.TracoSeVazio(dps.NDPS),
		SerieDPS:            presentation.TracoSeVazio(dps.Serie),
		DataHoraEmissaoDPS:  presentation.FormatarDataHora(dps.DhEmi),
		TipoEmitente:        tipoEmitente,
		Situacao:            situacao,
		Finalidade:          finalidade,
	}
}

func (p *ServicoApresentacaoDanfse) montarPartes(nota models.NFSe) presentation.DadosPartes {
	dps := nota.InfNFSe.DPS.InfDPS
	dadosPartes := presentation.DadosPartes{}

	// Emitente
	dadosPartes.Emitente = p.montarEmitente(nota, nota.InfNFSe.Emit)

	// Tomador
	if dps.Toma != nil {
		dadosPartes.Tomador = p.montarTomador(*dps.Toma)
	} else {
		dadosPartes.Tomador = presentation.SecaoParte{
			Visivel:           false,
			MensagemSupressao: "TOMADOR/ADQUIRENTE DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e",
		}
	}

	// Destinatário
	destinatario := p.montarDestinatario(nota)
	if destinatario.Visivel {
		dadosPartes.Destinatario = destinatario
	} else if destinatario.MensagemSupressao != "" {
		dadosPartes.Destinatario = destinatario
	} else {
		dadosPartes.Destinatario = presentation.SecaoParte{
			Visivel:           false,
			MensagemSupressao: "DESTINATÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e",
		}
	}

	// Intermediário
	if dps.Interm != nil {
		dadosPartes.Intermediario = p.montarIntermediario(*dps.Interm)
	} else {
		dadosPartes.Intermediario = presentation.SecaoParte{
			Visivel:           false,
			MensagemSupressao: "INTERMEDIÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e",
		}
	}

	return dadosPartes
}

func (p *ServicoApresentacaoDanfse) montarEmitente(nota models.NFSe, e models.Emitente) presentation.SecaoParte {
	dps := nota.InfNFSe.DPS.InfDPS
	simplesNacional := "-"
	regimeApuracaoSN := "-"
	if dps.Prest != nil {
		simplesNacional = presentation.MapearOpcaoSimplesNacional(dps.Prest.RegTrib.OpSimpNac)
		regimeApuracaoSN = presentation.MapearRegimeApuracaoSN(dps.Prest.RegTrib.RegApTribSN)
	}
	return presentation.SecaoParte{
		Titulo:             "PRESTADOR / FORNECEDOR",
		CNPJCPFNIF:         presentation.TracoSeVazio(presentation.FormatarDocumento(presentation.PrimeiroNaoVazio(e.CNPJ, e.CPF, e.NIF))),
		InscricaoMunicipal: presentation.TracoSeVazio(e.IM),
		Telefone:           presentation.TracoSeVazio(presentation.FormatarTelefone(e.Fone)),
		Nome:               presentation.TracoSeVazio(e.XNome),
		MunicipioUF:        presentation.FormatarMunicipioUF(e.EnderNac.XMun, e.EnderNac.UF),
		CodigoIBGECEP:      presentation.FormatarCodigoIBGECEP(e.EnderNac.CMun, e.EnderNac.CEP),
		Endereco:           presentation.TracoSeVazio(presentation.MontarEndereco(e.EnderNac.XLgr, e.EnderNac.Nro, e.EnderNac.XCpl, e.EnderNac.XBairro)),
		Email:              presentation.TracoSeVazio(e.Email),
		Visivel:            true,
		SimplesNacional:    simplesNacional,
		RegimeApuracaoSN:   regimeApuracaoSN,
	}
}

func (p *ServicoApresentacaoDanfse) montarTomador(t models.Tomador) presentation.SecaoParte {
	municipioUF := "-"
	ibgeCep := "-"
	endereco := "-"
	if t.End != nil && t.End.EndNac != nil {
		municipioUF = presentation.FormatarMunicipioUF(t.End.EndNac.XMun, t.End.EndNac.UF)
		ibgeCep = presentation.FormatarCodigoIBGECEP(t.End.EndNac.CMun, t.End.EndNac.CEP)
	}
	if t.End != nil {
		endereco = presentation.TracoSeVazio(presentation.MontarEndereco(t.End.XLgr, t.End.Nro, t.End.XCpl, t.End.XBairro))
	}
	return presentation.SecaoParte{
		Titulo:             "TOMADOR / ADQUIRENTE",
		CNPJCPFNIF:         presentation.TracoSeVazio(presentation.FormatarDocumento(presentation.PrimeiroNaoVazio(t.CNPJ, t.CPF, t.NIF))),
		InscricaoMunicipal: presentation.TracoSeVazio(t.IM),
		Telefone:           presentation.TracoSeVazio(presentation.FormatarTelefone(t.Fone)),
		Nome:               presentation.TracoSeVazio(t.XNome),
		MunicipioUF:        municipioUF,
		CodigoIBGECEP:      ibgeCep,
		Endereco:           endereco,
		Email:              presentation.TracoSeVazio(t.Email),
		Visivel:            true,
	}
}

func (p *ServicoApresentacaoDanfse) montarDestinatario(nota models.NFSe) presentation.SecaoParte {
	dps := nota.InfNFSe.DPS.InfDPS
	possuiDestinatario := dps.IBSCBS != nil && dps.IBSCBS.Dest != nil
	if !possuiDestinatario {
		return presentation.SecaoParte{Visivel: false}
	}
	destinatario := dps.IBSCBS.Dest

	// Check if destinatario == tomador
	if p.destinatarioEhTomador(nota) {
		return presentation.SecaoParte{
			Visivel:           false,
			MensagemSupressao: "O DESTINATÁRIO É O PRÓPRIO TOMADOR/ADQUIRENTE DA OPERAÇÃO",
		}
	}

	municipioUF := "-"
	ibgeCep := "-"
	endereco := "-"
	if destinatario.End != nil && destinatario.End.EndNac != nil {
		municipioUF = presentation.FormatarMunicipioUF(destinatario.End.EndNac.XMun, destinatario.End.EndNac.UF)
		ibgeCep = presentation.FormatarCodigoIBGECEP(destinatario.End.EndNac.CMun, destinatario.End.EndNac.CEP)
	}
	if destinatario.End != nil {
		endereco = presentation.TracoSeVazio(presentation.MontarEndereco(destinatario.End.XLgr, destinatario.End.Nro, destinatario.End.XCpl, destinatario.End.XBairro))
	}
	return presentation.SecaoParte{
		Titulo:        "DESTINATÁRIO DA OPERAÇÃO",
		CNPJCPFNIF:    presentation.TracoSeVazio(presentation.FormatarDocumento(presentation.PrimeiroNaoVazio(destinatario.CNPJ, destinatario.CPF, destinatario.NIF))),
		Telefone:      presentation.TracoSeVazio(presentation.FormatarTelefone(destinatario.Fone)),
		Nome:          presentation.TracoSeVazio(destinatario.XNome),
		MunicipioUF:   municipioUF,
		CodigoIBGECEP: ibgeCep,
		Endereco:      endereco,
		Email:         presentation.TracoSeVazio(destinatario.Email),
		Visivel:       true,
	}
}

func (p *ServicoApresentacaoDanfse) montarIntermediario(i models.Interm) presentation.SecaoParte {
	municipioUF := "-"
	ibgeCep := "-"
	endereco := "-"
	if i.End.EndNac != nil {
		municipioUF = presentation.FormatarMunicipioUF(i.End.EndNac.XMun, i.End.EndNac.UF)
		ibgeCep = presentation.FormatarCodigoIBGECEP(i.End.EndNac.CMun, i.End.EndNac.CEP)
	}
	endereco = presentation.TracoSeVazio(presentation.MontarEndereco(i.End.XLgr, i.End.Nro, i.End.XCpl, i.End.XBairro))
	return presentation.SecaoParte{
		Titulo:             "INTERMEDIÁRIO DA OPERAÇÃO",
		CNPJCPFNIF:         presentation.TracoSeVazio(presentation.FormatarDocumento(presentation.PrimeiroNaoVazio(i.CNPJ, i.CPF, i.NIF))),
		InscricaoMunicipal: presentation.TracoSeVazio(i.IM),
		Telefone:           presentation.TracoSeVazio(presentation.FormatarTelefone(i.Fone)),
		Nome:               presentation.TracoSeVazio(i.XNome),
		MunicipioUF:        municipioUF,
		CodigoIBGECEP:      ibgeCep,
		Endereco:           endereco,
		Email:              presentation.TracoSeVazio(i.Email),
		Visivel:            true,
	}
}

func (p *ServicoApresentacaoDanfse) montarServico(nota models.NFSe) presentation.DadosServico {
	dps := nota.InfNFSe.DPS.InfDPS
	cs := dps.Serv.CServ
	return presentation.DadosServico{
		CodigoTribNacMun: codigoTributacaoNacionalOuMunicipal(cs.CTribNac, cs.CTribMun),
		CodigoNBS:        presentation.TracoSeVazio(presentation.FormatarNBS(cs.CNBS)),
		LocalPrestacaoUF: p.combinarComBarra(nota.InfNFSe.XLocPrestacao, dps.Serv.LocPrest.UF, p.codigoPaisOuBR(dps.Serv.LocPrest.CPaisPrestacao)),
		XTribNac:         presentation.TracoSeVazio(presentation.PrimeiroNaoVazio(nota.InfNFSe.XTribMun, nota.InfNFSe.XTribNac)),
		DescServ:         nota.ExibicaoDescServ,
		DescServOriginal: nota.TextoDescServOriginal,
	}
}

func codigoTributacaoNacionalOuMunicipal(nacional, municipal string) string {
	nacional = strings.TrimSpace(nacional)
	municipal = strings.TrimSpace(municipal)
	if nacional == "" {
		return municipal
	}
	if municipal == "" {
		return presentation.FormatarCodigoTributacaoNacional(nacional)
	}
	return presentation.FormatarCodigoTributacaoNacional(nacional) + " / " + municipal
}

func (p *ServicoApresentacaoDanfse) montarTributacaoMunicipal(nota models.NFSe) presentation.DadosTributacaoMunicipal {
	dps := nota.InfNFSe.DPS.InfDPS
	dadosTribMun := dps.Valores.Trib.TribMun
	if dadosTribMun == nil {
		dadosTribMun = &models.TribMun{}
	}

	// BM calculation: prefer nota.InfNFSe.Valores.VCalcBM, fallback to dadosTribMun.BM.VRedBCBM
	calculoBM := nota.InfNFSe.Valores.VCalcBM
	if calculoBM == 0 {
		calculoBM = dadosTribMun.BM.VRedBCBM
	}

	// Check if optional BM fields should be shown
	regEsp := 0
	if dps.Prest != nil {
		regEsp = dps.Prest.RegTrib.RegEspTrib
	}
	exibirOpcionais := nota.InfNFSe.Valores.TpBM > 0 ||
		nota.InfNFSe.Valores.VCalcBM > 0 ||
		dps.Valores.VDedRed.VDR > 0 ||
		dps.Valores.VDescCondIncond.VDescIncond > 0

	retorno := presentation.DadosTributacaoMunicipal{
		TribISSQN:            presentation.MapearTributacaoISSQN(dadosTribMun.TribISSQN),
		MunicipioUFPAISIncid: p.combinarComBarra(nota.InfNFSe.XLocIncid, nota.InfNFSe.XUFIncid, p.codigoPaisOuBR(dadosTribMun.CPaisResult)),
		RegEspTrib:           presentation.MapearRegimeEspecialTributacao(regEsp),
		TpImunidade:          presentation.MapearTipoImunidade(dadosTribMun.TpImunidade),
		SuspensaoExig:        presentation.MapearSuspensaoExigibilidade(dadosTribMun.ExigSusp.TpSusp),
		NProcessoSusp:        presentation.TracoSeVazio(dadosTribMun.ExigSusp.NProcesso),
		BeneficioMunicipal:   presentation.MapearTipoBeneficioMunicipal(nota.InfNFSe.Valores.TpBM),
		CalculoBM:            presentation.FormatarValorMonetarioOuTraco(calculoBM),
		TotalDeducoes:        presentation.FormatarValorMonetarioOuTraco(dps.Valores.VDedRed.VDR),
		DescontoIncond:       presentation.FormatarValorMonetarioOuTraco(dps.Valores.VDescCondIncond.VDescIncond),
		BCISSQN:              presentation.FormatarValorMonetarioOuTraco(nota.InfNFSe.Valores.VBC),
		AliqAplic:            presentation.FormatarNumeroPercentualOuTraco(ptrFloat(nota.InfNFSe.Valores.PAliqAplic)),
		RetISSQN:             presentation.MapearTipoRetencaoISSQN(dadosTribMun.TpRetISSQN),
		ISSQNApurado:         presentation.FormatarValorMonetarioOuTraco(nota.InfNFSe.Valores.VISSQN),
		Visivel:              true,
	}

	if !exibirOpcionais {
		retorno.BeneficioMunicipal = "-"
		retorno.CalculoBM = "-"
		retorno.TotalDeducoes = "-"
		retorno.DescontoIncond = "-"
	}

	if !temDadosTributacaoMunicipal(nota, dadosTribMun, regEsp, calculoBM) {
		retorno.Visivel = false
		retorno.MensagemSupressao = "TRIBUTAÇÃO MUNICIPAL (ISSQN) - OPERAÇÃO NÃO SUJEITA AO ISSQN"
	}

	return retorno
}

func temDadosTributacaoMunicipal(nota models.NFSe, trib *models.TribMun, regEsp int, calculoBM float64) bool {
	if trib == nil {
		return false
	}
	return trib.TribISSQN != 0 ||
		strings.TrimSpace(nota.InfNFSe.XLocIncid) != "" ||
		strings.TrimSpace(nota.InfNFSe.XUFIncid) != "" ||
		strings.TrimSpace(trib.CPaisResult) != "" ||
		regEsp != 0 ||
		trib.TpImunidade != 0 ||
		trib.ExigSusp.TpSusp != 0 ||
		strings.TrimSpace(trib.ExigSusp.NProcesso) != "" ||
		nota.InfNFSe.Valores.TpBM != 0 ||
		calculoBM != 0 ||
		nota.InfNFSe.DPS.InfDPS.Valores.VDedRed.VDR != 0 ||
		nota.InfNFSe.DPS.InfDPS.Valores.VDescCondIncond.VDescIncond != 0 ||
		nota.InfNFSe.Valores.VBC != 0 ||
		nota.InfNFSe.Valores.PAliqAplic != nil ||
		trib.TpRetISSQN != 0 ||
		nota.InfNFSe.Valores.VISSQN != 0
}

func (p *ServicoApresentacaoDanfse) montarTributacaoFederal(nota models.NFSe) presentation.DadosTributacaoFederal {
	dps := nota.InfNFSe.DPS.InfDPS
	dadosTribFed := dps.Valores.Trib.TribFed
	if dadosTribFed == nil {
		dadosTribFed = &models.TribFed{}
	}

	retorno := presentation.DadosTributacaoFederal{
		IRRF:             presentation.FormatarValorMonetarioOuTraco(dadosTribFed.VRetIRRF),
		ContribPrevidRet: presentation.FormatarValorMonetarioOuTraco(dadosTribFed.VRetCP),
		ContribSocRet:    presentation.FormatarValorMonetarioOuTraco(dadosTribFed.VRetCSLL),
		PISDebito:        "-",
		COFINSDebito:     "-",
		DescContribRet:   "-",
		ExibirPisCofins:  p.exibirPisCofins(dps.DCompet),
		Visivel:          true,
	}

	if dadosTribFed.PisCofins != nil {
		tpRet := dadosTribFed.PisCofins.TpRetPisCofins
		if tpRet == 1 {
			// NT-008 v1.02 §2.4.5: when tpRetPisCofins=1, PIS/COFINS próprios = 0,00
			retorno.PISDebito = presentation.FormatarValorMonetario(0)
			retorno.COFINSDebito = presentation.FormatarValorMonetario(0)
			// Contribuições Sociais Retidas = vRetCSLL + vPis + vCofins
			sumCSLL := dadosTribFed.VRetCSLL + dadosTribFed.PisCofins.VPis + dadosTribFed.PisCofins.VCofins
			retorno.ContribSocRet = presentation.FormatarValorMonetario(sumCSLL)
		} else {
			retorno.PISDebito = presentation.FormatarValorMonetarioOuTraco(dadosTribFed.PisCofins.VPis)
			retorno.COFINSDebito = presentation.FormatarValorMonetarioOuTraco(dadosTribFed.PisCofins.VCofins)
		}
		retorno.DescContribRet = presentation.MapearTipoRetencaoPISCOFINS(tpRet)
	}

	return retorno
}

// exibirPisCofins returns true when the competence date is on or before 2026-12-31.
// Empty or invalid dCompet defaults to true for backward compatibility.
func (p *ServicoApresentacaoDanfse) exibirPisCofins(dCompet string) bool {
	if dCompet == "" {
		return true
	}
	t, err := time.Parse("2006-01-02", dCompet)
	if err != nil {
		return true
	}
	cutoff := time.Date(2026, 12, 31, 0, 0, 0, 0, time.UTC)
	return !t.After(cutoff)
}

func (p *ServicoApresentacaoDanfse) montarTributacaoIBSCBS(nota models.NFSe) presentation.DadosTributacaoIBSCBS {
	dps := nota.InfNFSe.DPS.InfDPS
	tributacaoIBSCBS := nota.InfNFSe.IBSCBS

	// When IBSCBS is nil (no IBS/CBS data in the NFSe), use a local empty
	// struct so that all field accesses produce placeholders instead of panicking.
	if tributacaoIBSCBS == nil {
		tributacaoIBSCBS = &models.IBSCBS{}
	}

	// DPS components (may be present even when NFSe IBSCBS is nil, but we only
	// use them when NFSe IBSCBS is also present).
	cst := "-"
	cClassTrib := "-"
	cIndOp := "-"
	if nota.InfNFSe.IBSCBS != nil && dps.IBSCBS != nil {
		cst = presentation.TracoSeVazio(dps.IBSCBS.Valores.Trib.GIBSCBS.CST)
		cClassTrib = presentation.TracoSeVazio(dps.IBSCBS.Valores.Trib.GIBSCBS.CClassTrib)
		cIndOp = presentation.TracoSeVazio(dps.IBSCBS.CIndOp)
	}

	localidade := presentation.TracoSeVazio(tributacaoIBSCBS.CLocalidadeIncid)
	nomeLocalidade := presentation.TracoSeVazio(tributacaoIBSCBS.XLocalidadeIncid)
	uf := presentation.TracoSeVazio(tributacaoIBSCBS.UF)

	dadosTributacaoIBSCBS := presentation.DadosTributacaoIBSCBS{
		CSTcClassTrib:            p.combinarValoresComBarra(cst, cClassTrib),
		IndOpCodIBGELocalidadeUF: p.combinarValoresComBarra(cIndOp, localidade, nomeLocalidade, uf),
		ExclusoesReducoesBC:      presentation.FormatarValorMonetarioOuTraco(nota.VExclusoesReducoesBC),
		BCPosExclusoes:           presentation.FormatarValorMonetarioOuTraco(tributacaoIBSCBS.Valores.VBC),
		RedAliqIBSRedAliqCBS: p.combinarValoresComBarra(
			presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.UF.PRedAliqUF)),
			presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.Mun.PRedAliqMun)),
			presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.Fed.PRedAliqCBS)),
		),
		AliqIBSUFIBSMun: p.combinarValoresComBarra(
			presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.UF.PIBSUF)),
			presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.Mun.PIBSMun)),
		),
		AliqEfetMunIBS: presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.Mun.PAliqEfetMun)),
		ValorMunIBS:    presentation.FormatarValorMonetarioOuTraco(ptrFloat(tributacaoIBSCBS.TotCIBS.GIBS.GIBSMunTot.VIBSMun)),
		AliqEfetEstIBS: presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.UF.PAliqEfetUF)),
		ValorEstIBS:    presentation.FormatarValorMonetarioOuTraco(ptrFloat(tributacaoIBSCBS.TotCIBS.GIBS.GIBSUFTot.VIBSUF)),
		ValorTotalIBS:  presentation.FormatarValorMonetarioOuTraco(tributacaoIBSCBS.TotCIBS.GIBS.VIBSTot),
		AliqCBS:        presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.Fed.PCBS)),
		AliqEfetCBS:    presentation.FormatarPercentualOuTraco(ptrFloat(tributacaoIBSCBS.Valores.Fed.PAliqEfetCBS)),
		ValorTotalCBS:  presentation.FormatarValorMonetarioOuTraco(tributacaoIBSCBS.TotCIBS.GCBS.VCBS),
		Visivel:        true,
	}

	return dadosTributacaoIBSCBS
}

func (p *ServicoApresentacaoDanfse) montarTotais(nota models.NFSe) presentation.DadosTotais {
	dps := nota.InfNFSe.DPS.InfDPS
	v := dps.Valores.VServPrest.VServ
	return presentation.DadosTotais{
		ValorOperacaoServico: presentation.FormatarValorMonetario(v),
		DescontoIncond:       presentation.FormatarValorMonetarioOuTraco(dps.Valores.VDescCondIncond.VDescIncond),
		DescontoCond:         presentation.FormatarValorMonetarioOuTraco(dps.Valores.VDescCondIncond.VDescCond),
		TotalRetencoes:       presentation.FormatarValorMonetarioOuTraco(nota.InfNFSe.Valores.VTotalRet),
		ValorLiquido:         presentation.FormatarValorMonetario(nota.InfNFSe.Valores.VLiq),
		TotalIBSCBS:          p.montarTotalIBSCBS(nota.InfNFSe.IBSCBS),
		ValorLiquidoIBSCBS:   p.montarValorLiquidoIBSCBS(nota.InfNFSe.IBSCBS),
	}
}

// montarTotalIBSCBS computes the sum of VIBSTot + VCBS from the IBSCBS totals.
// Returns "-" when IBSCBS is nil or when both values are zero.
func (p *ServicoApresentacaoDanfse) montarTotalIBSCBS(ibscbs *models.IBSCBS) string {
	if ibscbs == nil {
		return "-"
	}
	sum := ibscbs.TotCIBS.GIBS.VIBSTot + ibscbs.TotCIBS.GCBS.VCBS
	if sum == 0 {
		return "-"
	}
	return presentation.FormatarValorMonetarioOuTraco(sum)
}

// montarValorLiquidoIBSCBS returns VTotNF from the IBSCBS totals.
// Returns "-" when IBSCBS is nil or when VTotNF is zero.
func (p *ServicoApresentacaoDanfse) montarValorLiquidoIBSCBS(ibscbs *models.IBSCBS) string {
	if ibscbs == nil {
		return "-"
	}
	vTotNF := ibscbs.TotCIBS.VTotNF
	if vTotNF == 0 {
		return "-"
	}
	return presentation.FormatarValorMonetarioOuTraco(vTotNF)
}

func (p *ServicoApresentacaoDanfse) montarInformacoesComplementares(nota models.NFSe) presentation.DadosInformacoesComplementares {
	return presentation.DadosInformacoesComplementares{
		Conteudo:         nota.ExibicaoCompl,
		ConteudoOriginal: nota.TextoComplOriginal,
		LinhaTributos:    nota.ExibicaoLinhaTributos,
		Visivel:          true,
	}
}

// ---- pure helpers ----

// combinarComBarra applies TracoSeVazio to each component and joins with " / ".
func (p *ServicoApresentacaoDanfse) combinarComBarra(valores ...string) string {
	parts := make([]string, 0, len(valores))
	temInformacao := false
	for _, v := range valores {
		parte := presentation.TracoSeVazio(v)
		if parte != "-" {
			temInformacao = true
		}
		parts = append(parts, parte)
	}
	if !temInformacao {
		return "-"
	}
	return strings.Join(parts, " / ")
}

// combinarValoresComBarra joins already-formatted values with " / ", converting
// empty or whitespace-only components to "-" while preserving existing "-".
func (p *ServicoApresentacaoDanfse) combinarValoresComBarra(valores ...string) string {
	parts := make([]string, 0, len(valores))
	temInformacao := false
	for _, v := range valores {
		trimmed := strings.TrimSpace(v)
		if trimmed == "" {
			parts = append(parts, "-")
		} else {
			parts = append(parts, v)
			if trimmed != "-" {
				temInformacao = true
			}
		}
	}
	if !temInformacao {
		return "-"
	}
	return strings.Join(parts, " / ")
}

func (p *ServicoApresentacaoDanfse) destinatarioEhTomador(nota models.NFSe) bool {
	dps := nota.InfNFSe.DPS.InfDPS
	toma := dps.Toma
	if toma == nil {
		return false
	}
	destinatario := dps.IBSCBS.Dest
	if destinatario == nil {
		return false
	}
	for _, par := range [][2]string{{toma.CNPJ, destinatario.CNPJ}, {toma.CPF, destinatario.CPF}, {toma.NIF, destinatario.NIF}} {
		if strings.TrimSpace(par[0]) != "" && par[0] == par[1] {
			return true
		}
	}
	return false
}

func ptrFloat(v *float64) float64 {
	if v == nil {
		return 0
	}
	return *v
}

// codigoPaisOuBR trims, uppercases, and returns "BR" when empty.
func (p *ServicoApresentacaoDanfse) codigoPaisOuBR(s string) string {
	s = strings.TrimSpace(s)
	if s == "" {
		return "BR"
	}
	return strings.ToUpper(s)
}
