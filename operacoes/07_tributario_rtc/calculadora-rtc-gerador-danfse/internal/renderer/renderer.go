package renderer

import (
	"bytes"
	"context"
	"embed"
	"fmt"
	"strings"

	"danfse-service/internal/domain/presentation"

	"github.com/carlos7ags/folio/barcode"
	"github.com/carlos7ags/folio/document"
	foliofont "github.com/carlos7ags/folio/font"
	folioimage "github.com/carlos7ags/folio/image"
	"github.com/carlos7ags/folio/layout"
)

//go:embed assets/logo-nfse.png
var directAssets embed.FS

// Renderer renderiza um DocumentoDanfse preparado diretamente em PDF. Nunca
// analisa HTML e não possui estado mutável compartilhado entre requisições.
type Renderer struct {
	regular *foliofont.Standard
	bold    *foliofont.Standard
	logo    *folioimage.Image
}

func NewRenderer() (*Renderer, error) {
	logoData, err := directAssets.ReadFile("assets/logo-nfse.png")
	if err != nil {
		return nil, fmt.Errorf("folio direto: logo: %w", err)
	}
	logo, err := folioimage.NewPNG(logoData)
	if err != nil {
		return nil, fmt.Errorf("folio direto: parse logo: %w", err)
	}
	return &Renderer{
		regular: foliofont.Helvetica,
		bold:    foliofont.HelveticaBold,
		logo:    logo,
	}, nil
}

func (r *Renderer) Renderizar(ctx context.Context, doc presentation.DocumentoDanfse) ([]byte, error) {
	if err := ctx.Err(); err != nil {
		return nil, fmt.Errorf("folio direto: contexto: %w", err)
	}
	pdfDoc := document.NewDocument(document.PageSizeA4)
	pdfDoc.SetMargins(layout.Margins{})
	if doc.MarcaDagua.Texto != "" {
		pdfDoc.SetWatermarkConfig(document.WatermarkConfig{
			Text: doc.MarcaDagua.Texto, FontSize: doc.MarcaDagua.TamanhoFonte,
			ColorR: doc.MarcaDagua.ColorR, ColorG: doc.MarcaDagua.ColorG, ColorB: doc.MarcaDagua.ColorB,
			Angle: doc.MarcaDagua.Angulo, Opacity: doc.MarcaDagua.Opacidade,
		})
	}
	page := pdfDoc.AddPage()
	r.desenharPagina(page, doc)
	var buf bytes.Buffer
	if _, err := pdfDoc.WriteToWithContext(ctx, &buf, document.WriteOptions{Deterministic: true}); err != nil {
		return nil, fmt.Errorf("folio direto: serialização: %w", err)
	}
	return buf.Bytes(), nil
}

func (r *Renderer) desenharPagina(page *document.Page, doc presentation.DocumentoDanfse) {
	layout := montarLayoutPagina(doc)
	desc := doc.Servico.DescServ
	if desc == "" {
		desc = doc.Servico.DescServOriginal
	}
	descLines := r.medirLinhas(desc, bodyRight()-bodyLeft()-3, valueSizePt)
	content := r.selecionarConteudoComplementar(doc)
	contentLines := r.medirLinhas(content, bodyRight()-bodyLeft()-3, 6.5)
	allocation := calcularAlocacaoTexto(layout, doc, descLines, contentLines)

	r.desenharCabecalho(page, doc)
	r.desenharInfoNfse(page, doc)
	r.desenharPartes(page, doc, layout)
	r.desenharServico(page, doc, allocation.descriptionLines, layout)
	r.desenharTributacoes(page, doc, layout, allocation.shiftCM)
	r.desenharTotais(page, doc, layout, allocation.shiftCM)
	r.desenharInformacoesComplementares(page, doc, layout, allocation.shiftCM, allocation.complementaryLines)
	r.desenharCanhoto(page, doc)
	r.drawFrame(page)
}

func (r *Renderer) drawFrame(page *document.Page) {
	page.AddRect(pageLeft(), pageHeightPt-pageBottom(), pageRight()-pageLeft(), pageBottom()-pageTop(), 1, blackRGB)
}

func (r *Renderer) desenharCabecalho(page *document.Page, doc presentation.DocumentoDanfse) {
	page.AddRectFilled(bodyLeft(), topToPDFY(0.30+1.16), cm(20.40), cm(1.16), grayHeaderRGB)
	page.AddImage(r.logo, cm(0.49), topToPDFY(0.44+0.85), cm(4.00), cm(0.85))
	r.centerText(page, labelDANFSeVersao, 5.41*cmPt, 0.35, 10.19*cmPt, titleSizePt+2, r.bold)
	r.centerText(page, labelDocumentoAuxiliar, 5.41*cmPt, 0.65, 10.19*cmPt, titleSizePt+2, r.bold)
	if doc.Cabecalho.TipoAmbiente == "Homologação" {
		r.centerText(page, labelNFSemValidadeJuridica, 5.41*cmPt, 0.95, 10.19*cmPt, 9, r.bold, redRGB)
	}
	r.textTop(page, fmt.Sprintf("Município: %s - %s", doc.Cabecalho.Municipio, doc.Cabecalho.UF), gridX4(), 0.45, 8, r.regular)
	r.textTop(page, "Ambiente Gerador: "+doc.Cabecalho.AmbienteGerador, gridX4(), 0.97, 6, r.regular)
	r.textTop(page, "Tipo de Ambiente: "+doc.Cabecalho.TipoAmbiente, gridX4(), 1.22, 6, r.regular)
	page.AddLine(bodyLeft(), topToPDFY(1.48), bodyRight(), topToPDFY(1.48), 0.5, blackRGB)
}

func (r *Renderer) desenharInfoNfse(page *document.Page, doc presentation.DocumentoDanfse) {
	r.desenharCelula(page, bodyLeft(), accessKeyTopCM, accessKeyWidthCM, labelChaveAcessoNfse, doc.Acesso.ChaveAcesso, estiloAcesso)
	r.desenharQRCode(page, doc.Acesso.CodigoQR)
	r.desenharTextoQRCode(page)

	r.desenharCelula(page, colunaX(0), nfseFieldsTopCM, larguraColunas(0, 1)/cmPt, labelNumeroNFSe, doc.Acesso.NumeroNFSe, estiloAcesso)
	r.desenharCelula(page, colunaX(1), nfseFieldsTopCM, larguraColunas(1, 1)/cmPt, labelCompetenciaNFSe, doc.Acesso.Competencia, estiloAcesso)
	r.desenharCelula(page, colunaX(2), nfseFieldsTopCM, larguraColunas(2, 1)/cmPt, labelDataHoraEmissaoNFSe, doc.Acesso.DataHoraEmissaoNFSe, estiloAcesso)

	r.desenharCelula(page, colunaX(0), dpsFieldsTopCM, larguraColunas(0, 1)/cmPt, labelNumeroDPS, doc.Acesso.NumeroDPS, estiloAcesso)
	r.desenharCelula(page, colunaX(1), dpsFieldsTopCM, larguraColunas(1, 1)/cmPt, labelSerieDPS, doc.Acesso.SerieDPS, estiloAcesso)
	r.desenharCelula(page, colunaX(2), dpsFieldsTopCM, larguraColunas(2, 1)/cmPt, labelDataHoraEmissaoDPS, doc.Acesso.DataHoraEmissaoDPS, estiloAcesso)
}

func (r *Renderer) desenharQRCode(page *document.Page, codigo string) {
	qr, err := barcode.NewQR(codigo)
	if err == nil {
		qr.Draw(page.ContentStream(), cm(qrLeftCM), topToPDFY(qrTopCM+qrSizeCM), cm(qrSizeCM), cm(qrSizeCM))
	}
}

func (r *Renderer) desenharTextoQRCode(page *document.Page) {
	linhas := [...]string{
		labelQrLine1,
		labelQrLine2,
		labelQrLine3,
	}
	for i, linha := range linhas {
		r.textTop(page, linha, cm(qrTextLeftCM), qrTextTopCM+float64(i)*qrTextStepCM, 6, r.regular)
	}
}

func (r *Renderer) desenharPartes(page *document.Page, doc presentation.DocumentoDanfse, layout pageLayout) {
	r.desenharCampoIdentificacao(page, bodyLeft(), 3.65, col1CM, labelEmitenteNfse, doc.Acesso.TipoEmitente, true)
	r.desenharCampoIdentificacao(page, gridX2(), 3.65, col2CM, labelSituacaoNfse, doc.Acesso.Situacao, false)
	r.desenharCampoIdentificacao(page, gridX3(), 3.65, col3CM, labelFinalidade, doc.Acesso.Finalidade, false)

	r.desenharSecaoParte(page, 4.34, labelPrestadorFornecedor, doc.Partes.Emitente, variantePrestador)

	if doc.Partes.Tomador.Visivel {
		r.desenharSecaoParte(page, layout.alturaTomador, labelTomadorAdquirente, doc.Partes.Tomador, varianteTomador)
	} else {
		r.desenharBlocoSuprimido(page, layout.alturaTomador, doc.Partes.Tomador.MensagemSupressao, suppressedPartyBlockHeightCM)
	}
	if doc.Partes.Destinatario.Visivel {
		r.desenharSecaoParte(page, layout.alturaDestinatario, labelDestinatarioOperacao, doc.Partes.Destinatario, varianteDestinatario)
	} else {
		r.desenharBlocoSuprimido(page, layout.alturaDestinatario, doc.Partes.Destinatario.MensagemSupressao, suppressedPartyBlockHeightCM)
	}
	if doc.Partes.Intermediario.Visivel {
		r.desenharSecaoParte(page, layout.alturaIntermediario, labelIntermediarioOperacao, doc.Partes.Intermediario, varianteIntermediario)
	} else {
		r.desenharBlocoSuprimido(page, layout.alturaIntermediario, doc.Partes.Intermediario.MensagemSupressao, suppressedPartyBlockHeightCM)
	}
}

func (r *Renderer) desenharServico(page *document.Page, doc presentation.DocumentoDanfse, descLines int, layout pageLayout) {
	y := layout.serviceTop
	desc := doc.Servico.DescServ
	if desc == "" {
		desc = doc.Servico.DescServOriginal
	}
	r.desenharSecao(page, y, labelServicoPrestado)
	r.desenharCampo(page, colunaX(1), y, larguraColunas(1, 1)/cmPt, labelCodigoTribNacMun, doc.Servico.CodigoTribNacMun, false)
	r.desenharCampo(page, colunaX(2), y, larguraColunas(2, 1)/cmPt, labelCodigoNBS, doc.Servico.CodigoNBS, false)
	r.desenharCampo(page, colunaX(3), y, larguraColunas(3, 1)/cmPt, labelLocalPrestacao, doc.Servico.LocalPrestacaoUF, false)
	r.textTop(page, truncateText(doc.Servico.XTribNac, r.regular, valueSizePt, bodyRight()-bodyLeft()-cm(colGutterCM)), bodyLeft()+cm(fieldTextInsetCM), y+serviceTaxDescriptionOffsetCM, valueSizePt, r.regular)
	r.desenharMultiplasLinhas(page, bodyLeft(), y+serviceDescriptionOffsetCM, bodyRight()-bodyLeft(), float64(descLines)*lineStepCM*cmPt, labelDescricaoServico, desc, valueSizePt)
}

func (r *Renderer) desenharTributacoes(page *document.Page, doc presentation.DocumentoDanfse, layout pageLayout, shift float64) {
	r.desenharTributacaoMunicipal(page, doc, layout, shift)
	r.desenharTributacaoFederal(page, doc, layout, shift)
	r.desenharTributacaoIBSCBS(page, doc, layout, shift)
}

func (r *Renderer) desenharTributacaoMunicipal(page *document.Page, doc presentation.DocumentoDanfse, layout pageLayout, shift float64) {
	mun := doc.Tributacoes.Municipal
	if !mun.Visivel {
		r.desenharBlocoSuprimido(page, layout.municipalTop+shift, mun.MensagemSupressao, suppressedMunicipalBlockHeightCM)
		return
	}
	r.desenharSecao(page, layout.municipalTop+shift, labelTribMunicipal)
	r.desenharCampo(page, colunaX(1), layout.municipalTop+shift, larguraColunas(1, 1)/cmPt, labelTipoTribIssqn, mun.TribISSQN, false)
	r.desenharCampo(page, colunaX(2), layout.municipalTop+shift, larguraColunas(2, 2)/cmPt, labelMunicipioUFPAISSIssqn, mun.MunicipioUFPAISIncid, false)
	r.desenharCampo(page, colunaX(0), layout.municipalTop+municipalRowStepCM+shift, larguraColunas(0, 1)/cmPt, labelRegEspTribIssqn, mun.RegEspTrib, false)
	r.desenharCampo(page, colunaX(1), layout.municipalTop+municipalRowStepCM+shift, larguraColunas(1, 1)/cmPt, labelTpImunidadeIssqn, mun.TpImunidade, false)
	r.desenharCampo(page, colunaX(2), layout.municipalTop+municipalRowStepCM+shift, larguraColunas(2, 1)/cmPt, labelSuspensaoExigIssqn, mun.SuspensaoExig, false)
	r.desenharCampo(page, colunaX(3), layout.municipalTop+municipalRowStepCM+shift, larguraColunas(3, 1)/cmPt, labelNumeroProcessoSusp, mun.NProcessoSusp, false)

	r.desenharCampo(page, colunaX(0), layout.municipalTop+2*municipalRowStepCM+shift, larguraColunas(0, 1)/cmPt, labelBeneficioMunicipal, mun.BeneficioMunicipal, false)
	r.desenharCampo(page, colunaX(1), layout.municipalTop+2*municipalRowStepCM+shift, larguraColunas(1, 1)/cmPt, labelCalculoBM, mun.CalculoBM, false)
	r.desenharCampo(page, colunaX(2), layout.municipalTop+2*municipalRowStepCM+shift, larguraColunas(2, 1)/cmPt, labelTotalDeducoes, mun.TotalDeducoes, false)
	r.desenharCampo(page, colunaX(3), layout.municipalTop+2*municipalRowStepCM+shift, larguraColunas(3, 1)/cmPt, labelDescontoIncondicionado, mun.DescontoIncond, false)
	r.desenharCampo(page, colunaX(0), layout.municipalTop+3*municipalRowStepCM+shift, larguraColunas(0, 1)/cmPt, labelBCIssqn, mun.BCISSQN, false)
	r.desenharCampo(page, colunaX(1), layout.municipalTop+3*municipalRowStepCM+shift, larguraColunas(1, 1)/cmPt, labelAliqAplicada, mun.AliqAplic, false)
	r.desenharCampo(page, colunaX(2), layout.municipalTop+3*municipalRowStepCM+shift, larguraColunas(2, 1)/cmPt, labelRetencaoIssqn, mun.RetISSQN, false)
	r.desenharCampo(page, colunaX(3), layout.municipalTop+3*municipalRowStepCM+shift, larguraColunas(3, 1)/cmPt, labelIssqnApurado, mun.ISSQNApurado, false)
}

func (r *Renderer) desenharTributacaoFederal(page *document.Page, doc presentation.DocumentoDanfse, layout pageLayout, shift float64) {
	fed := doc.Tributacoes.Federal
	r.desenharSecao(page, layout.federalTop+shift, labelTribFederal)
	r.desenharCampo(page, colunaX(1), layout.federalTop+shift, larguraColunas(1, 1)/cmPt, labelIRRF, fed.IRRF, false)
	r.desenharCampo(page, colunaX(2), layout.federalTop+shift, larguraColunas(2, 1)/cmPt, labelContribPrevidRet, fed.ContribPrevidRet, false)
	r.desenharCampo(page, colunaX(3), layout.federalTop+shift, larguraColunas(3, 1)/cmPt, labelContribSocRet, fed.ContribSocRet, false)
	if fed.ExibirPisCofins {
		r.desenharCampo(page, colunaX(0), layout.federalTop+federalRowStepCM+shift, larguraColunas(0, 1)/cmPt, labelPISDebito, fed.PISDebito, false)
		r.desenharCampo(page, colunaX(1), layout.federalTop+federalRowStepCM+shift, larguraColunas(1, 1)/cmPt, labelCOFINSDebito, fed.COFINSDebito, false)
	}
	if fed.ExibirPisCofins {
		r.desenharCampo(page, colunaX(2), layout.federalTop+federalRowStepCM+shift, larguraColunas(2, 2)/cmPt, labelDescricaoContribRet, fed.DescContribRet, false)
	}
}

func (r *Renderer) desenharTributacaoIBSCBS(page *document.Page, doc presentation.DocumentoDanfse, layout pageLayout, shift float64) {
	r.desenharSecao(page, layout.ibsTop+shift, labelTribIBSCBS)
	ibs := doc.Tributacoes.IBSCBS
	r.desenharCampo(page, colunaX(1), layout.ibsTop+shift, larguraColunas(1, 1)/cmPt, labelCSTcClassTrib, ibs.CSTcClassTrib, false)
	r.desenharCampo(page, colunaX(2), layout.ibsTop+shift, larguraColunas(2, 2)/cmPt, labelIndOpCodIBGELocalidadeUF, ibs.IndOpCodIBGELocalidadeUF, false)
	r.desenharCampo(page, colunaX(0), layout.ibsTop+ibsRowStepCM+shift, larguraColunas(0, 1)/cmPt, labelExclusoesReducoesBC, ibs.ExclusoesReducoesBC, false)
	r.desenharCampo(page, colunaX(1), layout.ibsTop+ibsRowStepCM+shift, larguraColunas(1, 1)/cmPt, labelBCPosExclusoes, ibs.BCPosExclusoes, false)
	r.desenharCampo(page, colunaX(2), layout.ibsTop+ibsRowStepCM+shift, larguraColunas(2, 1)/cmPt, labelRedAliqIBSRedAliqCBS, ibs.RedAliqIBSRedAliqCBS, false)
	r.desenharCampo(page, colunaX(3), layout.ibsTop+ibsRowStepCM+shift, larguraColunas(3, 1)/cmPt, labelAliqIBSUFIBSMun, ibs.AliqIBSUFIBSMun, false)
	r.desenharCampo(page, colunaX(0), layout.ibsTop+ibsSecondRowOffsetCM+shift, larguraColunas(0, 1)/cmPt, labelAliqEfetMunIBS, ibs.AliqEfetMunIBS, false)
	r.desenharCampo(page, colunaX(1), layout.ibsTop+ibsSecondRowOffsetCM+shift, larguraColunas(1, 1)/cmPt, labelValorMunIBS, ibs.ValorMunIBS, false)
	r.desenharCampo(page, colunaX(2), layout.ibsTop+ibsSecondRowOffsetCM+shift, larguraColunas(2, 1)/cmPt, labelAliqEfetEstIBS, ibs.AliqEfetEstIBS, false)
	r.desenharCampo(page, colunaX(3), layout.ibsTop+ibsSecondRowOffsetCM+shift, larguraColunas(3, 1)/cmPt, labelValorEstIBS, ibs.ValorEstIBS, false)
	r.desenharCampo(page, colunaX(0), layout.ibsTop+ibsThirdRowOffsetCM+shift, larguraColunas(0, 1)/cmPt, labelValorTotalIBS, ibs.ValorTotalIBS, false)
	r.desenharCampo(page, colunaX(1), layout.ibsTop+ibsThirdRowOffsetCM+shift, larguraColunas(1, 1)/cmPt, labelAliqCBS, ibs.AliqCBS, false)
	r.desenharCampo(page, colunaX(2), layout.ibsTop+ibsThirdRowOffsetCM+shift, larguraColunas(2, 1)/cmPt, labelAliqEfetCBS, ibs.AliqEfetCBS, false)
	r.desenharCampo(page, colunaX(3), layout.ibsTop+ibsThirdRowOffsetCM+shift, larguraColunas(3, 1)/cmPt, labelValorTotalCBS, ibs.ValorTotalCBS, false)
}

func (r *Renderer) desenharTotais(page *document.Page, doc presentation.DocumentoDanfse, layout pageLayout, shift float64) {
	top := layout.totalsTop + shift
	r.desenharSecao(page, top, labelValorTotalNfse, totalSectionHeightCM)
	r.desenharCampo(page, colunaX(1), top, larguraColunas(1, 1)/cmPt, labelValorOperacaoServico, doc.Totais.ValorOperacaoServico, false)
	r.desenharCampo(page, colunaX(2), top, larguraColunas(2, 1)/cmPt, labelDescontoIncondicionado, doc.Totais.DescontoIncond, false)
	r.desenharCampo(page, colunaX(3), top, larguraColunas(3, 1)/cmPt, labelDescontoCondicionado, doc.Totais.DescontoCond, false)
	r.desenharCampo(page, colunaX(0), top+totalsRowStepCM, larguraColunas(0, 1)/cmPt, labelTotalRetencoes, doc.Totais.TotalRetencoes, false)
	r.desenharCampo(page, colunaX(1), top+totalsRowStepCM, larguraColunas(1, 1)/cmPt, labelValorLiquidoNfse, doc.Totais.ValorLiquido, false)
	r.desenharCampo(page, colunaX(2), top+totalsRowStepCM, larguraColunas(2, 1)/cmPt, labelTotalIBSCBS, doc.Totais.TotalIBSCBS, false)
	r.desenharCampo(page, colunaX(3), top+totalsRowStepCM, larguraColunas(3, 1)/cmPt, labelValorLiquidoIBSCBS, doc.Totais.ValorLiquidoIBSCBS, true)
}

// topoLinhaTotaisTributos retorna a posição y (cm) da linha de totais de tributos
// usada por desenharInformacoesComplementares. Quando o canhoto está visível, a linha fica
// em yComplTributosCM; quando oculto, desce em canhotoHeightCM para que o
// espaço liberado seja redistribuído ao bloco de informações complementares.
// Mantida para compatibilidade com testes.
func topoLinhaTotaisTributos(doc presentation.DocumentoDanfse) float64 {
	if doc.Canhoto.Visivel {
		return yComplTributosCM
	}
	return yComplTributosCM + canhotoHeightCM
}

// combinarConteudoComplementar retorna o texto completo de informações
// complementares, incorporando LinhaTributos sem duplicação:
//   - se Conteudo já contém LinhaTributos, retorna Conteudo;
//   - se Conteudo está vazio, retorna LinhaTributos (compatibilidade);
//   - caso contrário, anexa " | " + LinhaTributos.
func combinarConteudoComplementar(conteudo, linhaTributos string) string {
	if conteudo == "" {
		return linhaTributos
	}
	if strings.Contains(conteudo, linhaTributos) {
		return conteudo
	}
	if linhaTributos != "" {
		return conteudo + " | " + linhaTributos
	}
	return conteudo
}

// selecionarConteudoComplementar prioriza Conteudo, que já respeita o limite
// de exibição aplicado na preparação. ConteudoOriginal é apenas fallback para
// documentos montados diretamente sem Conteudo. Em seguida, incorpora a linha
// tributária sem duplicação.
func (r *Renderer) selecionarConteudoComplementar(doc presentation.DocumentoDanfse) string {
	src := doc.InformacoesComplementares.Conteudo
	if src == "" {
		src = doc.InformacoesComplementares.ConteudoOriginal
	}
	return combinarConteudoComplementar(src, doc.InformacoesComplementares.LinhaTributos)
}

func (r *Renderer) desenharInformacoesComplementares(page *document.Page, doc presentation.DocumentoDanfse, layout pageLayout, shift float64, lines int) {
	r.desenharTituloSecao(page, layout.complementaryTop+shift, labelInformacoesComplementares, complementarySectionHeightCM, false)
	contentTop := layout.complementaryTop + complementaryContentOffsetCM + shift
	content := r.selecionarConteudoComplementar(doc)
	content = r.ajustarInformacoesComplementares(content, doc.InformacoesComplementares.LinhaTributos, bodyRight()-bodyLeft()-3, 6.5, lines)
	r.desenharMultiplasLinhas(page, bodyLeft(), contentTop, bodyRight()-bodyLeft(), float64(lines)*complementaryLineStepCM*cmPt, "", content, 6.5)
}

func (r *Renderer) desenharCanhoto(page *document.Page, doc presentation.DocumentoDanfse) {
	if !doc.Canhoto.Visivel {
		return
	}
	addLineH(page, yCanhotoCM, 0.5, blackRGB)
	addLineH(page, canhotoBottomCM, 0.5, blackRGB)
	addLineV(page, 0.30, yCanhotoCM, canhotoBottomCM, 0.5, blackRGB)
	addLineV(page, colX2CM, yCanhotoCM, canhotoBottomCM, 0.5, blackRGB)
	addLineV(page, colX3CM, yCanhotoCM, canhotoBottomCM, 0.5, blackRGB)
	addLineV(page, 20.70, yCanhotoCM, canhotoBottomCM, 0.5, blackRGB)
	r.textTop(page, labelDataCientificacao, bodyLeft()+2, 28.17, 6, r.bold)
	r.textTop(page, labelIdentificacaoAssinatura, gridX2()+2, 28.17, 6, r.bold)
	r.textTop(page, labelNumeroNFSeChave, gridX3()+2, 28.17, 6, r.bold)
	r.wrapText(page, doc.Acesso.NumeroNFSe+" / "+doc.Acesso.ChaveAcesso, gridX3()+2, 28.47, bodyRight()-gridX3(), 6.5, r.regular, 2)
}

type varianteParte int

const (
	variantePrestador varianteParte = iota
	varianteTomador
	varianteDestinatario
	varianteIntermediario
)

func (r *Renderer) desenharSecao(page *document.Page, topCM float64, titulo string, alturas ...float64) {
	alturaCM := normalSectionHeightCM
	if len(alturas) > 0 {
		alturaCM = alturas[0]
	}
	r.desenharTituloSecao(page, topCM, titulo, alturaCM, true)
}

func (r *Renderer) desenharTituloSecao(page *document.Page, topCM float64, titulo string, alturaCM float64, sombreado bool) {
	y := topToPDFY(topCM)
	h := alturaCM * cmPt
	if sombreado {
		page.AddRectFilled(bodyLeft(), y-h, col1CM*cmPt, h, grayHeaderRGB)
	}
	r.textTop(page, titulo, bodyLeft()+cm(fieldTextInsetCM), topCM+sectionTitleOffsetCM, titleSizePt, r.bold)
	page.AddLine(bodyLeft(), y, bodyRight(), y, 0.5, blackRGB)
}

func (r *Renderer) desenharBlocoSuprimido(page *document.Page, topCM float64, valor string, alturas ...float64) {
	alturaCM := suppressedMunicipalBlockHeightCM
	if len(alturas) > 0 {
		alturaCM = alturas[0]
	}
	bottom := topCM + alturaCM
	page.AddLine(bodyLeft(), topToPDFY(topCM), bodyRight(), topToPDFY(topCM), 0.5, blackRGB)
	page.AddLine(bodyLeft(), topToPDFY(bottom), bodyRight(), topToPDFY(bottom), 0.5, blackRGB)
	size := 7.5
	width := bodyRight() - bodyLeft() - 2
	text := fit(valor, r.regular, size, width)
	textTop := topCM + (alturaCM-(size/cmPt))/2
	r.centerText(page, text, bodyLeft()+1, textTop, width, size, r.regular)
}

func (r *Renderer) desenharSecaoParte(page *document.Page, topCM float64, titulo string, p presentation.SecaoParte, variante varianteParte) {
	r.desenharSecao(page, topCM, titulo)
	identificacaoColunas := 1
	if variante == varianteDestinatario {
		identificacaoColunas = 2
	}
	r.desenharCampo(page, colunaX(1), topCM, larguraColunas(1, identificacaoColunas)/cmPt, labelCNPJCPFNIF, p.CNPJCPFNIF, false)
	if variante != varianteDestinatario {
		r.desenharCampo(page, colunaX(2), topCM, larguraColunas(2, 1)/cmPt, labelIndicadorMunicipal, p.InscricaoMunicipal, false)
	}
	r.desenharCampo(page, colunaX(3), topCM, larguraColunas(3, 1)/cmPt, labelTelefone, p.Telefone, false)
	r.desenharCampo(page, colunaX(0), topCM+partyRowStepCM, larguraColunas(0, 2)/cmPt, labelNomeNomeEmpresarial, p.Nome, false)
	r.desenharCampo(page, colunaX(2), topCM+partyRowStepCM, larguraColunas(2, 1)/cmPt, labelMunicipioUF, p.MunicipioUF, false)
	r.desenharCampo(page, colunaX(3), topCM+partyRowStepCM, larguraColunas(3, 1)/cmPt, labelCodigoIBGECEP, p.CodigoIBGECEP, false)
	r.desenharCampo(page, colunaX(0), topCM+2*partyRowStepCM, larguraColunas(0, 2)/cmPt, labelEndereco, p.Endereco, false)
	r.desenharCampo(page, colunaX(2), topCM+2*partyRowStepCM, larguraColunas(2, 2)/cmPt, labelEmail, p.Email, false)
	if variante == variantePrestador {
		r.desenharCampo(page, colunaX(0), topCM+3*partyRowStepCM, larguraColunas(0, 2)/cmPt, labelSimplesNacional, p.SimplesNacional, false)
		r.desenharCampo(page, colunaX(2), topCM+3*partyRowStepCM, larguraColunas(2, 2)/cmPt, labelRegimeApuracaoSN, p.RegimeApuracaoSN, false)
	}
}
