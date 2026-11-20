package br.gov.serpro.rtc.testesunitarios.versao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import br.gov.serpro.rtc.api.versao.dto.VersaoDomain;
import br.gov.serpro.rtc.api.versao.service.VersaoOfflineService;
import br.gov.serpro.rtc.api.versao.service.VersaoService;

/**
 * Testes unitários da comparação de versões feita na subida do modo offline
 * ({@link VersaoOfflineService#verificarVersaoNaInicializacao()}): até 3
 * tentativas de consulta à instalação online e, se todas falharem, só um aviso
 * no log — a subida nunca é interrompida por causa dela.
 */
class Teste_VersaoOfflineService {

    private static final String URL_REMOTA = "http://online.exemplo/versao";
    private static final String CORPO_REMOTO = """
            {"versaoApp":"1.0.0","versaoDb":"V0001","dataVersaoDb":"2026-01-01"}""";

    private VersaoService versaoService;
    private VersaoOfflineService servico;
    private MockRestServiceServer servidorRemoto;

    @BeforeEach
    void preparar() {
        versaoService = mock(VersaoService.class);
        when(versaoService.getVersao()).thenReturn(VersaoDomain.builder()
                .versaoApp("1.0.0").versaoDb("V0001").dataVersaoDb("2026-01-01").build());

        servico = new VersaoOfflineService(versaoService, new RestTemplateBuilder(),
                Duration.ofSeconds(5), Duration.ofSeconds(10));
        ReflectionTestUtils.setField(servico, "urlVersaoRemota", URL_REMOTA);
        ReflectionTestUtils.setField(servico, "intervaloEntreTentativas", Duration.ofMillis(1));

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(servico, "restTemplate");
        servidorRemoto = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void teste_ConsultaRespondeNaPrimeira_NaoRepete() {
        servidorRemoto.expect(ExpectedCount.once(), requestTo(URL_REMOTA))
                .andRespond(withSuccess(CORPO_REMOTO, MediaType.APPLICATION_JSON));

        servico.verificarVersaoNaInicializacao();

        servidorRemoto.verify();
    }

    @Test
    void teste_ConsultaFalhaUmaVez_RespondeNaSegundaTentativa() {
        servidorRemoto.expect(ExpectedCount.once(), requestTo(URL_REMOTA)).andRespond(withServerError());
        servidorRemoto.expect(ExpectedCount.once(), requestTo(URL_REMOTA))
                .andRespond(withSuccess(CORPO_REMOTO, MediaType.APPLICATION_JSON));

        servico.verificarVersaoNaInicializacao();

        servidorRemoto.verify(); // exatamente 2 requisições
    }

    @Test
    void teste_ConsultaFalhaSempre_TresTentativasESubidaSegue() {
        servidorRemoto.expect(ExpectedCount.times(3), requestTo(URL_REMOTA)).andRespond(withServerError());

        assertDoesNotThrow(servico::verificarVersaoNaInicializacao);

        servidorRemoto.verify(); // exatamente 3 requisições: nem menos, nem uma quarta
    }

    @Test
    void teste_IllegalArgumentException_NaoERepetidaESubidaSegue() {
        when(versaoService.getVersao()).thenThrow(new IllegalArgumentException("versão local inválida"));

        assertDoesNotThrow(servico::verificarVersaoNaInicializacao);

        verify(versaoService, times(1)).getVersao();
        servidorRemoto.verify(); // nenhuma requisição
    }
}
