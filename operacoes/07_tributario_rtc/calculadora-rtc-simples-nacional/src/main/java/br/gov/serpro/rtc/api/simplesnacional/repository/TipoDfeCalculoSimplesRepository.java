package br.gov.serpro.rtc.api.simplesnacional.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.simplesnacional.entity.TipoDfeCalculoSimples;

/**
 * Repositório Spring Data JPA, somente leitura, dos tipos de documento fiscal
 * suportados pelo cálculo do Simples Nacional ({@link TipoDfeCalculoSimples}).
 *
 * <p>
 * ISOLADO. A consulta lê a tabela de catálogo compartilhada {@code TIPO_DFE}
 * por consulta nativa, sem importar entidades do domínio do regime geral.
 *
 * <p>
 * Todos os lookups de vigência usam a DATA DO FATO GERADOR da operação,
 * nunca a data corrente.
 */
@Repository
public interface TipoDfeCalculoSimplesRepository extends JpaRepository<TipoDfeCalculoSimples, Long> {

    /** Sigla da linha TIPO_DFE exata previamente selecionada para o cálculo. */
    @NativeQuery(value = """
            SELECT td.TPDF_SIGLA
            FROM TIPO_DFE td
            WHERE td.TPDF_ID = :tipoDfeId
              AND :data BETWEEN td.TPDF_INICIO_VIGENCIA AND COALESCE(td.TPDF_FIM_VIGENCIA, :data)
            """)
    Optional<String> buscarSiglaVigentePorId(@Param("tipoDfeId") Long tipoDfeId, @Param("data") LocalDate data);

    /** Indicador do tipo de DF-e real selecionado, pela mesma vigência e ID. */
    @NativeQuery(value = """
            SELECT td.TPDF_B_PERMITE_NCM_ZERO
            FROM TIPO_DFE td
            WHERE td.TPDF_ID = :tipoDfeId
              AND :data BETWEEN td.TPDF_INICIO_VIGENCIA AND COALESCE(td.TPDF_FIM_VIGENCIA, :data)
            """)
    Optional<Boolean> permiteNcmZeroVigentePorId(@Param("tipoDfeId") Long tipoDfeId,
            @Param("data") LocalDate data);

    /**
     * Conta as linhas vigentes de {@code TIPO_DFE} do tipo de documento
     * ({@code tpDoc}, coluna {@code TPDF_TIPO}) marcadas como SEM tratamento
     * na Calculadora ({@code TPDF_B_POSSUI_TRATAMENTO_CALCULADORA = 0}; hoje,
     * 77, 94, 95 e 96). Maior que zero = documento conhecido, ainda sem
     * tratamento implementado (VSN-015); zero = tem tratamento ou não existe.
     */
    @NativeQuery(value = """
            SELECT COUNT(1)
            FROM TIPO_DFE td
            WHERE td.TPDF_TIPO = :tpDoc
              AND td.TPDF_B_POSSUI_TRATAMENTO_CALCULADORA = 0
              AND :data BETWEEN td.TPDF_INICIO_VIGENCIA AND COALESCE(td.TPDF_FIM_VIGENCIA, :data)
            """)
    long contarSemTratamentoCalculadoraVigentes(@Param("tpDoc") int tpDoc, @Param("data") LocalDate data);

    /**
     * Conta os vínculos vigentes do tipo de documento ({@code tpDoc}: 55, 65,
     * 91, ...) com o cálculo do Simples Nacional, via {@code TIPO_DFE} (coluna
     * {@code TPDF_TIPO}). Zero = tpDoc não suportado (a operação inteira é
     * rejeitada com HTTP 422, erro VSN-015).
     */
    @NativeQuery(value = """
            SELECT COUNT(1)
            FROM TIPO_DFE_CALCULO_SIMPLES_NACIONAL t
            JOIN TIPO_DFE td ON td.TPDF_ID = t.TDCS_TPDF_ID
            WHERE td.TPDF_TIPO = :tpDoc
            AND :data BETWEEN t.TDCS_INICIO_VIGENCIA AND COALESCE(t.TDCS_FIM_VIGENCIA, :data)
            AND :data BETWEEN td.TPDF_INICIO_VIGENCIA AND COALESCE(td.TPDF_FIM_VIGENCIA, :data)
            """)
    long contarVinculosVigentes(@Param("tpDoc") int tpDoc, @Param("data") LocalDate data);

    /**
     * Retorna as linhas vigentes do tipo de documento ({@code tpDoc}) no cálculo
     * do Simples Nacional, com as características do cálculo por documento
     * (vocabulário do regime, casas da alíquota, máximo de itens e Imposto
     * Seletivo), da vigência mais recente para a mais antiga. Lista vazia =
     * tpDoc não suportado (VSN-015).
     */
    @NativeQuery(value = """
            SELECT t.*
            FROM TIPO_DFE_CALCULO_SIMPLES_NACIONAL t
            JOIN TIPO_DFE td ON td.TPDF_ID = t.TDCS_TPDF_ID
            WHERE td.TPDF_TIPO = :tpDoc
            AND :data BETWEEN t.TDCS_INICIO_VIGENCIA AND COALESCE(t.TDCS_FIM_VIGENCIA, :data)
            AND :data BETWEEN td.TPDF_INICIO_VIGENCIA AND COALESCE(td.TPDF_FIM_VIGENCIA, :data)
            ORDER BY t.TDCS_INICIO_VIGENCIA DESC
            """)
    List<TipoDfeCalculoSimples> buscarVigentes(@Param("tpDoc") int tpDoc, @Param("data") LocalDate data);

}
