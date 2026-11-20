/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.domain.service.pedagio;

import static br.gov.serpro.rtc.api.model.output.pedagio.TotalPedagioOutput.getTotal;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.arredondarAliquotaPublicada;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.dividir;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.fracaoParaPercentual;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.multiplicar;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.percentualParaFracao;
import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.ratearValorFiscal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.model.input.pedagio.PedagioInput;
import br.gov.serpro.rtc.api.model.input.pedagio.TrechoPedagioInput;
import br.gov.serpro.rtc.api.model.output.pedagio.PedagioOutput;
import br.gov.serpro.rtc.api.model.output.pedagio.TrechoPedagioOutput;
import br.gov.serpro.rtc.api.model.output.pedagio.TributoPedagioOutput;
import br.gov.serpro.rtc.domain.model.enumeration.TipoWarningDadosSimulados;
import br.gov.serpro.rtc.domain.model.enumeration.TributoEnum;
import br.gov.serpro.rtc.domain.service.AliquotaPadraoService;
import br.gov.serpro.rtc.domain.service.MunicipioService;
import br.gov.serpro.rtc.domain.service.UfService;
import br.gov.serpro.rtc.domain.service.exception.CampoInvalidoException;
import lombok.RequiredArgsConstructor;

/**
 * Serviço responsável por calcular CBS e IBS em operações de pedágio,
 * distribuindo o valor de cada tributo por trecho e identificando avisos de dados simulados.
 * <p>
 * Política numérica (ADR003): a parcela exata de cada trecho é {@code base × alíquota do trecho ×
 * extensão do trecho ÷ extensão total}. O total de cada tributo é a soma exata das parcelas,
 * arredondada uma vez a 2 casas. Cada trecho recebe a sua parcela truncada no centavo, e os
 * centavos que faltam para o total vão, um a um, aos trechos de maior resto (empate: menor número
 * de trecho). As parcelas publicadas somam o total. A alíquota efetiva do trecho é informativa,
 * com 4 casas.
 */
@RequiredArgsConstructor
@Service
public class PedagioService {

    private final AliquotaPadraoService aliquotaPadraoService;

    private final UfService ufService;

    private final MunicipioService municipioService;

    public PedagioOutput calcularCIBS(PedagioInput operacao) {

        validarNumerosUnicos(operacao);
        municipioService.validarMunicipio(operacao.getCodigoMunicipioOrigem(), operacao.getUfMunicipioOrigem());
        operacao.getTrechos().forEach(t -> municipioService.validarMunicipio(t.getMunicipio(), t.getUf()));

        final List<TrechoPedagioOutput> trechosPedagio = getTrechosPedagio(operacao);
        return PedagioOutput.builder()
                .cst(operacao.getCst())
                .cClassTrib(operacao.getCClassTrib())
                .extensaoTotal(operacao.getExtensaoTotal())
                .dataHoraEmissao(operacao.getDataHoraEmissao())
                .municipioOrigem(operacao.getCodigoMunicipioOrigem())
                .baseCalculo(operacao.getBaseCalculo())
                .ufMunicipioOrigem(operacao.getUfMunicipioOrigem())
                .trechos(trechosPedagio)
                .total(getTotal(trechosPedagio))
                .build();
    }

    private List<TrechoPedagioOutput> getTrechosPedagio(PedagioInput operacao) {

        final BigDecimal extensaoTotal = operacao.getExtensaoTotal();
        final BigDecimal baseCalculoCIBS = operacao.getBaseCalculo();
        final LocalDate dataFatoGerador = operacao.getDataHoraEmissao().toLocalDate();

        // ordem do rateio: número do trecho (no empate de resto, o centavo vai ao menor número)
        final List<TrechoPedagioInput> trechos = operacao.getTrechos().stream()
                .sorted(Comparator.comparing(TrechoPedagioInput::getNumero))
                .toList();

        final List<BigDecimal> aliquotasCbs = new ArrayList<>(trechos.size());
        final List<BigDecimal> aliquotasIbsEstadual = new ArrayList<>(trechos.size());
        final List<BigDecimal> aliquotasIbsMunicipal = new ArrayList<>(trechos.size());
        for (TrechoPedagioInput trecho : trechos) {
            final Long codigoUf = ufService.buscar(trecho.getUf());

            final BigDecimal valorAliquotaCbs = buscarAliquotaPadrao(TributoEnum.CBS, dataFatoGerador, null, null);
            final BigDecimal valorAliquotaIbsEstadual = buscarAliquotaPadrao(TributoEnum.IBS_ESTADUAL, dataFatoGerador, codigoUf, null);
            final BigDecimal valorAliquotaIbsMunicipal = buscarAliquotaPadrao(TributoEnum.IBS_MUNICIPAL, dataFatoGerador, null, trecho.getMunicipio());

            if (valorAliquotaCbs == null || valorAliquotaIbsEstadual == null || valorAliquotaIbsMunicipal == null) {
                throw new CampoInvalidoException("Erro ao obter alíquotas");
            }
            aliquotasCbs.add(valorAliquotaCbs);
            aliquotasIbsEstadual.add(valorAliquotaIbsEstadual);
            aliquotasIbsMunicipal.add(valorAliquotaIbsMunicipal);
        }

        final List<TributoPedagioOutput> cbs = ratearTributo(trechos, aliquotasCbs, baseCalculoCIBS, extensaoTotal);
        final List<TributoPedagioOutput> ibsEstadual = ratearTributo(trechos, aliquotasIbsEstadual, baseCalculoCIBS, extensaoTotal);
        final List<TributoPedagioOutput> ibsMunicipal = ratearTributo(trechos, aliquotasIbsMunicipal, baseCalculoCIBS, extensaoTotal);

        final List<TrechoPedagioOutput> saida = new ArrayList<>(trechos.size());
        for (int i = 0; i < trechos.size(); i++) {
            final TrechoPedagioInput trecho = trechos.get(i);
            saida.add(TrechoPedagioOutput
                    .builder()
                    .numero(trecho.getNumero())
                    .uf(trecho.getUf())
                    .municipio(trecho.getMunicipio())
                    .baseCalculo(baseCalculoCIBS)
                    .cbs(cbs.get(i))
                    .ibsEstadual(ibsEstadual.get(i))
                    .ibsMunicipal(ibsMunicipal.get(i))
                    .extensaoTrecho(trecho.getExtensao())
                    .build());
        }
        return saida;
    }

    /**
     * Reparte um tributo entre os trechos. A parcela exata do trecho é {@code base × alíquota ×
     * extensão do trecho ÷ extensão total}; as parcelas publicadas saem do rateio pelo maior resto
     * ({@code ArredondamentoUtils.ratearValorFiscal}) e somam o total do tributo.
     *
     * @param trechos trechos em ordem de número
     * @param aliquotas alíquota (fração) do tributo em cada trecho, na mesma ordem
     */
    private static List<TributoPedagioOutput> ratearTributo(List<TrechoPedagioInput> trechos, List<BigDecimal> aliquotas,
            BigDecimal baseCalculo, BigDecimal extensaoTotal) {
        final List<BigDecimal> parcelasExatas = new ArrayList<>(trechos.size());
        for (int i = 0; i < trechos.size(); i++) {
            parcelasExatas.add(dividir(
                    multiplicar(multiplicar(baseCalculo, aliquotas.get(i)), trechos.get(i).getExtensao()),
                    extensaoTotal));
        }
        final List<BigDecimal> parcelas = ratearValorFiscal(parcelasExatas);

        final List<TributoPedagioOutput> tributos = new ArrayList<>(trechos.size());
        for (int i = 0; i < trechos.size(); i++) {
            // alíquota efetiva do trecho: informativa, com 4 casas (em pontos percentuais)
            final BigDecimal aliquotaEfetiva = arredondarAliquotaPublicada(fracaoParaPercentual(
                    dividir(multiplicar(aliquotas.get(i), trechos.get(i).getExtensao()), extensaoTotal)));
            tributos.add(TributoPedagioOutput
                    .builder()
                    .aliquota(fracaoParaPercentual(aliquotas.get(i)))
                    .aliquotaEfetiva(aliquotaEfetiva)
                    .tributoCalculado(parcelas.get(i))
                    .build());
        }
        return tributos;
    }

    public BigDecimal buscarAliquotaPadrao(TributoEnum tributo, LocalDate data, Long codigoUf, Long codigoMunicipio) {
        final var aliquota = aliquotaPadraoService.buscarAliquota(tributo.getCodigo(), codigoUf, codigoMunicipio, data);
        return percentualParaFracao(aliquota.valorAplicavel());
    }

    private static void validarNumerosUnicos(PedagioInput operacao) {
        Set<Integer> numerosVistos = new HashSet<>();
        for (var trecho : operacao.getTrechos()) {
            if (!numerosVistos.add(trecho.getNumero())) {
                throw new CampoInvalidoException(
                        "Número de trecho duplicado: " + trecho.getNumero());
            }
        }
    }

    public TipoWarningDadosSimulados getWarningDadosSimulados(PedagioInput operacao) {
        LocalDate dataOperacao = operacao.getDataHoraEmissao().toLocalDate();
        LocalDate dataLimite = LocalDate.of(2027, 1, 1);
        
        if (dataOperacao.isBefore(dataLimite)) {
            return null;
        }
        
        return TipoWarningDadosSimulados.CASO_GERAL;
    }

}
