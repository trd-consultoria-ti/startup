package br.gov.serpro.rtc.api.versao.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.gov.serpro.rtc.api.versao.dto.VersaoDomain;
import br.gov.serpro.rtc.api.versao.service.VersaoAplicacaoService;
import br.gov.serpro.rtc.api.versao.service.VersaoService;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST, disponível em todos os perfis, com as informações de versão
 * desta instalação (aplicação, base de dados e ambiente).
 *
 * <p>Segue o modelo dos demais controllers do projeto: implementa a interface
 * OpenAPI ({@link VersaoControllerOpenApi}) e devolve os headers de versão
 * ({@code X-CALC-*}) via {@link VersaoAplicacaoService}.
 */
@RestController
@RequiredArgsConstructor
public class VersaoController implements VersaoControllerOpenApi {

    private final VersaoService versaoService;
    private final VersaoAplicacaoService versaoAplicacaoService;

    @Override
    @GetMapping(value = "/versao", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<VersaoDomain> getVersao() {
        return ResponseEntity.ok()
                .headers(versaoAplicacaoService.getHeaders())
                .body(versaoService.getVersao());
    }
}
