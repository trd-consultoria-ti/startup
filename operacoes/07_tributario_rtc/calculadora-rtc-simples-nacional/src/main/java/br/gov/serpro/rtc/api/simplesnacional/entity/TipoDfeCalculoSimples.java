package br.gov.serpro.rtc.api.simplesnacional.entity;

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
 * Entidade JPA somente leitura da tabela {@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}:
 * os tipos de documento fiscal eletrônico suportados pelo cálculo do Simples
 * Nacional. Operação com tpDoc sem vínculo vigente nesta tabela é rejeitada com
 * HTTP 422 (erro VSN-015) antes de qualquer item ser processado.
 *
 * <p>
 * Esta whitelist global é uma camada distinta da associativa por atividade
 * ({@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}): aqui decide-se se a operação
 * inteira pode ser calculada; lá decide-se, por item, se o código de atividade é
 * admissível no documento.
 *
 * <p>
 * Também parametriza, por documento, o que muda no cálculo: o vocabulário do
 * regime, a precisão da alíquota usada no cálculo, o número máximo de itens e
 * a admissão do Imposto Seletivo. O endpoint clássico ignora essas colunas
 * (contrato e resultados inalterados).
 *
 * <p>
 * Suportar um novo documento é um INSERT nesta tabela, sem alteração de código Java.
 */
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "TIPO_DFE_CALCULO_SIMPLES_NACIONAL")
public class TipoDfeCalculoSimples {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "TDCS_ID")
    private Long id;

    /** Chave do tipo de documento na tabela compartilhada {@code TIPO_DFE}. */
    @NotNull
    @Column(name = "TDCS_TPDF_ID")
    private Long tipoDfeId;

    /** Vocabulário em que o documento informa o regime ({@code CRT} ou {@code NFSE}). */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "TDCS_VOCABULARIO_REGIME")
    private VocabularioRegimeSimplesEnum vocabularioRegime;

    /**
     * Casas decimais da alíquota usada no cálculo, arredondada em HALF_EVEN
     * ({@code ArredondamentoUtils}) depois da transformação da compra
     * governamental. Nulo = precisão integral.
     * NFS-e: 2 (campos 1-2V2 do gTribSN).
     */
    @Column(name = "TDCS_CASAS_ALIQUOTA_CALCULO")
    private Integer casasAliquotaCalculo;

    /** Número máximo de itens da operação. Nulo = sem limite. NFS-e: 1. */
    @Column(name = "TDCS_MAXIMO_ITENS")
    private Integer maximoItens;

    /** {@code false}: o documento não tem Imposto Seletivo (valor diferente de zero ⇒ VSN-035). */
    @NotNull
    @Column(name = "TDCS_IN_ADMITE_IS")
    private Boolean admiteImpostoSeletivo;

    @NotNull
    @Column(name = "TDCS_INICIO_VIGENCIA")
    private LocalDate inicioVigencia;

    @Column(name = "TDCS_FIM_VIGENCIA")
    private LocalDate fimVigencia;

}
