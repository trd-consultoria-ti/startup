package br.gov.serpro.rtc.api.simplesnacional.model.output;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Bloco {@code resolucaoAliquotas} da resposta dos endpoints não assistido e
 * assistido: o cabeçalho da linha de alíquotas usada, a resolução de cada item
 * ({@link ResolucaoAliquotaItemDomain}) e os avisos da operação. O cabeçalho depende de onde veio a linha:
 * <ul>
 * <li>{@link ResolucaoAliquotasAssistidoDomain} — Tabela de Alíquotas Efetivas:
 * {@code niContribuinte}, {@code pa}, {@code indSublimite}, {@code fatorR},
 * {@code dataHoraProcessamento} e {@code nsu};</li>
 * <li>{@link ResolucaoAliquotasNaoAssistidoDomain} — corpo da requisição:
 * {@code fatorR} e {@code indSublimite}.</li>
 * </ul>
 * Ausente no endpoint clássico e quando o regime é {@code SOMENTE_RECEITA}.
 *
 * <p>O schema OpenAPI declara só a variante do não assistido; a do assistido é
 * acrescentada onde a rota do assistido existe.
 *
 */
@Schema(description = "Resolução das alíquotas efetivas por item",
        oneOf = { ResolucaoAliquotasNaoAssistidoDomain.class })
public sealed interface ResolucaoAliquotasDomain
        permits ResolucaoAliquotasAssistidoDomain, ResolucaoAliquotasNaoAssistidoDomain {

    /** Resolução de cada item, na ordem dos itens de entrada. */
    List<ResolucaoAliquotaItemDomain> itens();

    /** Avisos da operação, que não bloqueiam o cálculo. */
    List<String> avisos();
}
