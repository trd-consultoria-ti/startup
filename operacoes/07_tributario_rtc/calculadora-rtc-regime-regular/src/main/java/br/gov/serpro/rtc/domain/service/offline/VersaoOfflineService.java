package br.gov.serpro.rtc.domain.service.offline;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import br.gov.serpro.rtc.api.model.output.dadosabertos.VersaoOutput;
import br.gov.serpro.rtc.api.model.output.versaooffline.VersaoOfflineOutput;
import br.gov.serpro.rtc.domain.service.VersaoBaseDadosService;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável por comparar as versões locais e remotas da aplicação e
 * da base de dados quando a API opera em modo offline.
 *
 * <p>A versão REMOTA vem de {@code dados-abertos/versao} da instalação online
 * apontada por {@code versao.url}. A consulta usa os tempos limite
 * {@code versao.connect-timeout} e {@code versao.read-timeout}, para não
 * prender a requisição quando a rede não responde.
 */
@Slf4j
@Service
@Profile("offline")
public class VersaoOfflineService {

    private static final int TENTATIVAS = 3;

    @Value("${versao.url}")
    private String urlVersaoRemota;

    @Value("${info.app.version:unknown}")
    private String versaoAplicacaoLocal;

    private final VersaoBaseDadosService versaoBaseDadosService;
    private final RestTemplate restTemplate;

    public VersaoOfflineService(VersaoBaseDadosService versaoBaseDadosService,
            @Value("${versao.connect-timeout:5s}") Duration connectTimeout,
            @Value("${versao.read-timeout:10s}") Duration readTimeout) {
        this.versaoBaseDadosService = versaoBaseDadosService;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(connectTimeout);
        fabrica.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(fabrica);
    }

    /**
     * Compara as versões na subida, com até {@value #TENTATIVAS} tentativas.
     * Aqui a falha da consulta remota não é absorvida: é ela que aciona a nova
     * tentativa. Esgotadas as tentativas fica só o aviso de
     * {@link #recuperarDeErroDeVersao(Exception)}: a subida do modo offline
     * não depende da instalação online.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Retryable(
        maxAttempts = TENTATIVAS,
        backoff = @Backoff(delay = 2000),
        noRetryFor = {IllegalArgumentException.class}
    )
    public void verificarVersaoNaInicializacao() {
        exibirStatusVersao(new VersaoOfflineOutput(versaoLocal(), consultarVersaoRemota()));
    }

    @Recover
    public void recuperarDeErroDeVersao(Exception e) {
        log.warn("Não foi possível verificar versão remota após {} tentativas: {}", TENTATIVAS, e.getMessage());
    }

    /**
     * Compara a versão local com a remota. Quando a consulta remota falha
     * (conexão recusada, tempo esgotado, erro HTTP ou resposta que não é o JSON
     * esperado), a comparação é devolvida com os campos remotos nulos e os
     * indicadores falsos, em vez de propagar o erro: o modo offline não pode
     * depender da disponibilidade da instalação online.
     *
     * <p>Só {@link RestClientException} é absorvida. Um defeito desta
     * aplicação (por exemplo, uma classe que o Jackson não consegue montar)
     * continua aparecendo como erro, em vez de passar por indisponibilidade.
     */
    public VersaoOfflineOutput verificarVersao() {
        VersaoOutput remota;
        try {
            remota = consultarVersaoRemota();
        } catch (RestClientException e) {
            log.warn("Consulta da versão remota indisponível em [{}]: {}", urlVersaoRemota, e.getMessage());
            remota = null;
        }
        return new VersaoOfflineOutput(versaoLocal(), remota);
    }

    private VersaoOutput versaoLocal() {
        var ultimaVersao = versaoBaseDadosService.getUltimaVersao();

        return VersaoOutput.builder()
                .versaoApp(versaoAplicacaoLocal)
                .descricaoVersaoApp(versaoAplicacaoLocal)
                .versaoDb(ultimaVersao.getNumeroVersao())
                .dataVersaoDb(ultimaVersao.getData().toString())
                .build();
    }

    private VersaoOutput consultarVersaoRemota() {
        return restTemplate.getForObject(urlVersaoRemota, VersaoOutput.class);
    }

    private void exibirStatusVersao(VersaoOfflineOutput status) {
        List<String> atualizados = new ArrayList<>();
        List<String> desatualizados = new ArrayList<>();

        // Aplicação
        if (status.isAplicacaoAtualizada()) {
            atualizados.add("Aplicação (LOCAL):  " + status.getVersaoAplicacaoLocal());
            atualizados.add("Aplicação (ONLINE): " + status.getVersaoAplicacaoRemota());
        } else {
            desatualizados.add("Aplicação (LOCAL):  " + status.getVersaoAplicacaoLocal());
            desatualizados.add("Aplicação (ONLINE): " + status.getVersaoAplicacaoRemota());
        }

        // Banco de Dados
        if (status.isDbAtualizada()) {
            atualizados.add("Banco de Dados (LOCAL):  " + status.getVersaoDbLocal());
            atualizados.add("Banco de Dados (ONLINE): " + status.getVersaoDbRemota());
        } else {
            desatualizados.add("Banco de Dados (LOCAL):  " + status.getVersaoDbLocal());
            desatualizados.add("Banco de Dados (ONLINE): " + status.getVersaoDbRemota());
        }

        // Data do Banco
        if (status.isDataDbAtualizada()) {
            atualizados.add("Data do Banco (LOCAL):  " + status.getDataDbLocal());
            atualizados.add("Data do Banco (ONLINE): " + status.getDataDbRemota());
        } else {
            desatualizados.add("Data do Banco (LOCAL):  " + status.getDataDbLocal());
            desatualizados.add("Data do Banco (ONLINE): " + status.getDataDbRemota());
        }

        if (!atualizados.isEmpty()) {
            log.info("= VERSÃO ATUALIZADA " + "=".repeat(30));
            for (String linha : atualizados) {
                log.info(linha);
            }
        }
        if (!desatualizados.isEmpty()) {
            log.info("= VERSÃO DESATUALIZADA " + "=".repeat(27));
            for (String linha : desatualizados) {
                log.info(linha);
            }
        }
    }
}
