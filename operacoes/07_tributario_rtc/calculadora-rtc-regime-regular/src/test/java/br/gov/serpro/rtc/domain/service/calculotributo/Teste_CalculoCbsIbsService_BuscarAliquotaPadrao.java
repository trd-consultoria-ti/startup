/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.domain.service.calculotributo;

import static br.gov.serpro.rtc.domain.model.enumeration.FormaAplicacaoEnum.ACRESCIMO;
import static br.gov.serpro.rtc.domain.model.enumeration.FormaAplicacaoEnum.DECRESCIMO;
import static br.gov.serpro.rtc.domain.model.enumeration.FormaAplicacaoEnum.SUBSTITUICAO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.serpro.rtc.domain.model.dto.AliquotaResultadoDTO;
import br.gov.serpro.rtc.domain.service.AliquotaAdValoremServicoService;
import br.gov.serpro.rtc.domain.service.AliquotaPadraoService;
import br.gov.serpro.rtc.domain.service.AliquotaReferenciaService;
import br.gov.serpro.rtc.domain.service.ClassificacaoTributariaService;
import br.gov.serpro.rtc.domain.service.PercentualReducaoService;
import br.gov.serpro.rtc.domain.service.TratamentoTributarioService;

/**
 * Testes unitários para o método protected buscarAliquotaPadrao de CalculoCbsIbsService
 */
@ExtendWith(MockitoExtension.class)
class Teste_CalculoCbsIbsService_BuscarAliquotaPadrao {

    @Mock
    private PercentualReducaoService percentualReducaoService;
    
    @Mock
    private AliquotaReferenciaService aliquotaReferenciaService;
    
    @Mock
    private AliquotaPadraoService aliquotaPadraoService;
    
    @Mock
    private AliquotaAdValoremServicoService aliquotaAdValoremServicoService;
    
    @Mock
    private AvaliadorExpressaoAritmetica avaliador;
    
    @Mock
    private TratamentoTributarioService tratamentoService;
    
    @Mock
    private ClassificacaoTributariaService classificacaoTributariaService;

    @InjectMocks
    private CalculoCbsIbsService calculoCbsIbsService;

    private Long idTributo;
    private Long codigoUf;
    private Long codigoMunicipio;
    private LocalDate data;

    @BeforeEach
    void setup() {
        idTributo = 1L; // CBS
        codigoUf = 35L; // São Paulo
        codigoMunicipio = 3550308L; // São Paulo/SP
        data = LocalDate.of(2026, 1, 1);
    }

    @Test
    void teste_BuscarAliquotaPadrao_SemValorPadrao() {
        // Arrange
        BigDecimal valorReferencia = new BigDecimal("26.50");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, null, null);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.265");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_ComAcrescimo() {
        // Arrange - valorAplicavel = 26.50 + 1.50 = 28.00
        BigDecimal valorReferencia = new BigDecimal("26.50");
        BigDecimal valorPadrao = new BigDecimal("1.50");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, valorPadrao, ACRESCIMO);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.28");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_ComDecrescimo() {
        // Arrange - valorAplicavel = 26.50 - 1.50 = 25.00
        BigDecimal valorReferencia = new BigDecimal("26.50");
        BigDecimal valorPadrao = new BigDecimal("1.50");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, valorPadrao, DECRESCIMO);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.25");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_ComSubstituicao() {
        // Arrange - valorAplicavel = 12.75 (substitui 26.50)
        BigDecimal valorReferencia = new BigDecimal("26.50");
        BigDecimal valorPadrao = new BigDecimal("12.75");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, valorPadrao, SUBSTITUICAO);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.1275");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_ValorZero() {
        // Arrange
        BigDecimal valorReferencia = new BigDecimal("0.00");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, null, null);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_PrecisaoDecimal() {
        // Arrange - 17.345 dividido por 100 = 0.17345000
        BigDecimal valorReferencia = new BigDecimal("17.345");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, null, null);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.17345");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_TributoIBS() {
        // Arrange
        Long idTributoIBS = 2L; // IBS
        BigDecimal valorReferencia = new BigDecimal("25.00");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, null, null);
        
        when(aliquotaPadraoService.buscarAliquota(idTributoIBS, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributoIBS, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.25");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributoIBS, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_UFDiferente() {
        // Arrange
        Long codigoUfRJ = 33L; // Rio de Janeiro
        BigDecimal valorReferencia = new BigDecimal("24.00");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, null, null);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUfRJ, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUfRJ, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.24");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUfRJ, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_DataDiferente() {
        // Arrange
        LocalDate dataFutura = LocalDate.of(2027, 6, 15);
        BigDecimal valorReferencia = new BigDecimal("28.00");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, null, null);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, dataFutura))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, dataFutura);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.28");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, dataFutura);
    }

    @Test
    void teste_BuscarAliquotaPadrao_SubstituicaoPorZero() {
        // Arrange - aliquota padrão substitui a referência por zero
        BigDecimal valorReferencia = new BigDecimal("26.50");
        BigDecimal valorPadrao = new BigDecimal("0.00");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, valorPadrao, SUBSTITUICAO);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_AcrescimoComPrecisao() {
        // Arrange - 15.123 + 2.877 = 18.000
        BigDecimal valorReferencia = new BigDecimal("15.123");
        BigDecimal valorPadrao = new BigDecimal("2.877");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, valorPadrao, ACRESCIMO);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.18");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_DecrescimoComPrecisao() {
        // Arrange - 20.500 - 5.250 = 15.250
        BigDecimal valorReferencia = new BigDecimal("20.500");
        BigDecimal valorPadrao = new BigDecimal("5.250");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, valorPadrao, DECRESCIMO);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertThat(resultado).isEqualByComparingTo("0.1525");
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_VerificaDivisaoPor100() {
        // Arrange - verifica que 100.00 / 100 = 1.0000: a conversão só desloca a vírgula
        BigDecimal valorReferencia = new BigDecimal("100.00");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, null, null);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        BigDecimal resultado = calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert
        assertNotNull(resultado);
        assertEquals(new BigDecimal("1.0000"), resultado);
        verify(aliquotaPadraoService, times(1)).buscarAliquota(idTributo, codigoUf, codigoMunicipio, data);
    }

    @Test
    void teste_BuscarAliquotaPadrao_VerificaChamadaAoService() {
        // Arrange
        BigDecimal valorReferencia = new BigDecimal("26.50");
        AliquotaResultadoDTO aliquotaDTO = new AliquotaResultadoDTO(valorReferencia, null, null);
        
        when(aliquotaPadraoService.buscarAliquota(idTributo, codigoUf, codigoMunicipio, data))
            .thenReturn(aliquotaDTO);

        // Act
        calculoCbsIbsService.buscarAliquotaPadrao(idTributo, codigoUf, codigoMunicipio, data);

        // Assert - verifica que o service foi chamado exatamente uma vez com os parâmetros corretos
        verify(aliquotaPadraoService, times(1)).buscarAliquota(
            idTributo,
            codigoUf,
            codigoMunicipio,
            data
        );
    }
}
