package br.gov.serpro.rtc.api.simplesnacional.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.simplesnacional.entity.SituacaoIssParAliquotaSimples;

/**
 * Repositório Spring Data JPA, somente leitura, das situações do ISS que levam
 * ao par de alíquotas sem redistribuição ({@link SituacaoIssParAliquotaSimples}).
 *
 * <p>
 * Todos os lookups de vigência usam a DATA DO FATO GERADOR da operação,
 * nunca a data corrente.
 */
@Repository
public interface SituacaoIssParAliquotaSimplesRepository extends JpaRepository<SituacaoIssParAliquotaSimples, Long> {

    /** Linhas vigentes na data do fato gerador, pela ordem da regra. */
    @Query("""
            SELECT s
            FROM SituacaoIssParAliquotaSimples s
            WHERE :data BETWEEN s.inicioVigencia AND COALESCE(s.fimVigencia, :data)
            ORDER BY s.ordem ASC, s.id ASC
            """)
    List<SituacaoIssParAliquotaSimples> buscarVigentes(@Param("data") LocalDate data);

}
