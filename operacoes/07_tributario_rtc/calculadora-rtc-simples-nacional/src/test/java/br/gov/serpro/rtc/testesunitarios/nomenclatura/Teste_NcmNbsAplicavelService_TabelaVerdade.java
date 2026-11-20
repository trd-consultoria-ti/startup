package br.gov.serpro.rtc.testesunitarios.nomenclatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.gov.serpro.rtc.domain.repository.NbsAplicavelRepository;
import br.gov.serpro.rtc.domain.repository.NcmAplicavelRepository;
import br.gov.serpro.rtc.domain.service.NbsAplicavelService;
import br.gov.serpro.rtc.domain.service.NcmAplicavelService;
import br.gov.serpro.rtc.domain.service.exception.NbsCompletoNaoInformadoException;
import br.gov.serpro.rtc.domain.service.exception.NbsNaoVinculadaException;
import br.gov.serpro.rtc.domain.service.exception.NcmCompletoNaoInformadoException;
import br.gov.serpro.rtc.domain.service.exception.NcmNaoVinculadaException;

class Teste_NcmNbsAplicavelService_TabelaVerdade {
    private static final LocalDate DATA = LocalDate.of(2026, 1, 1);
    private static final long ID = 9L;
    private static final String CODIGO = "200044";
    private static final String TRIBUTOS = "CBS e IBS";

    @Test void tabelaVerdadeNcm() {
        // A/E/S: rejeitar !A ou E&&!S. S implica A; linhas impossíveis não são exercitadas.
        boolean[][] casos = {{false,false,false}, {false,true,false}, {true,false,false},
                {true,false,true}, {true,true,false}, {true,true,true}};
        for (boolean[] c : casos) {
            NcmAplicavelRepository repo = mock(NcmAplicavelRepository.class);
            when(repo.tem(ID, DATA)).thenReturn(true);
            when(repo.temNcmAplicavel("12345678", ID, DATA)).thenReturn(c[0]);
            when(repo.temExcecaoNcmAplicavel("12345678", ID, DATA)).thenReturn(c[1]);
            when(repo.temNcmAplicavelSemExcecao("12345678", ID, DATA)).thenReturn(c[2]);
            NcmAplicavelService service = new NcmAplicavelService(repo);
            boolean rejeita = !c[0] || (c[1] && !c[2]);
            if (rejeita) {
                assertThatThrownBy(() -> service.criticarNcmContraAnexo("12345678", ID, CODIGO, DATA, TRIBUTOS))
                        .as("NCM A/E/S=%s/%s/%s", c[0], c[1], c[2]).isInstanceOf(NcmNaoVinculadaException.class);
            } else {
                service.criticarNcmContraAnexo("12345678", ID, CODIGO, DATA, TRIBUTOS);
            }
        }
    }

    @Test void tabelaVerdadeNbs() {
        boolean[][] casos = {{false,false,false}, {false,true,false}, {true,false,false},
                {true,false,true}, {true,true,false}, {true,true,true}};
        for (boolean[] c : casos) {
            NbsAplicavelRepository repo = mock(NbsAplicavelRepository.class);
            when(repo.tem(ID, DATA)).thenReturn(true);
            when(repo.temNbsAplicavel("123456789", ID, DATA)).thenReturn(c[0]);
            when(repo.temExcecaoNbsAplicavel("123456789", ID, DATA)).thenReturn(c[1]);
            when(repo.temNbsAplicavelSemExcecao("123456789", ID, DATA)).thenReturn(c[2]);
            NbsAplicavelService service = new NbsAplicavelService(repo);
            boolean rejeita = !c[0] || (c[1] && !c[2]);
            if (rejeita) {
                assertThatThrownBy(() -> service.criticarNbsContraAnexo("123456789", ID, CODIGO, DATA, TRIBUTOS))
                        .as("NBS A/E/S=%s/%s/%s", c[0], c[1], c[2]).isInstanceOf(NbsNaoVinculadaException.class);
            } else {
                service.criticarNbsContraAnexo("123456789", ID, CODIGO, DATA, TRIBUTOS);
            }
        }
    }

    @Test void codigoIncompletoComAnexoFalhaSemConsultarVinculoEspecifico() {
        NcmAplicavelRepository ncmRepo = mock(NcmAplicavelRepository.class);
        when(ncmRepo.tem(ID, DATA)).thenReturn(true);
        assertThatThrownBy(() -> new NcmAplicavelService(ncmRepo).validarNcmAplicavel("123", ID, CODIGO, DATA, TRIBUTOS))
                .isInstanceOf(NcmCompletoNaoInformadoException.class);
        verify(ncmRepo, never()).temNcmAplicavel(anyString(), anyLong(), any());

        NbsAplicavelRepository nbsRepo = mock(NbsAplicavelRepository.class);
        when(nbsRepo.tem(ID, DATA)).thenReturn(true);
        assertThatThrownBy(() -> new NbsAplicavelService(nbsRepo).validarNbsAplicavel("123", ID, CODIGO, DATA, TRIBUTOS))
                .isInstanceOf(NbsCompletoNaoInformadoException.class);
        verify(nbsRepo, never()).temNbsAplicavel(anyString(), anyLong(), any());
    }

    @Test void semAnexoAceitaCodigoCompletoSemCriticaEIncompleto() {
        NcmAplicavelRepository ncmRepo = mock(NcmAplicavelRepository.class);
        when(ncmRepo.tem(ID, DATA)).thenReturn(false);
        NcmAplicavelService ncm = new NcmAplicavelService(ncmRepo);
        assertThat(ncm.validarNcmAplicavel("12345678", ID, CODIGO, DATA, TRIBUTOS)).isTrue();
        assertThat(ncm.validarNcmAplicavel("123", ID, CODIGO, DATA, TRIBUTOS)).isTrue();
        verify(ncmRepo, never()).temNcmAplicavel(anyString(), anyLong(), any());
        verify(ncmRepo, never()).temExcecaoNcmAplicavel(anyString(), anyLong(), any());

        NbsAplicavelRepository nbsRepo = mock(NbsAplicavelRepository.class);
        when(nbsRepo.tem(ID, DATA)).thenReturn(false);
        NbsAplicavelService nbs = new NbsAplicavelService(nbsRepo);
        assertThat(nbs.validarNbsAplicavel("123456789", ID, CODIGO, DATA, TRIBUTOS)).isTrue();
        assertThat(nbs.validarNbsAplicavel("123", ID, CODIGO, DATA, TRIBUTOS)).isTrue();
        verify(nbsRepo, never()).temNbsAplicavel(anyString(), anyLong(), any());
        verify(nbsRepo, never()).temExcecaoNbsAplicavel(anyString(), anyLong(), any());
    }
}
