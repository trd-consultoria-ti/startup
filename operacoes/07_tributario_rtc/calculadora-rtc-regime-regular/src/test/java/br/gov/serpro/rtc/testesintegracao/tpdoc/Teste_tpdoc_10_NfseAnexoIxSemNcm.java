/*
* Versão de Homologação/Testes
*/
package br.gov.serpro.rtc.testesintegracao.tpdoc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import tools.jackson.databind.ObjectMapper;

import br.gov.serpro.rtc.api.model.input.OperacaoInput;
import br.gov.serpro.rtc.domain.service.CalculadoraService;
import br.gov.serpro.rtc.domain.service.exception.NbsCompletoNaoInformadoException;
import br.gov.serpro.rtc.domain.service.exception.NbsNaoVinculadaException;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaIncompativelTipoDfeException;

/**
 * cClassTrib 515001 (diferimento dos insumos agropecuários e aquícolas do
 * Anexo IX) com tpDoc 91 (NFS-e). O Anexo IX é misto (NCM e NBS), mas o NCM não
 * representa locação de bens. Por isso, como na 200038, a nomenclatura é
 * EXCECAO_NBS (não MISTO): a NFS-e exige NBS do anexo e o NCM é bloqueado.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class Teste_tpdoc_10_NfseAnexoIxSemNcm {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CalculadoraService calculadoraService;

    private OperacaoInput operacao;

    @BeforeEach
    void beforeEach(final @Value("classpath:entradas/tpdoc/tpdoc_nfse_515001.json") Resource resourceFile)
            throws IOException {
        operacao = objectMapper.readValue(resourceFile.getInputStream(), OperacaoInput.class);
    }

    @Test
    @DisplayName("NFS-e + 515001: NBS válida do Anexo IX passa")
    void teste_nbsValidaDoAnexoPassa() {
        assertThatCode(() -> calculadoraService.calcularTributos(operacao))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("NFS-e + 515001: NCM do Anexo IX é bloqueado (nomenclatura incompatível)")
    void teste_ncmDoAnexoBloqueado() {
        operacao.getItens().get(0).setNbs(null);
        operacao.getItens().get(0).setNcm("31010000");
        assertThatThrownBy(() -> calculadoraService.calcularTributos(operacao))
                .isExactlyInstanceOf(NomenclaturaIncompativelTipoDfeException.class);
    }

    @Test
    @DisplayName("NFS-e + 200038: NCM do Anexo IX é bloqueado (mesmo comportamento da 515001)")
    void teste_ncmDoAnexoBloqueado200038() {
        operacao.getItens().get(0).setNbs(null);
        operacao.getItens().get(0).setNcm("31010000");
        operacao.getItens().get(0).setCst("200");
        operacao.getItens().get(0).setCClassTrib("200038");
        assertThatThrownBy(() -> calculadoraService.calcularTributos(operacao))
                .isExactlyInstanceOf(NomenclaturaIncompativelTipoDfeException.class);
    }

    @Test
    @DisplayName("NFS-e + 515001: nenhuma nomenclatura informada gera erro de NBS completa")
    void teste_semNomenclaturaErro() {
        operacao.getItens().get(0).setNbs(null);
        assertThatThrownBy(() -> calculadoraService.calcularTributos(operacao))
                .isExactlyInstanceOf(NbsCompletoNaoInformadoException.class);
    }

    @Test
    @DisplayName("NFS-e + 515001: NBS fora do Anexo IX gera erro")
    void teste_nbsForaDoAnexoErro() {
        operacao.getItens().get(0).setNbs("115012000");
        assertThatThrownBy(() -> calculadoraService.calcularTributos(operacao))
                .isExactlyInstanceOf(NbsNaoVinculadaException.class);
    }

}
