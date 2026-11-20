package br.gov.serpro.rtc.testesunitarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import br.gov.serpro.rtc.api.model.output.versaooffline.VersaoOfflineOutput;
import br.gov.serpro.rtc.domain.model.entity.VersaoBaseDados;
import br.gov.serpro.rtc.domain.service.VersaoBaseDadosService;
import br.gov.serpro.rtc.domain.service.offline.VersaoOfflineService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Comparador de versões do perfil offline ({@code GET /versao/status}): a versão remota é lida da
 * instalação online pelo {@link RestTemplate} do próprio serviço, com os conversores que ele usa
 * em execução. A instalação online é simulada por {@link MockRestServiceServer} ou por uma porta
 * local (127.0.0.1): nenhum teste depende de rede.
 *
 * <p>Dois comportamentos são cobertos: a leitura do JSON publicado pela instalação online, e a
 * rota diante de uma falha da consulta remota — o modo offline não depende da disponibilidade da
 * instalação online.
 */
@ExtendWith(MockitoExtension.class)
class Teste_VersaoOfflineService {

    private static final String URL_REMOTA = "http://online.exemplo/api/calculadora/dados-abertos/versao";

    /** Resposta da instalação online em 18/11/2026. */
    private static final String CORPO_REMOTO = """
            {"ambiente":"apr","dataVersaoDb":"2026-11-09","descricaoVersaoApp":"1.7.0-753903df",
             "descricaoVersaoDb":"Nova tabela de Classificações Tributárias (IT 2025.002 v.1.71), publicada em 30/10/2026.",
             "versaoApp":"1.7.0-753903df","versaoDb":"V0073"}""";

    private VersaoBaseDadosService versaoBaseDadosService;
    private VersaoOfflineService servico;
    private MockRestServiceServer servidorRemoto;
    private ListAppender<ILoggingEvent> registros;

    @BeforeEach
    void preparar() {
        VersaoBaseDados versaoLocal = new VersaoBaseDados();
        versaoLocal.setNumeroVersao("V0073");
        versaoLocal.setData(LocalDate.of(2026, 11, 9));
        versaoBaseDadosService = mock(VersaoBaseDadosService.class);
        when(versaoBaseDadosService.getUltimaVersao()).thenReturn(versaoLocal);

        servico = novoServico(URL_REMOTA, Duration.ofSeconds(5), Duration.ofSeconds(10));

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(servico, "restTemplate");
        servidorRemoto = MockRestServiceServer.createServer(restTemplate);

        registros = new ListAppender<>();
        registros.start();
        registrador().addAppender(registros);
    }

    @AfterEach
    void desfazer() {
        registrador().detachAppender(registros);
    }

    @Test
    void leAVersaoPublicadaPelaInstalacaoOnlineECompara() {
        servidorRemoto.expect(ExpectedCount.once(), requestTo(URL_REMOTA))
                .andRespond(withSuccess(CORPO_REMOTO, MediaType.APPLICATION_JSON));

        VersaoOfflineOutput status = servico.verificarVersao();

        assertThat(status.getVersaoAplicacaoRemota()).isEqualTo("1.7.0-753903df");
        assertThat(status.getVersaoDbRemota()).isEqualTo("V0073");
        assertThat(status.getDataDbRemota()).isEqualTo("2026-11-09");
        assertThat(status.getVersaoAplicacaoLocal()).isEqualTo("1.7.2-c7512f9a");
        assertThat(status.getVersaoDbLocal()).isEqualTo("V0073");
        assertThat(status.getDataDbLocal()).isEqualTo("2026-11-09");
        assertThat(status.isAplicacaoAtualizada()).isFalse();
        assertThat(status.isDbAtualizada()).isTrue();
        assertThat(status.isDataDbAtualizada()).isTrue();
        servidorRemoto.verify();
    }

    /**
     * A instalação online pode estar numa versão mais nova, que publique campos que esta ainda não
     * conhece — é justamente o caso em que a comparação importa.
     */
    @Test
    void campoDesconhecidoNaRespostaRemotaNaoImpedeALeitura() {
        String corpo = """
                {"versaoApp":"1.8.0-abcdef12","versaoDb":"V0074","dataVersaoDb":"2026-12-01",
                 "arquivoDb":"calculadora-pro.db","campoFuturo":{"a":1}}""";
        servidorRemoto.expect(ExpectedCount.once(), requestTo(URL_REMOTA))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));

        VersaoOfflineOutput status = servico.verificarVersao();

        assertThat(status.getVersaoAplicacaoRemota()).isEqualTo("1.8.0-abcdef12");
        assertThat(status.getVersaoDbRemota()).isEqualTo("V0074");
        assertThat(status.getDataDbRemota()).isEqualTo("2026-12-01");
        assertThat(status.isAplicacaoAtualizada()).isFalse();
        assertThat(status.isDbAtualizada()).isFalse();
        assertThat(status.isDataDbAtualizada()).isFalse();
    }

    static Stream<Arguments> falhasDaConsultaRemota() {
        return Stream.of(
                Arguments.of("conexão recusada", withException(new ConnectException("Conexão recusada"))),
                Arguments.of("tempo esgotado", withException(new SocketTimeoutException("Read timed out"))),
                Arguments.of("erro 500 na instalação online", withServerError()),
                Arguments.of("rota inexistente (404)", withResourceNotFound()),
                Arguments.of("resposta 200 que não é JSON",
                        withSuccess("<html><body>Portal de acesso à rede</body></html>", MediaType.TEXT_HTML)),
                Arguments.of("JSON incompleto", withSuccess("{\"versaoApp\":", MediaType.APPLICATION_JSON)),
                Arguments.of("JSON de outro formato", withSuccess("[1,2,3]", MediaType.APPLICATION_JSON)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("falhasDaConsultaRemota")
    void falhaDaConsultaRemotaDevolveComparacaoSemVersaoRemota(String caso, ResponseCreator resposta) {
        servidorRemoto.expect(ExpectedCount.once(), requestTo(URL_REMOTA)).andRespond(resposta);

        VersaoOfflineOutput status = servico.verificarVersao();

        assertThat(status.getVersaoAplicacaoRemota()).isNull();
        assertThat(status.getVersaoDbRemota()).isNull();
        assertThat(status.getDataDbRemota()).isNull();
        assertThat(status.isAplicacaoAtualizada()).isFalse();
        assertThat(status.isDbAtualizada()).isFalse();
        assertThat(status.isDataDbAtualizada()).isFalse();
        assertThat(status.getVersaoAplicacaoLocal()).isEqualTo("1.7.2-c7512f9a");
        assertThat(status.getVersaoDbLocal()).isEqualTo("V0073");
        assertThat(status.getDataDbLocal()).isEqualTo("2026-11-09");

        List<ILoggingEvent> avisos = registros.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).getFormattedMessage()).contains(URL_REMOTA);
        servidorRemoto.verify(); // uma consulta só: a rota não repete
    }

    /**
     * Na subida, a falha da consulta sai do método: é ela que aciona a nova tentativa do
     * {@code @Retryable}. Se o método a absorvesse, a subida registraria uma comparação com a
     * versão remota nula em vez de tentar de novo.
     */
    @Test
    void naSubidaAFalhaDaConsultaRemotaAcionaNovaTentativa() {
        servidorRemoto.expect(ExpectedCount.once(), requestTo(URL_REMOTA)).andRespond(withServerError());

        assertThatThrownBy(servico::verificarVersaoNaInicializacao).isInstanceOf(RestClientException.class);

        assertThat(registros.list).isEmpty();
        servidorRemoto.verify();
    }

    @Test
    void naSubidaAComparacaoERegistradaQuandoAConsultaResponde() {
        servidorRemoto.expect(ExpectedCount.once(), requestTo(URL_REMOTA))
                .andRespond(withSuccess(CORPO_REMOTO, MediaType.APPLICATION_JSON));

        servico.verificarVersaoNaInicializacao();

        assertThat(registros.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("Aplicação (LOCAL):  1.7.2-c7512f9a", "Aplicação (ONLINE): 1.7.0-753903df",
                        "Banco de Dados (LOCAL):  V0073", "Banco de Dados (ONLINE): V0073");
        assertThat(registros.list).noneMatch(e -> e.getLevel() == Level.WARN);
    }

    /**
     * Sem simulação do cliente HTTP: a porta local aceita a conexão e nunca responde. O tempo
     * limite de leitura encerra a espera, e a rota responde sem a versão remota.
     */
    @Test
    void instalacaoOnlineQueNaoRespondeEsgotaOTempoLimiteDeLeitura() throws Exception {
        try (ServerSocket mudo = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            String url = "http://127.0.0.1:" + mudo.getLocalPort() + "/versao";
            VersaoOfflineService semSimulacao = novoServico(url, Duration.ofSeconds(2), Duration.ofMillis(300));

            long inicio = System.nanoTime();
            VersaoOfflineOutput status = semSimulacao.verificarVersao();
            Duration espera = Duration.ofNanos(System.nanoTime() - inicio);

            assertThat(status.getVersaoAplicacaoRemota()).isNull();
            assertThat(status.isAplicacaoAtualizada()).isFalse();
            assertThat(status.isDbAtualizada()).isFalse();
            assertThat(espera).isBetween(Duration.ofMillis(250), Duration.ofSeconds(5));
            assertThat(registros.list).filteredOn(e -> e.getLevel() == Level.WARN).hasSize(1);
        }
    }

    /** Sem simulação do cliente HTTP: nada escuta na porta local, e a conexão é recusada. */
    @Test
    void conexaoRecusadaDevolveComparacaoSemVersaoRemota() throws Exception {
        int portaFechada;
        try (ServerSocket reserva = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            portaFechada = reserva.getLocalPort();
        }
        String url = "http://127.0.0.1:" + portaFechada + "/versao";
        VersaoOfflineService semSimulacao = novoServico(url, Duration.ofSeconds(2), Duration.ofSeconds(2));

        VersaoOfflineOutput status = semSimulacao.verificarVersao();

        assertThat(status.getVersaoAplicacaoRemota()).isNull();
        assertThat(status.getVersaoDbRemota()).isNull();
        assertThat(status.getDataDbRemota()).isNull();
        assertThat(status.isAplicacaoAtualizada()).isFalse();
        assertThat(status.isDbAtualizada()).isFalse();
        assertThat(status.isDataDbAtualizada()).isFalse();
        assertThat(registros.list).filteredOn(e -> e.getLevel() == Level.WARN).hasSize(1);
    }

    private VersaoOfflineService novoServico(String url, Duration tempoConexao, Duration tempoLeitura) {
        VersaoOfflineService novo = new VersaoOfflineService(versaoBaseDadosService, tempoConexao, tempoLeitura);
        ReflectionTestUtils.setField(novo, "urlVersaoRemota", url);
        ReflectionTestUtils.setField(novo, "versaoAplicacaoLocal", "1.7.2-c7512f9a");
        return novo;
    }

    private static Logger registrador() {
        return (Logger) LoggerFactory.getLogger(VersaoOfflineService.class);
    }
}
