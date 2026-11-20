package service

import (
	"bytes"
	"context"
	"danfse-service/internal/domain/enums"
	"danfse-service/internal/domain/models"
	"danfse-service/internal/domain/utils/utils_string"
	"encoding/xml"
	"errors"
	"fmt"
	"log"
	"net/url"
	"strconv"
	"strings"
)

const (
	maxCaracteresDescricao = 1300
	maxCaracteresInfoCompl = 2000
	maxLinhasConteudo      = 22
	linhasPorParte         = 5
	linhasPorLinhaISSQN    = 2

	// consultaPublicaBaseURL is the fixed official base URL for public
	// consultation of NFS-e, used for all environments.
	consultaPublicaBaseURL = "https://www.nfse.gov.br"
)

// ServicoPreparacaoDanfse analisa e enriquece o modelo NFSe compartilhado por todos
// os renderizadores. Intencionalmente não possui preocupações com HTML ou PDF.
type ServicoPreparacaoDanfse struct {
	resolverMun ResolverMunicipio
	// maxInputSize é o limite máximo (em bytes) da entrada XML. Quando não
	// configurado explicitamente, usa MaxXMLInputSize como padrão. Valor
	// explícito inválido (zero, negativo ou acima de MaxXMLInputSizeLimit)
	// é rejeitado em Executar — nunca tratado como ilimitado.
	maxInputSize    int
	maxInputSizeSet bool
}

// ResolverMunicipio resolve nome e UF a partir do código IBGE do município.
type ResolverMunicipio func(codigo int) (nome, uf string, encontrado bool, err error)

func (g *ServicoPreparacaoDanfse) Executar(ctx context.Context, xmlData []byte, situacao enums.SituacaoNotaTipo) (models.NFSe, error) {
	var notaFiscal models.NFSe
	notaFiscal.MarcaDagua = situacao.MarcaDagua()

	// Resolve o limite de tamanho: padrão quando não configurado; valor explícito
	// sempre precisará ser válido (rejeitado antes de qualquer processamento).
	maxInputSize := MaxXMLInputSize
	if g.maxInputSizeSet {
		maxInputSize = g.maxInputSize
		if maxInputSize <= 0 || maxInputSize > MaxXMLInputSizeLimit {
			return models.NFSe{}, fmt.Errorf("invalid XML max input size %d bytes", maxInputSize)
		}
	}

	// Reject empty input.
	if len(bytes.TrimSpace(xmlData)) == 0 {
		return models.NFSe{}, errors.New("input XML is empty")
	}

	// Reject input over the maximum size.
	if len(xmlData) > maxInputSize {
		return models.NFSe{}, fmt.Errorf("input exceeds maximum size of %d bytes", maxInputSize)
	}

	xmlData = utils_strings.SanitizarAmpersand(xmlData)

	// Reject sanitized output over the same limit.
	if len(xmlData) > maxInputSize {
		return models.NFSe{}, fmt.Errorf("sanitized input exceeds maximum size of %d bytes", maxInputSize)
	}

	// Reject DOCTYPE / custom entity declarations before unmarshalling. This
	// runs after the legacy ampersand repair so valid legacy inputs retain their
	// existing behavior while declarations remain unchanged and are rejected.
	if err := RejectDTDAndEntities(xmlData); err != nil {
		return models.NFSe{}, err
	}

	if err := xml.Unmarshal(xmlData, &notaFiscal); err != nil {
		return models.NFSe{}, err
	}

	// Validate root element is NFSe.
	if notaFiscal.XMLName.Local != "NFSe" {
		return models.NFSe{}, fmt.Errorf("XML root element is %q, expected NFSe", notaFiscal.XMLName.Local)
	}

	// Validate Id is non-empty.
	if strings.TrimSpace(notaFiscal.InfNFSe.Id) == "" {
		return models.NFSe{}, errors.New("infNFSe Id is empty")
	}

	// Build the QR code URL using the fixed official base.
	// tpAmb still controls which notice environment is used, but the URL
	// base is always the same official endpoint.
	notaFiscal.InfNFSe.Qrcode = fmt.Sprintf("%s/ConsultaPublica/?tpc=1&chave=%s",
		consultaPublicaBaseURL,
		url.QueryEscape(strings.ReplaceAll(notaFiscal.InfNFSe.Id, "NFS", "")),
	)

	g.enriquecerMunicipios(&notaFiscal)
	g.calcularExclusoesReducoesBC(&notaFiscal)
	g.calcularLinhas(&notaFiscal)

	return notaFiscal, nil
}

func (g *ServicoPreparacaoDanfse) enriquecerMunicipios(notaFiscal *models.NFSe) {
	if nome, uf, encontrado := g.resolverMunicipio(notaFiscal.InfNFSe.CLocIncid); encontrado {
		notaFiscal.InfNFSe.XLocIncid = nome
		notaFiscal.InfNFSe.XUFIncid = uf
	}

	if nome, uf, encontrado := g.resolverMunicipio(notaFiscal.InfNFSe.Emit.EnderNac.CMun); encontrado {
		notaFiscal.InfNFSe.Emit.EnderNac.XMun = nome
		notaFiscal.InfNFSe.Emit.EnderNac.UF = uf
	}

	if toma := notaFiscal.InfNFSe.DPS.InfDPS.Toma; toma != nil && toma.End != nil && toma.End.EndNac != nil {
		if nome, uf, encontrado := g.resolverMunicipio(toma.End.EndNac.CMun); encontrado {
			toma.End.EndNac.XMun = nome
			toma.End.EndNac.UF = uf
		}
	}

	if dadosIBSCBS := notaFiscal.InfNFSe.DPS.InfDPS.IBSCBS; dadosIBSCBS != nil && dadosIBSCBS.Dest != nil && dadosIBSCBS.Dest.End != nil && dadosIBSCBS.Dest.End.EndNac != nil {
		if nome, uf, encontrado := g.resolverMunicipio(dadosIBSCBS.Dest.End.EndNac.CMun); encontrado {
			dadosIBSCBS.Dest.End.EndNac.XMun = nome
			dadosIBSCBS.Dest.End.EndNac.UF = uf
		}
	}

	if interm := notaFiscal.InfNFSe.DPS.InfDPS.Interm; interm != nil && interm.End.EndNac != nil {
		if nome, uf, encontrado := g.resolverMunicipio(interm.End.EndNac.CMun); encontrado {
			interm.End.EndNac.XMun = nome
			interm.End.EndNac.UF = uf
		}
	}

	if notaFiscal.InfNFSe.IBSCBS != nil {
		if nome, uf, encontrado := g.resolverMunicipio(notaFiscal.InfNFSe.IBSCBS.CLocalidadeIncid); encontrado {
			notaFiscal.InfNFSe.IBSCBS.XLocalidadeIncid = nome
			notaFiscal.InfNFSe.IBSCBS.UF = uf
		}
	}

	locPrest := &notaFiscal.InfNFSe.DPS.InfDPS.Serv.LocPrest
	if _, uf, encontrado := g.resolverMunicipio(locPrest.CLocPrestacao); encontrado {
		locPrest.UF = uf
	}
}

func (g *ServicoPreparacaoDanfse) resolverMunicipio(codigo string) (nome, uf string, encontrado bool) {
	if codigo == "" {
		return "", "", false
	}
	cod, err := strconv.Atoi(codigo)
	if err != nil {
		log.Printf("Código IBGE inválido [%s]: %v", codigo, err)
		return "", "", false
	}
	if g.resolverMun == nil {
		log.Printf("Município não encontrado para o código IBGE [%s]", codigo)
		return "", "", false
	}
	nome, uf, encontrado, err2 := g.resolverMun(cod)
	if err2 != nil {
		log.Printf("Erro ao buscar município pelo código IBGE [%s]: %v", codigo, err2)
		return "", "", false
	}
	if !encontrado {
		log.Printf("Município não encontrado para o código IBGE [%s]", codigo)
		return "", "", false
	}
	return nome, uf, true
}

func (g *ServicoPreparacaoDanfse) calcularExclusoesReducoesBC(nota *models.NFSe) {
	valoresDps := nota.InfNFSe.DPS.InfDPS.Valores
	total := valoresDps.VDescCondIncond.VDescIncond + nota.InfNFSe.Valores.VISSQN + nota.InfNFSe.Valores.VCalcDR
	if nota.InfNFSe.IBSCBS != nil {
		total += nota.InfNFSe.IBSCBS.Valores.VCalcReeRepRes
	}
	if valoresDps.Trib.TribFed != nil && valoresDps.Trib.TribFed.PisCofins != nil {
		total += valoresDps.Trib.TribFed.PisCofins.VPis + valoresDps.Trib.TribFed.PisCofins.VCofins
	}
	nota.VExclusoesReducoesBC = total
}

func (g *ServicoPreparacaoDanfse) calcularLinhas(nota *models.NFSe) {
	textoDescricaoServico := strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.CServ.XDescServ)
	textoBase := montarTextoCompl(nota)
	linhaTributos := montarLinhaTotaisTributos(nota)

	var textoInformacoesComplementares string
	if textoBase == "" {
		textoInformacoesComplementares = linhaTributos
	} else {
		textoInformacoesComplementares = textoBase + " | " + linhaTributos
	}

	nota.TextoDescServOriginal = textoDescricaoServico
	// Preservar textos originais antes de qualquer truncamento para renderização direta.
	nota.TextoComplOriginal = textoInformacoesComplementares
	nota.ExibicaoDescServ = utils_strings.TruncarComReticencias(textoDescricaoServico, maxCaracteresDescricao-3)
	nota.ExibicaoLinhaTributos = linhaTributos
	nota.ExibicaoCompl = truncarTextoComplPreservandoTotais(textoBase, linhaTributos, maxCaracteresInfoCompl)
}

// truncarTextoComplPreservandoTotais aplica a regra da NT-008: quando a
// descrição complementar excede 1.997 caracteres, ela recebe reticências e a
// linha fixa de totais é anexada integralmente depois do separador.
func truncarTextoComplPreservandoTotais(textoBase, linhaTributos string, limite int) string {
	if textoBase == "" {
		return linhaTributos
	}

	if len([]rune(textoBase)) <= limite-3 {
		if linhaTributos == "" {
			return textoBase
		}
		return textoBase + " | " + linhaTributos
	}

	limiteDescricao := limite - 3
	if limiteDescricao < 0 {
		limiteDescricao = 0
	}

	descricao := strings.TrimRight(string([]rune(textoBase)[:limiteDescricao]), " \n") + "..."
	if linhaTributos == "" {
		return descricao
	}
	return descricao + " | " + linhaTributos
}

func montarTextoCompl(nota *models.NFSe) string {
	type campo struct{ label, valor string }
	inscImob := strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.Obra.InscImobFisc)
	if inscImob == "" && nota.InfNFSe.DPS.InfDPS.IBSCBS != nil && nota.InfNFSe.DPS.InfDPS.IBSCBS.Imovel != nil {
		inscImob = strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.IBSCBS.Imovel.InscImobFisc)
	}
	var itemPedParts []string
	for _, item := range nota.InfNFSe.DPS.InfDPS.Serv.InfoCompl.GItemPed {
		if v := strings.TrimSpace(item.XItemPed); v != "" {
			itemPedParts = append(itemPedParts, v)
		}
	}
	gruposCampos := [][]campo{
		{{"Inf. Cont.:", strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.InfoCompl.XInfComp)}},
		{{"NFS-e Subst.:", strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Subst.ChSubstda)}},
		{{"Doc. Ref.:", strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.InfoCompl.DocRef)}},
		{{"Cod. Obra:", strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.Obra.CObra)}, {"Insc. Imob.:", inscImob}},
		{{"Cod. Evt.:", strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.AtvEvento.IdAtvEvt)}},
		{{"Doc. Tec.:", strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.InfoCompl.IdDocTec)}},
		{{"Núm. Ped.:", strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.InfoCompl.XPed)}, {"Item Ped.:", strings.Join(itemPedParts, ", ")}, {"Inf. A. T. Mun.:", strings.TrimSpace(nota.InfNFSe.DPS.InfDPS.Serv.InfoCompl.XOutInf)}},
	}
	var partes []string
	for _, grupo := range gruposCampos {
		var grupoPartes []string
		for _, c := range grupo {
			if c.valor != "" {
				grupoPartes = append(grupoPartes, c.label+" "+c.valor)
			}
		}
		if len(grupoPartes) > 0 {
			partes = append(partes, grupoPartes...)
		}
	}
	return strings.Join(partes, " | ")
}

// NovoServicoPreparacaoDanfse constrói o preparador. O parâmetro variádico
// maxInputSize, quando fornecido, define o limite máximo (em bytes) da entrada
// XML; se omitido, é usado MaxXMLInputSize (5 MiB) como padrão único. Esta é a
// única opção aceita. Valores explícitos inválidos (zero, negativo ou acima de
// MaxXMLInputSizeLimit) são rejeitados em Executar, nunca tratados como
// ilimitado.
func NovoServicoPreparacaoDanfse(resolver ResolverMunicipio, maxInputSize ...int) *ServicoPreparacaoDanfse {
	g := &ServicoPreparacaoDanfse{
		resolverMun: resolver,
	}
	if len(maxInputSize) > 0 {
		g.maxInputSize = maxInputSize[0]
		g.maxInputSizeSet = true
	}
	return g
}
