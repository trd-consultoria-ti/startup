/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.domain.service;

import static br.gov.serpro.rtc.shared.util.ArredondamentoUtils.MODO;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.model.output.CbsIbsOutput;
import br.gov.serpro.rtc.api.model.roc.ImpostoSeletivoDomain;
import br.gov.serpro.rtc.domain.model.dto.FundamentacaoClassificacaoDTO;
import br.gov.serpro.rtc.domain.model.dto.TratamentoClassificacaoDTO;
import br.gov.serpro.rtc.domain.model.entity.TratamentoTributario;
import br.gov.serpro.rtc.domain.service.token.TokenizerService;
import lombok.RequiredArgsConstructor;

/**
 * Serviço responsável por gerar a memória de cálculo textual de CBS, IBS e
 * Imposto Seletivo com base nas fórmulas e fundamentações aplicáveis.
 * <p>
 * A memória mostra os números publicados na resposta (base com 2 casas, alíquotas com até 4), no
 * formato brasileiro. Os valores chegam aqui já arredondados no cálculo; a formatação usa o modo
 * único de {@code ArredondamentoUtils} e não altera o número.
 */
@RequiredArgsConstructor
@Service
public class MemoriaCalculoService {

    private final TokenizerService tokenizerService;
    private final FundamentacaoClassificacaoService fundamentacaoClassificacaoService;
    private final TratamentoTributarioService tratamentoService;
    
    @Value("${application.memoriacalculo.enabled}")
    private boolean memoriaCalculoHabilitada;

    public String gerarMemoriaCalculoCbsIbs(TratamentoClassificacaoDTO tratamentoClassificacao, CbsIbsOutput tributo, BigDecimal baseCalculoInformada, BigDecimal quantidade, String unidade, LocalDate data) {
        if (memoriaCalculoHabilitada) {
            // CST sem gIBSCBS (ex: imunidade 410): usa a base informada no item e alíquota zero
            if (tributo == null) {
                tributo = CbsIbsOutput.builder().baseCalculo(baseCalculoInformada).build();
            }

            FundamentacaoClassificacaoDTO fundamentacaoClassificacao = fundamentacaoClassificacaoService
                    .buscar(tratamentoClassificacao.idClassificacaoTributaria(), data);
            
            TratamentoTributario tt = tratamentoService
                    .buscar(tratamentoClassificacao.idTratamentoTributario());
            
            String norma = fundamentacaoClassificacao.textoCurto();
            String tratamento = tt.getDescricao();
            String baseCalculo = formatarMoeda(tributo.getBaseCalculo());
            String aliquotaAdValorem = formatarPercentual(tributo.getAliquota());
            
            String aliquotaAdRem;
            if (tributo.getGrupoMonofasia() != null && tributo.getGrupoMonofasia().getTributoMonofasico() != null &&
                    tributo.getGrupoMonofasia().getTributoMonofasico().getAliquotaAdRem() != null) {
                aliquotaAdRem = formatarPercentual(tributo.getGrupoMonofasia().getTributoMonofasico().getAliquotaAdRem());
            } else {
                aliquotaAdRem = formatarPercentual(null);
            }
            
            String percentualReducao;
            if (tributo.getGrupoReducao() != null && tributo.getGrupoReducao().getPRedAliq() != null) {
                percentualReducao = formatarPercentual(tributo.getGrupoReducao().getPRedAliq());
            } else {
                percentualReducao = formatarPercentual(null);
            }
            
            String aliquotaDesoneracao = formatarPercentual(null);
            String montanteDesoneracao = formatarMoeda(null);
            
            String percentualDiferimento;
            String valorDiferimento;
            if (tributo.getGrupoDiferimento() != null && tributo.getGrupoDiferimento().getPDif() != null &&
                    tributo.getGrupoDiferimento().getVDif() != null) {
                percentualDiferimento = formatarPercentual(tributo.getGrupoDiferimento().getPDif());
                valorDiferimento = formatarMoeda(tributo.getGrupoDiferimento().getVDif());
            } else {
                percentualDiferimento = formatarPercentual(null);
                valorDiferimento = formatarMoeda(null);
            }
            Map<String, String> valores = Map.ofEntries(
                    Map.entry("norma", defaultString(norma)),
                    Map.entry("tratamento", defaultString(tratamento)),
                    Map.entry("base_calculo", defaultString(baseCalculo)),
                    Map.entry("aliquota_ad_valorem", defaultString(aliquotaAdValorem)),
                    Map.entry("aliquota_ad_rem", defaultString(aliquotaAdRem)),
                    Map.entry("quantidade", defaultString(quantidade != null ? quantidade.toString() : "0")),
                    Map.entry("unidade", defaultString(unidade)),
                    
                    Map.entry("percentual_reducao", defaultString(percentualReducao)),
                    Map.entry("percentual_diferimento", defaultString(percentualDiferimento)),
                    Map.entry("valor_diferimento", defaultString(valorDiferimento)),
                    Map.entry("aliquota_desoneracao", defaultString(aliquotaDesoneracao)),
                    Map.entry("montante_desoneracao", defaultString(montanteDesoneracao))
                    );
            String texto = fundamentacaoClassificacao.memoriaCalculo();
            return StringUtils.isBlank(texto) ? "Texto de memória de cálculo não encontrado" : tokenizerService.substituirPlaceholders(texto, valores);
        }
        return null;
    }

    public void gerarMemoriaCalculoImpostoSeletivo(TratamentoClassificacaoDTO tratamentoClassificacao, ImpostoSeletivoDomain tributo, BigDecimal quantidade, String unidade, LocalDate data) {
        if (memoriaCalculoHabilitada) {
            FundamentacaoClassificacaoDTO fundamentacaoClassificacao = fundamentacaoClassificacaoService
                    .buscar(tratamentoClassificacao.idClassificacaoTributaria(), data);
            String norma = fundamentacaoClassificacao.textoCurto();
            
            TratamentoTributario tt = tratamentoService
                    .buscar(tratamentoClassificacao.idTratamentoTributario());
            String tratamento = tt.getDescricao();
            
            String baseCalculo = formatarMoeda(tributo.getVBCIS());
            String aliquotaAdValorem = formatarPercentual(tributo.getPIS());
            String aliquotaAdRem = formatarPercentual(tributo.getPISEspec());
            Map<String, String> valores = Map.ofEntries(
                    Map.entry("norma", defaultString(norma)),
                    Map.entry("tratamento", defaultString(tratamento)),
                    Map.entry("base_calculo", defaultString(baseCalculo)),
                    Map.entry("aliquota_ad_valorem", defaultString(aliquotaAdValorem)),
                    Map.entry("aliquota_ad_rem", defaultString(aliquotaAdRem)),
                    Map.entry("quantidade", Objects.toString(quantidade, "")),
                    Map.entry("unidade", defaultString(unidade)));
            String texto = fundamentacaoClassificacao.memoriaCalculo();
            String memoria = StringUtils.isBlank(texto) ? "Texto de memória de cálculo não encontrado": tokenizerService.substituirPlaceholders(texto, valores);
            tributo.setMemoriaCalculo(memoria);
        }
    }

    // DecimalFormat não é thread-safe: nova instância a cada chamada
    private static String formatarMoeda(BigDecimal valor) {
        final var formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        formato.setRoundingMode(MODO);
        return formato.format(valor != null ? valor : BigDecimal.ZERO);
    }

    private static String formatarPercentual(BigDecimal valor) {
        final var formato = new DecimalFormat("#,##0.00##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        formato.setRoundingMode(MODO);
        return formato.format(valor != null ? valor : BigDecimal.ZERO);
    }

}