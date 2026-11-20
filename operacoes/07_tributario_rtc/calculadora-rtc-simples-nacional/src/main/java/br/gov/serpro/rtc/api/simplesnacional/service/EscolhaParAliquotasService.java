package br.gov.serpro.rtc.api.simplesnacional.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import br.gov.serpro.rtc.api.aliquotasimples.model.AliquotasAnexoSimples;
import br.gov.serpro.rtc.api.aliquotasimples.model.LinhaAliquotasSimples;
import br.gov.serpro.rtc.api.aliquotasimples.model.MotivoResolucaoAliquota;
import br.gov.serpro.rtc.api.aliquotasimples.model.ParAliquotasSimples;
import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;
import br.gov.serpro.rtc.api.simplesnacional.collector.ItemCollectorSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.SituacaoIssParAliquotaSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegraAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.TipoAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;

/**
 *
 * ESCOLHA DO PAR DE ALÍQUOTAS do item — com ou sem redistribuição do ISS — e
 * avaliação dos campos do ISS que alimentam a escolha.
 *
 * <p>
 * Princípio (Solução de Consulta Cosit 298/2019; Fale Conosco da RFB, 2018 e
 * 2021): usa-se o par SEM redistribuição quando, naquela operação, o
 * percentual do ISS não entra no cálculo do Simples. A redistribuição existe
 * para levar aos outros tributos o ISS que passa do teto; sem ISS no cálculo
 * não há excedente. Nos demais casos, o par COM redistribuição.
 *
 * <p>
 * Regras, na ordem de avaliação ({@link RegraAliquotaSimplesEnum}; a primeira
 * que se aplica decide):
 * <ol>
 * <li>{@code DOCUMENTO} — atividade não admitida no tpDoc: sem par (item
 * {@code NAO_CALCULADO});</li>
 * <li>{@code ATIVIDADE_NAO_TRIBUTADA} — atividade sem anexo (código 90):
 * {@code SEM_ALIQUOTA}, zero;</li>
 * <li>{@code ANEXO_SEM_ISS} — anexo I ou II: {@code PAR_UNICO};</li>
 * <li>{@code ISS_FORA_DO_SIMPLES} — impedimento pelo sublimite (da linha de
 * alíquotas, avaliado aqui) ou linha da tabela de situações do ISS (CRT 2,
 * regApIBSCBSSN 2, regApTribSN 2 ou 3);</li>
 * <li>{@code ATIVIDADE_SEM_ISS} — atividade sem ISS no Simples (indicador do
 * catálogo);</li>
 * <li>{@code ISS_NAO_INCIDE}, 7. {@code ISENCAO_MUNICIPAL} e 8.
 * {@code ISS_RETIDO} — linhas da tabela de situações do ISS;</li>
 * <li>{@code PADRAO} — {@code COM_REDISTRIBUICAO}.</li>
 * </ol>
 * Antes das regras 3 a 9, o anexo é a coluna "sem fator r" do catálogo; na
 * atividade sujeita ao fator "r" (a 09), "com fator r" (III) quando
 * {@code fatorR = true}, senão "sem fator r" (V).
 *
 * <p>
 * Componente PURO: os fatos do documento, a linha de alíquotas, a atividade e
 * as situações do ISS (lidas do banco uma vez por operação) chegam prontos.
 *
 */
@Component
public class EscolhaParAliquotasService {

    /** Nomes dos campos no contrato da API — os mesmos da coluna {@code SIPA_CAMPO}. */
    public static final String CAMPO_CRT = "CRT";
    public static final String CAMPO_REGAPIBSCBSSN = "regApIBSCBSSN";
    public static final String CAMPO_REGAPTRIBSN = "regApTribSN";
    public static final String CAMPO_TRIBISSQN = "tribISSQN";
    public static final String CAMPO_TPRETISSQN = "tpRetISSQN";
    public static final String CAMPO_TPBM = "tpBM";

    private static final Set<Integer> DOMINIO_TRIBISSQN = Set.of(1, 2, 3, 4);
    private static final Set<Integer> DOMINIO_TPRETISSQN = Set.of(1, 2, 3);
    private static final Set<Integer> DOMINIO_TPBM = Set.of(1, 2, 3, 4);

    /** tribISSQN = 1: operação tributável — a única em que a retenção do ISS se aplica. */
    private static final int TRIBUTAVEL = 1;
    /** tpBM = 1: isenção. */
    private static final int ISENCAO = 1;
    /** tpRetISSQN 2 e 3: ISS retido pelo tomador ou pelo intermediário. */
    private static final Set<Integer> RETIDO = Set.of(2, 3);

    // ─── Contratos ─────────────────────────────────────────────────────────────

    /** Campos do ISS do item, como chegaram (texto). */
    public record CamposIssItem(String tribISSQN, String tpRetISSQN, String tpBM) {
    }

    /**
     * Campos do ISS que valem para a escolha do par, depois das
     * desconsiderações (nulos quando ausentes, inválidos ou desconsiderados),
     * e os avisos do item.
     */
    public record AvaliacaoIssItem(Integer tribISSQN, Integer tpRetISSQN, Integer tpBM, List<String> avisos) {
    }

    /**
     * Par escolhido para o item.
     *
     * @param anexo                       anexo aplicado (I..V), ou {@code null} nas regras 1 e 2 e com
     *                                    fator "r" ausente
     * @param tipoAliquota                tipo da alíquota, ou {@code null} na regra 1 e com fator "r" ausente
     * @param regra                       regra que decidiu, ou {@code null} com fator "r" ausente
     * @param par                         par de alíquotas a usar (valores podem ser nulos), ou {@code null}
     *                                    quando não há par
     * @param parSemRedistribuicaoAusente o par sem redistribuição faltou na linha e valeu o par com
     * @param caminhoPar                  caminho do par dentro de {@code aliquotasEfetivas} (ex.:
     *                                    {@code anexoIII/semRedistribuicaoISS}), para os ponteiros de erro
     * @param motivo                      desfecho da resolução
     */
    public record EscolhaParAliquotas(String anexo, TipoAliquotaSimplesEnum tipoAliquota,
            RegraAliquotaSimplesEnum regra, ParAliquotasSimples par, boolean parSemRedistribuicaoAusente,
            String caminhoPar, MotivoResolucaoAliquota motivo) {
    }

    // ─── Campos do ISS ─────────────────────────────────────────────────────────

    /**
     *
     * Avalia os campos do ISS do item:
     * <ul>
     * <li>família CRT: os três campos são desconsiderados, com aviso;</li>
     * <li>{@code tribISSQN}: obrigatório na atividade com ISS no Simples;
     * ausente ou fora de 1 a 4 ⇒ VSN-036;</li>
     * <li>{@code tpRetISSQN}: obrigatório com {@code tribISSQN = 1} na
     * atividade com ISS no Simples; ausente ou fora de 1 a 3 ⇒ VSN-037. Com
     * {@code tribISSQN} 2, 3 ou 4 é desconsiderado, com aviso (a NFS-e o
     * recusaria: E0580);</li>
     * <li>{@code tpBM}: opcional — ausente = sem benefício municipal, nunca
     * erro; fora de 1 a 4 ⇒ VSN-038. Na atividade que não admite benefício
     * municipal (só 12 e 13 admitem) é desconsiderado, com aviso (E0536);</li>
     * <li>{@code tpBM = 1} com {@code tpRetISSQN} 2 ou 3: combinação recusada
     * pela NFS-e (E0594) — aviso; vale a regra 7.</li>
     * </ul>
     * Só é chamada para item com atividade válida e regime que calcula
     * tributos; os erros vão para o coletor do item.
     *
     */
    public AvaliacaoIssItem avaliarCamposIss(VocabularioRegimeSimplesEnum vocabulario,
            AtividadeSimplesNacional atividade, CamposIssItem campos, ItemCollectorSimples collector) {
        List<String> avisos = new ArrayList<>();
        boolean tribInformado = informado(campos.tribISSQN());
        boolean retInformado = informado(campos.tpRetISSQN());
        boolean bmInformado = informado(campos.tpBM());

        if (vocabulario != VocabularioRegimeSimplesEnum.NFSE) {
            List<String> desconsiderados = new ArrayList<>();
            if (tribInformado) {
                desconsiderados.add(CAMPO_TRIBISSQN);
            }
            if (retInformado) {
                desconsiderados.add(CAMPO_TPRETISSQN);
            }
            if (bmInformado) {
                desconsiderados.add(CAMPO_TPBM);
            }
            if (!desconsiderados.isEmpty()) {
                avisos.add(String.format("%s desconsiderado(s): os campos do ISS só valem na NFS-e.",
                        String.join(", ", desconsiderados)));
            }
            return new AvaliacaoIssItem(null, null, null, avisos);
        }

        boolean issNoSimples = Boolean.TRUE.equals(atividade.getIssNoSimples());
        String cAtivSN = atividade.getCAtivSN();

        // tribISSQN
        Integer tribISSQN = inteiro(campos.tribISSQN());
        if (tribInformado && (tribISSQN == null || !DOMINIO_TRIBISSQN.contains(tribISSQN))) {
            collector.addErroValidacao(CodigoValidacaoSimples.TRIBISSQN_INVALIDO, CAMPO_TRIBISSQN, String.format(
                    "A tributação do ISSQN (tribISSQN) deve ser 1, 2, 3 ou 4, recebido: '%s'.", campos.tribISSQN()));
            tribISSQN = null;
        } else if (!tribInformado && issNoSimples) {
            collector.addErroValidacao(CodigoValidacaoSimples.TRIBISSQN_INVALIDO, CAMPO_TRIBISSQN, String.format(
                    "A tributação do ISSQN (tribISSQN) é obrigatória para a atividade %s, que tem ISS no Simples "
                            + "Nacional.", cAtivSN));
        }

        // tpRetISSQN
        Integer tpRetISSQN = null;
        if (retInformado && tribISSQN != null && tribISSQN != TRIBUTAVEL) {
            avisos.add(String.format("tpRetISSQN desconsiderado: não há retenção do ISS com tribISSQN %d "
                    + "(a NFS-e recusaria a combinação: regra E0580).", tribISSQN));
        } else if (retInformado) {
            tpRetISSQN = inteiro(campos.tpRetISSQN());
            if (tpRetISSQN == null || !DOMINIO_TPRETISSQN.contains(tpRetISSQN)) {
                collector.addErroValidacao(CodigoValidacaoSimples.TPRETISSQN_INVALIDO, CAMPO_TPRETISSQN,
                        String.format("O tipo de retenção do ISSQN (tpRetISSQN) deve ser 1, 2 ou 3, recebido: '%s'.",
                                campos.tpRetISSQN()));
                tpRetISSQN = null;
            }
        } else if (issNoSimples && Integer.valueOf(TRIBUTAVEL).equals(tribISSQN)) {
            collector.addErroValidacao(CodigoValidacaoSimples.TPRETISSQN_INVALIDO, CAMPO_TPRETISSQN, String.format(
                    "O tipo de retenção do ISSQN (tpRetISSQN) é obrigatório com tribISSQN = 1 na atividade %s, que "
                            + "tem ISS no Simples Nacional.", cAtivSN));
        }

        // tpBM
        Integer tpBM = null;
        if (bmInformado && !Boolean.TRUE.equals(atividade.getAdmiteBeneficioMunicipal())) {
            avisos.add(String.format("tpBM desconsiderado: a atividade %s não admite benefício municipal do ISS "
                    + "(regra E0536 da NFS-e: só os subitens 7.02 e 7.05, atividades 12 e 13).", cAtivSN));
        } else if (bmInformado) {
            tpBM = inteiro(campos.tpBM());
            if (tpBM == null || !DOMINIO_TPBM.contains(tpBM)) {
                collector.addErroValidacao(CodigoValidacaoSimples.TPBM_INVALIDO, CAMPO_TPBM, String.format(
                        "O tipo de benefício municipal (tpBM) deve ser 1, 2, 3 ou 4, recebido: '%s'.", campos.tpBM()));
                tpBM = null;
            }
        }

        if (Integer.valueOf(ISENCAO).equals(tpBM) && tpRetISSQN != null && RETIDO.contains(tpRetISSQN)) {
            avisos.add(String.format("tpBM 1 (isenção) com tpRetISSQN %d: combinação recusada pela NFS-e "
                    + "(regra E0594); vale a isenção municipal (par sem redistribuição).", tpRetISSQN));
        }
        return new AvaliacaoIssItem(tribISSQN, tpRetISSQN, tpBM, avisos);
    }

    // ─── Escolha do par ────────────────────────────────────────────────────────

    /**
     *
     * Escolhe o anexo e o par de alíquotas do item.
     *
     * @param atividade    atividade vigente do item
     * @param admissivel   a atividade é admitida no tpDoc da operação
     * @param linha        linha de alíquotas do contribuinte
     * @param fatos        valores dos campos do documento que valem para a escolha, pelo nome do contrato
     *                     ({@code CRT}, {@code regApIBSCBSSN}, {@code regApTribSN}, {@code tribISSQN},
     *                     {@code tpBM}, {@code tpRetISSQN}); ausentes ou desconsiderados ficam fora do mapa
     * @param situacoes    situações do ISS vigentes (tabela {@code SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL})
     * @return o par escolhido
     *
     */
    public EscolhaParAliquotas escolher(AtividadeSimplesNacional atividade, boolean admissivel,
            LinhaAliquotasSimples linha, Map<String, Integer> fatos, List<SituacaoIssParAliquotaSimples> situacoes) {
        // Regra 1 — DOCUMENTO.
        if (!admissivel) {
            return new EscolhaParAliquotas(null, null, RegraAliquotaSimplesEnum.DOCUMENTO, null, false, null,
                    MotivoResolucaoAliquota.ATIVIDADE_INADMISSIVEL_NO_DOCUMENTO);
        }

        // Regra 2 — ATIVIDADE_NAO_TRIBUTADA: atividade sem anexo (código 90).
        String anexoComFatorR = atividade.getAnexoComFatorR();
        String anexoSemFatorR = atividade.getAnexoSemFatorR();
        if (anexoComFatorR == null && anexoSemFatorR == null) {
            return new EscolhaParAliquotas(null, TipoAliquotaSimplesEnum.SEM_ALIQUOTA,
                    RegraAliquotaSimplesEnum.ATIVIDADE_NAO_TRIBUTADA, ParAliquotasSimples.ZERO, false, null,
                    MotivoResolucaoAliquota.ATIVIDADE_SEM_ANEXO);
        }

        // Anexo: "sem fator r", ou "com fator r" na atividade sujeita ao fator r.
        String anexo = anexoSemFatorR;
        if (anexoComFatorR != null) {
            if (linha.fatorR() == null) {
                return new EscolhaParAliquotas(null, null, null, null, false, null,
                        MotivoResolucaoAliquota.FATOR_R_AUSENTE);
            }
            anexo = linha.fatorR() ? anexoComFatorR : anexoSemFatorR;
        }
        AliquotasAnexoSimples aliquotasAnexo = linha.anexo(anexo);
        ParAliquotasSimples parDoAnexo = aliquotasAnexo == null ? null : aliquotasAnexo.par();
        String caminhoAnexo = "anexo" + anexo;

        // Regra 3 — ANEXO_SEM_ISS: anexos I e II têm um só par.
        if (!LinhaAliquotasSimples.anexoComIss(anexo)) {
            return escolhido(anexo, TipoAliquotaSimplesEnum.PAR_UNICO, RegraAliquotaSimplesEnum.ANEXO_SEM_ISS,
                    parDoAnexo, false, caminhoAnexo);
        }

        // Regras 4 a 9: anexos III, IV e V.
        RegraAliquotaSimplesEnum regra = RegraAliquotaSimplesEnum.PADRAO;
        TipoAliquotaSimplesEnum tipo = TipoAliquotaSimplesEnum.COM_REDISTRIBUICAO;
        SituacaoIssParAliquotaSimples situacao;
        if (linha.impedimentoSublimite()) {
            // Regra 4 — o impedimento vem da linha de alíquotas, não do documento.
            regra = RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES;
            tipo = TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO;
        } else if ((situacao = situacao(RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES, fatos, situacoes)) != null) {
            regra = situacao.getRegra();
            tipo = situacao.getTipoAliquota();
        } else if (!Boolean.TRUE.equals(atividade.getIssNoSimples())) {
            // Regra 5 — atividade sem ISS no Simples (indicador do catálogo).
            regra = RegraAliquotaSimplesEnum.ATIVIDADE_SEM_ISS;
            tipo = TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO;
        } else {
            // Regras 6, 7 e 8, nesta ordem.
            for (RegraAliquotaSimplesEnum candidata : List.of(RegraAliquotaSimplesEnum.ISS_NAO_INCIDE,
                    RegraAliquotaSimplesEnum.ISENCAO_MUNICIPAL, RegraAliquotaSimplesEnum.ISS_RETIDO)) {
                situacao = situacao(candidata, fatos, situacoes);
                if (situacao != null) {
                    regra = situacao.getRegra();
                    tipo = situacao.getTipoAliquota();
                    break;
                }
            }
        }

        if (tipo != TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO) {
            return escolhido(anexo, tipo, regra, parDoAnexo, false, caminhoAnexo);
        }
        // Par sem redistribuição nulo: igual ao par com.
        ParAliquotasSimples sem = aliquotasAnexo == null ? null : aliquotasAnexo.semRedistribuicaoISS();
        if (sem == null) {
            return escolhido(anexo, tipo, regra, parDoAnexo, true, caminhoAnexo);
        }
        return escolhido(anexo, tipo, regra, sem, false, caminhoAnexo + "/semRedistribuicaoISS");
    }

    /** Aviso do item quando o par sem redistribuição faltou e valeu o par com. */
    public static String avisoParSemRedistribuicaoAusente(String anexo) {
        return String.format("Par sem redistribuição do ISS ausente no anexo %s; aplicado o par com redistribuição, "
                + "igual quando o ISS não passa do teto.", anexo);
    }

    private static EscolhaParAliquotas escolhido(String anexo, TipoAliquotaSimplesEnum tipo,
            RegraAliquotaSimplesEnum regra, ParAliquotasSimples par, boolean semAusente, String caminho) {
        MotivoResolucaoAliquota motivo = par == null || par.pCBSSN() == null
                ? MotivoResolucaoAliquota.ANEXO_SEM_ALIQUOTA
                : MotivoResolucaoAliquota.RESOLVIDA;
        return new EscolhaParAliquotas(anexo, tipo, regra, par, semAusente, caminho, motivo);
    }

    /** Primeira situação vigente da regra cujo campo tem, no documento, o valor da linha. */
    private static SituacaoIssParAliquotaSimples situacao(RegraAliquotaSimplesEnum regra, Map<String, Integer> fatos,
            List<SituacaoIssParAliquotaSimples> situacoes) {
        return situacoes.stream()
                .filter(s -> s.getRegra() == regra)
                .filter(s -> s.getValor().equals(fatos.get(s.getCampo())))
                .findFirst()
                .orElse(null);
    }

    private static boolean informado(String valor) {
        return valor != null && !valor.isBlank();
    }

    /** Converte texto em inteiro de forma tolerante ({@code null} quando inválido). */
    static Integer inteiro(String valor) {
        if (!informado(valor)) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
