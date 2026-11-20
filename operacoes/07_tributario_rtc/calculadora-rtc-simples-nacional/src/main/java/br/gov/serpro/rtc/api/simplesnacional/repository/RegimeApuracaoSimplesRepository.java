package br.gov.serpro.rtc.api.simplesnacional.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;

/**
 * Repositório Spring Data JPA, somente leitura, da matriz de regime do cálculo
 * do Simples Nacional ({@link RegimeApuracaoSimples}). A linha aplicável à
 * operação é resolvida pelo enquadramento dos campos de regime do documento
 * ({@code EnquadramentoRegimeSimplesRepository}), que a traz junto.
 */
@Repository
public interface RegimeApuracaoSimplesRepository extends JpaRepository<RegimeApuracaoSimples, Long> {

}
