package br.gov.serpro.rtc.api.atividadesimples.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * Entidade JPA da tabela {@code ATIVIDADE_SIMPLES_NACIONAL}, que representa as
 * atividades do Simples Nacional e o(s) anexo(s) aplicável(is).
 *
 * <p>
 *
 * ISOLADO.
 *
 * Vive sob {@code br.gov.serpro.rtc.api.atividadesimples} e não depende do
 * pacote {@code simplesnacional} nem do domínio do regime geral.
 *
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "ATIVIDADE_SIMPLES_NACIONAL")
public class AtividadeSimplesNacional {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "ATSN_ID")
    private Long id;

    @NotNull
    @Column(name = "ATSN_ORDEM")
    private Integer ordem;

    @NotNull
    @Column(name = "ATSN_DESCRICAO")
    private String descricao;

    /**
     * Anexo aplicável quando o fator "r" é INFERIOR a 28% — ou quando a
     * atividade não depende do fator "r" (é então o seu único anexo). Na carga
     * oficial, a atividade 09 tem aqui o Anexo V (LC 123/2006, art. 18, § 5º-M;
     * Resolução CGSN 190/2026, cabeçalho do Anexo V). Anulável: a linha de
     * operações não tributáveis (código 90) não possui anexo de apuração.
     */
    @Column(name = "ATSN_ANEXO_SEM_FATOR_R")
    private String anexoSemFatorR;

    /**
     * Anexo aplicável quando o fator "r" é IGUAL OU SUPERIOR a 28% (fator "r"
     * atingido). Preenchido apenas para a atividade sujeita ao fator "r" (a 09,
     * com o Anexo III — LC 123/2006, art. 18, § 5º-J; Resolução CGSN 190/2026,
     * cabeçalho do Anexo III); nulo nas demais.
     */
    @Column(name = "ATSN_ANEXO_COM_FATOR_R")
    private String anexoComFatorR;

    @NotNull
    @Column(name = "ATSN_ROTULO")
    private String rotulo;

    @NotNull
    @Column(name = "ATSN_ROTULO_CURTO")
    private String rotuloCurto;

    @NotNull
    @Column(name = "ATSN_INICIO_VIGENCIA")
    private LocalDate inicioVigencia;

    @Column(name = "ATSN_FIM_VIGENCIA")
    private LocalDate fimVigencia;

    /**
     * Código oficial de atividade do Simples Nacional (tag {@code cAtivSN} dos
     * leiautes de DF-e, NT 2026.005): 2 caracteres com zero à esquerda. Único
     * entre as linhas vigentes. A carga oficial traz os códigos '01' a '14' e
     * o código especial '90' (Operações não Tributáveis), sem anexos.
     */
    @Column(name = "ATSN_CATIVSN")
    private String cAtivSN;

    /**
     * {@code true}: linha de operações não tributáveis (hoje, apenas o código
     * 90). O cálculo usa este indicador na validação de incompatibilidade com
     * classificações tributárias que declaram receita (regra UB14-90 da
     * NT 2026.005) — o valor '90' não fica fixo no código Java.
     */
    @NotNull
    @Column(name = "ATSN_IN_NAO_TRIBUTAVEL")
    private Boolean naoTributavel;

    /**
     * {@code true}: o percentual do ISS entra no cálculo do Simples da
     * atividade (carga: 07, 09, 10, 12, 13 e 14). Sem ISS no Simples, o item
     * usa o par de alíquotas sem redistribuição do ISS e não exige
     * {@code tribISSQN} nem {@code tpRetISSQN}.
     */
    @NotNull
    @Column(name = "ATSN_IN_ISS_NO_SIMPLES")
    private Boolean issNoSimples;

    /**
     * {@code true}: a atividade admite benefício municipal do ISS ({@code tpBM};
     * carga: 12 e 13 — regra E0536 da NFS-e, subitens 7.02 e 7.05). Nas demais,
     * o {@code tpBM} informado é desconsiderado, com aviso.
     */
    @NotNull
    @Column(name = "ATSN_IN_ADMITE_BENEFICIO_MUNICIPAL")
    private Boolean admiteBeneficioMunicipal;

}
