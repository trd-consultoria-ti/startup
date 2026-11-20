package br.gov.serpro.rtc.api.simplesnacional.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
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
 * Entidade JPA somente leitura da tabela {@code TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL}:
 * o efeito de cada Tipo de Receita Bruta do Simples Nacional (tpRBSN) sobre o cálculo,
 * conforme a NT 2026.005 (seções 5.3.2 e 5.3.4).
 *
 * <p>
 * Cada linha parametriza, para um código de tpRBSN vigente:
 * <ul>
 * <li>se a receita bruta do item é computada (senão, vRBSNitem = 0);</li>
 * <li>se o vIS integra a receita bruta computada;</li>
 * <li>a fração da receita tributada (1.0 = integral, 0.5 = metade, 0.0 = nada);</li>
 * <li>se o valor calculado fica pendente/suspenso em vez de devido;</li>
 * <li>se as alíquotas efetivas são exigidas quando o cálculo as usa;</li>
 * <li>se o tipo é incompatível com código de atividade de operações não
 * tributáveis (regra UB14-90 da NT 2026.005);</li>
 * <li>a descrição oficial, exposta na saída da API (campo {@code tpRBSNDescricao}).</li>
 * </ul>
 *
 * <p>
 * Linhas por vocabulário ({@code TRBS_VOCABULARIO_REGIME}): a linha PADRÃO tem
 * vocabulário nulo (família CRT e endpoint clássico); a NFS-e tem linhas
 * próprias (só o tpRBSN 1 gera valor, sem pendente/suspenso — AnexoVI, regras
 * 203/204 e 207/208). A busca procura primeiro a linha do vocabulário do
 * documento e, se não houver, a padrão.
 *
 * <p>
 * Tabela exclusiva do cálculo do Simples Nacional; mudanças de efeito são feitas por
 * script de migração, sem alteração de código Java.
 */
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "TIPO_RECEITA_BRUTA_SIMPLES_NACIONAL")
public class TipoReceitaBrutaSimples {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "TRBS_ID")
    private Long id;

    /** Código do tpRBSN (0, 1, 2, 3, 4, 5 ou 9), mesmo domínio da coluna {@code CLTR_TPRBSN}. */
    @NotNull
    @Column(name = "TRBS_CD")
    private Integer codigo;

    /** Descrição oficial do tipo (campo P27 do leiaute do evento da NT 2026.005). */
    @NotNull
    @Column(name = "TRBS_DESCRICAO")
    private String descricao;

    /**
     * {@code false}: a receita bruta do item é zero; {@code true}: baseCalculo,
     * somada ao vIS quando {@code impostoSeletivoCompoeReceita}.
     */
    @NotNull
    @Column(name = "TRBS_IN_COMPOE_RECEITA")
    private Boolean compoeReceita;

    /**
     * {@code true}: o vIS integra a receita bruta do item quando ela é computada;
     * {@code false}: a receita bruta é apenas a baseCalculo. Parametrizado por
     * vigência (carga oficial: {@code false} em TODAS as vigências — o vIS fica
     * fora da receita bruta; a coluna é a alavanca para mudanças legais futuras,
     * sem alteração de código); inerte quando {@code compoeReceita} é {@code false}.
     */
    @NotNull
    @Column(name = "TRBS_IN_IS_COMPOE_RECEITA")
    private Boolean impostoSeletivoCompoeReceita;

    /** Fração da receita bruta tributada (1.0, 0.5 ou 0.0). */
    @NotNull
    @Column(name = "TRBS_FATOR_RECEITA")
    private BigDecimal fatorReceita;

    /** {@code true}: o valor calculado fica pendente/suspenso (exportação indireta). */
    @NotNull
    @Column(name = "TRBS_IN_PENDENTE_SUSPENSO")
    private Boolean pendenteSuspenso;

    /** {@code true}: as alíquotas efetivas (pIBSSN/pCBSSN) são exigidas quando o cálculo as usa. */
    @NotNull
    @Column(name = "TRBS_IN_EXIGE_ALIQUOTA")
    private Boolean exigeAliquota;

    /**
     * {@code true}: incompatível com código de atividade de operações não tributáveis
     * (regra UB14-90 da NT 2026.005, msg 1277).
     */
    @NotNull
    @Column(name = "TRBS_IN_VEDADO_NAO_TRIBUTAVEL")
    private Boolean vedadoNaoTributavel;

    /** Vocabulário da linha; nulo = linha padrão. */
    @Enumerated(EnumType.STRING)
    @Column(name = "TRBS_VOCABULARIO_REGIME")
    private VocabularioRegimeSimplesEnum vocabularioRegime;

    @NotNull
    @Column(name = "TRBS_INICIO_VIGENCIA")
    private LocalDate inicioVigencia;

    @Column(name = "TRBS_FIM_VIGENCIA")
    private LocalDate fimVigencia;

}
