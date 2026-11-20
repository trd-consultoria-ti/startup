/*
* Versão de Homologação/Testes
*/
package br.gov.serpro.rtc.testesintegracao.tpdoc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import br.gov.serpro.rtc.domain.model.dto.ClassificacaoTributariaDTO;
import br.gov.serpro.rtc.domain.repository.NbsAplicavelRepository;
import br.gov.serpro.rtc.domain.repository.NcmAplicavelRepository;
import br.gov.serpro.rtc.domain.service.ClassificacaoTributariaService;
import br.gov.serpro.rtc.domain.service.NcmAplicavelService;
import br.gov.serpro.rtc.domain.service.exception.NcmNaoVinculadaException;

/**
 * Regressões das correções da lógica de anexo (Requisito 6):
 * 1. {@code tem()} passa a filtrar vigência — vínculos futuros/expirados não
 *    caracterizam anexo (ids 139/140 são as versões 2027 das classificações
 *    000001/000002, cujos vínculos só vigoram a partir de 2027-01-01);
 * 2. tabela-verdade da crítica contra o anexo com as exceções de anexo
 *    (classificação 200003, id 14: NCM genérico 0207 aplicável com exceções
 *    02074300/02075300 — o excepcionado não é aceito, o não excepcionado sim).
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class Teste_tpdoc_9_RegressaoAnexoVigenciaExcecao {

    private static final LocalDate DATA_2026 = LocalDate.of(2026, 6, 1);
    private static final LocalDate DATA_2027 = LocalDate.of(2027, 6, 1);

    @Autowired
    private NcmAplicavelRepository ncmAplicavelRepository;

    @Autowired
    private NbsAplicavelRepository nbsAplicavelRepository;

    @Autowired
    private NcmAplicavelService ncmAplicavelService;

    @Autowired
    private ClassificacaoTributariaService classificacaoTributariaService;

    @Test
    @DisplayName("tem(): vínculo NCM com vigência futura não caracteriza anexo")
    void teste_temNcm_filtraVigencia() {
        assertThat(ncmAplicavelRepository.tem(139L, DATA_2026)).isFalse();
        assertThat(ncmAplicavelRepository.tem(139L, DATA_2027)).isTrue();
    }

    @Test
    @DisplayName("tem(): vínculo NBS com vigência futura não caracteriza anexo")
    void teste_temNbs_filtraVigencia() {
        assertThat(nbsAplicavelRepository.tem(139L, DATA_2026)).isFalse();
        assertThat(nbsAplicavelRepository.tem(139L, DATA_2027)).isTrue();
    }

    @Test
    @DisplayName("Tabela-verdade: NCM excepcionado do anexo não é aceito (A=1, E=1, S=0)")
    void teste_ncmExcepcionadoRejeitado() {
        ClassificacaoTributariaDTO classificacao = classificacaoTributariaService
                .buscarClassificacaoTributariaCbsIbs("200003", DATA_2026);

        assertThat(ncmAplicavelRepository.temNcmAplicavel("02074300", classificacao.id(), DATA_2026)).isTrue();
        assertThat(ncmAplicavelRepository.temExcecaoNcmAplicavel("02074300", classificacao.id(), DATA_2026)).isTrue();
        assertThat(ncmAplicavelRepository.temNcmAplicavelSemExcecao("02074300", classificacao.id(), DATA_2026)).isFalse();

        assertThatThrownBy(() -> ncmAplicavelService.criticarNcmContraAnexo("02074300",
                classificacao.id(), classificacao.codigo(), DATA_2026, "CBS e IBS"))
                .isExactlyInstanceOf(NcmNaoVinculadaException.class);
    }

    @Test
    @DisplayName("Tabela-verdade: NCM coberto pelo genérico e não excepcionado é aceito (A=1, E=0)")
    void teste_ncmNaoExcepcionadoAceito() {
        ClassificacaoTributariaDTO classificacao = classificacaoTributariaService
                .buscarClassificacaoTributariaCbsIbs("200003", DATA_2026);

        assertThat(ncmAplicavelRepository.temNcmAplicavel("02071100", classificacao.id(), DATA_2026)).isTrue();
        assertThat(ncmAplicavelRepository.temExcecaoNcmAplicavel("02071100", classificacao.id(), DATA_2026)).isFalse();

        assertThatCode(() -> ncmAplicavelService.criticarNcmContraAnexo("02071100",
                classificacao.id(), classificacao.codigo(), DATA_2026, "CBS e IBS"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "2026-01-01", "2026-06-01" })
    @DisplayName("Anexo XV (200014): 0710.80.00 é aceito (exclusão alcança só cogumelos e trufas), inclusive em 01/01/2026")
    void teste_anexoXv_07108000Aceito(final String data) {
        final LocalDate dataVigencia = LocalDate.parse(data);
        ClassificacaoTributariaDTO classificacao = classificacaoTributariaService
                .buscarClassificacaoTributariaCbsIbs("200014", dataVigencia);

        assertThat(ncmAplicavelRepository.temExcecaoNcmAplicavel("07108000", classificacao.id(), dataVigencia))
                .isFalse();
        assertThatCode(() -> ncmAplicavelService.criticarNcmContraAnexo("07108000",
                classificacao.id(), classificacao.codigo(), dataVigencia, "CBS e IBS"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "2026-01-01", "2026-06-01" })
    @DisplayName("Anexo XV (200014): cogumelos e trufas da subposição 0709.5 continuam excepcionados")
    void teste_anexoXv_07095Rejeitado(final String data) {
        final LocalDate dataVigencia = LocalDate.parse(data);
        ClassificacaoTributariaDTO classificacao = classificacaoTributariaService
                .buscarClassificacaoTributariaCbsIbs("200014", dataVigencia);

        assertThatThrownBy(() -> ncmAplicavelService.criticarNcmContraAnexo("07095100",
                classificacao.id(), classificacao.codigo(), dataVigencia, "CBS e IBS"))
                .isExactlyInstanceOf(NcmNaoVinculadaException.class);
    }

    @Test
    @DisplayName("Tabela-verdade: NCM fora do anexo é rejeitado (A=0, E=0, S=0)")
    void teste_ncmForaDoAnexoRejeitado() {
        ClassificacaoTributariaDTO classificacao = classificacaoTributariaService
                .buscarClassificacaoTributariaCbsIbs("200003", DATA_2026);

        assertThatThrownBy(() -> ncmAplicavelService.criticarNcmContraAnexo("24021000",
                classificacao.id(), classificacao.codigo(), DATA_2026, "CBS e IBS"))
                .isExactlyInstanceOf(NcmNaoVinculadaException.class);
    }

}
