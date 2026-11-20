package br.gov.serpro.rtc.api.versao.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;

import br.gov.serpro.rtc.api.versao.dto.VersaoOfflineDomain;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *
 * Contrato OpenAPI do endpoint de status de versão da aplicação offline.
 *
 * <p>
 *
 * Espelha o estilo de {@code AtividadeSimplesControllerOpenApi}: a interface
 * concentra as anotações Swagger e o controller concentra o fluxo HTTP.
 *
 * <p>
 * O endpoint só existe no perfil {@code offline}: compara a instalação local
 * com as versões publicadas no endpoint {@code /versao} da instalação online
 * apontada por {@code versao.url}.
 *
 */
@Tag(name = "Versão da aplicação offline",
        description = "Status de atualização da aplicação e do banco de dados locais em relação às versões remotas")
public interface VersaoOfflineControllerOpenApi {

    @Operation(summary = "Consultar o status de versão da aplicação offline",
            description = "Compara as versões locais da aplicação e do banco de dados com as versões remotas "
                    + "publicadas no endpoint /versao da instalação online e informa, campo a campo, se a "
                    + "instalação local está atualizada. Quando a consulta remota está indisponível, os campos "
                    + "remotos retornam nulos e os indicadores resultam falsos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comparativo de versões calculado", content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VersaoOfflineDomain.class)) }) })
    ResponseEntity<VersaoOfflineDomain> getVersaoStatus();
}
