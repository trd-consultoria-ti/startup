package br.gov.serpro.rtc.api.simplesnacional.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entidade JPA somente leitura da tabela
 * {@code TRANSICAO_ALIQUOTA_COMPRA_GOV_SIMPLES_NACIONAL}: fator de deslocamento
 * de alíquotas CBS→IBS em compras governamentais de ente estadual, distrital ou
 * municipal ({@code tpEnteGov} = 2, 3 ou 4), conforme NT 2026.005, seção 5.3.3,
 * e arts. 372 e 473 da LC 214/2025.
 *
 * <p>
 * O fator é FRAÇÃO (0.10, e não 10) e alimenta as fórmulas
 * {@code pIBS' = pIBS + fator × pCBS} e {@code pCBS' = (1 − fator) × pCBS}.
 *
 * <p>
 * O caso União ({@code tpEnteGov} = 1) não está nesta tabela: é regra estrutural
 * e atemporal do art. 372 da LC 214/2025 ({@code pCBS' = pCBS + pIBS; pIBS' = 0}),
 * implementada no serviço de cálculo.
 *
 * <p>
 * Tabela exclusiva do Simples Nacional — não confundir com as tabelas de compra
 * governamental do regime geral ({@code TRANSFERENCIA_CBS_ENTE_GOV} e
 * {@code REDUTOR_COMPRA_GOVERNAMENTAL}), proibidas para este cálculo.
 */
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "TRANSICAO_ALIQUOTA_COMPRA_GOV_SIMPLES_NACIONAL")
public class TransicaoAliquotaCompraGovSimples {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "TACG_ID")
    private Long id;

    /** Fator de deslocamento, em fração (0.00 a 1.00). */
    @NotNull
    @Column(name = "TACG_FATOR")
    private BigDecimal fator;

    @NotNull
    @Column(name = "TACG_INICIO_VIGENCIA")
    private LocalDate inicioVigencia;

    @Column(name = "TACG_FIM_VIGENCIA")
    private LocalDate fimVigencia;

}
