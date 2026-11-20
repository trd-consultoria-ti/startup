package br.gov.serpro.rtc.api.versao.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.gov.serpro.rtc.api.versao.dto.VersaoOfflineDomain;
import br.gov.serpro.rtc.api.versao.service.VersaoAplicacaoService;
import br.gov.serpro.rtc.api.versao.service.VersaoOfflineService;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST disponível no perfil {@code offline} que informa o status da
 * versão da aplicação offline.
 *
 * <p>Segue o modelo dos demais controllers do projeto: implementa a interface
 * OpenAPI ({@link VersaoOfflineControllerOpenApi}) e devolve os headers de
 * versão ({@code X-CALC-*}) via {@link VersaoAplicacaoService}.
 */
@Profile("offline")
@RestController
@RequiredArgsConstructor
public class VersaoOfflineController implements VersaoOfflineControllerOpenApi {

    private final VersaoOfflineService versaoOfflineService;
    private final VersaoAplicacaoService versaoAplicacaoService;

    @Override
    @GetMapping(value = "/versao/status", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<VersaoOfflineDomain> getVersaoStatus() {
        return ResponseEntity.ok()
                .headers(versaoAplicacaoService.getHeaders())
                .body(versaoOfflineService.verificarVersao());
    }
}
