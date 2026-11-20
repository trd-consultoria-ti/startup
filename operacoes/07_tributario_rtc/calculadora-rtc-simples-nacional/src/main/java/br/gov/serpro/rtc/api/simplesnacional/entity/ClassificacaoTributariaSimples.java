package br.gov.serpro.rtc.api.simplesnacional.entity;

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
 * Projeção JPA ENXUTA e somente leitura da tabela {@code CLASSIFICACAO_TRIBUTARIA},
 * restrita ao que o cálculo do Simples Nacional precisa: código do cClassTrib,
 * Tipo de Receita Bruta do Simples Nacional ({@code tpRBSN}, coluna
 * {@code CLTR_TPRBSN}, NT 2026.005) e vigências.
 *
 * <p>
 * ISOLADO E DELIBERADAMENTE DUPLICADO.
 * A entidade completa {@code ClassificacaoTributaria} vivia no domínio do antigo
 * regime geral (removido do projeto) e NÃO devia ser importada
 * pelo pacote do Simples. Compartilhar os DADOS de catálogo é permitido;
 * compartilhar o CÓDIGO do regime geral, não — daí esta projeção própria.
 */
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "CLASSIFICACAO_TRIBUTARIA")
public class ClassificacaoTributariaSimples {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "CLTR_ID")
    private Long id;

    @NotNull
    @Column(name = "CLTR_CD")
    private String codigo;

    /**
     * Tipo de Receita Bruta do Simples Nacional do cClassTrib (coluna
     * {@code tpRBSN} da Tabela de Código de Classificação Tributária do IBS e
     * CBS): 0, 1, 2, 3, 4, 5 ou 9.
     */
    @NotNull
    @Column(name = "CLTR_TPRBSN")
    private Integer tpRBSN;

    @NotNull
    @Column(name = "CLTR_INICIO_VIGENCIA")
    private LocalDate inicioVigencia;

    @Column(name = "CLTR_FIM_VIGENCIA")
    private LocalDate fimVigencia;

}
