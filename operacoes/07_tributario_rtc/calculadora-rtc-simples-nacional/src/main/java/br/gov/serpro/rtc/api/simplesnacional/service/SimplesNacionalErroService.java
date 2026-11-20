package br.gov.serpro.rtc.api.simplesnacional.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.dto.ErroDescricaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;

/**
 *
 * Serviço de consulta ao catálogo de erros do Simples Nacional (códigos {@code VSN-}).
 *
 * <p>Duplicado deliberadamente do análogo {@code ErroService} do antigo regime
 * geral (removido do projeto), para preservar o isolamento entre os pacotes e a
 * independência dos catálogos de erro.
 *
 */
@Service
public class SimplesNacionalErroService {

    public Optional<ErroDescricaoSimples> buscarPorCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return CodigoValidacaoSimples.fromCodigo(codigo)
                .map(cv -> new ErroDescricaoSimples(codigo, cv.getTitulo(), cv.getDescricao()));
    }
    
}
