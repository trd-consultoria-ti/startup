package br.gov.serpro.rtc.api.aliquotasimples.model;

/**
 * As alíquotas de UM anexo na linha do contribuinte: o par do anexo (com
 * redistribuição do ISS, nos anexos III a V; o único par, nos anexos I e II) e,
 * só nos anexos III, IV e V, o par SEM redistribuição do ISS.
 *
 * <p>
 * ISOLADO. Valor do núcleo, sem acesso a banco.
 *
 * @param par                  par do anexo; {@code null} quando a linha não o traz
 * @param semRedistribuicaoISS par sem redistribuição do ISS; {@code null} quando a linha não o
 *                             traz — vale então o par com redistribuição (o nulo da
 *                             carga queria dizer que o ISS não passou do teto e os dois pares
 *                             coincidem)
 */
public record AliquotasAnexoSimples(ParAliquotasSimples par, ParAliquotasSimples semRedistribuicaoISS) {
}
