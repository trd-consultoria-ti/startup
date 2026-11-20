package br.gov.serpro.rtc.api.simplesnacional.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.simplesnacional.entity.EnquadramentoRegimeSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;

/**
 * Repositório Spring Data JPA, somente leitura, do enquadramento dos campos de
 * regime do documento no regime de apuração do Simples Nacional
 * ({@link EnquadramentoRegimeSimples}).
 *
 * <p>
 * Todos os lookups de vigência usam a DATA DO FATO GERADOR da operação,
 * nunca a data corrente.
 */
@Repository
public interface EnquadramentoRegimeSimplesRepository extends JpaRepository<EnquadramentoRegimeSimples, Long> {

    /**
     * Retorna os enquadramentos vigentes na data do fato gerador para a
     * combinação de campos de regime do documento, no vocabulário informado,
     * da vigência mais recente para a mais antiga (o primeiro é o aplicável;
     * lista vazia = combinação fora do escopo do cálculo ou falha de
     * parametrização, conforme o endpoint). Campo nulo casa apenas com coluna
     * nula: no vocabulário CRT, {@code tpRegimeIBSCBS} nulo é o documento sem a
     * tag; os campos do outro vocabulário são sempre nulos.
     */
    @Query("""
            SELECT e
            FROM EnquadramentoRegimeSimples e
            JOIN FETCH e.regime r
            WHERE e.vocabularioRegime = :vocabulario
            AND ((:crt IS NULL AND e.crt IS NULL) OR e.crt = :crt)
            AND ((:tpRegimeIBSCBS IS NULL AND e.tpRegimeIBSCBS IS NULL) OR e.tpRegimeIBSCBS = :tpRegimeIBSCBS)
            AND ((:opSimpNac IS NULL AND e.opSimpNac IS NULL) OR e.opSimpNac = :opSimpNac)
            AND ((:regApIBSCBSSN IS NULL AND e.regApIBSCBSSN IS NULL) OR e.regApIBSCBSSN = :regApIBSCBSSN)
            AND :data BETWEEN e.inicioVigencia AND COALESCE(e.fimVigencia, :data)
            AND :data BETWEEN r.inicioVigencia AND COALESCE(r.fimVigencia, :data)
            ORDER BY e.inicioVigencia DESC, r.inicioVigencia DESC
            """)
    List<EnquadramentoRegimeSimples> buscarEnquadramentosVigentes(
            @Param("vocabulario") VocabularioRegimeSimplesEnum vocabulario,
            @Param("crt") Integer crt,
            @Param("tpRegimeIBSCBS") Integer tpRegimeIBSCBS,
            @Param("opSimpNac") Integer opSimpNac,
            @Param("regApIBSCBSSN") Integer regApIBSCBSSN,
            @Param("data") LocalDate data);

}
