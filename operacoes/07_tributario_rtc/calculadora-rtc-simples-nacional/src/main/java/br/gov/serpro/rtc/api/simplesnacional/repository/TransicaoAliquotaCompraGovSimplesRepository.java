package br.gov.serpro.rtc.api.simplesnacional.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.simplesnacional.entity.TransicaoAliquotaCompraGovSimples;

/**
 * Repositório Spring Data JPA, somente leitura, do fator de transição de
 * alíquotas de compra governamental do Simples Nacional
 * ({@link TransicaoAliquotaCompraGovSimples}).
 *
 * <p>
 * Todos os lookups de vigência usam a DATA DO FATO GERADOR da operação,
 * nunca a data corrente.
 */
@Repository
public interface TransicaoAliquotaCompraGovSimplesRepository
        extends JpaRepository<TransicaoAliquotaCompraGovSimples, Long> {

    /**
     * Retorna as linhas vigentes na data do fato gerador, da vigência mais
     * recente para a mais antiga (a primeira da lista é a aplicável; lista
     * vazia = data fora das vigências, erro VSN-025 — o cálculo nunca assume
     * fator zero implícito).
     */
    @Query("""
            SELECT t
            FROM TransicaoAliquotaCompraGovSimples t
            WHERE :data BETWEEN t.inicioVigencia AND COALESCE(t.fimVigencia, :data)
            ORDER BY t.inicioVigencia DESC
            """)
    List<TransicaoAliquotaCompraGovSimples> buscarVigentes(@Param("data") LocalDate data);

}
