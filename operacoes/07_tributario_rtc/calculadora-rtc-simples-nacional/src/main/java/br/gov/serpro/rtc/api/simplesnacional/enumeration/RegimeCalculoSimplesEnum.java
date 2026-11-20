package br.gov.serpro.rtc.api.simplesnacional.enumeration;

/**
 * Regime de apuração do IBS/CBS que CALCULA no endpoint CLÁSSICO, resolvido a
 * partir da combinação {@code CRT} × {@code tpRegimeIBSCBS} (NT 2026.005, seção
 * 5.3.4).
 *
 * <p>
 * Este enum é apenas o DISCRIMINADOR tipado das combinações válidas no
 * clássico; a linha do regime (código neutro e comportamento: gera IBS ou não,
 * destino dos valores devidos e pendentes/suspensos) vem da tabela de
 * enquadramento, no vocabulário CRT, e da tabela
 * {@code REGIME_APURACAO_SIMPLES_NACIONAL}, pela vigência da data do fato
 * gerador. Os endpoints novos usam o enquadramento diretamente.
 *
 * <p>
 * Combinações fora deste enum são rejeitadas na validação global (HTTP 422):
 * {@code CRT=4} (MEI), {@code tpRegimeIBSCBS=1} (regime regular),
 * {@code CRT=3} sem {@code tpRegimeIBSCBS=2} (regime normal puro) e as
 * incoerências CRT × tpRegimeIBSCBS das regras C21a-10/20/30 da NT.
 */
public enum RegimeCalculoSimplesEnum {

    /** CRT=1 — Simples Nacional (sem {@code tpRegimeIBSCBS}). */
    CRT_1,

    /**
     * CRT=2 — Simples Nacional com excesso de sublimite de receita bruta
     * (sem {@code tpRegimeIBSCBS}). Na parametrização vigente, o regime não
     * gera o bloco de IBS na saída: o IBS desse contribuinte é declarado pelo
     * emitente no gIBSCBS da nota (regime regular), fora deste endpoint.
     */
    CRT_2,

    /**
     * CRT=3 + tpRegimeIBSCBS=2 — Regime Normal com apuração pelo regime do
     * Simples Nacional. Na parametrização vigente, os valores calculados ficam
     * integralmente pendentes ou suspensos
     * ({@code vIBSPendSusp}/{@code vCBSPendSusp}), e a exportação indireta
     * (tpRBSN=4) zera tudo (fórmulas P32/P33 da NT — literal e intencional).
     */
    CRT_3_TPREGIME_2
}
