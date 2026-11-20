package br.gov.serpro.rtc.api.versao.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;

import br.gov.serpro.rtc.api.versao.dto.VersaoDomain;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Contrato OpenAPI do endpoint de informações de versão da aplicação.
 *
 * <p>Espelha o estilo de {@code AtividadeSimplesControllerOpenApi}: a interface
 * concentra as anotações Swagger e o controller concentra o fluxo HTTP.
 *
 * <p>O endpoint existe em todos os perfis. É também o contrato que o comparador
 * do modo offline ({@code /versao/status}) consome na instalação online
 * apontada por {@code versao.url}.
 */
@Tag(name = "Versão da aplicação",
        description = "Informações de versão da aplicação, da base de dados e do ambiente")
public interface VersaoControllerOpenApi {

    @Operation(summary = "Consultar as informações de versão da aplicação",
            description = "Informa a versão da aplicação, a versão, descrição e data da base de dados "
                    + "em uso e o ambiente em que a aplicação está publicada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informações de versão da instalação", content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VersaoDomain.class)) }) })
    ResponseEntity<VersaoDomain> getVersao();
}
