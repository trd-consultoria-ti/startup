package br.gov.serpro.rtc.testesintegracao.simplesnacional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import br.gov.serpro.rtc.api.atividadesimples.entity.AtividadeSimplesNacional;
import br.gov.serpro.rtc.api.atividadesimples.repository.AtividadeSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.entity.RegimeApuracaoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.SituacaoIssParAliquotaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoDfeCalculoSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TipoReceitaBrutaSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.TransicaoAliquotaCompraGovSimples;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.DestinoValorSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.CodigoRegimeApuracaoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegimeCalculoSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.RegraAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.TipoAliquotaSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.enumeration.VocabularioRegimeSimplesEnum;
import br.gov.serpro.rtc.api.simplesnacional.repository.ClassificacaoTributariaSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.repository.TransicaoAliquotaCompraGovSimplesRepository;
import br.gov.serpro.rtc.api.simplesnacional.service.ParametroCalculoSimplesService;

/**
 * Testes de integração da CARGA das tabelas de parametrização do cálculo do
 * Simples Nacional (NT 2026.005), contra o banco real gerado pelas migrações.
 *
 * <p>Estes testes são o espelho em código da parametrização: qualquer mudança
 * de carga que altere o comportamento do cálculo deve falhar aqui antes de
 * chegar aos testes de API.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class ParametrizacaoSimplesNacionalTest {

    private static final LocalDate DATA_PADRAO = LocalDate.of(2027, 3, 15);

    @Autowired
    private ParametroCalculoSimplesService parametroService;

    @Autowired
    private TransicaoAliquotaCompraGovSimplesRepository transicaoCompraGovRepository;

    @Autowired
    private AtividadeSimplesRepository atividadeRepository;

    @Autowired
    private ClassificacaoTributariaSimplesRepository classificacaoRepository;

    // ─── Classificações sem tratamento implementado (indicador de ajuste) ──────

    @Test
    void classificacoesSemTratamentoImplementado_conformeACargaVigente() {
        // Tratamentos tributários com TRTR_IN_POSSUI_AJUSTE = 1 na carga atual:
        // monofasia de combustíveis (62xxxx), hidrocarbonetos (550023),
        // transferência de crédito/ajustes/documento específico/exclusão de BC
        // (8xxxxx, exceto documento específico 820001/820005/820008/820009)
        // e incorporação imobiliária (2210xx). Toda classificação da
        // CBS vinculada a eles, vigente na data, é NAO_IMPLEMENTADO no cálculo.
        for (String codigo : List.of("550023", "620001", "620002", "620006", "620007", "800001", "800002",
                "810001", "811001", "811003", "830001", "221002",
                "221004")) {
            assertTrue(classificacaoRepository.contarTratamentosNaoImplementados(codigo, DATA_PADRAO) > 0,
                    () -> "cClassTrib " + codigo + " deveria estar marcado como sem implementação em "
                            + DATA_PADRAO);
        }
        // O tratamento 32 (documento específico) não possui ajuste na carga
        // vigente: esses quatro códigos não levam a NAO_IMPLEMENTADO.
        for (String codigo : List.of("820001", "820005", "820008", "820009")) {
            assertEquals(0, classificacaoRepository.contarTratamentosNaoImplementados(codigo, DATA_PADRAO),
                    () -> "cClassTrib " + codigo + " não deveria estar marcado como sem implementação");
        }
        // Classificações implementadas (tributação integral, imunidade, tpRBSN
        // 9 e 5, tributação em documento específico 820xxx) e código
        // inexistente: contagem zero.
        for (String codigo : List.of("000001", "410001", "010001", "222001", "820001", "820002", "820003",
                "820004", "820005", "820006", "820007", "820008", "820009", "999999")) {
            assertEquals(0, classificacaoRepository.contarTratamentosNaoImplementados(codigo, DATA_PADRAO),
                    () -> "cClassTrib " + codigo + " NÃO deveria estar marcado como sem implementação");
        }
    }

    @Test
    void classificacaoSemImplementacaoEncerradaEm2026_naoContaEm2027() {
        // 220001/220002/220003 foram substituídas por 221002/221003/221004 e
        // encerradas (classificação e vínculo) em 2026-01-01: o indicador conta
        // apenas dentro da vigência — em 2027 a resolução segue para VSN-021.
        for (String codigo : List.of("220001", "220002", "220003")) {
            assertTrue(classificacaoRepository.contarTratamentosNaoImplementados(codigo,
                    LocalDate.of(2026, 1, 1)) > 0, () -> codigo + " em 2026-01-01");
            assertEquals(0, classificacaoRepository.contarTratamentosNaoImplementados(codigo, DATA_PADRAO),
                    () -> codigo + " em " + DATA_PADRAO);
        }
    }

    // ─── Tipos de documento suportados pelo cálculo ────────────────────────────

    @Test
    void tiposDeDocumentoSuportados_conformeACargaVigente() {
        // Habilitados exatamente os documentos com nota técnica publicada:
        // NF-e, NFC-e e NFS-e (V0044) e, desde a V0045 (NTs 2026.003), CT-e,
        // CT-e OS, BP-e, BP-e TM e NFCom.
        assertTrue(parametroService.tpDocSuportado(55, DATA_PADRAO), "NF-e (55) deveria ser suportada");
        assertTrue(parametroService.tpDocSuportado(65, DATA_PADRAO), "NFC-e (65) deveria ser suportada");
        assertTrue(parametroService.tpDocSuportado(91, DATA_PADRAO), "NFS-e (91) deveria ser suportada");
        assertTrue(parametroService.tpDocSuportado(57, DATA_PADRAO), "CT-e (57) deveria ser suportado");
        assertTrue(parametroService.tpDocSuportado(67, DATA_PADRAO), "CT-e OS (67) deveria ser suportado");
        assertTrue(parametroService.tpDocSuportado(63, DATA_PADRAO), "BP-e (63) deveria ser suportado");
        assertTrue(parametroService.tpDocSuportado(93, DATA_PADRAO), "BP-e TM (93) deveria ser suportado");
        assertTrue(parametroService.tpDocSuportado(62, DATA_PADRAO), "NFCom (62) deveria ser suportada");
        // Fora da whitelist: documentos não abrangidos pelas NTs publicadas.
        assertTrue(!parametroService.tpDocSuportado(97, DATA_PADRAO),
                "CT-e Simplificado (97) não deveria ser suportado");
        assertTrue(!parametroService.tpDocSuportado(83, DATA_PADRAO), "BP-e TA (83) não deveria ser suportado");
    }

    // ─── Efeitos dos tipos de receita bruta (matriz da NT 2026.005, seção 5.3.2) ─

    @Test
    void tiposDeReceitaBruta_cargaConformeAMatrizDaNt() {
        // Carga oficial, vigência única: o vIS NÃO compõe a receita bruta em
        // NENHUMA vigência (TRBS_IN_IS_COMPOE_RECEITA = 0 em todas as linhas —
        // divergência deliberada com a fórmula P26 da NT; a coluna permanece
        // como alavanca de parametrização para mudanças legais futuras).
        assertMatrizTiposReceita(DATA_PADRAO, false);
        assertMatrizTiposReceita(LocalDate.of(2028, 6, 15), false);
        assertMatrizTiposReceita(LocalDate.of(2033, 6, 15), false);
    }

    private void assertMatrizTiposReceita(LocalDate data, boolean isCompoeReceita) {
        // código -> {descrição, compõe receita, fator, pendente/suspenso,
        //            exige alíquota, vedado p/ não tributável}
        assertTipoReceita(data, isCompoeReceita, 0, "Não é receita bruta", false, "0", false, false, false);
        assertTipoReceita(data, isCompoeReceita, 1, "Receita bruta - interna", true, "1", false, true, true);
        assertTipoReceita(data, isCompoeReceita, 2, "Receita bruta - interna sem cálculo de IBS e CBS", true, "0",
                false, false, true);
        assertTipoReceita(data, isCompoeReceita, 3, "Receita bruta - exportação direta", true, "0", false, false, true);
        assertTipoReceita(data, isCompoeReceita, 4, "Receita bruta - exportação indireta", true, "1", true, true, true);
        assertTipoReceita(data, isCompoeReceita, 5, "Receita bruta - mercado interno e exportação", true, "0.5",
                false, true, true);
        assertTipoReceita(data, isCompoeReceita, 9, "Fornecimento incompatível com SN", true, "0", false, false, false);
    }

    private void assertTipoReceita(LocalDate data, boolean isCompoeReceita, int codigo, String descricao,
            boolean compoeReceita, String fatorReceita, boolean pendenteSuspenso, boolean exigeAliquota,
            boolean vedadoNaoTributavel) {
        Optional<TipoReceitaBrutaSimples> vigente = parametroService.buscarTipoReceitaBruta(codigo, data);
        assertTrue(vigente.isPresent(), () -> "tpRBSN " + codigo + " deveria ter linha vigente em " + data);
        TipoReceitaBrutaSimples tipo = vigente.get();
        assertEquals(descricao, tipo.getDescricao());
        assertEquals(compoeReceita, tipo.getCompoeReceita());
        assertEquals(isCompoeReceita, tipo.getImpostoSeletivoCompoeReceita(),
                () -> "composição do vIS na receita bruta do tpRBSN " + codigo + " em " + data);
        assertEquals(0, new BigDecimal(fatorReceita).compareTo(tipo.getFatorReceita()),
                () -> "fator de receita do tpRBSN " + codigo);
        assertEquals(pendenteSuspenso, tipo.getPendenteSuspenso());
        assertEquals(exigeAliquota, tipo.getExigeAliquota());
        assertEquals(vedadoNaoTributavel, tipo.getVedadoNaoTributavel());
    }

    // ─── Matriz de regime (fórmulas P28 a P33 da NT 2026.005) ──────────────────

    @Test
    void matrizDeRegime_cargaConformeANt() {
        assertRegime(RegimeCalculoSimplesEnum.CRT_1, true,
                DestinoValorSimplesEnum.DEVIDO, DestinoValorSimplesEnum.PENDENTE_SUSPENSO);
        assertRegime(RegimeCalculoSimplesEnum.CRT_2, false,
                DestinoValorSimplesEnum.DEVIDO, DestinoValorSimplesEnum.PENDENTE_SUSPENSO);
        // CRT=3 + tpRegimeIBSCBS=2: devido vira pendente/suspenso e o já
        // suspenso vira ZERO — literal nas fórmulas P32/P33 da NT, intencional.
        assertRegime(RegimeCalculoSimplesEnum.CRT_3_TPREGIME_2, true,
                DestinoValorSimplesEnum.PENDENTE_SUSPENSO, DestinoValorSimplesEnum.ZERO);
    }

    private void assertRegime(RegimeCalculoSimplesEnum regime, boolean geraIbs,
            DestinoValorSimplesEnum destinoDevido, DestinoValorSimplesEnum destinoPendenteSuspenso) {
        Optional<RegimeApuracaoSimples> vigente = parametroService.buscarRegimeApuracao(regime, DATA_PADRAO);
        assertTrue(vigente.isPresent(), () -> "regime " + regime + " deveria ter linha vigente");
        assertEquals(geraIbs, vigente.get().getGeraIbs(), () -> "geraIbs do regime " + regime);
        assertEquals(destinoDevido, vigente.get().getDestinoDevido());
        assertEquals(destinoPendenteSuspenso, vigente.get().getDestinoPendenteSuspenso());
    }

    // ─── V0067: características do cálculo por documento ───────────────────────

    @Test
    void tiposDeDocumento_caracteristicasDoCalculoPorDocumento() {
        // Família CRT: vocabulário CRT, precisão integral, sem limite de itens, admite IS.
        for (int tpDoc : List.of(55, 65, 57, 67, 63, 93, 62)) {
            TipoDfeCalculoSimples documento = parametroService.buscarTipoDfeCalculo(tpDoc, DATA_PADRAO).orElseThrow();
            assertEquals(VocabularioRegimeSimplesEnum.CRT, documento.getVocabularioRegime(), () -> "tpDoc " + tpDoc);
            assertNull(documento.getCasasAliquotaCalculo(), () -> "tpDoc " + tpDoc);
            assertNull(documento.getMaximoItens(), () -> "tpDoc " + tpDoc);
            assertEquals(Boolean.TRUE, documento.getAdmiteImpostoSeletivo(), () -> "tpDoc " + tpDoc);
        }
        // NFS-e: vocabulário próprio, alíquota de 2 casas, 1 item, sem IS.
        TipoDfeCalculoSimples nfse = parametroService.buscarTipoDfeCalculo(91, DATA_PADRAO).orElseThrow();
        assertEquals(VocabularioRegimeSimplesEnum.NFSE, nfse.getVocabularioRegime());
        assertEquals(2, nfse.getCasasAliquotaCalculo());
        assertEquals(1, nfse.getMaximoItens());
        assertEquals(Boolean.FALSE, nfse.getAdmiteImpostoSeletivo());
        // Fora da whitelist: vazio.
        assertTrue(parametroService.buscarTipoDfeCalculo(97, DATA_PADRAO).isEmpty());
    }

    // ─── V0067: enquadramento do regime ─────────────────────────────────────────

    @Test
    void enquadramentoDoRegime_onzeCombinacoesQueLevamAUmRegime() {
        assertEnquadramentoCrt(1, null, CodigoRegimeApuracaoSimplesEnum.SN_PLENO);
        assertEnquadramentoCrt(2, null, CodigoRegimeApuracaoSimplesEnum.SN_SUBLIMITE);
        assertEnquadramentoCrt(1, 1, CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA);
        assertEnquadramentoCrt(2, 1, CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA);
        assertEnquadramentoCrt(3, 2, CodigoRegimeApuracaoSimplesEnum.PENDENTE);
        assertEnquadramentoNfse(3, 1, CodigoRegimeApuracaoSimplesEnum.SN_PLENO);
        assertEnquadramentoNfse(3, 2, CodigoRegimeApuracaoSimplesEnum.SN_SUBLIMITE);
        assertEnquadramentoNfse(3, 3, CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA);
        assertEnquadramentoNfse(4, 1, CodigoRegimeApuracaoSimplesEnum.PENDENTE);
        assertEnquadramentoNfse(4, 2, CodigoRegimeApuracaoSimplesEnum.PENDENTE_SUBLIMITE);
        assertEnquadramentoNfse(4, 3, CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA);
    }

    @Test
    void enquadramentoDoRegime_combinacoesValidasSemLinha_foraDoEscopo() {
        // CRT 3 sem tpRegime (regime normal), CRT 4 (MEI), opSimpNac 1 (não optante) e 2 (MEI).
        assertTrue(enquadramentoCrt(3, null).isEmpty());
        assertTrue(enquadramentoCrt(4, null).isEmpty());
        for (int opSimpNac : List.of(1, 2)) {
            for (int regAp = 1; regAp <= 3; regAp++) {
                assertTrue(enquadramentoNfse(opSimpNac, regAp).isEmpty(), "opSimpNac " + opSimpNac);
            }
        }
        // O vocabulário separa as linhas: a combinação da NFS-e não vale no vocabulário CRT.
        assertTrue(parametroService.buscarRegimeApuracao(VocabularioRegimeSimplesEnum.CRT, null, null, 3, 1,
                DATA_PADRAO).isEmpty());
    }

    @Test
    void regimesDeApuracao_comportamentoPorCodigoNeutro() {
        assertComportamento(enquadramentoNfse(4, 2).orElseThrow(), CodigoRegimeApuracaoSimplesEnum.PENDENTE_SUBLIMITE,
                true, false, DestinoValorSimplesEnum.PENDENTE_SUSPENSO, DestinoValorSimplesEnum.ZERO);
        RegimeApuracaoSimples somenteReceita = enquadramentoCrt(1, 1).orElseThrow();
        assertEquals(CodigoRegimeApuracaoSimplesEnum.SOMENTE_RECEITA, somenteReceita.getCodigo());
        assertEquals(Boolean.FALSE, somenteReceita.getCalculaTributos());
        assertNull(somenteReceita.getGeraIbs());
        assertNull(somenteReceita.getDestinoDevido());
        assertNull(somenteReceita.getDestinoPendenteSuspenso());
        // As três linhas de antes mantêm IDs e comportamento.
        assertComportamento(enquadramentoCrt(1, null).orElseThrow(), CodigoRegimeApuracaoSimplesEnum.SN_PLENO, true,
                true, DestinoValorSimplesEnum.DEVIDO, DestinoValorSimplesEnum.PENDENTE_SUSPENSO);
        assertEquals(1L, enquadramentoCrt(1, null).orElseThrow().getId());
        assertEquals(2L, enquadramentoCrt(2, null).orElseThrow().getId());
        assertEquals(3L, enquadramentoCrt(3, 2).orElseThrow().getId());
        // A NFS-e e o CRT chegam à MESMA linha do regime.
        assertEquals(enquadramentoCrt(2, null).orElseThrow().getId(), enquadramentoNfse(3, 2).orElseThrow().getId());
    }

    private Optional<RegimeApuracaoSimples> enquadramentoCrt(int crt, Integer tpRegime) {
        return parametroService.buscarRegimeApuracao(VocabularioRegimeSimplesEnum.CRT, crt, tpRegime, null, null,
                DATA_PADRAO);
    }

    private Optional<RegimeApuracaoSimples> enquadramentoNfse(int opSimpNac, int regApIBSCBSSN) {
        return parametroService.buscarRegimeApuracao(VocabularioRegimeSimplesEnum.NFSE, null, null, opSimpNac,
                regApIBSCBSSN, DATA_PADRAO);
    }

    private void assertEnquadramentoCrt(int crt, Integer tpRegime, CodigoRegimeApuracaoSimplesEnum esperado) {
        assertEquals(esperado, enquadramentoCrt(crt, tpRegime).map(RegimeApuracaoSimples::getCodigo).orElse(null),
                () -> "CRT " + crt + " + tpRegimeIBSCBS " + tpRegime);
    }

    private void assertEnquadramentoNfse(int opSimpNac, int regAp, CodigoRegimeApuracaoSimplesEnum esperado) {
        assertEquals(esperado, enquadramentoNfse(opSimpNac, regAp).map(RegimeApuracaoSimples::getCodigo).orElse(null),
                () -> "opSimpNac " + opSimpNac + " + regApIBSCBSSN " + regAp);
    }

    private void assertComportamento(RegimeApuracaoSimples regime, CodigoRegimeApuracaoSimplesEnum codigo,
            boolean calcula, boolean geraIbs, DestinoValorSimplesEnum devido, DestinoValorSimplesEnum pendente) {
        assertEquals(codigo, regime.getCodigo());
        assertEquals(calcula, regime.getCalculaTributos());
        assertEquals(geraIbs, regime.getGeraIbs());
        assertEquals(devido, regime.getDestinoDevido());
        assertEquals(pendente, regime.getDestinoPendenteSuspenso());
    }

    // ─── V0067: efeito do tpRBSN na NFS-e ──────────────────────────────────────

    @Test
    void tiposDeReceitaBruta_linhasDaNfse() {
        // NFS-e: só o tpRBSN 1 gera valor (fator 1, exige alíquota); os demais, zero e sem
        // pendente/suspenso. Receita bruta e vedação para não tributável como no padrão.
        for (int codigo : List.of(0, 1, 2, 3, 4, 5, 9)) {
            TipoReceitaBrutaSimples padrao = parametroService.buscarTipoReceitaBruta(codigo, DATA_PADRAO).orElseThrow();
            TipoReceitaBrutaSimples nfse = parametroService
                    .buscarTipoReceitaBruta(codigo, VocabularioRegimeSimplesEnum.NFSE, DATA_PADRAO).orElseThrow();
            assertNull(padrao.getVocabularioRegime(), () -> "padrão do tpRBSN " + codigo);
            assertEquals(VocabularioRegimeSimplesEnum.NFSE, nfse.getVocabularioRegime(), () -> "tpRBSN " + codigo);
            assertEquals(padrao.getDescricao(), nfse.getDescricao());
            assertEquals(padrao.getCompoeReceita(), nfse.getCompoeReceita());
            assertEquals(padrao.getVedadoNaoTributavel(), nfse.getVedadoNaoTributavel());
            assertEquals(Boolean.FALSE, nfse.getImpostoSeletivoCompoeReceita());
            assertEquals(Boolean.FALSE, nfse.getPendenteSuspenso(), () -> "tpRBSN " + codigo);
            assertEquals(0, new BigDecimal(codigo == 1 ? "1" : "0").compareTo(nfse.getFatorReceita()),
                    () -> "fator do tpRBSN " + codigo);
            assertEquals(codigo == 1, nfse.getExigeAliquota(), () -> "exige alíquota no tpRBSN " + codigo);
        }
        // Vocabulário CRT não tem linhas próprias: vale a padrão.
        assertNull(parametroService.buscarTipoReceitaBruta(4, VocabularioRegimeSimplesEnum.CRT, DATA_PADRAO)
                .orElseThrow().getVocabularioRegime());
    }

    // ─── V0067: indicadores de ISS do catálogo e situações do par de alíquotas ─

    @Test
    void catalogoDeAtividades_indicadoresDeIss() {
        Map<String, AtividadeSimplesNacional> porCodigo = atividadeRepository.buscarVigentes(DATA_PADRAO).stream()
                .collect(Collectors.toMap(AtividadeSimplesNacional::getCAtivSN, Function.identity()));
        List<String> comIss = List.of("07", "09", "10", "12", "13", "14");
        List<String> comBeneficio = List.of("12", "13");
        porCodigo.forEach((codigo, atividade) -> {
            assertEquals(comIss.contains(codigo), atividade.getIssNoSimples(), () -> "ISS no Simples da " + codigo);
            assertEquals(comBeneficio.contains(codigo), atividade.getAdmiteBeneficioMunicipal(),
                    () -> "benefício municipal da " + codigo);
        });
    }

    @Test
    void situacoesDoIss_cargaConformeASecao6() {
        List<SituacaoIssParAliquotaSimples> situacoes = parametroService.buscarSituacoesIssParAliquota(DATA_PADRAO);
        assertEquals(10, situacoes.size());
        Map<String, List<String>> porRegra = situacoes.stream()
                .collect(Collectors.groupingBy(s -> s.getRegra().name(), Collectors.mapping(
                        s -> s.getCampo() + "=" + s.getValor(), Collectors.toList())));
        assertEquals(List.of("CRT=2", "regApIBSCBSSN=2", "regApTribSN=2", "regApTribSN=3"),
                porRegra.get(RegraAliquotaSimplesEnum.ISS_FORA_DO_SIMPLES.name()));
        assertEquals(List.of("tribISSQN=2", "tribISSQN=3", "tribISSQN=4"),
                porRegra.get(RegraAliquotaSimplesEnum.ISS_NAO_INCIDE.name()));
        assertEquals(List.of("tpBM=1"), porRegra.get(RegraAliquotaSimplesEnum.ISENCAO_MUNICIPAL.name()));
        assertEquals(List.of("tpRetISSQN=2", "tpRetISSQN=3"), porRegra.get(RegraAliquotaSimplesEnum.ISS_RETIDO.name()));
        for (SituacaoIssParAliquotaSimples situacao : situacoes) {
            assertEquals(TipoAliquotaSimplesEnum.SEM_REDISTRIBUICAO, situacao.getTipoAliquota());
            // A ordem gravada é o número da regra (posição no enum + 1).
            assertEquals(situacao.getRegra().ordinal() + 1, situacao.getOrdem(), () -> "ordem de " + situacao);
        }
    }

    // ─── Transição de compra governamental (NT 2026.005, seção 5.3.3) ──────────

    /**
     * Para toda data de 2027-01-01 a 2035-12-31 deve haver exatamente UMA linha
     * vigente (vigências contíguas, sem sobreposição). A varredura diária é
     * feita em memória sobre as linhas carregadas.
     */
    @Test
    void transicaoCompraGov_vigenciasContiguasESemSobreposicaoDe2027a2035() {
        List<TransicaoAliquotaCompraGovSimples> linhas = transicaoCompraGovRepository.findAll();
        assertEquals(6, linhas.size(), "a tabela de transição deveria ter 6 linhas");

        LocalDate data = LocalDate.of(2027, 1, 1);
        LocalDate fim = LocalDate.of(2035, 12, 31);
        while (!data.isAfter(fim)) {
            final LocalDate referencia = data;
            long vigentes = linhas.stream()
                    .filter(t -> !referencia.isBefore(t.getInicioVigencia())
                            && (t.getFimVigencia() == null || !referencia.isAfter(t.getFimVigencia())))
                    .count();
            assertEquals(1, vigentes,
                    () -> String.format("data %s deveria ter exatamente uma vigência, encontrou %d",
                            referencia, vigentes));
            data = data.plusDays(1);
        }
    }

    @Test
    void transicaoCompraGov_dataAnteriorATodaVigencia_retornaVazio() {
        assertTrue(parametroService.buscarFatorTransicaoCompraGov(LocalDate.of(2025, 12, 31)).isEmpty());
    }

    @Test
    void transicaoCompraGov_fatoresConformeANt() {
        assertFator("0.00", LocalDate.of(2027, 1, 1));
        assertFator("0.00", LocalDate.of(2028, 12, 31));
        assertFator("0.10", LocalDate.of(2029, 6, 1));
        assertFator("0.20", LocalDate.of(2030, 6, 1));
        assertFator("0.30", LocalDate.of(2031, 6, 1));
        assertFator("0.40", LocalDate.of(2032, 6, 1));
        assertFator("1.00", LocalDate.of(2033, 1, 1));
        assertFator("1.00", LocalDate.of(2099, 12, 31));
    }

    private void assertFator(String esperado, LocalDate data) {
        Optional<BigDecimal> fator = parametroService.buscarFatorTransicaoCompraGov(data);
        assertTrue(fator.isPresent(), () -> "sem vigência para " + data);
        assertEquals(0, new BigDecimal(esperado).compareTo(fator.get()),
                () -> String.format("fator em %s: esperado %s, obtido %s", data, esperado, fator.get()));
    }

    // ─── Catálogo oficial de atividades ────────────────────────────────────────

    @Test
    void catalogoDeAtividades_quinzeLinhasComCodigosOficiaisEIndicadorNaoTributavel() {
        List<AtividadeSimplesNacional> atividades = atividadeRepository.buscarVigentes(DATA_PADRAO);
        assertEquals(15, atividades.size());

        Map<Integer, AtividadeSimplesNacional> porOrdem = atividades.stream()
                .collect(Collectors.toMap(AtividadeSimplesNacional::getOrdem, Function.identity()));

        // Atividades 1..14: códigos oficiais com zero à esquerda, tributáveis.
        for (int ordem = 1; ordem <= 14; ordem++) {
            AtividadeSimplesNacional atividade = porOrdem.get(ordem);
            assertNotNull(atividade, "atividade de ordem " + ordem);
            assertEquals(String.format("%02d", ordem), atividade.getCAtivSN());
            assertEquals(Boolean.FALSE, atividade.getNaoTributavel());
        }

        // Ordem 15 = código '90' (Operações não Tributáveis): sem anexos e com o
        // indicador de não tributável ligado.
        AtividadeSimplesNacional naoTributavel = porOrdem.get(15);
        assertNotNull(naoTributavel);
        assertEquals("90", naoTributavel.getCAtivSN());
        assertEquals(Boolean.TRUE, naoTributavel.getNaoTributavel());
        assertNull(naoTributavel.getAnexoSemFatorR());
        assertNull(naoTributavel.getAnexoComFatorR());

        // A 09 é a única atividade sujeita ao Fator R: fator r >= 28% => Anexo III
        // (anexoComFatorR); fator r < 28% => Anexo V (anexoSemFatorR) — LC 123/2006,
        // art. 18, §§ 5º-J e 5º-M; Resolução CGSN 190/2026 (correção da V0061).
        assertEquals("V", porOrdem.get(9).getAnexoSemFatorR());
        assertEquals("III", porOrdem.get(9).getAnexoComFatorR());
        for (int ordem = 1; ordem <= 14; ordem++) {
            if (ordem != 9) {
                assertNull(porOrdem.get(ordem).getAnexoComFatorR(),
                        "somente a atividade 09 possui anexo com Fator R");
            }
        }

        // Descrições da carga oficial: construção civil (12 e 13) referencia os
        // subitens 7.02 e 7.05 da LC 116/2003; transporte municipal (10)
        // referencia o subitem 16.01 e inclui o modal ferroviário.
        assertTrue(porOrdem.get(12).getDescricao().contains("7.02 e 7.05"));
        assertTrue(porOrdem.get(13).getDescricao().contains("7.02 e 7.05"));
        assertTrue(porOrdem.get(10).getDescricao().contains("ferroviário"));
        assertTrue(porOrdem.get(10).getDescricao().contains("16.01"));
    }

    // ─── Associativa atividade × documento (whitelist) ─────────────────────────

    /**
     * Carga dos vínculos vigentes da associativa
     * {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}, resolvidos em uma única
     * query pelo repositório do endpoint de atividades: '01'–'04' vinculadas a
     * NF-e/NFC-e; '07'–'14' à NFS-e; '05' aos documentos de transporte das NTs
     * 2026.003 (CT-e, CT-e OS, BP-e e BP-e TM, V0045); '06' à NFCom (V0045);
     * '90' a todos os documentos habilitados. Total: 29 vínculos.
     */
    @Test
    void associativaDeAtividades_vinculosVigentesConformeACarga() {
        List<AtividadeSimplesNacional> atividades = atividadeRepository.buscarVigentes(DATA_PADRAO);
        Map<Long, AtividadeSimplesNacional> porId = atividades.stream()
                .collect(Collectors.toMap(AtividadeSimplesNacional::getId, Function.identity()));

        List<AtividadeSimplesRepository.VinculoTpDocAtividade> vinculos =
                atividadeRepository.buscarVinculosTpDocVigentes(DATA_PADRAO);
        assertEquals(29, vinculos.size(), "a associativa deveria ter 29 vínculos vigentes");

        Map<String, List<Integer>> tpDocsPorCodigo = vinculos.stream()
                .collect(Collectors.groupingBy(v -> porId.get(v.getAtividadeId()).getCAtivSN(),
                        Collectors.collectingAndThen(Collectors.toList(),
                                lista -> lista.stream()
                                        .map(AtividadeSimplesRepository.VinculoTpDocAtividade::getTpDoc)
                                        .sorted()
                                        .toList())));

        for (String codigo : List.of("01", "02", "03", "04")) {
            assertEquals(List.of(55, 65), tpDocsPorCodigo.get(codigo),
                    () -> "atividade " + codigo + " deveria estar vinculada a NF-e/NFC-e");
        }
        for (String codigo : List.of("07", "08", "09", "10", "11", "12", "13", "14")) {
            assertEquals(List.of(91), tpDocsPorCodigo.get(codigo),
                    () -> "atividade " + codigo + " deveria estar vinculada somente à NFS-e");
        }
        assertEquals(List.of(57, 63, 67, 93), tpDocsPorCodigo.get("05"),
                "a atividade 05 deveria estar vinculada a CT-e, BP-e, CT-e OS e BP-e TM");
        assertEquals(List.of(62), tpDocsPorCodigo.get("06"),
                "a atividade 06 deveria estar vinculada somente à NFCom");
        assertEquals(List.of(55, 57, 62, 63, 65, 67, 91, 93), tpDocsPorCodigo.get("90"),
                "o código 90 deveria estar vinculado a todos os documentos habilitados");
    }
}
