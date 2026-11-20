package br.gov.serpro.rtc.api.atividadesimples.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.gov.serpro.rtc.api.atividadesimples.dto.AtividadeSimplesDomain;
import br.gov.serpro.rtc.api.atividadesimples.service.AtividadeSimplesService;
import br.gov.serpro.rtc.api.atividadesimples.service.DerivacaoAnexosSimples;
import br.gov.serpro.rtc.api.versao.service.VersaoAplicacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * Controlador REST do catálogo de atividades VERSUS anexos do Simples Nacional.
 *
 * <p>
 *
 * Vive sob {@code br.gov.serpro.rtc.api.atividadesimples}
 * e não depende do domínio do regime geral.
 *
 * Reutiliza infraestrutura genérica/compartilhada ({@link VersaoAplicacaoService} para os headers de versão),
 * como faz o {@code SimplesNacionalController}.
 *
 * <p>
 * A data de referência ({@code data}, formato YYYY-MM-DD) é query param OBRIGATÓRIO:
 * como em todos os endpoints do Simples Nacional, os lookups de vigência usam a data
 * informada pelo cliente, nunca a data corrente do servidor.
 *
 * <p>
 * O query param opcional {@code anexos} informa os anexos configurados pelo usuário e
 * funciona como filtro-por-flag: a resposta NÃO omite atividades incompatíveis — ela as
 * marca via {@code disponivel}, {@code anexosFaltantes} e {@code modoFatorR}.
 *
 * <p>
 * O query param opcional {@code tpDoc} (tipo do documento fiscal, coluna {@code TPDF_TIPO}
 * da {@code TIPO_DFE}) segue o mesmo princípio: atividades inadmissíveis no documento
 * (associativa {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}, semântica de whitelist —
 * atividade admissível somente nos documentos com vínculo vigente) NÃO são removidas —
 * vêm com {@code admissivelNoDocumento=false} e {@code disponivel=false}. Sem
 * {@code tpDoc}, os campos derivados dos demais eixos não mudam; {@code tpDocsAdmissiveis}
 * é sempre devolvido (lista vazia = nenhum documento publicado para a atividade).
 * A validação do valor (formato/existência na {@code TIPO_DFE}) fica no service, que tem
 * acesso ao repositório — deliberadamente desacoplada da whitelist do cálculo
 * ({@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}): "este documento calcula?" é pergunta do
 * endpoint de cálculo (VSN-015), não deste.
 *
 * <p>
 * O query param opcional {@code cClassTrib} (código de classificação tributária) segue o
 * mesmo princípio: as atividades incompatíveis com a classificação (espelho da regra
 * UB14-90 da NT 2026.005 — o mesmo erro VSN-023 do cálculo) NÃO são removidas — vêm com
 * {@code compativelComClassTrib=false} e {@code disponivel=false}. É prevenção na origem:
 * o consumidor evita selecionar uma atividade que o cálculo rejeitaria. Código
 * inexistente ou fora de vigência na data (no grupo da CBS) ⇒ 400 — o filtro nunca é
 * ignorado silenciosamente.
 *
 * <p>
 *
 * Sem catálogo de erros próprio (decisão deliberada):
 * o 404 usa o mecanismo padrão do Spring ({@code ResponseEntity.notFound().build()})
 * e os 400 usam o tratamento padrão da API — falta ou má-formação do query param {@code data}
 * via binding; anexo fora de {@code I..V}, tpDoc desconhecido e cClassTrib
 * desconhecido/fora de vigência via {@link ResponseStatusException} —, que
 * devolve a estrutura de erro unificada do projeto ({@code ProblemDetail} com
 * {@code errors[]} e {@code pointer} RFC 6901, montada pelo handler único
 * {@code shared.exceptionhandler.ApiExceptionHandler}) — sem códigos de catálogo,
 * que continuam exclusivos das features que os possuem.
 *
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("calculadora/simples-nacional/atividades")
public class AtividadeSimplesController implements AtividadeSimplesControllerOpenApi {

    private final AtividadeSimplesService atividadeService;
    private final VersaoAplicacaoService versaoAplicacaoService;

    @Override
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AtividadeSimplesDomain>> listarAtividades(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate data,
            @RequestParam(required = false) List<String> anexos,
            @RequestParam(required = false) String tpDoc,
            @RequestParam(required = false) String cClassTrib) {
        log.debug("Listando atividades do Simples Nacional vigentes em {} com anexos configurados {}, tpDoc {} "
                + "e cClassTrib {}", data, anexos, tpDoc, cClassTrib);
        return ResponseEntity.ok()
                .headers(versaoAplicacaoService.getHeaders())
                .body(atividadeService.listar(data, converterAnexos(anexos), tpDoc, cClassTrib));
    }

    @Override
    @GetMapping(value = "{codigo}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<AtividadeSimplesDomain> buscarAtividade(@PathVariable String codigo,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate data,
            @RequestParam(required = false) String tpDoc,
            @RequestParam(required = false) String cClassTrib) {
        log.debug("Buscando atividade do Simples Nacional por código {} vigente em {} com tpDoc {} e cClassTrib {}",
                codigo, data, tpDoc, cClassTrib);
        return atividadeService.buscarPorCodigo(codigo, data, tpDoc, cClassTrib)
                .map(atividade -> ResponseEntity.ok()
                        .headers(versaoAplicacaoService.getHeaders())
                        .body(atividade))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Converte o parâmetro {@code anexos} no conjunto de anexos configurados:
     * {@code null} (parâmetro ausente) = sem filtro; presente porém vazio
     * ({@code anexos=}) = nenhum anexo configurado. Aceita tanto CSV
     * ({@code anexos=I,II}) quanto parâmetro repetido ({@code anexos=I&anexos=II});
     * duplicatas são toleradas (conjunto). Valor fora de {@code I..V}
     * (case-sensitive maiúsculo) ⇒ 400 via {@link ResponseStatusException}.
     */
    private static Set<String> converterAnexos(List<String> anexos) {
        if (anexos == null) {
            return null;
        }
        Set<String> configurados = new HashSet<>();
        for (String elemento : anexos) {
            for (String anexo : elemento.split(",", -1)) {
                if (anexo.isEmpty()) {
                    continue;
                }
                if (!DerivacaoAnexosSimples.ANEXOS_VALIDOS.contains(anexo)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Anexo inválido: '" + anexo + "'. Valores aceitos: "
                                    + String.join(", ", DerivacaoAnexosSimples.ANEXOS_VALIDOS) + ".");
                }
                configurados.add(anexo);
            }
        }
        return configurados;
    }
}
