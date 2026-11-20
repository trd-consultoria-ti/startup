package br.gov.serpro.rtc.api.simplesnacional.entity;

import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import br.gov.serpro.rtc.api.simplesnacional.enumeration.CodigoRegimeApuracaoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.DestinoValorSimplesEnum;
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
 * Entidade JPA somente leitura da tabela {@code REGIME_APURACAO_SIMPLES_NACIONAL}:
 * a matriz de regime do cálculo do Simples Nacional (fórmulas P28 a P33 da
 * NT 2026.005). Cada linha é identificada por um código NEUTRO
 * ({@link CodigoRegimeApuracaoSimplesEnum}) e define se os tributos são
 * calculados, quais campos são gerados e o destino dos valores.
 *
 * <p>
 * A combinação dos campos de regime do documento (CRT e tpRegimeIBSCBS, ou
 * opSimpNac e regApIBSCBSSN) leva a uma linha desta tabela pela tabela de
 * enquadramento ({@link EnquadramentoRegimeSimples}). As combinações recusadas
 * pelo leiaute são rejeitadas pela validação estrutural, em código, antes.
 *
 * <p>
 * Tabela exclusiva do cálculo do Simples Nacional; mudanças de comportamento por
 * regime são feitas por script de migração, sem alteração de código Java.
 */
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "REGIME_APURACAO_SIMPLES_NACIONAL")
public class RegimeApuracaoSimples {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "RASN_ID")
    private Long id;

    /** Código neutro do regime (SN_PLENO, SN_SUBLIMITE, PENDENTE, PENDENTE_SUBLIMITE ou SOMENTE_RECEITA). */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "RASN_CD")
    private CodigoRegimeApuracaoSimplesEnum codigo;

    @NotNull
    @Column(name = "RASN_DESCRICAO")
    private String descricao;

    /**
     * {@code false}: os tributos não são calculados — regime
     * {@code SOMENTE_RECEITA}, em que o item sai {@code NAO_CALCULADO}, só com a
     * receita bruta. Nesse caso as colunas seguintes são nulas.
     */
    @NotNull
    @Column(name = "RASN_IN_CALCULA_TRIBUTOS")
    private Boolean calculaTributos;

    /**
     * {@code false}: o bloco de IBS ({@code pIBSSN}, {@code vIBSSN},
     * {@code vIBSPendSusp}) é suprimido da saída. Caso do excesso de sublimite
     * (CRT=2): o IBS desse contribuinte é declarado pelo próprio emitente no
     * gIBSCBS da nota, fora do evento do Simples. Nulo quando o regime não
     * calcula tributos.
     */
    @Column(name = "RASN_IN_GERA_IBS")
    private Boolean geraIbs;

    /** Destino do valor que o tipo de receita bruta qualificou como devido (nulo quando não calcula). */
    @Enumerated(EnumType.STRING)
    @Column(name = "RASN_DESTINO_DEVIDO")
    private DestinoValorSimplesEnum destinoDevido;

    /**
     * Destino do valor que o tipo de receita bruta qualificou como pendente/suspenso
     * (exportação indireta). O destino {@code ZERO} do regime {@code PENDENTE}
     * reproduz literalmente as fórmulas P32/P33 da NT 2026.005 — intencional.
     * Nulo quando o regime não calcula tributos.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "RASN_DESTINO_PENDENTE_SUSPENSO")
    private DestinoValorSimplesEnum destinoPendenteSuspenso;

    @NotNull
    @Column(name = "RASN_INICIO_VIGENCIA")
    private LocalDate inicioVigencia;

    @Column(name = "RASN_FIM_VIGENCIA")
    private LocalDate fimVigencia;

}
