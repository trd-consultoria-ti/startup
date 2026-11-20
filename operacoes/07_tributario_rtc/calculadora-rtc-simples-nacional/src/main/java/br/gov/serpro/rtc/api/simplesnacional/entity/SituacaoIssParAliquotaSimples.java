package br.gov.serpro.rtc.api.simplesnacional.entity;

import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegraAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.TipoAliquotaSimplesEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entidade JPA somente leitura da tabela
 * {@code SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL}: um fato do documento
 * (campo e valor) que tira o percentual do ISS do cálculo do Simples e, por
 * isso, leva o item ao par de alíquotas sem redistribuição do ISS.
 *
 * <p>
 * Alimenta as regras 4 ({@code ISS_FORA_DO_SIMPLES}), 6
 * ({@code ISS_NAO_INCIDE}), 7 ({@code ISENCAO_MUNICIPAL}) e 8
 * ({@code ISS_RETIDO}). A ordem de avaliação das regras fica em código
 * ({@link RegraAliquotaSimplesEnum}); {@code ordem} documenta o número da
 * regra e é conferida pelo teste de carga.
 */
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "SITUACAO_ISS_PAR_ALIQUOTA_SIMPLES_NACIONAL")
public class SituacaoIssParAliquotaSimples {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "SIPA_ID")
    private Long id;

    /** Nome do campo no contrato da API ({@code CRT}, {@code regApIBSCBSSN}, {@code tribISSQN}, ...). */
    @NotNull
    @Column(name = "SIPA_CAMPO")
    private String campo;

    @NotNull
    @Column(name = "SIPA_VALOR")
    private Integer valor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "SIPA_REGRA")
    private RegraAliquotaSimplesEnum regra;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "SIPA_TIPO_ALIQUOTA")
    private TipoAliquotaSimplesEnum tipoAliquota;

    @NotNull
    @Column(name = "SIPA_ORDEM")
    private Integer ordem;

    @NotNull
    @Column(name = "SIPA_INICIO_VIGENCIA")
    private LocalDate inicioVigencia;

    @Column(name = "SIPA_FIM_VIGENCIA")
    private LocalDate fimVigencia;

}
