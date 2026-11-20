package br.gov.serpro.rtc.api.versao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.api.versao.dto.VersaoDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável por montar as informações de versão desta instalação:
 * versão da aplicação, arquivo e versão/descrição/data da base de dados e
 * ambiente.
 *
 * <p>Disponível em todos os perfis: alimenta o endpoint {@code /versao}
 * (visível em qualquer modo) e serve de versão LOCAL para o comparador do modo
 * offline ({@link VersaoOfflineService}). Na subida, registra no log o arquivo
 * e a versão do banco em uso, em todos os perfis.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VersaoService {

    @Value("${info.app.version:unknown}")
    private String versaoAplicacao;

    @Value("${versao.ambiente:desconhecido}")
    private String ambiente;

    @Value("${application.db.filename:unknown}")
    private String arquivoBaseDados;

    private final VersaoBaseDadosService versaoBaseDadosService;

    public VersaoDomain getVersao() {
        final var ultimaVersao = versaoBaseDadosService.getUltimaVersao();
        return VersaoDomain.builder()
                .versaoApp(versaoAplicacao)
                .versaoDb(ultimaVersao.getNumeroVersao())
                .descricaoVersaoDb(ultimaVersao.getDescricao())
                .dataVersaoDb(ultimaVersao.getData().toString())
                .arquivoDb(arquivoBaseDados)
                .ambiente(ambiente)
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    void registrarBancoEmUso() {
        VersaoDomain versao = getVersao();
        log.info(">>> Banco de dados: arquivo [{}], versão [{}] de [{}], ambiente [{}] <<<", versao.arquivoDb(),
                versao.versaoDb(), versao.dataVersaoDb(), versao.ambiente());
    }

}
