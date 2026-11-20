package br.gov.serpro.rtc.api.atividadesimples.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;

/**
 *
 * Repositório Spring Data JPA para acesso às atividades do Simples Nacional.
 *
 * <p>
 *
 * ISOLADO.
 *
 * Vive sob {@code br.gov.serpro.rtc.api.atividadesimples} e não depende do
 * domínio do regime geral. Lê as atividades do banco de dados SQLite.
 *
 */
@Repository
public interface AtividadeSimplesRepository extends JpaRepository<AtividadeSimplesNacional, Long> {

    /**
     * Retorna as atividades vigentes na data informada, já ordenadas por
     * {@code ATSN_ORDEM}. Considera vigente a atividade cuja data de referência
     * esteja entre o início e o fim de vigência (fim nulo = vigência aberta).
     */
    @Query("""
            SELECT a
            FROM AtividadeSimplesNacional a
            WHERE :data BETWEEN a.inicioVigencia AND COALESCE(a.fimVigencia, :data)
            ORDER BY a.ordem ASC
            """)
    List<AtividadeSimplesNacional> buscarVigentes(@Param("data") LocalDate data);

    /**
     * Projeção de um vínculo vigente da associativa
     * {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}: a atividade e o tipo do
     * documento fiscal ({@code TPDF_TIPO}) do vínculo.
     */
    interface VinculoTpDocAtividade {
        Long getAtividadeId();
        Integer getTpDoc();
    }

    /**
     * Retorna, em UMA única consulta, todos os vínculos vigentes na data da
     * associativa {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}, com o
     * {@code TPDF_TIPO} do documento de cada vínculo (55, 65, 91, ...).
     * Somente contam os vínculos cujo {@code TIPO_DFE} também está vigente na
     * data. Semântica de WHITELIST (mundo fechado): a atividade é admissível
     * apenas nos documentos aqui devolvidos — atividade ausente do resultado
     * não é admissível em documento nenhum.
     *
     * <p>DUPLICAÇÃO DELIBERADA em relação a
     * {@code simplesnacional.repository.AtividadeSimplesCalculoRepository}
     * (consulta {@code contarVinculosComTpDoc}, usada na validação do cAtivSN
     * do cálculo): este pacote é ISOLADO em relação à validação do cálculo,
     * então a regra de admissibilidade é reproduzida aqui — em forma de
     * listagem única, pois a listagem resolve todas as atividades de uma vez
     * (uma query, não uma por atividade). A coerência entre as duas
     * implementações é garantida por teste automatizado
     * ({@code AtividadeSimplesControllerTest.admissibilidadeCoerenteComOCalculo...}).
     */
    @NativeQuery("""
            SELECT t.TDAS_ATSN_ID AS atividadeId, td.TPDF_TIPO AS tpDoc
            FROM TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL t
            JOIN TIPO_DFE td ON td.TPDF_ID = t.TDAS_TPDF_ID
                AND :data BETWEEN td.TPDF_INICIO_VIGENCIA AND COALESCE(td.TPDF_FIM_VIGENCIA, :data)
            WHERE :data BETWEEN t.TDAS_INICIO_VIGENCIA AND COALESCE(t.TDAS_FIM_VIGENCIA, :data)
            """)
    List<VinculoTpDocAtividade> buscarVinculosTpDocVigentes(@Param("data") LocalDate data);

    /**
     * Conta as linhas de {@code TIPO_DFE} com o tipo informado
     * ({@code TPDF_TIPO}), para a validação do query param {@code tpDoc}:
     * zero = tpDoc desconhecido ⇒ HTTP 400. A validação NÃO consulta a
     * whitelist {@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}: este endpoint
     * responde "esta atividade é admissível neste documento?" (associativa);
     * "este documento calcula?" é papel do endpoint de cálculo (VSN-015).
     */
    @NativeQuery("""
            SELECT COUNT(1)
            FROM TIPO_DFE td
            WHERE td.TPDF_TIPO = :tpDoc
            """)
    long contarTiposDfePorTipo(@Param("tpDoc") int tpDoc);

}
