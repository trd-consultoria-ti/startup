package br.gov.serpro.rtc.api.simplesnacional.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;

/**
 * Repositório Spring Data JPA de {@link AtividadeSimplesNacional} para o
 * CÁLCULO do Simples Nacional: resolve o código oficial de atividade
 * ({@code cAtivSN}, coluna {@code ATSN_CATIVSN}) e a admissibilidade por
 * documento da associativa {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}
 * (NT 2026.005).
 *
 * <p>
 * SEMÂNTICA DE WHITELIST (mundo fechado) da associativa: uma atividade é
 * admissível em um documento se e somente se existe vínculo vigente entre a
 * atividade e o tpDoc na associativa. Atividade sem vínculo vigente algum não
 * é admissível em documento nenhum — a admissibilidade é um fato afirmativo
 * publicado por NT; ausência de publicação significa ausência de permissão.
 * A associativa NÃO substitui a whitelist global de tpDoc do cálculo
 * ({@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}) — são camadas distintas.
 *
 * <p>
 * ISOLADO. Consome o catálogo do pacote {@code atividadesimples} e as tabelas
 * de catálogo compartilhadas por consulta nativa, sem importar entidades do
 * domínio do regime geral.
 *
 * <p>
 * Todos os lookups de vigência usam a DATA DO FATO GERADOR da operação,
 * nunca a data corrente.
 */
@Repository
public interface AtividadeSimplesCalculoRepository extends JpaRepository<AtividadeSimplesNacional, Long> {

    /**
     * Retorna as atividades com o código oficial informado, vigentes na data do
     * fato gerador, da vigência mais recente para a mais antiga (o código é
     * único entre as linhas vigentes; a primeira da lista é a aplicável).
     */
    @Query("""
            SELECT a
            FROM AtividadeSimplesNacional a
            WHERE a.cAtivSN = :cAtivSN
            AND :data BETWEEN a.inicioVigencia AND COALESCE(a.fimVigencia, :data)
            ORDER BY a.inicioVigencia DESC
            """)
    List<AtividadeSimplesNacional> buscarVigentesPorCAtivSN(@Param("cAtivSN") String cAtivSN,
            @Param("data") LocalDate data);

    /**
     * Conta os vínculos vigentes da atividade com o tipo de documento da
     * operação ({@code tpDoc}: 55, 65, 91, ...), via {@code TIPO_DFE} (coluna
     * {@code TPDF_TIPO}). Zero = código inadmissível para o tpDoc (whitelist:
     * sem vínculo vigente, não há permissão) — o item resulta em
     * {@code NAO_CALCULADO}, sem erro.
     */
    @NativeQuery(value = """
            SELECT COUNT(1)
            FROM TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL t
            JOIN TIPO_DFE td ON td.TPDF_ID = t.TDAS_TPDF_ID
            WHERE t.TDAS_ATSN_ID = :atividadeId
            AND td.TPDF_TIPO = :tpDoc
            AND :data BETWEEN t.TDAS_INICIO_VIGENCIA AND COALESCE(t.TDAS_FIM_VIGENCIA, :data)
            AND :data BETWEEN td.TPDF_INICIO_VIGENCIA AND COALESCE(td.TPDF_FIM_VIGENCIA, :data)
            """)
    long contarVinculosComTpDoc(@Param("atividadeId") Long atividadeId, @Param("tpDoc") int tpDoc,
            @Param("data") LocalDate data);

}
