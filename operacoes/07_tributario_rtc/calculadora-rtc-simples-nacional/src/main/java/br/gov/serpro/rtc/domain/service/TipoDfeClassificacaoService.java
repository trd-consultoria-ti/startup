package br.gov.serpro.rtc.domain.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.domain.model.entity.TipoDfeClassificacao;
import br.gov.serpro.rtc.domain.repository.TipoDfeClassificacaoRepository;
import lombok.RequiredArgsConstructor;

/** Ponte para a consulta de vínculos DFe/classificação usada na validação. */
@Service
@RequiredArgsConstructor
public class TipoDfeClassificacaoService {
    private final TipoDfeClassificacaoRepository repository;

    @Cacheable(cacheNames = "TipoDfeClassificacaoService.buscar")
    public List<TipoDfeClassificacao> buscar(Long idClassificacaoTributaria, LocalDate data) {
        return repository.buscar(idClassificacaoTributaria, data);
    }
}
