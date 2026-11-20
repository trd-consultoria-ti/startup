package br.gov.serpro.rtc.api.simplesnacional.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.simplesnacional.entity.ClassificacaoTributariaSimples;

/**
 * Repositório Spring Data JPA, somente leitura, da projeção enxuta
 * {@link ClassificacaoTributariaSimples}, usada pelo cálculo do Simples
 * Nacional para resolver o {@code tpRBSN} do cClassTrib (NT 2026.005).
 *
 * <p>
 * IMPORTANTE — {@code CLTR_CD} NÃO é único: a tabela contém DOIS grupos de
 * classificações, do Imposto Seletivo (tributo 1) e da CBS (tributo 2), com
 * códigos que colidem entre os grupos. O tpRBSN pertence exclusivamente à
 * tabela cClassTrib do IBS/CBS; por isso TODA resolução aqui restringe ao
 * grupo da CBS via {@code TRIBUTO_SITUACAO_TRIBUTARIA} com
 * {@code TRST_TBTO_ID = 2}, além do filtro de vigência pela data da operação.
 *
 * <p>
 * ISOLADO. Não reutiliza o repositório do regime geral; as consultas leem as
 * tabelas de catálogo compartilhadas ({@code TIPO_DFE_CLASSIFICACAO},
 * {@code TIPO_DFE}, {@code TRIBUTO_SITUACAO_TRIBUTARIA}) por consulta nativa,
 * sem importar entidades do domínio do regime geral.
 *
 * <p>
 * EXCEÇÃO DELIBERADA ao isolamento: {@link #contarTratamentosNaoImplementados}
 * lê {@code TRATAMENTO_CLASSIFICACAO} e {@code TRATAMENTO_TRIBUTARIO} — tabelas
 * de parâmetro do regime geral — EXCLUSIVAMENTE para o indicador de ajuste
 * ({@code TRTR_IN_POSSUI_AJUSTE}), que na Calculadora marca a classificação
 * tributária cujo tratamento ainda não foi implementado. Nenhuma expressão,
 * alíquota ou outro parâmetro de cálculo do regime geral é lido.
 *
 * <p>
 * Todos os lookups de vigência usam a DATA DO FATO GERADOR da operação,
 * nunca a data corrente.
 */
@Repository
public interface ClassificacaoTributariaSimplesRepository extends JpaRepository<ClassificacaoTributariaSimples, Long> {

    /**
     * Retorna as classificações tributárias DO GRUPO DA CBS (tributo 2) com o
     * código informado, vigentes na data do fato gerador, da vigência mais
     * recente para a mais antiga (a primeira da lista é a aplicável quando
     * houver mais de uma).
     */
    @NativeQuery(value = """
            SELECT ct.*
            FROM CLASSIFICACAO_TRIBUTARIA ct
            WHERE ct.CLTR_CD = :codigo
            AND :data BETWEEN ct.CLTR_INICIO_VIGENCIA AND COALESCE(ct.CLTR_FIM_VIGENCIA, :data)
            AND ct.CLTR_SITR_ID IN (
                SELECT tst.TRST_SITR_ID
                FROM TRIBUTO_SITUACAO_TRIBUTARIA tst
                WHERE tst.TRST_TBTO_ID = 2
            )
            ORDER BY ct.CLTR_INICIO_VIGENCIA DESC
            """)
    List<ClassificacaoTributariaSimples> buscarVigentes(@Param("codigo") String codigo,
            @Param("data") LocalDate data);

    /**
     * Conta os vínculos vigentes entre o cClassTrib (grupo da CBS) e o tipo de
     * documento fiscal ({@code tpDoc}: 55, 65, 91, ...), via
     * {@code TIPO_DFE_CLASSIFICACAO} e {@code TIPO_DFE} (coluna
     * {@code TPDF_TIPO}). Zero = cClassTrib incompatível com o tpDoc.
     */
    @NativeQuery(value = """
            SELECT COUNT(1)
            FROM TIPO_DFE_CLASSIFICACAO tdcl
            JOIN TIPO_DFE td ON td.TPDF_ID = tdcl.TDCL_TPDF_ID
            JOIN CLASSIFICACAO_TRIBUTARIA ct ON ct.CLTR_ID = tdcl.TDCL_CLTR_ID
            WHERE ct.CLTR_CD = :codigo
            AND td.TPDF_TIPO = :tpDoc
            AND ct.CLTR_SITR_ID IN (
                SELECT tst.TRST_SITR_ID
                FROM TRIBUTO_SITUACAO_TRIBUTARIA tst
                WHERE tst.TRST_TBTO_ID = 2
            )
            AND :data BETWEEN tdcl.TDCL_INICIO_VIGENCIA AND COALESCE(tdcl.TDCL_FIM_VIGENCIA, :data)
            AND :data BETWEEN td.TPDF_INICIO_VIGENCIA AND COALESCE(td.TPDF_FIM_VIGENCIA, :data)
            """)
    long contarVinculosComTpDoc(@Param("codigo") String codigo, @Param("tpDoc") int tpDoc,
            @Param("data") LocalDate data);

    /**
     * Conta os vínculos VIGENTES na data do fato gerador entre o cClassTrib (grupo
     * da CBS) e tratamentos tributários marcados como "sem implementação" na
     * Calculadora — coluna {@code TRTR_IN_POSSUI_AJUSTE = 1} de
     * {@code TRATAMENTO_TRIBUTARIO}, via {@code TRATAMENTO_CLASSIFICACAO}. Maior
     * que zero = classificação ainda não implementada: o item recebe o estado
     * {@code NAO_IMPLEMENTADO} (erro VSN-027) e nada mais é validado nem calculado.
     *
     * <p>
     * É, por decisão de projeto, a PRIMEIRA consulta de banco executada para o
     * item (prioridade sobre as demais validações). Mesma semântica do regime
     * geral, que lança {@code ErroFaltaImplementacaoException} (CAL-007) ao
     * encontrar o indicador no tratamento tributário vigente.
     *
     * <p>
     * Vigências: as três tabelas são filtradas pela data do fato gerador. As
     * vigências do regime geral começam, em regra, em 2026-01-01 e as do
     * cálculo do Simples em 2027-01-01; como toda data aceita pelo Simples é
     * posterior, os vínculos do regime geral já estão em vigor — mas uma
     * classificação encerrada em 2026 (ex.: {@code 220001}) NÃO conta aqui e
     * segue para o erro de vigência (VSN-021). Código desconhecido conta zero
     * (segue para VSN-021 pelo fluxo normal).
     */
    @NativeQuery(value = """
            SELECT COUNT(1)
            FROM TRATAMENTO_CLASSIFICACAO tc
            JOIN TRATAMENTO_TRIBUTARIO tt ON tt.TRTR_ID = tc.TRCL_TRTR_ID
            JOIN CLASSIFICACAO_TRIBUTARIA ct ON ct.CLTR_ID = tc.TRCL_CLTR_ID
            WHERE ct.CLTR_CD = :codigo
            AND tt.TRTR_IN_POSSUI_AJUSTE = 1
            AND ct.CLTR_SITR_ID IN (
                SELECT tst.TRST_SITR_ID
                FROM TRIBUTO_SITUACAO_TRIBUTARIA tst
                WHERE tst.TRST_TBTO_ID = 2
            )
            AND :data BETWEEN ct.CLTR_INICIO_VIGENCIA AND COALESCE(ct.CLTR_FIM_VIGENCIA, :data)
            AND :data BETWEEN tc.TRCL_INICIO_VIGENCIA AND COALESCE(tc.TRCL_FIM_VIGENCIA, :data)
            AND :data BETWEEN tt.TRTR_INICIO_VIGENCIA AND COALESCE(tt.TRTR_FIM_VIGENCIA, :data)
            """)
    long contarTratamentosNaoImplementados(@Param("codigo") String codigo, @Param("data") LocalDate data);

}
