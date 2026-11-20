package br.gov.serpro.rtc.api.atividadesimples.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;

import br.gov.serpro.rtc.api.atividadesimples.dto.AtividadeSimplesDomain;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *
 * Contrato OpenAPI do endpoint de atividades VERSUS anexos do Simples Nacional.
 *
 * <p>
 *
 * Espelha o estilo de {@code SimplesNacionalControllerOpenApi},
 * porém sem catálogo de erros próprio — os erros de requisição (ex.:
 * {@code data} ausente ou mal-formada, anexo fora do domínio, tpDoc desconhecido,
 * cClassTrib desconhecido ou fora de vigência) usam o tratamento padrão
 * da API, que devolve a estrutura unificada ({@code ProblemDetail} com
 * {@code errors[]}/{@code pointer}).
 *
 * O endpoint resolve apenas atividade → anexo(s) e as marcações de
 * disponibilidade; NÃO devolve alíquotas.
 *
 * <p>
 * A data de referência ({@code data}) é obrigatória em todas as consultas:
 * os lookups de vigência usam a data informada, nunca a data corrente do servidor.
 *
 */
@Tag(name = "Atividades do Simples Nacional",
        description = "Catálogo de atividades do Simples Nacional e seus anexos (não inclui alíquotas)")
public interface AtividadeSimplesControllerOpenApi {

    @Operation(summary = "Listar atividades do Simples Nacional",
            description = "Retorna TODAS as atividades do Simples Nacional vigentes na data de referência informada, "
                    + "cada uma com seu(s) anexo(s) e com o código oficial de Atividade do Simples Nacional "
                    + "(cAtivSN, NT 2026.005) como campo codigo. Três filtros-por-flag opcionais — nenhum deles "
                    + "remove atividades da listagem; as incompatíveis vêm marcadas, com motivos distinguíveis e "
                    + "combináveis: 'anexos' informa os anexos configurados pelo usuário e deriva "
                    + "'disponivel'/'anexosFaltantes'/'modoFatorR'; 'tpDoc' informa o tipo do documento fiscal e "
                    + "devolve 'admissivelNoDocumento' (whitelist da associativa atividade × documento: admissível "
                    + "somente nos documentos com vínculo vigente — 'tpDocsAdmissiveis' é sempre devolvido, lista "
                    + "vazia = nenhum documento publicado para a atividade); 'cClassTrib' informa a classificação "
                    + "tributária escolhida e devolve 'compativelComClassTrib' (espelho da regra UB14-90 da "
                    + "NT 2026.005: classificações que declaram receita tornam indisponíveis as atividades de "
                    + "operações não tributáveis — prevenção na origem do erro VSN-023 do cálculo). Com qualquer "
                    + "filtro informado, 'disponivel' agrega os eixos correspondentes. Não inclui alíquotas: a "
                    + "relação anexo → alíquota é configurada externamente (via serviço de terceiros, por exemplo).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de atividades vigentes na data", content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AtividadeSimplesDomain.class))) }),
            @ApiResponse(responseCode = "400",
                    description = "Data de referência ausente ou inválida, anexo fora do domínio I..V, tpDoc "
                            + "mal-formado/inexistente na TIPO_DFE, ou cClassTrib inexistente/fora de vigência "
                            + "na data (no grupo da CBS)") })
    ResponseEntity<List<AtividadeSimplesDomain>> listarAtividades(
            @Parameter(description = "Data de referência da consulta (YYYY-MM-DD); os lookups de vigência usam "
                    + "esta data, nunca a data corrente do servidor", required = true, example = "2026-01-01")
            LocalDate data,
            @Parameter(description = "Anexos configurados pelo usuário, em numerais romanos maiúsculos (I..V). "
                    + "Aceita CSV (anexos=I,II) ou parâmetro repetido (anexos=I&anexos=II). "
                    + "Ausente = sem filtro (todos configurados); presente porém vazio (anexos=) = nenhum configurado.",
                    example = "I,II")
            List<String> anexos,
            @Parameter(description = "Tipo do documento fiscal (valor da coluna TPDF_TIPO da TIPO_DFE: 55, 65, 91, "
                    + "...). Opcional; ausente = admissibilidade não avaliada. Valor mal-formado ou sem linha "
                    + "correspondente na TIPO_DFE = 400. Não é exigido que o documento esteja habilitado para "
                    + "o cálculo (whitelist TIPO_DFE_CALCULO_SIMPLES_NACIONAL): essa rejeição (VSN-015) é papel do "
                    + "endpoint de cálculo.", example = "55")
            String tpDoc,
            @Parameter(description = "Código de classificação tributária (cClassTrib, 6 dígitos) escolhido pelo "
                    + "consumidor. Opcional; ausente = compatibilidade não avaliada. O tpRBSN do código é resolvido "
                    + "como no cálculo: restrito ao grupo da CBS (tributo 2) e pela vigência da data. Código "
                    + "inexistente ou fora de vigência = 400 (o filtro nunca é ignorado silenciosamente).",
                    example = "000001")
            String cClassTrib);

    @Operation(summary = "Consultar atividade do Simples Nacional por código",
            description = "Retorna uma atividade vigente na data de referência informada, pelo seu código oficial "
                    + "cAtivSN (ex.: 09, 90), ou 404 se não existir/não estar vigente. Os campos derivados de anexos "
                    + "vêm preenchidos como \"sem filtro\" (disponivel=true, anexosFaltantes=[]). Os parâmetros "
                    + "opcionais 'tpDoc' e 'cClassTrib' recebem o mesmo tratamento da listagem: "
                    + "'tpDocsAdmissiveis' é sempre devolvido e, com os filtros informados, 'admissivelNoDocumento' "
                    + "e 'compativelComClassTrib' aparecem e compõem o 'disponivel'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atividade encontrada", content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = AtividadeSimplesDomain.class)) }),
            @ApiResponse(responseCode = "400", description = "Data de referência ausente ou inválida, tpDoc "
                    + "mal-formado/inexistente na TIPO_DFE, ou cClassTrib inexistente/fora de vigência na data "
                    + "(no grupo da CBS)"),
            @ApiResponse(responseCode = "404", description = "Atividade não encontrada") })
    ResponseEntity<AtividadeSimplesDomain> buscarAtividade(
            @Parameter(description = "Código oficial cAtivSN da atividade (ex.: 09)", required = true,
                    example = "09") String codigo,
            @Parameter(description = "Data de referência da consulta (YYYY-MM-DD); os lookups de vigência usam "
                    + "esta data, nunca a data corrente do servidor", required = true, example = "2026-01-01")
            LocalDate data,
            @Parameter(description = "Tipo do documento fiscal (valor da coluna TPDF_TIPO da TIPO_DFE: 55, 65, 91, "
                    + "...). Opcional; mal-formado ou inexistente = 400.", example = "55")
            String tpDoc,
            @Parameter(description = "Código de classificação tributária (cClassTrib, 6 dígitos). Opcional; "
                    + "inexistente ou fora de vigência na data (no grupo da CBS) = 400.", example = "000001")
            String cClassTrib);
}
