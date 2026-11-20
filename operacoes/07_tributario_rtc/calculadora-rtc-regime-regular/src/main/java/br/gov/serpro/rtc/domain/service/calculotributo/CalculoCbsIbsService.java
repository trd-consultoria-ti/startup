/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.domain.service.calculotributo;

import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.arredondarAliquotaPublicada;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.arredondarValorFiscal;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.dividir;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.fracaoParaPercentual;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.multiplicar;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.percentualParaFracao;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.AJUSTE;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.ALIQUOTA;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.ALIQUOTA_AD_REM;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.ALIQUOTA_AD_REM_PRINCIPAL;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.ALIQUOTA_AD_REM_SECUNDARIA;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.ALIQUOTA_AD_VALOREM;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.ALIQUOTA_EFETIVA;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.ALIQUOTA_PADRAO_OU_REFERENCIA;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.ALIQUOTA_REFERENCIA;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.BASE_CALCULO;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.BASE_CALCULO_INFORMADA;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.IMPOSTO_SELETIVO_CALCULADO;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.IMPOSTO_SELETIVO_INFORMADO;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.PERCENTUAL_BIOCOMBUSTIVEL;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.PERCENTUAL_DIFERIMENTO;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.PERCENTUAL_REDUCAO;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.PERCENTUAL_REDUCAO_COMPRA_GOV;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.QUANTIDADE;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.REDUTOR;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.TRIBUTO_CALCULADO;
import static br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao.VARIACAO_PONTO_PERCENTUAL;
import static java.math.BigDecimal.ZERO;
import static java.util.Map.ofEntries;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.model.input.ItemOperacaoInput;
import br.gov.serpro.rtc.api.model.input.calculadora.enumeration.TipoEnteGovernamental;
import br.gov.serpro.rtc.api.model.output.CbsIbsOutput;
import br.gov.serpro.rtc.api.model.output.GrupoDiferimentoMonofasiaOutput;
import br.gov.serpro.rtc.api.model.output.GrupoEtapaMonofasiaOutput;
import br.gov.serpro.rtc.api.model.output.GrupoMonofasiaOutput;
import br.gov.serpro.rtc.api.model.output.TributacaoRegularOutput;
import br.gov.serpro.rtc.api.model.roc.DiferimentoDomain;
import br.gov.serpro.rtc.api.model.roc.ReducaoAliquotaDomain;
import br.gov.serpro.rtc.api.model.roc.TributacaoCompraGovernamentalDomain;
import br.gov.serpro.rtc.domain.model.dto.ClassificacaoTributariaCalculoDTO;
import br.gov.serpro.rtc.domain.model.dto.TratamentoClassificacaoDTO;
import br.gov.serpro.rtc.domain.model.entity.SituacaoTributaria;
import br.gov.serpro.rtc.domain.model.entity.TratamentoTributario;
import br.gov.serpro.rtc.domain.model.enumeration.TributoEnum;
import br.gov.serpro.rtc.domain.service.AliquotaAdValoremServicoService;
import br.gov.serpro.rtc.domain.service.AliquotaPadraoService;
import br.gov.serpro.rtc.domain.service.AliquotaReferenciaService;
import br.gov.serpro.rtc.domain.service.ClassificacaoTributariaService;
import br.gov.serpro.rtc.domain.service.PercentualReducaoService;
import br.gov.serpro.rtc.domain.service.SituacaoTributariaService;
import br.gov.serpro.rtc.domain.service.TratamentoTributarioService;
import br.gov.serpro.rtc.domain.service.calculotributo.domain.VariavelExpressao;
import br.gov.serpro.rtc.domain.service.exception.ErroFaltaImplementacaoException;
import br.gov.serpro.rtc.domain.service.exception.ErroGenericoValidacaoException;
import br.gov.serpro.rtc.domain.service.exception.TipoAliquotaDesconhecidoException;
import lombok.RequiredArgsConstructor;

/**
 * Serviço responsável por calcular CBS e IBS a partir do tratamento tributário,
 * das alíquotas aplicáveis e das variáveis da operação.
 * <p>
 * Política numérica (ADR003): a conta é exata, e cada número publicado é arredondado uma vez,
 * aqui, na casa do leiaute. O número publicado é o que entra na conta seguinte:
 * <ul>
 *   <li>a base ({@code vBC}) é arredondada a 2 casas antes de multiplicar;</li>
 *   <li>a alíquota e a alíquota efetiva são arredondadas a 4 casas (em pontos percentuais) e é a
 *       efetiva publicada que multiplica a base: {@code valor = arred2(vBC × alíquota publicada ÷
 *       100)};</li>
 *   <li>com diferimento, {@code vDif = arred2(vBC × alíquota publicada ÷ 100 × pDif ÷ 100)} e o
 *       valor devido é {@code arred2(vBC × alíquota publicada ÷ 100) − vDif}.</li>
 * </ul>
 * Na monofasia (ainda não liberada), a base é uma quantidade e as alíquotas são ad rem: os
 * valores saem com 2 casas e as alíquotas com 4, sem outra mudança na conta.
 */
@RequiredArgsConstructor
@Service
public class CalculoCbsIbsService {
    private static final BigDecimal CEM_POR_CENTO = new BigDecimal("100");
    private static final BigDecimal PBIO = new BigDecimal("30");
    private static final BigDecimal PDIF = new BigDecimal("100");
    private static final BigDecimal VARIACAO_EAC = new BigDecimal("5");
    private static final BigDecimal ALIQUOTA_EAC = new BigDecimal("0.1922");
    private static final BigDecimal ALIQUOTA_GASOLINA_TIPO_A = new BigDecimal("0.7925");
    
    private final PercentualReducaoService percentualReducaoService;
    private final AliquotaReferenciaService aliquotaReferenciaService;
    private final AliquotaPadraoService aliquotaPadraoService;
    private final AliquotaAdValoremServicoService aliquotaAdValoremServicoService;
    private final AvaliadorExpressaoAritmetica avaliador;
    private final TratamentoTributarioService tratamentoService;
    private final ClassificacaoTributariaService classificacaoTributariaService;
    private final SituacaoTributariaService situacaoTributariaService;
       
    public CbsIbsOutput calcularCbsIbs(
        TributoEnum tributo,
        Long codigoUf,
        Long codigoMunicipio,
        ItemOperacaoInput item,
        TratamentoClassificacaoDTO tratamentoClassificacao,
        BigDecimal impostoSeletivoCalculado,
        Boolean temTributacaoRegular,
        LocalDate data,
        TipoEnteGovernamental tpEnteGov,
        BigDecimal pRedutorCompraGov) {
        
        Long idTributo = tributo.getCodigo();

        String cst = null;
        String cClassTrib = null;

        TratamentoTributario tratamentoTributario = tratamentoService
                .buscar(tratamentoClassificacao.idTratamentoTributario());
        ClassificacaoTributariaCalculoDTO classificacaoTributaria = classificacaoTributariaService
                .buscarClassificacaoTributariaCalculo(tratamentoClassificacao.idClassificacaoTributaria());

        SituacaoTributaria situacaoTributaria = situacaoTributariaService
                .consultarSituacaoTributariaPorCodigo(item.getCst(), idTributo, data);

        if (temTributacaoRegular) {
            cst = item.getTributacaoRegular().getCst();
            cClassTrib = item.getTributacaoRegular().getCClassTrib();
        }

        String expressaoAliquota = tratamentoTributario.getExpressaoAliquota();
        String expressaoBaseCalculo = tratamentoTributario.getExpressaoBaseCalculo();
        String expressaoAliquotaEfetiva = tratamentoTributario.getExpressaoAliquotaEfetiva();
        String expressaoTributoCalculado = tratamentoTributario.getExpressaoTributoCalculado();
        String expressaoTributoDevido = tratamentoTributario.getExpressaoTributoDevido();
        String expressaoPercentualDiferimento = tratamentoTributario.getExpressaoPercentualDiferimento();
        String expressaoValorDiferimento = tratamentoTributario.getExpressaoValorDiferimento();
        
        boolean possuiAjuste = tratamentoTributario.isInPossuiAjuste();

        if (possuiAjuste) {
            throw new ErroFaltaImplementacaoException(classificacaoTributaria.codigo());
        }

        /*
        boolean incompativelComSuspensao = tratamentoTributario.isInIncompativelComSuspensao();
        boolean exigeGrupoDesoneracao = tratamentoTributario.isInExigeGrupoDesoneracao();
        boolean possuiAjuste = tratamentoTributario.isInPossuiAjuste();
        boolean possuiRedutor = tratamentoTributario.isInPossuiRedutor();
        */

        boolean possuiPercentualReducao = tratamentoTributario.isInPossuiPercentualReducao();

        BigDecimal valorPercentualReducao = null;
        BigDecimal valorAjuste = null;
        BigDecimal valorRedutor = null;

        // A base de cálculo informada já chega validada (ou derivada) como
        // BC IS + IS informado quando há grupo de Imposto Seletivo; assim, a expressão
        // baseCalculoInformada - impostoSeletivoInformado + impostoSeletivoCalculado
        // resolve-se para BC IS + impostoSeletivoCalculado. IS informado ausente vale zero.
        BigDecimal baseCalculoInformada = item.getBaseCalculo();
        BigDecimal quantidade = item.getQuantidade();
        BigDecimal impostoSeletivoInformado = null;
        if (item.getImpostoSeletivo() != null) {
            impostoSeletivoInformado = item.getImpostoSeletivo().getValorImpostoSeletivoInformado();
        }

        /////////////////////////////////
        BigDecimal valorAliquotaPadraoOuReferencia = null;
        BigDecimal valorAliquotaReferencia = null;
        BigDecimal valorAliquotaAdValorem = null;
        BigDecimal valorAliquotaAdRem = null;
        BigDecimal valorAliquota = null;
        // para tratar a monofasia
        BigDecimal valorAliquotaAdRemPrincipal = null;
        BigDecimal valorAliquotaAdRemSecundaria = null;
        BigDecimal variacaoPontoPercentual = null;
        BigDecimal pBio = null;
        BigDecimal pDif = null;
        /////////////////////////////////

        /////////////////////////////
        BigDecimal resultadoAliquota = null;
        BigDecimal resultadoAliquotaEfetiva = null;
        BigDecimal resultadoBaseCalculo = null;
        BigDecimal resultadoTributoCalculado = null;
        // novos
        BigDecimal resultadoTributoDevido = null;
        BigDecimal resultadoPercentualDiferimento = null;
        BigDecimal resultadoValorDiferimento = null;
        /////////////////////////////

        if (possuiPercentualReducao) {
            BigDecimal percentualReducao = percentualReducaoService
                    .buscar(classificacaoTributaria.id(), idTributo, data);
            valorPercentualReducao = percentualParaFracao(percentualReducao);
        }

        // ajuste para 510001 => seria interessante que o tipo de alíquota fosse o padrão na planilha...
        String tipoAliquota;
        if ("510001".equals(classificacaoTributaria.codigo())) {
            tipoAliquota = "Padrão";
        } else {
            tipoAliquota = classificacaoTributaria.tipoAliquota();
        }

        BigDecimal aliquotaInformada = obterAliquotaNominal(tributo, item);

        boolean aliquotaInformadaAplicada = false;
        boolean aliquotaDivididaPorCem = false;
        switch (tipoAliquota) {
            case "Padrão":
                if (aliquotaInformada != null) {
                    valorAliquota = percentualParaFracao(aliquotaInformada);
                    aliquotaInformadaAplicada = true;
                } else {
                    valorAliquota = buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);
                }
                aliquotaDivididaPorCem = true;
                break;

            case "Uniforme nacional (referência)":
                if (aliquotaInformada != null) {
                    valorAliquota = percentualParaFracao(aliquotaInformada);
                    aliquotaInformadaAplicada = true;
                } else {
                    valorAliquota = buscarAliquotaReferencia(idTributo, data);
                }
                aliquotaDivididaPorCem = true;
                break;

            case "Uniforme setorial":
                // esse tipo de alíquota provavelmente vai exigir vínculo com cClassTrib para serviços financeiros
                if (tratamentoTributario.isInPossuiMonofasia()) {
                    // resolver o problema da alíquota com nova modelagem de dados, usando código ANP em vez de NCM
                    // valorAliquota = buscarAliquotaAdRem(item.getNcm(), idTributo, data);
                    // valorAliquotaAdRem = valorAliquota;
                    valorAliquotaAdRemPrincipal = ALIQUOTA_GASOLINA_TIPO_A;
                    valorAliquotaAdRemSecundaria = ALIQUOTA_EAC;
                    pBio = percentualParaFracao(PBIO);
                    pDif = percentualParaFracao(PDIF);
                    variacaoPontoPercentual = VARIACAO_EAC;

                } else {
                    if (aliquotaInformada != null) {
                        valorAliquota = percentualParaFracao(aliquotaInformada);
                        aliquotaInformadaAplicada = true;
                    } else {
                        valorAliquota = buscarAliquotaUniformeSetorial(item.getNbs(), idTributo,
                                classificacaoTributaria.id(), data);
                    }
                    aliquotaDivididaPorCem = true;
                }
                break;

            case "Fixa":
                if (aliquotaInformada != null) {
                    valorAliquota = percentualParaFracao(aliquotaInformada);
                    aliquotaInformadaAplicada = true;
                } else {
                    valorAliquota = buscarAliquotaFixa(idTributo, classificacaoTributaria.id(), data);
                }
                aliquotaDivididaPorCem = true;
                break;

            case "Sem alíquota":
                if (aliquotaInformada != null) {
                    valorAliquota = percentualParaFracao(aliquotaInformada);
                    aliquotaInformadaAplicada = true;
                    aliquotaDivididaPorCem = true;
                } else {
                    // fixar o valor 0 no tratamento tributário, por enquanto
                    valorAliquota = ZERO;
                }
                break;

            case "Alíquotas Combinadas (Ad Valorem e Ad Rem)":
                // para fazer
                break;

            default:
                throw new TipoAliquotaDesconhecidoException(tipoAliquota, cClassTrib, cst);
        }

        if (aliquotaInformadaAplicada && !expressaoUsaAliquota(expressaoAliquota)) {
            // A alíquota nominal informada prevalece sobre uma expressão fixa
            // persistida, como "0" ou "2.08/100".
            resultadoAliquota = valorAliquota;
        } else if (isNotBlank(expressaoAliquota)) {
            var variaveis0 = ofEntries(
                    entry(ALIQUOTA, valorAliquota),
                    entry(QUANTIDADE, quantidade),
                    entry(PERCENTUAL_REDUCAO, valorPercentualReducao),
                    entry(ALIQUOTA_PADRAO_OU_REFERENCIA, valorAliquotaPadraoOuReferencia),
                    entry(ALIQUOTA_REFERENCIA, valorAliquotaReferencia),
                    entry(ALIQUOTA_AD_VALOREM, valorAliquotaAdValorem),
                    entry(ALIQUOTA_AD_REM, valorAliquotaAdRem),
                    entry(AJUSTE, valorAjuste),
                    entry(REDUTOR, valorRedutor),
                    entry(ALIQUOTA_AD_REM_PRINCIPAL, valorAliquotaAdRemPrincipal),
                    entry(ALIQUOTA_AD_REM_SECUNDARIA, valorAliquotaAdRemSecundaria),
                    entry(VARIACAO_PONTO_PERCENTUAL, variacaoPontoPercentual),
                    entry(PERCENTUAL_BIOCOMBUSTIVEL, pBio),
                    entry(PERCENTUAL_DIFERIMENTO, pDif));

            resultadoAliquota = avaliador.evaluate(expressaoAliquota, variaveis0);
        }

        if (aliquotaDivididaPorCem) {
            // a alíquota vai ao documento com até 4 casas e é ela que segue na conta
            resultadoAliquota = publicarAliquota(resultadoAliquota);
        }

        if (isNotBlank(expressaoAliquotaEfetiva)) {
            var variaveis1 = ofEntries(
                    entry(QUANTIDADE, quantidade),
                    entry(ALIQUOTA, resultadoAliquota),
                    entry(PERCENTUAL_REDUCAO, valorPercentualReducao),
                    entry(PERCENTUAL_REDUCAO_COMPRA_GOV, pRedutorCompraGov),
                    entry(ALIQUOTA_PADRAO_OU_REFERENCIA, valorAliquotaPadraoOuReferencia),
                    entry(ALIQUOTA_REFERENCIA, valorAliquotaReferencia),
                    entry(ALIQUOTA_AD_VALOREM, valorAliquotaAdValorem),
                    entry(ALIQUOTA_AD_REM, valorAliquotaAdRem),
                    entry(AJUSTE, valorAjuste),
                    entry(REDUTOR, valorRedutor),
                    entry(ALIQUOTA_AD_REM_PRINCIPAL, valorAliquotaAdRemPrincipal),
                    entry(ALIQUOTA_AD_REM_SECUNDARIA, valorAliquotaAdRemSecundaria),
                    entry(VARIACAO_PONTO_PERCENTUAL, variacaoPontoPercentual),
                    entry(PERCENTUAL_BIOCOMBUSTIVEL, pBio),
                    entry(PERCENTUAL_DIFERIMENTO, pDif));

            resultadoAliquotaEfetiva = avaliador.evaluate(expressaoAliquotaEfetiva,
                    variaveis1);
            if (aliquotaDivididaPorCem) {
                // alíquota efetiva: arredondada uma vez a 4 casas; é ela que multiplica a base
                resultadoAliquotaEfetiva = publicarAliquota(resultadoAliquotaEfetiva);
            }
        }

        var variaveis2 = ofEntries(
                entry(QUANTIDADE, quantidade),
                entry(ALIQUOTA, resultadoAliquota),
                entry(ALIQUOTA_EFETIVA, resultadoAliquotaEfetiva),
                entry(IMPOSTO_SELETIVO_INFORMADO, impostoSeletivoInformado),
                entry(BASE_CALCULO_INFORMADA, baseCalculoInformada),
                entry(IMPOSTO_SELETIVO_CALCULADO, impostoSeletivoCalculado),
                entry(PERCENTUAL_REDUCAO, valorPercentualReducao),
                entry(ALIQUOTA_PADRAO_OU_REFERENCIA, valorAliquotaPadraoOuReferencia),
                entry(ALIQUOTA_REFERENCIA, valorAliquotaReferencia),
                entry(ALIQUOTA_AD_VALOREM, valorAliquotaAdValorem),
                entry(ALIQUOTA_AD_REM, valorAliquotaAdRem),
                entry(AJUSTE, valorAjuste),
                entry(REDUTOR, valorRedutor),
                entry(ALIQUOTA_AD_REM_PRINCIPAL, valorAliquotaAdRemPrincipal),
                entry(ALIQUOTA_AD_REM_SECUNDARIA, valorAliquotaAdRemSecundaria),
                entry(VARIACAO_PONTO_PERCENTUAL, variacaoPontoPercentual),
                entry(PERCENTUAL_BIOCOMBUSTIVEL, pBio),
                entry(PERCENTUAL_DIFERIMENTO, pDif));

        resultadoBaseCalculo = avaliador.evaluate(expressaoBaseCalculo, variaveis2);
        if (!tratamentoTributario.isInPossuiMonofasia()) {
            // a base é arredondada a 2 casas antes de multiplicar (na monofasia é uma quantidade)
            resultadoBaseCalculo = arredondarValorFiscal(resultadoBaseCalculo);
        }

        if (resultadoBaseCalculo.compareTo(ZERO) < 0) {
            resultadoBaseCalculo = ZERO;
            // aqui deveria ser lançada uma exceção
            // flexibilidade para a plataforma
        }

        var variaveis3 = ofEntries(
                entry(QUANTIDADE, quantidade),
                entry(BASE_CALCULO, resultadoBaseCalculo),
                entry(ALIQUOTA, resultadoAliquota),
                entry(ALIQUOTA_EFETIVA, resultadoAliquotaEfetiva),
                entry(BASE_CALCULO_INFORMADA, baseCalculoInformada),
                entry(IMPOSTO_SELETIVO_INFORMADO, impostoSeletivoInformado),
                entry(IMPOSTO_SELETIVO_CALCULADO, impostoSeletivoCalculado),
                entry(PERCENTUAL_REDUCAO, valorPercentualReducao),
                entry(ALIQUOTA_PADRAO_OU_REFERENCIA, valorAliquotaPadraoOuReferencia),
                entry(ALIQUOTA_REFERENCIA, valorAliquotaReferencia),
                entry(ALIQUOTA_AD_VALOREM, valorAliquotaAdValorem),
                entry(ALIQUOTA_AD_REM, valorAliquotaAdRem),
                entry(AJUSTE, valorAjuste),
                entry(REDUTOR, valorRedutor),
                entry(ALIQUOTA_AD_REM_PRINCIPAL, valorAliquotaAdRemPrincipal),
                entry(ALIQUOTA_AD_REM_SECUNDARIA, valorAliquotaAdRemSecundaria),
                entry(VARIACAO_PONTO_PERCENTUAL, variacaoPontoPercentual),
                entry(PERCENTUAL_BIOCOMBUSTIVEL, pBio),
                entry(PERCENTUAL_DIFERIMENTO, pDif));

        // conta exata sobre os números publicados (base e alíquota efetiva), arredondada uma vez
        final BigDecimal tributoCalculadoExato = avaliador.evaluate(expressaoTributoCalculado, variaveis3);
        resultadoTributoCalculado = arredondarValorFiscal(tributoCalculadoExato);

        if (isNotBlank(expressaoTributoDevido)) {
            var variaveis4 = ofEntries(
                    entry(TRIBUTO_CALCULADO, resultadoTributoCalculado),
                    entry(QUANTIDADE, quantidade),
                    entry(BASE_CALCULO, resultadoBaseCalculo),
                    entry(ALIQUOTA, resultadoAliquota),
                    entry(ALIQUOTA_EFETIVA, resultadoAliquotaEfetiva),
                    entry(BASE_CALCULO_INFORMADA, baseCalculoInformada),
                    entry(IMPOSTO_SELETIVO_INFORMADO, impostoSeletivoInformado),
                    entry(IMPOSTO_SELETIVO_CALCULADO, impostoSeletivoCalculado),
                    entry(PERCENTUAL_REDUCAO, valorPercentualReducao),
                    entry(ALIQUOTA_PADRAO_OU_REFERENCIA, valorAliquotaPadraoOuReferencia),
                    entry(ALIQUOTA_REFERENCIA, valorAliquotaReferencia),
                    entry(ALIQUOTA_AD_VALOREM, valorAliquotaAdValorem),
                    entry(ALIQUOTA_AD_REM, valorAliquotaAdRem),
                    entry(AJUSTE, valorAjuste),
                    entry(REDUTOR, valorRedutor),
                    entry(ALIQUOTA_AD_REM_PRINCIPAL, valorAliquotaAdRemPrincipal),
                    entry(ALIQUOTA_AD_REM_SECUNDARIA, valorAliquotaAdRemSecundaria),
                    entry(VARIACAO_PONTO_PERCENTUAL, variacaoPontoPercentual),
                    entry(PERCENTUAL_BIOCOMBUSTIVEL, pBio),
                    entry(PERCENTUAL_DIFERIMENTO, pDif));

            resultadoTributoDevido = arredondarValorFiscal(avaliador.evaluate(expressaoTributoDevido, variaveis4));
        }

        if (isNoneBlank(expressaoPercentualDiferimento, expressaoValorDiferimento)) {
            var variaveis5 = ofEntries(
                    entry(TRIBUTO_CALCULADO, resultadoTributoCalculado),
                    entry(QUANTIDADE, quantidade),
                    entry(BASE_CALCULO, resultadoBaseCalculo),
                    entry(ALIQUOTA, resultadoAliquota),
                    entry(ALIQUOTA_EFETIVA, resultadoAliquotaEfetiva),
                    entry(BASE_CALCULO_INFORMADA, baseCalculoInformada),
                    entry(IMPOSTO_SELETIVO_INFORMADO, impostoSeletivoInformado),
                    entry(IMPOSTO_SELETIVO_CALCULADO, impostoSeletivoCalculado),
                    entry(PERCENTUAL_REDUCAO, valorPercentualReducao),
                    entry(ALIQUOTA_PADRAO_OU_REFERENCIA, valorAliquotaPadraoOuReferencia),
                    entry(ALIQUOTA_REFERENCIA, valorAliquotaReferencia),
                    entry(ALIQUOTA_AD_VALOREM, valorAliquotaAdValorem),
                    entry(ALIQUOTA_AD_REM, valorAliquotaAdRem),
                    entry(AJUSTE, valorAjuste),
                    entry(REDUTOR, valorRedutor));

            // pDif publicado com até 4 casas (em pontos percentuais)
            resultadoPercentualDiferimento = publicarAliquota(
                    avaliador.evaluate(expressaoPercentualDiferimento, variaveis5));

            // vDif = arred2(vBC × alíquota publicada ÷ 100 × pDif ÷ 100): a expressão recebe o
            // tributo calculado exato, e o produto é arredondado uma única vez
            var variaveis6 = ofEntries(
                    entry(PERCENTUAL_DIFERIMENTO, resultadoPercentualDiferimento),
                    entry(TRIBUTO_CALCULADO, tributoCalculadoExato),
                    entry(QUANTIDADE, quantidade),
                    entry(BASE_CALCULO, resultadoBaseCalculo),
                    entry(ALIQUOTA, resultadoAliquota),
                    entry(ALIQUOTA_EFETIVA, resultadoAliquotaEfetiva),
                    entry(BASE_CALCULO_INFORMADA, baseCalculoInformada),
                    entry(IMPOSTO_SELETIVO_INFORMADO, impostoSeletivoInformado),
                    entry(IMPOSTO_SELETIVO_CALCULADO, impostoSeletivoCalculado),
                    entry(PERCENTUAL_REDUCAO, valorPercentualReducao),
                    entry(ALIQUOTA_PADRAO_OU_REFERENCIA, valorAliquotaPadraoOuReferencia),
                    entry(ALIQUOTA_REFERENCIA, valorAliquotaReferencia),
                    entry(ALIQUOTA_AD_VALOREM, valorAliquotaAdValorem),
                    entry(ALIQUOTA_AD_REM, valorAliquotaAdRem),
                    entry(AJUSTE, valorAjuste),
                    entry(REDUTOR, valorRedutor));

            resultadoValorDiferimento = arredondarValorFiscal(
                    avaliador.evaluate(expressaoValorDiferimento, variaveis6));

            // valor devido: o tributo calculado menos o diferimento, descontado uma única vez
            resultadoTributoDevido = resultadoTributoCalculado.subtract(resultadoValorDiferimento);

            if (resultadoTributoDevido.compareTo(ZERO) < 0) {
                resultadoTributoDevido = ZERO;
            }
            
        }

        if (!situacaoTributaria.getInGrupoIbsCbs()) {
            return null;
            // return CbsIbsOutput
            //     .builder()
            //     .grupoReducao(null)
            //     .tributacaoRegular(null)
            //     .grupoDiferimento(null)
            //     .grupoMonofasia(null)
            //     .tributoCalculado(ZERO)
            //     .tributoDevido(ZERO)
            //     .aliquota(ZERO)
            //     .baseCalculo(ZERO)
            //     .quantidade(ZERO)
            //     .build();
        }
        
        return CbsIbsOutput
                .builder()
                .grupoReducao(!classificacaoTributaria.inGrupoReducao() || (temTributacaoRegular && tpEnteGov == null) ? null : obterGrupoReducao(classificacaoTributaria, resultadoAliquotaEfetiva, valorPercentualReducao, aliquotaDivididaPorCem))
                .compraGovernamental(obterGrupoCompraGov(tpEnteGov, TributoEnum.get(idTributo), resultadoAliquotaEfetiva, resultadoTributoDevido))
                .tributacaoRegular(temTributacaoRegular ?
                    obterGrupoDesoneracao(
                        cst,
                        cClassTrib,
                        resultadoBaseCalculo,
                        resultadoAliquota,
                        requireNonNullElse(valorPercentualReducao, ZERO),
                        aliquotaDivididaPorCem,
                        possuiPercentualReducao)
                    : null)
                .grupoDiferimento(obterGrupoDiferimento(resultadoValorDiferimento, resultadoPercentualDiferimento))
                .grupoMonofasia(obterGrupoMonofasia(classificacaoTributaria, quantidade, resultadoAliquota, resultadoAliquotaEfetiva, resultadoTributoCalculado, resultadoTributoDevido, variacaoPontoPercentual))
                .tributoCalculado(temTributacaoRegular ? ZERO : resultadoTributoCalculado)
                .tributoDevido(temTributacaoRegular ? ZERO : resultadoTributoDevido)
                .aliquota(temTributacaoRegular ? ZERO : (aliquotaDivididaPorCem && resultadoAliquota != null ? resultadoAliquota.movePointRight(2) : resultadoAliquota))
                .baseCalculo(resultadoBaseCalculo)
                .quantidade(quantidade)
                .build();
    }

    private TributacaoCompraGovernamentalDomain obterGrupoCompraGov(TipoEnteGovernamental tpEnteGov, TributoEnum t,
            BigDecimal aliquota, BigDecimal valor) {
        if (tpEnteGov == null) {
            return null;
        }
        final var b = TributacaoCompraGovernamentalDomain.builder();
        switch (t) {
        case CBS: {
            b.pAliqCBS(aliquota.movePointRight(2)); // nao divide por 100
            b.vTribCBS(valor);
            break;
        }
        case IBS_ESTADUAL: {
            b.pAliqIBSUF(aliquota.movePointRight(2));  // nao divide por 100
            b.vTribIBSUF(valor);
            break;
        }
        case IBS_MUNICIPAL: {
            b.pAliqIBSMun(aliquota.movePointRight(2));  // nao divide por 100
            b.vTribIBSMun(valor);
            break;
        }
        default:
            throw new RuntimeException("Tributo " + t + " nao possui compra governamental");
        }
        return b.build();
    }

    private BigDecimal buscarAliquotaReferencia(Long idTributo, LocalDate data) {
        return percentualParaFracao(
                aliquotaReferenciaService.buscar(idTributo, data));
    }

    protected BigDecimal buscarAliquotaPadrao(Long idTributo, Long codigoUf, Long codigoMunicipio, LocalDate data) {
        final var aliquota = aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
        return percentualParaFracao(aliquota.valorAplicavel());
    }

    private BigDecimal buscarAliquotaUniformeSetorial(String nbs, Long idTributo, Long idClassificacaoTributaria, LocalDate data) {
        BigDecimal aliquotaUniformeSetorial = aliquotaAdValoremServicoService.buscarAliquotaAdValorem(nbs, idTributo, idClassificacaoTributaria, data);
        if (aliquotaUniformeSetorial == null) {
            aliquotaUniformeSetorial = aliquotaAdValoremServicoService.buscarAliquotaAdValoremPorClassificacaoTributaria(idTributo, idClassificacaoTributaria, data);
            if (aliquotaUniformeSetorial == null) {
                throw new ErroGenericoValidacaoException("Alíquota uniforme setorial não encontrada em " + data);
            }
        }
        return percentualParaFracao(aliquotaUniformeSetorial);
    }

    private BigDecimal buscarAliquotaFixa(Long idTributo, Long idClassificacaoTributaria, LocalDate data) {
        BigDecimal aliquotaFixa = aliquotaAdValoremServicoService
                .buscarAliquotaAdValoremPorClassificacaoTributaria(idTributo, idClassificacaoTributaria, data);
        if (aliquotaFixa == null) {
            throw new ErroGenericoValidacaoException("Alíquota fixa não encontrada em " + data);
        }
        return percentualParaFracao(aliquotaFixa);
    }

    private BigDecimal obterAliquotaNominal(TributoEnum tributo, ItemOperacaoInput item) {
        if (item.getAliquotasNominais() == null) {
            return null;
        }
        return switch (tributo) {
            case CBS -> item.getAliquotasNominais().getCbs();
            case IBS_ESTADUAL -> item.getAliquotasNominais().getIbsEstadual();
            case IBS_MUNICIPAL -> item.getAliquotasNominais().getIbsMunicipal();
            default -> null;
        };
    }

    private static boolean expressaoUsaAliquota(String expressaoAliquota) {
        return isNotBlank(expressaoAliquota)
                && expressaoAliquota.matches(".*\\baliquota\\b.*");
    }

    private static ReducaoAliquotaDomain obterGrupoReducao(ClassificacaoTributariaCalculoDTO classificacaoTributaria, BigDecimal aliquotaEfetiva, BigDecimal percentualReducao, boolean aliquotaDivididaPorCem) {
        if (anyNull(percentualReducao, aliquotaEfetiva) || !classificacaoTributaria.inGrupoReducao()) {
            return null;
        }
        return ReducaoAliquotaDomain
            .builder()
            .pRedAliq(percentualReducao.movePointRight(2)) // para nao enviar o percentual dividido por 100
            .pAliqEfet(aliquotaDivididaPorCem ? aliquotaEfetiva.movePointRight(2) : aliquotaEfetiva) // para nao enviar a aliquota dividida por 100
            .build();
    }

    private static TributacaoRegularOutput obterGrupoDesoneracao(
        String cst,
        String cClassTrib,
        BigDecimal baseCalculo,
        BigDecimal aliquota,
        BigDecimal percentualReducao,
        boolean aliquotaDivididaPorCem,
        boolean possuiPercentualReducao) {
        
        BigDecimal aliquotaEfetiva = aliquota;
        if (possuiPercentualReducao && percentualReducao != null) {
            BigDecimal redutor = BigDecimal.ONE.subtract(percentualReducao);
            aliquotaEfetiva = multiplicar(aliquota, redutor);
        }
        if (aliquotaDivididaPorCem) {
            // alíquota efetiva da tributação regular: 4 casas, e é ela que multiplica a base
            aliquotaEfetiva = publicarAliquota(aliquotaEfetiva);
        }
        BigDecimal tributoDevido = arredondarValorFiscal(multiplicar(baseCalculo, aliquotaEfetiva));
        return TributacaoRegularOutput
            .builder()
            .cst(cst)
            .cClassTrib(cClassTrib)
            .baseCalculo(baseCalculo)
            .aliquotaEfetiva(aliquotaDivididaPorCem ? aliquotaEfetiva.movePointRight(2) : aliquotaEfetiva) // para nao enviar a aliquota dividida por 100
            .tributoDevido(tributoDevido)
            .build();
    }

    private static DiferimentoDomain obterGrupoDiferimento( BigDecimal valorDiferimento, BigDecimal percentualDiferimento) {
        if (anyNull(valorDiferimento, percentualDiferimento)) {
            return null;
        }
        return DiferimentoDomain
            .builder()
            .vDif(valorDiferimento)
            .pDif(percentualDiferimento.movePointRight(2))
            .build();
    }

    private static GrupoMonofasiaOutput obterGrupoMonofasia(ClassificacaoTributariaCalculoDTO classificacaoTributaria, BigDecimal quantidade, BigDecimal aliquotaAdRemPrincipal, BigDecimal aliquotaAdRemSecundaria, BigDecimal tributoCalculado, BigDecimal tributoDevido, BigDecimal variacaoPontoPercentual) {

        GrupoEtapaMonofasiaOutput grupoGMonoPadrao = null;
        GrupoEtapaMonofasiaOutput grupoGMonoReten = null;
        GrupoEtapaMonofasiaOutput grupoGMonoRet = null;
        GrupoDiferimentoMonofasiaOutput grupoGMonoDiferimento = null;

        if (classificacaoTributaria.inGrupoMonofasiaPadrao()) {
            grupoGMonoPadrao = GrupoEtapaMonofasiaOutput
                .builder()
                .quantidade(!classificacaoTributaria.inGrupoMonofasiaRet() ? quantidade : percentualParaFracao(quantidade.multiply(variacaoPontoPercentual)))
                .aliquotaAdRem(arredondarAliquotaPublicada(!classificacaoTributaria.inGrupoMonofasiaRet() ? aliquotaAdRemPrincipal : aliquotaAdRemSecundaria))
                .valor(tributoCalculado)
                .build();
        }
        if (classificacaoTributaria.inGrupoMonofasiaReten()) {
            grupoGMonoReten = GrupoEtapaMonofasiaOutput
                .builder()
                .quantidade(multiplicar(quantidade, obterFatorBioCombustivel()))
                .aliquotaAdRem(arredondarAliquotaPublicada(aliquotaAdRemSecundaria))
                .valor(tributoDevido)
                .build();
        }
        if (classificacaoTributaria.inGrupoMonofasiaRet()) {
            grupoGMonoRet = GrupoEtapaMonofasiaOutput
                .builder()
                .quantidade(quantidade)
                .aliquotaAdRem(arredondarAliquotaPublicada(aliquotaAdRemPrincipal))
                .valor(tributoDevido)
                .build();
        }
        if (classificacaoTributaria.inGrupoMonofasiaDiferimento()) {
            grupoGMonoDiferimento = GrupoDiferimentoMonofasiaOutput
                .builder()
                .percentualDiferimento(arredondarAliquotaPublicada(fracaoParaPercentual(
                        dividir(tributoCalculado.subtract(tributoDevido), tributoCalculado))))
                .valorDiferimento(tributoCalculado.subtract(tributoDevido))
                .build();
        }
        return GrupoMonofasiaOutput
            .builder()
            .tributoMonofasico(grupoGMonoPadrao)
            .tributoSujeitoRetencao(grupoGMonoReten)
            .tributoRetido(grupoGMonoRet)
            .tributoDiferido(grupoGMonoDiferimento)
            .build();
    }
    
    /**
     * Alíquota publicada: a fração recebida é levada a pontos percentuais, arredondada uma vez a
     * 4 casas e devolvida como fração, para seguir na conta. Nulo permanece nulo.
     */
    private static BigDecimal publicarAliquota(BigDecimal fracao) {
        return percentualParaFracao(arredondarAliquotaPublicada(fracaoParaPercentual(fracao)));
    }

    private static Entry<String, BigDecimal> entry(VariavelExpressao c, BigDecimal v) {
        return Map.entry(c.getNome(), requireNonNullElse(v, ZERO));
    }

    private static boolean anyNull(Object... params) {
        if (params != null && params.length > 0) {
            for (Object param : params) {
                if (param == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BigDecimal obterFatorBioCombustivel() {
        return dividir(PBIO, CEM_POR_CENTO.subtract(PBIO));
    }

}
