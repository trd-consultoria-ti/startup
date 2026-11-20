package br.gov.serpro.rtc.api.versao.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import br.gov.serpro.rtc.api.versao.dto.VersaoDomain;
import br.gov.serpro.rtc.api.versao.dto.VersaoOfflineDomain;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável por comparar as versões locais e remotas da aplicação e
 * da base de dados quando a API opera em modo offline.
 *
 * <p>A versão LOCAL vem de {@link VersaoService} (a mesma montagem do endpoint
 * {@code /versao}); a REMOTA vem do endpoint {@code /versao} da instalação
 * online apontada por {@code versao.url} — mesmo contrato
 * ({@link VersaoDomain}), o que permite apontar para qualquer nuvem/URL que
 * publique essa API. A consulta remota usa os timeouts
 * {@code versao.connect-timeout}/{@code versao.read-timeout} para não prender
 * requisições quando a rede está indisponível.
 */
@Slf4j
@Service
@Profile("offline")
public class VersaoOfflineService {

    private static final int TENTATIVAS = 3;

    @Value("${versao.url}")
    private String urlVersaoRemota;

    /** Espera entre uma tentativa de consulta e a seguinte, na inicialização. */
    private Duration intervaloEntreTentativas = Duration.ofSeconds(2);

    private final VersaoService versaoService;
    private final RestTemplate restTemplate;

    public VersaoOfflineService(VersaoService versaoService,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${versao.connect-timeout:5s}") Duration connectTimeout,
            @Value("${versao.read-timeout:10s}") Duration readTimeout) {
        this.versaoService = versaoService;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .build();
    }

    /**
     * Compara as versões na subida, com até {@value #TENTATIVAS} tentativas
     * ({@link RetryTemplate} do Spring Framework). Uma
     * {@link IllegalArgumentException} não é repetida. Esgotadas as tentativas
     * fica só o aviso no log: a subida do modo offline não depende da
     * instalação online.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verificarVersaoNaInicializacao() {
        RetryPolicy politica = RetryPolicy.builder()
                .maxRetries(TENTATIVAS - 1L) // maxRetries conta só as repetições, sem a 1ª tentativa
                .delay(intervaloEntreTentativas)
                .excludes(IllegalArgumentException.class)
                .build();
        try {
            // Sem tolerância a falha dentro do bloco: a exceção da consulta remota é o gatilho da retentativa.
            new RetryTemplate(politica).invoke(() -> exibirStatusVersao(
                    VersaoOfflineDomain.de(versaoService.getVersao(), consultarVersaoRemota())));
        } catch (RuntimeException e) {
            log.warn("Não foi possível verificar versão remota após {} tentativas: {}", TENTATIVAS, e.getMessage());
        }
    }

    /**
     * Compara a versão local com a remota. Quando a consulta remota falha, a
     * comparação é devolvida com os campos remotos nulos e os indicadores
     * falsos, em vez de propagar o erro — o modo offline não pode depender da
     * disponibilidade da instalação online.
     */
    public VersaoOfflineDomain verificarVersao() {
        VersaoDomain remota;
        try {
            remota = consultarVersaoRemota();
        } catch (RestClientException e) {
            log.warn("Consulta da versão remota indisponível em [{}]: {}", urlVersaoRemota, e.getMessage());
            remota = null;
        }
        return VersaoOfflineDomain.de(versaoService.getVersao(), remota);
    }

    private VersaoDomain consultarVersaoRemota() {
        return restTemplate.getForObject(urlVersaoRemota, VersaoDomain.class);
    }

    private void exibirStatusVersao(VersaoOfflineDomain status) {
        List<String> atualizados = new ArrayList<>();
        List<String> desatualizados = new ArrayList<>();

        // Aplicação
        if (status.aplicacaoAtualizada()) {
            atualizados.add("Aplicação (LOCAL):  " + status.versaoAplicacaoLocal());
            atualizados.add("Aplicação (ONLINE): " + status.versaoAplicacaoRemota());
        } else {
            desatualizados.add("Aplicação (LOCAL):  " + status.versaoAplicacaoLocal());
            desatualizados.add("Aplicação (ONLINE): " + status.versaoAplicacaoRemota());
        }

        // Banco de Dados
        if (status.dbAtualizada()) {
            atualizados.add("Banco de Dados (LOCAL):  " + status.versaoDbLocal());
            atualizados.add("Banco de Dados (ONLINE): " + status.versaoDbRemota());
        } else {
            desatualizados.add("Banco de Dados (LOCAL):  " + status.versaoDbLocal());
            desatualizados.add("Banco de Dados (ONLINE): " + status.versaoDbRemota());
        }

        // Data do Banco
        if (status.dataDbAtualizada()) {
            atualizados.add("Data do Banco (LOCAL):  " + status.dataDbLocal());
            atualizados.add("Data do Banco (ONLINE): " + status.dataDbRemota());
        } else {
            desatualizados.add("Data do Banco (LOCAL):  " + status.dataDbLocal());
            desatualizados.add("Data do Banco (ONLINE): " + status.dataDbRemota());
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
