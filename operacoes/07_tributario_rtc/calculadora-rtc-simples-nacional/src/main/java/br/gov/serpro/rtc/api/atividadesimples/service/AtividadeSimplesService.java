package br.gov.serpro.rtc.api.atividadesimples.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.gov.serpro.rtc.api.atividadesimples.dto.AtividadeSimplesDomain;
import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;
import br.gov.serpro.rtc.api.atividadesimples.repository.AtividadeSimplesRepository;
import br.gov.serpro.rtc.api.atividadesimples.repository.AtividadeSimplesRepository.VinculoTpDocAtividade;
import br.gov.serpro.rtc.api.simplesnacional.entity.ClassificacaoTributariaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.repository.ClassificacaoTributariaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.service.ParametroCalculoSimplesService;
import lombok.RequiredArgsConstructor;

/**
 *
 * Serviço de consulta às atividades do Simples Nacional e seus anexos.
 *
 * <p>
 * Lê as atividades do banco de dados SQLite via {@link AtividadeSimplesRepository}
 * e as expõe como {@link AtividadeSimplesDomain}. Não calcula alíquotas e não
 * depende do domínio do regime geral. A regra de derivação dos campos
 * {@code disponivel}, {@code anexosFaltantes} e {@code modoFatorR} fica
 * concentrada em {@link DerivacaoAnexosSimples}.
 *
 * <p>
 * A data de referência é OBRIGATÓRIA e informada pelo cliente: como em todos os
 * endpoints do Simples Nacional, os lookups de vigência usam a data recebida,
 * nunca a data corrente do servidor — inclusive para os vínculos da associativa
 * {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL} e para a resolução do cClassTrib.
 *
 * <p>
 * Admissibilidade por documento fiscal ({@code tpDoc} opcional): a associativa
 * por atividade tem semântica de WHITELIST (mundo fechado — atividade
 * admissível somente nos documentos com vínculo vigente; sem vínculo algum, em
 * documento nenhum) e é a MESMA regra que o cálculo aplica na validação do
 * cAtivSN; a duplicação da consulta é deliberada e a coerência é garantida
 * por teste — ver {@link AtividadeSimplesRepository#buscarVinculosTpDocVigentes}.
 * A validação do {@code tpDoc} NÃO consulta a whitelist
 * {@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}: este endpoint responde "esta
 * atividade é admissível neste documento?"; rejeitar documento não habilitado
 * para o cálculo (VSN-015) é papel do endpoint de cálculo.
 *
 * <p>
 * Compatibilidade com a classificação tributária ({@code cClassTrib} opcional):
 * o tpRBSN do código é resolvido com os MESMOS componentes do cálculo
 * ({@link ClassificacaoTributariaSimplesRepository}, restrito ao grupo da CBS,
 * e {@link ParametroCalculoSimplesService} para o efeito vigente) — reuso
 * deliberado, para que a regra nunca divirja entre os dois endpoints. A única
 * regra aplicada é o espelho da UB14-90 da NT 2026.005 (a mesma do erro
 * VSN-023 do cálculo): tpRBSN com {@code TRBS_IN_VEDADO_NAO_TRIBUTAVEL = 1}
 * torna indisponíveis as atividades com {@code ATSN_IN_NAO_TRIBUTAVEL = 1}.
 * Nenhuma outra atividade é vedada por classificação — tudo dirigido pelos
 * indicadores do banco, sem literais em código.
 *
 */
@Service
@RequiredArgsConstructor
public class AtividadeSimplesService {

    private final AtividadeSimplesRepository atividadeRepository;
    private final ClassificacaoTributariaSimplesRepository classificacaoTributariaRepository;
    private final ParametroCalculoSimplesService parametroService;

    /**
     * Lista as atividades vigentes na data informada, ordenadas por ordem de
     * exibição, com os campos {@code disponivel}, {@code anexosFaltantes} e
     * {@code modoFatorR} derivados da configuração de anexos do usuário e os
     * campos de admissibilidade por documento ({@code tpDocsAdmissiveis}
     * sempre; {@code admissivelNoDocumento} somente com {@code tpDoc}) e de
     * compatibilidade com a classificação tributária
     * ({@code compativelComClassTrib}, somente com {@code cClassTrib}).
     *
     * <p>Sem {@code tpDoc} e sem {@code cClassTrib}, nada muda em
     * {@code disponivel}, {@code anexosFaltantes} e {@code modoFatorR}; com
     * qualquer dos filtros, {@code disponivel} agrega os eixos informados
     * (anexos, admissibilidade no documento e compatibilidade com a
     * classificação). As atividades incompatíveis NÃO são removidas da
     * listagem — vêm marcadas (decisão de UX: o cliente exibe desabilitado com
     * a dica do motivo, e os motivos são distinguíveis e combináveis).
     *
     * @param data data de referência da vigência (obrigatória)
     * @param anexosConfigurados anexos configurados pelo usuário;
     *        {@code null} = sem filtro (todos configurados),
     *        conjunto vazio = nenhum configurado
     * @param tpDoc tipo do documento fiscal ({@code TPDF_TIPO}: "55", "65",
     *        "91", ...); {@code null}/em branco = não informado
     * @param cClassTrib código de classificação tributária (6 dígitos, grupo
     *        da CBS); {@code null}/em branco = não informado
     * @throws ResponseStatusException HTTP 400 se o {@code tpDoc} informado for
     *         mal-formado ou não existir em {@code TIPO_DFE}, ou se o
     *         {@code cClassTrib} informado não existir ou não estiver vigente
     *         na data (no grupo da CBS)
     */
    public List<AtividadeSimplesDomain> listar(LocalDate data, Set<String> anexosConfigurados, String tpDoc,
            String cClassTrib) {
        String tpDocValidado = validarTpDoc(tpDoc);
        Boolean vedadoNaoTributavel = resolverVedadoNaoTributavel(cClassTrib, data);
        Map<Long, List<String>> tpDocsAdmissiveisPorAtividade = buscarTpDocsAdmissiveis(data);
        return atividadeRepository.buscarVigentes(data).stream()
                .map(atividade -> toDomain(atividade, anexosConfigurados,
                        tpDocsAdmissiveisPorAtividade.getOrDefault(atividade.getId(), List.of()),
                        tpDocValidado, vedadoNaoTributavel))
                .toList();
    }

    /**
     * Busca uma atividade vigente na data de referência pelo código oficial
     * cAtivSN (ex.: {@code "09"}, {@code "90"}).
     *
     * <p>
     *
     * Decisão deliberada: o {@code GET /{codigo}} deste projeto já exige
     * {@code data} (nunca se usa a data corrente do servidor), então a data de
     * referência continua sendo a informada pelo cliente; como o endpoint não
     * recebe {@code anexos}, os campos derivados são preenchidos como
     * "sem filtro" ({@code disponivel=true}, {@code anexosFaltantes=[]},
     * {@code modoFatorR} coerente com todos os anexos configurados). Os
     * parâmetros opcionais {@code tpDoc} e {@code cClassTrib} recebem o mesmo
     * tratamento da listagem.
     */
    public Optional<AtividadeSimplesDomain> buscarPorCodigo(String codigo, LocalDate data, String tpDoc,
            String cClassTrib) {
        if (codigo == null) {
            return Optional.empty();
        }
        return listar(data, null, tpDoc, cClassTrib).stream()
                .filter(atividade -> atividade.codigo().equals(codigo))
                .findFirst();
    }

    /**
     * Valida o query param {@code tpDoc}: nulo/em branco = não informado
     * (retorna {@code null} e nada muda no comportamento); informado, deve ser
     * numérico e existir em {@code TIPO_DFE} por {@code TPDF_TIPO}, senão
     * HTTP 400 (mesmo padrão dos demais erros de query do projeto — corpo
     * RFC 7807 montado pelo handler único). O valor devolvido é a forma
     * canônica do tipo (sem zeros à esquerda), a mesma usada em
     * {@code tpDocsAdmissiveis}.
     *
     * <p>Deliberadamente NÃO acoplada à whitelist do cálculo
     * ({@code TIPO_DFE_CALCULO_SIMPLES_NACIONAL}): um tpDoc válido porém não
     * habilitado para o cálculo (ex.: 57, CT-e) é aceito aqui e a
     * admissibilidade é derivada normalmente — a rejeição da operação inteira
     * é responsabilidade do endpoint de cálculo (VSN-015).
     */
    private String validarTpDoc(String tpDoc) {
        if (tpDoc == null || tpDoc.isBlank()) {
            return null;
        }
        String informado = tpDoc.trim();
        int tipo;
        try {
            tipo = Integer.parseInt(informado);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de documento fiscal inválido: '" + informado
                            + "'. Informe o valor numérico da coluna TPDF_TIPO da TIPO_DFE (ex.: 55, 65, 91).");
        }
        if (tipo < 0 || atividadeRepository.contarTiposDfePorTipo(tipo) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de documento fiscal desconhecido: '" + informado
                            + "'. Não há linha na TIPO_DFE com TPDF_TIPO correspondente.");
        }
        return String.valueOf(tipo);
    }

    /**
     * Resolve o indicador {@code TRBS_IN_VEDADO_NAO_TRIBUTAVEL} do tpRBSN do
     * {@code cClassTrib} informado, pela vigência da data — a resolução é
     * IDÊNTICA à do cálculo (reuso dos mesmos repositório e fachada): o código
     * vem do cClassTrib restrito ao grupo da CBS (tributo 2; os códigos
     * colidem com o grupo do Imposto Seletivo) e o efeito vem da tabela de
     * tipos de receita bruta, também por vigência.
     *
     * @return {@code null} quando o parâmetro não foi informado; senão, o
     *         indicador da linha de efeito vigente
     * @throws ResponseStatusException HTTP 400 quando o código não existe ou
     *         não está vigente na data (no grupo da CBS), ou quando o tpRBSN
     *         resolvido está sem linha de efeito vigente — o filtro nunca é
     *         ignorado silenciosamente
     */
    private Boolean resolverVedadoNaoTributavel(String cClassTrib, LocalDate data) {
        if (cClassTrib == null || cClassTrib.isBlank()) {
            return null;
        }
        String codigo = cClassTrib.trim();
        List<ClassificacaoTributariaSimples> vigentes =
                classificacaoTributariaRepository.buscarVigentes(codigo, data);
        if (vigentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código de classificação tributária desconhecido ou fora de vigência: '" + codigo
                            + "'. Informe um cClassTrib do grupo da CBS vigente na data de referência.");
        }
        int tpRBSN = vigentes.get(0).getTpRBSN();
        return parametroService.buscarTipoReceitaBruta(tpRBSN, data)
                .map(TipoReceitaBrutaSimples::getVedadoNaoTributavel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Não há parametrização de efeito vigente para o tipo de receita bruta tpRBSN=" + tpRBSN
                                + " do cClassTrib '" + codigo + "' na data de referência."));
    }

    /**
     * Resolve, com UMA única query adicional (a listagem devolve ~15
     * atividades; nunca uma query por atividade), os tpDocs admissíveis de
     * cada atividade: o valor lista os {@code TPDF_TIPO} dos vínculos vigentes
     * na data cujo {@code TIPO_DFE} também está vigente (ordenados
     * numericamente). Atividade ausente do mapa = sem vínculo vigente algum =
     * não admissível em documento nenhum (whitelist; o chamador converte a
     * ausência em lista vazia).
     */
    private Map<Long, List<String>> buscarTpDocsAdmissiveis(LocalDate data) {
        return atividadeRepository.buscarVinculosTpDocVigentes(data).stream()
                .collect(Collectors.groupingBy(VinculoTpDocAtividade::getAtividadeId,
                        Collectors.collectingAndThen(Collectors.toList(),
                                vinculos -> vinculos.stream()
                                        .map(VinculoTpDocAtividade::getTpDoc)
                                        .sorted(Comparator.naturalOrder())
                                        .map(String::valueOf)
                                        .toList())));
    }

    /**
     * Converte a entidade persistida no contrato de saída. O {@code codigo}
     * estável é o código oficial de atividade do Simples Nacional
     * (coluna {@code ATSN_CATIVSN}, tag cAtivSN dos leiautes de DF-e;
     * ex.: {@code "09"}, {@code "90"}). A derivação dos campos de
     * disponibilidade por anexos (e a validação do invariante "não existe
     * atividade só com Fator R", que falha cedo com
     * {@link IllegalStateException}) fica em
     * {@link DerivacaoAnexosSimples#derivar}; a admissibilidade por documento
     * e a compatibilidade com a classificação NÃO interferem em
     * {@code anexosFaltantes}/{@code modoFatorR} — apenas em
     * {@code disponivel}, quando os respectivos parâmetros foram informados.
     */
    private static AtividadeSimplesDomain toDomain(AtividadeSimplesNacional atividade, Set<String> anexosConfigurados,
            List<String> tpDocsAdmissiveis, String tpDoc, Boolean vedadoNaoTributavel) {
        var derivacao = DerivacaoAnexosSimples.derivar(
                atividade.getAnexoSemFatorR(), atividade.getAnexoComFatorR(), anexosConfigurados);
        Boolean admissivelNoDocumento = tpDoc == null ? null : tpDocsAdmissiveis.contains(tpDoc);
        Boolean compativelComClassTrib = vedadoNaoTributavel == null ? null
                : !(vedadoNaoTributavel && Boolean.TRUE.equals(atividade.getNaoTributavel()));
        boolean disponivel = derivacao.disponivel()
                && !Boolean.FALSE.equals(admissivelNoDocumento)
                && !Boolean.FALSE.equals(compativelComClassTrib);
        return AtividadeSimplesDomain.de(
                atividade.getCAtivSN(),
                atividade.getOrdem(),
                atividade.getDescricao(),
                atividade.getAnexoSemFatorR(),
                atividade.getAnexoComFatorR(),
                atividade.getRotulo(),
                atividade.getRotuloCurto(),
                disponivel,
                derivacao.anexosFaltantes(),
                derivacao.modoFatorR(),
                tpDocsAdmissiveis,
                admissivelNoDocumento,
                compativelComClassTrib);
    }

}
