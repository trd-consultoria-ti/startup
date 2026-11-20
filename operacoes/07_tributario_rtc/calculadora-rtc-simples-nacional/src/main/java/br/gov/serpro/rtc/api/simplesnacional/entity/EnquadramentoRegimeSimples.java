package br.gov.serpro.rtc.api.simplesnacional.entity;

import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entidade JPA somente leitura da tabela
 * {@code ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL}: a combinação dos campos de
 * regime do documento que leva a um regime de apuração (código neutro).
 *
 * <p>
 * Vocabulário {@code CRT}: {@code crt} preenchido e {@code tpRegimeIBSCBS}
 * nulo quando o documento não traz a tag. Vocabulário {@code NFSE}:
 * {@code opSimpNac} e {@code regApIBSCBSSN} preenchidos. O {@code regApTribSN}
 * não entra no enquadramento: ele só participa da escolha do par de alíquotas.
 *
 * <p>
 * Combinação válida no leiaute sem linha vigente aqui está fora do escopo do
 * cálculo (VSN-019).
 */
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "ENQUADRAMENTO_REGIME_SIMPLES_NACIONAL")
public class EnquadramentoRegimeSimples {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "ERSN_ID")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ERSN_VOCABULARIO_REGIME")
    private VocabularioRegimeSimplesEnum vocabularioRegime;

    @Column(name = "ERSN_CRT")
    private Integer crt;

    @Column(name = "ERSN_TPREGIMEIBSCBS")
    private Integer tpRegimeIBSCBS;

    @Column(name = "ERSN_OPSIMPNAC")
    private Integer opSimpNac;

    @Column(name = "ERSN_REGAPIBSCBSSN")
    private Integer regApIBSCBSSN;

    /** Regime de apuração a que a combinação leva. */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ERSN_RASN_ID")
    private RegimeApuracaoSimples regime;

    @NotNull
    @Column(name = "ERSN_INICIO_VIGENCIA")
    private LocalDate inicioVigencia;

    @Column(name = "ERSN_FIM_VIGENCIA")
    private LocalDate fimVigencia;

}
