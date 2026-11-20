package br.gov.serpro.rtc.api.simplesnacional.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.aliquotasimples.model.AliquotasAnexoSimples;
import br.gov.serpro.rtc.api.aliquotasimples.model.LinhaAliquotasSimples;
import br.gov.serpro.rtc.api.aliquotasimples.model.OrigemLinhaAliquotasEnum;
import br.gov.serpro.rtc.api.aliquotasimples.model.ParAliquotasSimples;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.ErroGlobalSimples;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.model.input.AliquotasEfetivasInput;
import br.gov.serpro.rtc.api.simplesnacional.model.input.AliquotasEfetivasInput.AnexoComIssInput;
import br.gov.serpro.rtc.api.simplesnacional.model.input.AliquotasEfetivasInput.AnexoParUnicoInput;
import br.gov.serpro.rtc.api.simplesnacional.model.input.AliquotasEfetivasInput.ParAliquotasInput;
import br.gov.serpro.rtc.api.simplesnacional.model.input.OperacaoSimplesNaoAssistidaInput;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObservabilidadeSimplesDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ResolucaoAliquotaItemDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ResolucaoAliquotasDomain;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ResolucaoAliquotasNaoAssistidoDomain;
import br.gov.serpro.rtc.api.simplesnacional.service.CalculoSimplesUnificadoService.DadosOperacaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.service.CalculoSimplesUnificadoService.FonteLinhaAliquotasSimples;
import br.gov.serpro.rtc.shared.exceptionhandler.ErrosCalculoException;
import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;
import lombok.RequiredArgsConstructor;

/**
 *
 * Cálculo NÃO ASSISTIDO do Simples Nacional
 * ({@code POST calculadora/observabilidade/simples-nacional/nao-assistido}):
 * a linha de alíquotas do contribuinte vem no corpo, em
 * {@code aliquotasEfetivas}, e o cálculo é o do núcleo
 * ({@link CalculoSimplesUnificadoService}), o mesmo do assistido.
 *
 * <p>
 * A linha só é lida quando o regime calcula tributos. Ausente ⇒ VSN-039; valor
 * fora do formato (até 3 dígitos inteiros e 34 casas, sem sinal e sem
 * expoente) ⇒ VSN-005 (CBS) ou VSN-007 (IBS), erros GLOBAIS com o caminho
 * completo (ex.: {@code #/aliquotasEfetivas/anexoIII/semRedistribuicaoISS/pCBSSN}):
 * a linha é da operação, como o {@code tpEnteGov}. O par que nenhum item usa
 * pode faltar; a ausência do par usado é erro do item (VSN-004/VSN-006).
 *
 */
@RequiredArgsConstructor
@Service
public class SimplesNacionalNaoAssistidoService {

    private static final String RAIZ = "aliquotasEfetivas";

    private final CalculoSimplesUnificadoService calculoUnificado;

    public ObservabilidadeSimplesDomain processarOperacao(OperacaoSimplesNaoAssistidaInput operacao, String baseURL) {
        DadosOperacaoSimples dados = new DadosOperacaoSimples(operacao.id(), operacao.versao(), operacao.data(),
                operacao.tpDoc(), operacao.CRT(), operacao.tpRegimeIBSCBS(), operacao.opSimpNac(),
                operacao.regApIBSCBSSN(), operacao.regApTribSN(), operacao.tpEnteGov(), operacao.itens());
        return calculoUnificado.processar(dados, new FonteRequisicao(operacao.aliquotasEfetivas()), baseURL);
    }

    /** A linha de alíquotas recebida no corpo da requisição. */
    private record FonteRequisicao(AliquotasEfetivasInput aliquotas) implements FonteLinhaAliquotasSimples {

        @Override
        public LinhaAliquotasSimples obter(LocalDate data, String baseURL) {
            if (aliquotas == null) {
                throw new ErrosCalculoException(List.of(ErroGlobalSimples.de(
                        CodigoValidacaoSimples.ALIQUOTAS_EFETIVAS_AUSENTES, RAIZ,
                        "A linha de alíquotas efetivas do contribuinte (aliquotasEfetivas) é obrigatória no cálculo "
                                + "não assistido quando o regime calcula tributos.", baseURL)));
            }
            List<ErroDetalhe> erros = new ArrayList<>();
            LinhaAliquotasSimples linha = new LinhaAliquotasSimples(
                    aliquotas.fatorR(),
                    Boolean.TRUE.equals(aliquotas.impedimentoSublimite()),
                    anexo(aliquotas.anexoI(), "anexoI", erros, baseURL),
                    anexo(aliquotas.anexoII(), "anexoII", erros, baseURL),
                    anexo(aliquotas.anexoIII(), "anexoIII", erros, baseURL),
                    anexo(aliquotas.anexoIV(), "anexoIV", erros, baseURL),
                    anexo(aliquotas.anexoV(), "anexoV", erros, baseURL),
                    OrigemLinhaAliquotasEnum.REQUISICAO);
            if (!erros.isEmpty()) {
                throw new ErrosCalculoException(erros);
            }
            return linha;
        }

        @Override
        public ResolucaoAliquotasDomain montarResolucao(LinhaAliquotasSimples linha,
                List<ResolucaoAliquotaItemDomain> itens, List<String> avisos) {
            return new ResolucaoAliquotasNaoAssistidoDomain(linha.fatorR(), linha.impedimentoSublimite(), itens,
                    avisos);
        }

        private static AliquotasAnexoSimples anexo(AnexoParUnicoInput anexo, String caminho, List<ErroDetalhe> erros,
                String baseURL) {
            if (anexo == null) {
                return null;
            }
            return new AliquotasAnexoSimples(par(anexo.pCBSSN(), anexo.pIBSSN(), caminho, erros, baseURL), null);
        }

        private static AliquotasAnexoSimples anexo(AnexoComIssInput anexo, String caminho, List<ErroDetalhe> erros,
                String baseURL) {
            if (anexo == null) {
                return null;
            }
            ParAliquotasInput sem = anexo.semRedistribuicaoISS();
            boolean semInformado = sem != null && (informado(sem.pCBSSN()) || informado(sem.pIBSSN()));
            return new AliquotasAnexoSimples(par(anexo.pCBSSN(), anexo.pIBSSN(), caminho, erros, baseURL),
                    semInformado ? par(sem.pCBSSN(), sem.pIBSSN(), caminho + "/semRedistribuicaoISS", erros, baseURL)
                            : null);
        }

        private static ParAliquotasSimples par(String pCBSSN, String pIBSSN, String caminho, List<ErroDetalhe> erros,
                String baseURL) {
            return new ParAliquotasSimples(
                    aliquota(pCBSSN, caminho + "/pCBSSN", CodigoValidacaoSimples.ALIQUOTA_CBS_INVALIDA, erros, baseURL),
                    aliquota(pIBSSN, caminho + "/pIBSSN", CodigoValidacaoSimples.ALIQUOTA_IBS_INVALIDA, erros, baseURL));
        }

        /** Alíquota em pontos percentuais, com todas as casas; fora do formato ⇒ erro global. */
        private static BigDecimal aliquota(String valor, String caminho, CodigoValidacaoSimples codigo,
                List<ErroDetalhe> erros, String baseURL) {
            if (!informado(valor)) {
                return null;
            }
            String texto = valor.trim();
            if (ProcessadorItemSimples.FORMATO_ALIQUOTA.matcher(texto).matches()) {
                return new BigDecimal(texto);
            }
            boolean negativo = texto.startsWith("-")
                    && ProcessadorItemSimples.FORMATO_ALIQUOTA.matcher(texto.substring(1)).matches();
            erros.add(ErroGlobalSimples.de(codigo, RAIZ + "/" + caminho, negativo
                    ? String.format("O valor não pode ser negativo, recebido: '%s'.", valor)
                    : String.format("O valor não é um número decimal válido, recebido: '%s'. Informe %s.", valor,
                            ProcessadorItemSimples.DESCRICAO_FORMATO_ALIQUOTA), baseURL));
            return null;
        }

        private static boolean informado(String valor) {
            return valor != null && !valor.isBlank();
        }
    }
}
