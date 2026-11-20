package br.gov.serpro.rtc.api.simplesnacional.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;

/**
 * Repositório Spring Data JPA, somente leitura, dos efeitos dos Tipos de
 * Receita Bruta do Simples Nacional ({@link TipoReceitaBrutaSimples}).
 *
 * <p>
 * Todos os lookups de vigência usam a DATA DO FATO GERADOR da operação,
 * nunca a data corrente.
 */
@Repository
public interface TipoReceitaBrutaSimplesRepository extends JpaRepository<TipoReceitaBrutaSimples, Long> {

    /**
     * Retorna as linhas PADRÃO (vocabulário nulo) do código de tpRBSN vigentes
     * na data do fato gerador, da vigência mais recente para a mais antiga (a
     * primeira da lista é a aplicável; lista vazia = falha de parametrização,
     * erro VSN-026). É a busca do endpoint clássico e do endpoint de
     * atividades.
     */
    @Query("""
            SELECT t
            FROM TipoReceitaBrutaSimples t
            WHERE t.codigo = :codigo
            AND t.vocabularioRegime IS NULL
            AND :data BETWEEN t.inicioVigencia AND COALESCE(t.fimVigencia, :data)
            ORDER BY t.inicioVigencia DESC
            """)
    List<TipoReceitaBrutaSimples> buscarVigentesPorCodigo(@Param("codigo") int codigo,
            @Param("data") LocalDate data);

    /**
     * Retorna as linhas do código de tpRBSN vigentes na data do fato gerador
     * para o vocabulário do documento OU padrão, com as do vocabulário primeiro
     * (a primeira da lista é a aplicável: a linha do vocabulário, se houver;
     * senão, a padrão).
     */
    @Query("""
            SELECT t
            FROM TipoReceitaBrutaSimples t
            WHERE t.codigo = :codigo
            AND (t.vocabularioRegime = :vocabulario OR t.vocabularioRegime IS NULL)
            AND :data BETWEEN t.inicioVigencia AND COALESCE(t.fimVigencia, :data)
            ORDER BY CASE WHEN t.vocabularioRegime IS NULL THEN 1 ELSE 0 END, t.inicioVigencia DESC
            """)
    List<TipoReceitaBrutaSimples> buscarVigentesPorCodigoEVocabulario(@Param("codigo") int codigo,
            @Param("vocabulario") VocabularioRegimeSimplesEnum vocabulario, @Param("data") LocalDate data);

}
