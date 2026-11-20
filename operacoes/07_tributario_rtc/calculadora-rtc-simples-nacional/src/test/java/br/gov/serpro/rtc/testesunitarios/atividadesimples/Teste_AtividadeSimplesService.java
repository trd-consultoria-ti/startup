package br.gov.serpro.rtc.testesunitarios.atividadesimples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.gov.serpro.rtc.api.atividadesimples.dto.AtividadeSimplesDomain;
import br.gov.serpro.rtc.api.atividadesimples.dto.ModoFatorR;
import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;
import br.gov.serpro.rtc.api.atividadesimples.repository.AtividadeSimplesRepository;
import br.gov.serpro.rtc.api.atividadesimples.repository.AtividadeSimplesRepository.VinculoTpDocAtividade;
import br.gov.serpro.rtc.api.atividadesimples.service.AtividadeSimplesService;
import br.gov.serpro.rtc.api.simplesnacional.entity.ClassificacaoTributariaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.repository.ClassificacaoTributariaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.service.ParametroCalculoSimplesService;

/**
 * Testes unitários do serviço de atividades do Simples Nacional — derivação da
 * admissibilidade por documento fiscal (query param {@code tpDoc} opcional)
 * sobre a associativa {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL} (semântica
 * de whitelist — mundo fechado) e da compatibilidade com a classificação
 * tributária (query param {@code cClassTrib} opcional, espelho da regra
 * UB14-90), com os repositórios e a fachada de parâmetros mockados.
 *
 * <p>O contrato da query {@code buscarVinculosTpDocVigentes} (exercitado
 * contra a carga real em {@code ParametrizacaoSimplesNacionalTest} e
 * {@code AtividadeSimplesControllerTest}) é: apenas vínculos VIGENTES na data,
 * cujo {@code TIPO_DFE} também está vigente, são devolvidos. Os cenários de
 * vigência aqui simulam esse contrato — em especial o cenário "vínculo com fim
 * de vigência anterior à data é ignorado; sem outro vínculo vigente, a
 * atividade não é admissível em documento nenhum
 * ({@code tpDocsAdmissiveis = []})", que a carga oficial (toda com vigência
 * aberta) não permite reproduzir por teste de integração.
 */
@ExtendWith(MockitoExtension.class)
class Teste_AtividadeSimplesService {

    private static final LocalDate DATA = LocalDate.of(2027, 3, 15);

    @Mock
    private AtividadeSimplesRepository repository;

    @Mock
    private ClassificacaoTributariaSimplesRepository classificacaoRepository;

    @Mock
    private ParametroCalculoSimplesService parametroService;

    private AtividadeSimplesService service;

    @BeforeEach
    void setup() {
        service = new AtividadeSimplesService(repository, classificacaoRepository, parametroService);
        // tpDocs conhecidos da TIPO_DFE nos testes: 55, 65, 91, 57.
        lenient().when(repository.contarTiposDfePorTipo(anyInt()))
                .thenAnswer(inv -> List.of(55, 65, 91, 57).contains((int) inv.getArgument(0)) ? 1L : 0L);
        // Catálogo dos testes: '01' (anexo I, tributável) e '90' (sem anexos,
        // não tributável).
        lenient().when(repository.buscarVigentes(DATA))
                .thenReturn(List.of(atividade(1L, 1, "01", "I"), atividade(15L, 15, "90", null)));
    }

    // ─── Comportamento sem tpDoc ───────────────────────────────────────────────

    @Test
    void semTpDoc_naoValidaNaTipoDfeENaoDevolveAdmissivelNoDocumento() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of(vinculo(1L, 55), vinculo(1L, 65)));

        List<AtividadeSimplesDomain> atividades = service.listar(DATA, null, null, null);

        AtividadeSimplesDomain vinculada = atividades.get(0);
        assertEquals(List.of("55", "65"), vinculada.tpDocsAdmissiveis());
        assertNull(vinculada.admissivelNoDocumento(), "sem tpDoc o campo não deve ser preenchido");
        assertTrue(vinculada.disponivel(), "sem tpDoc, disponivel continua regido só pelos anexos");
        assertEquals(List.of(), atividades.get(1).tpDocsAdmissiveis(),
                "atividade sem vínculo vigente devolve lista vazia (nenhum documento publicado)");
        verify(repository, never()).contarTiposDfePorTipo(anyInt());
    }

    @Test
    void tpDocEmBrancoEquivaleAAusente() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of());

        assertNull(service.listar(DATA, null, "  ", null).get(0).admissivelNoDocumento());
        verify(repository, never()).contarTiposDfePorTipo(anyInt());
    }

    // ─── Derivação da admissibilidade com tpDoc (whitelist) ────────────────────

    @Test
    void atividadeComVinculoParaOTpDoc_admissivelEDisponivelPelosAnexos() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of(vinculo(1L, 55), vinculo(1L, 65)));

        AtividadeSimplesDomain vinculada = service.listar(DATA, null, "55", null).get(0);

        assertEquals(Boolean.TRUE, vinculada.admissivelNoDocumento());
        assertTrue(vinculada.disponivel());
        assertEquals(List.of("55", "65"), vinculada.tpDocsAdmissiveis());
    }

    @Test
    void atividadeSemVinculoParaOTpDoc_inadmissivelEIndisponivelMesmoComAnexosOk() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of(vinculo(1L, 55), vinculo(1L, 65)));

        AtividadeSimplesDomain vinculada = service.listar(DATA, null, "91", null).get(0);

        assertEquals(Boolean.FALSE, vinculada.admissivelNoDocumento());
        assertFalse(vinculada.disponivel(), "disponivel agrega os dois eixos quando tpDoc é informado");
        assertTrue(vinculada.anexosFaltantes().isEmpty(),
                "anexosFaltantes continua derivado exclusivamente dos anexos");
        assertEquals(ModoFatorR.NAO_APLICAVEL, vinculada.modoFatorR());
    }

    @Test
    void atividadeSemVinculoAlgum_inadmissivelEmQualquerTpDoc() {
        // Whitelist (mundo fechado): sem vínculo vigente algum, a atividade não
        // é admissível em documento nenhum — inclusive num tpDoc válido não
        // habilitado para o cálculo (57, CT-e), que este endpoint aceita
        // validar (a whitelist do cálculo, VSN-015, é outra camada).
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of(vinculo(15L, 55)));

        AtividadeSimplesDomain semVinculo = service.listar(DATA, null, "57", null).get(0);

        assertEquals(List.of(), semVinculo.tpDocsAdmissiveis());
        assertEquals(Boolean.FALSE, semVinculo.admissivelNoDocumento());
        assertFalse(semVinculo.disponivel());
    }

    @Test
    void doisMotivosSimultaneos_inadmissivelEComAnexosFaltantes() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of(vinculo(1L, 55)));

        // anexos configurados = {II}: a atividade '01' (anexo I) fica com anexo
        // faltante E inadmissível em 91 — ambos os motivos preenchidos.
        AtividadeSimplesDomain vinculada = service.listar(DATA, Set.of("II"), "91", null).get(0);

        assertEquals(Boolean.FALSE, vinculada.admissivelNoDocumento());
        assertEquals(List.of("I"), vinculada.anexosFaltantes());
        assertFalse(vinculada.disponivel());
    }

    // ─── Vigência dos vínculos ─────────────────────────────────────────────────

    @Test
    void vinculoForaDeVigenciaIgnorado_atividadeFicaSemDocumentoAlgum() {
        // Contrato da query: vínculo com fim de vigência anterior à data NÃO é
        // devolvido. Sem outro vínculo vigente, a atividade não é admissível em
        // documento nenhum (whitelist) — exatamente como no cálculo.
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of());

        AtividadeSimplesDomain atividade = service.listar(DATA, null, "91", null).get(0);

        assertEquals(List.of(), atividade.tpDocsAdmissiveis());
        assertEquals(Boolean.FALSE, atividade.admissivelNoDocumento());
        assertFalse(atividade.disponivel());
    }

    // ─── Validação do tpDoc ────────────────────────────────────────────────────

    @Test
    void tpDocMalFormado_retorna400() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.listar(DATA, null, "abc", null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void tpDocSemLinhaNaTipoDfe_retorna400() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.listar(DATA, null, "99", null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void tpDocComZeroAEsquerda_canonizadoAntesDaComparacao() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of(vinculo(1L, 55)));

        assertEquals(Boolean.TRUE, service.listar(DATA, null, "055", null).get(0).admissivelNoDocumento());
    }

    // ─── Compatibilidade com a classificação tributária (cClassTrib) ───────────

    @Test
    void semCClassTrib_naoResolveTpRBSNENaoDevolveCompativelComClassTrib() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of());

        List<AtividadeSimplesDomain> atividades = service.listar(DATA, null, null, null);

        assertNull(atividades.get(0).compativelComClassTrib());
        assertNull(atividades.get(1).compativelComClassTrib());
        verify(classificacaoRepository, never()).buscarVigentes(any(), any());
    }

    @Test
    void cClassTribVedadoParaNaoTributavel_marcaSomenteAAtividadeNaoTributavel() {
        // tpRBSN com TRBS_IN_VEDADO_NAO_TRIBUTAVEL = 1 (declara receita):
        // somente a atividade com ATSN_IN_NAO_TRIBUTAVEL = 1 (o '90') fica
        // indisponível, com o motivo próprio; a '01' não é afetada.
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of());
        stubClassTrib("000001", 1, true);

        List<AtividadeSimplesDomain> atividades = service.listar(DATA, null, null, "000001");

        AtividadeSimplesDomain comum = atividades.get(0);
        assertEquals(Boolean.TRUE, comum.compativelComClassTrib());
        assertTrue(comum.disponivel());

        AtividadeSimplesDomain naoTributavel = atividades.get(1);
        assertEquals(Boolean.FALSE, naoTributavel.compativelComClassTrib());
        assertFalse(naoTributavel.disponivel());
        assertTrue(naoTributavel.anexosFaltantes().isEmpty(),
                "o motivo de classificação não contamina o eixo dos anexos");
    }

    @Test
    void cClassTribNaoVedado_naoMarcaAtividadeAlguma() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of());
        stubClassTrib("410001", 0, false);

        List<AtividadeSimplesDomain> atividades = service.listar(DATA, null, null, "410001");

        assertEquals(Boolean.TRUE, atividades.get(0).compativelComClassTrib());
        assertEquals(Boolean.TRUE, atividades.get(1).compativelComClassTrib());
    }

    @Test
    void cClassTribDesconhecidoOuForaDeVigencia_retorna400() {
        when(classificacaoRepository.buscarVigentes(eq("999999"), any())).thenReturn(List.of());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.listar(DATA, null, null, "999999"));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void cClassTribSemEfeitoVigenteDoTpRBSN_retorna400() {
        // O filtro nunca é ignorado silenciosamente: sem linha de efeito
        // vigente para o tpRBSN resolvido, a consulta falha com 400.
        ClassificacaoTributariaSimples classificacao = new ClassificacaoTributariaSimples();
        classificacao.setTpRBSN(1);
        when(classificacaoRepository.buscarVigentes(eq("000001"), any())).thenReturn(List.of(classificacao));
        when(parametroService.buscarTipoReceitaBruta(eq(1), any())).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.listar(DATA, null, null, "000001"));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void motivosDeDocumentoEDeClassificacaoCombinados() {
        // O '90' inadmissível no documento E incompatível com a classificação:
        // os dois motivos vêm preenchidos e independentes.
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of(vinculo(1L, 55)));
        stubClassTrib("000001", 1, true);

        AtividadeSimplesDomain naoTributavel = service.listar(DATA, null, "55", "000001").get(1);

        assertEquals(Boolean.FALSE, naoTributavel.admissivelNoDocumento());
        assertEquals(Boolean.FALSE, naoTributavel.compativelComClassTrib());
        assertFalse(naoTributavel.disponivel());
    }

    // ─── Busca por código ──────────────────────────────────────────────────────

    @Test
    void buscarPorCodigoRecebeOMesmoTratamentoDosFiltros() {
        when(repository.buscarVinculosTpDocVigentes(DATA)).thenReturn(List.of(vinculo(1L, 55), vinculo(1L, 65)));

        AtividadeSimplesDomain atividade = service.buscarPorCodigo("01", DATA, "91", null).orElseThrow();

        assertEquals(Boolean.FALSE, atividade.admissivelNoDocumento());
        assertFalse(atividade.disponivel());
        assertEquals(List.of("55", "65"), atividade.tpDocsAdmissiveis());
    }

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    private void stubClassTrib(String codigo, int tpRBSN, boolean vedadoNaoTributavel) {
        ClassificacaoTributariaSimples classificacao = new ClassificacaoTributariaSimples();
        classificacao.setId(1L);
        classificacao.setCodigo(codigo);
        classificacao.setTpRBSN(tpRBSN);
        when(classificacaoRepository.buscarVigentes(eq(codigo), any())).thenReturn(List.of(classificacao));
        TipoReceitaBrutaSimples tipoReceita = new TipoReceitaBrutaSimples();
        tipoReceita.setCodigo(tpRBSN);
        tipoReceita.setVedadoNaoTributavel(vedadoNaoTributavel);
        when(parametroService.buscarTipoReceitaBruta(eq(tpRBSN), any())).thenReturn(Optional.of(tipoReceita));
    }

    private static AtividadeSimplesNacional atividade(Long id, int ordem, String cAtivSN, String anexoSemFatorR) {
        AtividadeSimplesNacional atividade = new AtividadeSimplesNacional();
        atividade.setId(id);
        atividade.setOrdem(ordem);
        atividade.setDescricao("Atividade " + cAtivSN);
        atividade.setAnexoSemFatorR(anexoSemFatorR);
        atividade.setRotulo(anexoSemFatorR == null ? "Não tributável" : anexoSemFatorR);
        atividade.setRotuloCurto(atividade.getRotulo());
        atividade.setInicioVigencia(LocalDate.of(2027, 1, 1));
        atividade.setCAtivSN(cAtivSN);
        atividade.setNaoTributavel(anexoSemFatorR == null);
        return atividade;
    }

    private static VinculoTpDocAtividade vinculo(Long atividadeId, Integer tpDoc) {
        return new VinculoTpDocAtividade() {
            @Override
            public Long getAtividadeId() {
                return atividadeId;
            }

            @Override
            public Integer getTpDoc() {
                return tpDoc;
            }
        };
    }
}
