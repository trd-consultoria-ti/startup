package br.gov.serpro.rtc.api.simplesnacional.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.dto.ErroDescricaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.model.input.OperacaoSimplesInput;
import br.gov.serpro.rtc.api.simplesnacional.model.input.OperacaoSimplesNaoAssistidaInput;
import br.gov.serpro.rtc.api.simplesnacional.model.output.ObservabilidadeSimplesDomain;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Contrato OpenAPI dos endpoints de observabilidade do Simples Nacional,
 * incluindo o cálculo com erros por item e a consulta ao catálogo de códigos
 * {@code VSN-}.
 */
@Tag(name = "Observabilidade - Calculadora do Simples Nacional",
        description = "Endpoint de observabilidade da Calculadora de Tributos do Simples Nacional")
public interface SimplesNacionalControllerOpenApi {

    /** Exemplo de requisição NFS-e (tpDoc 91), com a linha de alíquotas no corpo. */
    String EXEMPLO_NFSE = """
            {
              "id": "op-1", "versao": "3.0.0", "data": "2027-03-15", "tpDoc": "91",
              "opSimpNac": "3", "regApIBSCBSSN": "1", "regApTribSN": "1",
              "aliquotasEfetivas": {
                "fatorR": false, "impedimentoSublimite": false,
                "anexoIII": { "pCBSSN": "2.3874", "pIBSSN": "0.0263",
                              "semRedistribuicaoISS": { "pCBSSN": "2.3591", "pIBSSN": "0.0260" } }
              },
              "itens": [ { "numero": 1, "cAtivSN": "12", "cClassTrib": "000001", "nbs": "114051000",
                           "baseCalculo": "10000.00", "tribISSQN": "1", "tpRetISSQN": "1", "tpBM": "1" } ]
            }
            """;

    String EXEMPLO_NFE = """
            {
              "id": "op-2", "versao": "3.0.0", "data": "2027-03-15", "tpDoc": "55", "CRT": "1",
              "aliquotasEfetivas": {
                "fatorR": true, "impedimentoSublimite": false,
                "anexoI": { "pCBSSN": "1.1612", "pIBSSN": "0.0129" }
              },
              "itens": [ { "numero": 1, "cAtivSN": "01", "cClassTrib": "000001", "baseCalculo": "1000.00",
                           "valorImpostoSeletivo": "0.00" } ]
            }
            """;

    @Operation(summary = "OBSOLETO — Cálculo do Simples Nacional com alíquotas por item (NT 2026.005)",
            deprecated = true,
            description = "OBSOLETO: use POST calculadora/observabilidade/simples-nacional/nao-assistido (linha de "
                    + "alíquotas do contribuinte no corpo), que atende todos os documentos fiscais (NF-e, NFC-e, "
                    + "CT-e, BP-e, NFCom e NFS-e) com o mesmo contrato e escolhe o par de alíquotas com ou sem "
                    + "redistribuição do ISS. Este endpoint mantém o contrato e os resultados de "
                    + "sempre. Implementa a NT 2026.005 v1.00 (Reforma Tributária do Consumo — Simples Nacional; "
                    + "LC 123/2006, LC 214/2025, LC 227/2026). Recebe os dados da operação (data, tpDoc, CRT, "
                    + "tpRegimeIBSCBS e tpEnteGov — este último a nível de operação, espelhando a tag "
                    + "gCompraGov/tpEnteGov da identificação da nota, grupo B/ide do leiaute do DF-e) e os itens "
                    + "(cClassTrib, cAtivSN, base de cálculo, vIS, alíquotas efetivas "
                    + "pIBSSN/pCBSSN), resolve internamente o tpRBSN do cClassTrib (restrito ao grupo "
                    + "da CBS da tabela de classificação) e espelha, por item, o leiaute do Evento de Tributação do "
                    + "Simples Nacional (tpEvento 412140) — incluindo o código tpRBSN e sua descrição "
                    + "(tpRBSNDescricao) —, acumulando erros individuais por item. O comportamento do cálculo é "
                    + "parametrizado em banco de dados, sempre pela vigência da data do fato gerador: os tipos de "
                    + "documento suportados, o efeito de cada tipo de receita bruta, a matriz de regime de apuração "
                    + "e o fator de transição de compra governamental. O cAtivSN é validado contra o catálogo de "
                    + "atividades, com restrição de documento POR ATIVIDADE: atividade sem vínculo na associativa "
                    + "de tipos de DFE é admissível em qualquer tpDoc suportado pelo cálculo; atividade com "
                    + "vínculos, somente nos tpDoc vinculados — cAtivSN inadmissível leva ao estado NAO_CALCULADO, "
                    + "sem erro. Antes de qualquer outra validação do item, verifica-se (primeira consulta de banco) "
                    + "se o cClassTrib está vinculado, na data, a tratamento tributário ainda não implementado na "
                    + "Calculadora (indicador de ajuste): nesse caso o item sai com o estado NAO_IMPLEMENTADO e o "
                    + "erro VSN-027, sem cálculo e fora dos totais. Compras governamentais (tpEnteGov informado na operação) aplicam a todos os itens "
                    + "o deslocamento de alíquotas CBS-IBS dos arts. 372 e 473 da LC 214/2025 (o redutor dos "
                    + "arts. 370/472 não integra o evento). Precisão: as alíquotas são usadas com todas as "
                    + "casas informadas (até 34), inclusive após a transformação de compra governamental; cada "
                    + "valor de item é arredondado uma única vez a 2 casas (HALF_EVEN, ABNT NBR 5891) e os totais "
                    + "são a soma desses valores; pIBSSN/pCBSSN saem com 2 a 4 casas (tipo 3v2-4). Valores "
                    + "monetários de entrada no formato 13v2 (até 2 casas), sem notação científica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Processamento realizado (itens podem ter erros individuais)", content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ObservabilidadeSimplesDomain.class)) }),
            @ApiResponse(responseCode = "400", description = "Estrutura e/ou dados informados em formato não reconhecido", content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)) }),
            @ApiResponse(responseCode = "422", description = "Erro de validação global (data, itens, dados obrigatórios "
                    + "da operação, tpEnteGov inválido, parametrização de compra governamental indisponível) ou "
                    + "operação fora do escopo do cálculo (tpDoc não suportado, MEI, regime regular, "
                    + "regime normal puro, incoerências CRT × tpRegimeIBSCBS)", content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno na API", content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)) }) })
    ResponseEntity<ObservabilidadeSimplesDomain> calcularTributosSimplesNacional(
            @RequestBody(description = "Dados de uma operação do Simples Nacional", required = true, content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = OperacaoSimplesInput.class))) OperacaoSimplesInput operacao);

    @Operation(summary = "Cálculo NÃO ASSISTIDO do Simples Nacional para todos os documentos fiscais",
            description = "Calcula o IBS e a CBS no Simples Nacional para os documentos com CRT (55 NF-e, 65 NFC-e, "
                    + "57 CT-e, 67 CT-e OS, 63 BP-e, 93 BP-e TM e 62 NFCom) e para a NFS-e (91), com o mesmo "
                    + "contrato. O regime chega no vocabulário do documento — CRT e tpRegimeIBSCBS, ou opSimpNac, "
                    + "regApIBSCBSSN e regApTribSN — e é convertido no código neutro devolvido em "
                    + "oper.regimeApuracao (SN_PLENO, SN_SUBLIMITE, PENDENTE, PENDENTE_SUBLIMITE ou "
                    + "SOMENTE_RECEITA; este último, do optante pelo regime regular do IBS e da CBS, devolve só a "
                    + "receita bruta). A linha de alíquotas efetivas do contribuinte no período vem em "
                    + "aliquotasEfetivas (pontos percentuais, texto, precisão integral; obrigatória, exceto no "
                    + "SOMENTE_RECEITA): fatorR, impedimentoSublimite e um par CBS/IBS por anexo, mais o par sem "
                    + "redistribuição do ISS nos anexos III a V. Para cada item, a Calculadora escolhe o anexo "
                    + "(cAtivSN e fator r) e o par com ou sem redistribuição do ISS — o par sem quando o ISS não "
                    + "entra no cálculo do Simples: ISS fora do Simples "
                    + "(impedimento, CRT 2, regApIBSCBSSN 2, regApTribSN 2 ou 3), atividade sem ISS, imunidade, "
                    + "exportação ou não incidência (tribISSQN 2, 3 ou 4), isenção municipal (tpBM 1) e ISS retido "
                    + "(tpRetISSQN 2 ou 3) — e devolve a decisão em resolucaoAliquotas (tipoAliquota, "
                    + "regraAliquota, alíquotas integrais, motivo e avisos). Na NFS-e: exatamente 1 item, sem "
                    + "Imposto Seletivo, alíquota com 2 casas no cálculo (arredondada depois da compra "
                    + "governamental) e só o tpRBSN 1 gera valor. Erros por item no padrão RFC 7807, como no "
                    + "endpoint clássico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Processamento realizado (itens podem ter erros individuais)", content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ObservabilidadeSimplesDomain.class)) }),
            @ApiResponse(responseCode = "400", description = "Estrutura e/ou dados informados em formato não reconhecido", content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)) }),
            @ApiResponse(responseCode = "422", description = "Erro de validação global: dados obrigatórios da operação, "
                    + "tpDoc não habilitado, campos de regime inválidos, incoerentes ou do outro vocabulário "
                    + "(VSN-016 a 018, VSN-030 a 033), operação fora do escopo (VSN-019), número de itens (VSN-034), "
                    + "aliquotasEfetivas ausente (VSN-039) ou fora do formato (VSN-005/007), compra governamental "
                    + "(VSN-024/025)", content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno na API", content = {
                    @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)) }) })
    ResponseEntity<ObservabilidadeSimplesDomain> calcularTributosSimplesNacionalNaoAssistido(
            @RequestBody(description = "Dados de uma operação do Simples Nacional, com a linha de alíquotas do "
                    + "contribuinte", required = true, content = @Content(mediaType = APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OperacaoSimplesNaoAssistidaInput.class),
                    examples = {
                            @ExampleObject(name = "NFS-e — construção civil com isenção municipal",
                                    summary = "Regra 7 (tpBM 1): par sem redistribuição; alíquota de 2 casas",
                                    value = EXEMPLO_NFSE),
                            @ExampleObject(name = "NF-e — CRT 1",
                                    summary = "Anexo I, par único", value = EXEMPLO_NFE) }))
            OperacaoSimplesNaoAssistidaInput operacao);

    @Operation(summary = "Consultar descrição de erro do Simples Nacional",
            description = "Retorna título e descrição em PT-BR para o código de erro VSN- informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Descrição do erro encontrada", content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErroDescricaoSimples.class)) }),
            @ApiResponse(responseCode = "404", description = "Código de erro não encontrado") })
    ResponseEntity<ErroDescricaoSimples> buscarErro(
            @Parameter(description = "Código do erro (ex: VSN-001)", required = true) String codigo);
}
