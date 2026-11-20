/**
 * Alíquotas efetivas do Simples Nacional, sem acesso a banco: o que o núcleo
 * do cálculo precisa saber sobre a resolução das alíquotas de um item,
 * qualquer que seja a origem da linha de alíquotas (corpo da requisição ou
 * Tabela de Alíquotas Efetivas do Simples Nacional).
 *
 * <p>
 * ISOLADO. Depende apenas de {@code atividadesimples} e {@code shared}; NUNCA
 * de {@code simplesnacional}. Direção de dependência obrigatória:
 * {@code simplesnacional → aliquotasimples/atividadesimples}.
 *
 * <p>
 * Conteúdo:
 * <ul>
 * <li>{@code model} — a linha de alíquotas do contribuinte como valor do núcleo
 * ({@code LinhaAliquotasSimples}, {@code AliquotasAnexoSimples},
 * {@code ParAliquotasSimples}, em pontos percentuais, e
 * {@code OrigemLinhaAliquotasEnum}) e o desfecho da resolução de cada item
 * ({@code MotivoResolucaoAliquota});</li>
 * <li>{@code serializer} — serialização das alíquotas com a precisão integral
 * como TEXTO em notação decimal, sem alterar os serializers
 * compartilhados.</li>
 * </ul>
 */
package br.gov.serpro.rtc.api.aliquotasimples;
