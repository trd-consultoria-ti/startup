/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.domain.service.calculotributo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.gov.serpro.rtc.api.model.input.AliquotasNominaisInput;
import br.gov.serpro.rtc.api.model.input.ItemOperacaoInput;
import br.gov.serpro.rtc.api.model.output.CbsIbsOutput;
import br.gov.serpro.rtc.domain.model.dto.ClassificacaoTributariaCalculoDTO;
import br.gov.serpro.rtc.domain.model.dto.TratamentoClassificacaoDTO;
import br.gov.serpro.rtc.domain.model.entity.SituacaoTributaria;
import br.gov.serpro.rtc.domain.model.entity.TratamentoTributario;
import br.gov.serpro.rtc.domain.model.enumeration.TributoEnum;
import br.gov.serpro.rtc.domain.service.AliquotaAdValoremServicoService;
import br.gov.serpro.rtc.domain.service.AliquotaPadraoService;
import br.gov.serpro.rtc.domain.service.AliquotaReferenciaService;
import br.gov.serpro.rtc.domain.service.ClassificacaoTributariaService;
import br.gov.serpro.rtc.domain.service.PercentualReducaoService;
import br.gov.serpro.rtc.domain.service.SituacaoTributariaService;
import br.gov.serpro.rtc.domain.service.TratamentoTributarioService;

/**
 * Diferimento parcial em {@link CalculoCbsIbsService}. O banco só tem diferimento de 100%, em que
 * o valor devido é zero de qualquer forma; por isso o diferimento parcial é conferido aqui, com um
 * tratamento tributário montado no teste e o avaliador de expressões real.
 * <p>
 * Regra: {@code vDif = arred2(vBC × alíquota publicada ÷ 100 × pDif ÷ 100)} e o valor devido é
 * {@code arred2(vBC × alíquota publicada ÷ 100) − vDif}, com o diferimento descontado uma única
 * vez.
 */
@DisplayName("Diferimento parcial no cálculo de CBS e IBS")
class Teste_CalculoCbsIbsService_Diferimento {

    private static final LocalDate DATA = LocalDate.of(2027, 3, 15);

    private TratamentoTributarioService tratamentoService;
    private CalculoCbsIbsService service;

    @BeforeEach
    void setup() throws Exception {
        final var avaliador = new AvaliadorExpressaoAritmetica();
        final var postConstruct = AvaliadorExpressaoAritmetica.class.getDeclaredMethod("postConstruct");
        postConstruct.setAccessible(true);
        postConstruct.invoke(avaliador);

        tratamentoService = mock(TratamentoTributarioService.class);
        final var classificacaoService = mock(ClassificacaoTributariaService.class);
        final var situacaoService = mock(SituacaoTributariaService.class);

        when(classificacaoService.buscarClassificacaoTributariaCalculo(anyLong()))
                .thenReturn(new ClassificacaoTributariaCalculoDTO(1L, "510001", "Padrão", false, false, false, false, false));
        final var situacao = new SituacaoTributaria();
        situacao.setInGrupoIbsCbs(true);
        when(situacaoService.consultarSituacaoTributariaPorCodigo(anyString(), anyLong(), any())).thenReturn(situacao);

        service = new CalculoCbsIbsService(
                mock(PercentualReducaoService.class),
                mock(AliquotaReferenciaService.class),
                mock(AliquotaPadraoService.class),
                mock(AliquotaAdValoremServicoService.class),
                avaliador,
                tratamentoService,
                classificacaoService,
                situacaoService);
    }

    @Test
    @DisplayName("Diferimento de 50%: base 1.000,00 e alíquota de 1% — diferido 5,00 e devido 5,00")
    void diferimentoDe50PorCento_descontadoUmaVez() {
        final CbsIbsOutput cbs = calcular("0.50", "1000.00", "1");

        assertThat(cbs.getBaseCalculo()).isEqualByComparingTo("1000.00");
        assertThat(cbs.getAliquota()).isEqualByComparingTo("1");
        assertThat(cbs.getGrupoDiferimento().getPDif()).isEqualByComparingTo("50");
        assertThat(cbs.getGrupoDiferimento().getVDif()).isEqualByComparingTo("5.00");
        assertThat(cbs.getTributoDevido()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("Diferimento de 100%: o devido é zero e o diferido é o tributo calculado")
    void diferimentoDe100PorCento_devidoZero() {
        final CbsIbsOutput cbs = calcular("1.00", "1234.57", "8.8123");

        // 1.234,57 × 8,8123% = 108,79401…
        assertThat(cbs.getGrupoDiferimento().getPDif()).isEqualByComparingTo("100");
        assertThat(cbs.getGrupoDiferimento().getVDif()).isEqualByComparingTo("108.79");
        assertThat(cbs.getTributoDevido()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Diferimento parcial: vDif é a conta exata arredondada uma vez, e devido + vDif fecha com o valor")
    void diferimentoParcial_vDifArredondadoUmaVez() {
        // 2,90 × 1% = 0,029 → valor 0,03; vDif = arred2(0,029 × 50%) = arred2(0,0145) = 0,01
        // (o diferimento não é a metade do valor já arredondado, que daria 0,015 → 0,02)
        final CbsIbsOutput cbs = calcular("0.50", "2.90", "1");

        assertThat(cbs.getGrupoDiferimento().getVDif()).isEqualByComparingTo("0.01");
        assertThat(cbs.getTributoDevido()).isEqualByComparingTo("0.02");
        assertThat(cbs.getTributoDevido().add(cbs.getGrupoDiferimento().getVDif())).isEqualByComparingTo("0.03");
        assertThat(cbs.getTributoDevido().scale()).isEqualTo(2);
        assertThat(cbs.getGrupoDiferimento().getVDif().scale()).isEqualTo(2);
    }

    private CbsIbsOutput calcular(String fracaoDiferimento, String base, String aliquotaCbs) {
        final var tratamento = new TratamentoTributario();
        tratamento.setExpressaoAliquota("aliquota");
        tratamento.setExpressaoBaseCalculo("baseCalculoInformada-impostoSeletivoInformado+impostoSeletivoCalculado");
        tratamento.setExpressaoAliquotaEfetiva("aliquota*(1-percentualReducao)*(1-pRedutorCompraGov/100)");
        tratamento.setExpressaoTributoCalculado("baseCalculo*aliquotaEfetiva");
        tratamento.setExpressaoTributoDevido("tributoCalculado");
        tratamento.setExpressaoPercentualDiferimento(fracaoDiferimento);
        tratamento.setExpressaoValorDiferimento("tributoCalculado*" + fracaoDiferimento);
        when(tratamentoService.buscar(anyLong())).thenReturn(tratamento);

        final var aliquotas = new AliquotasNominaisInput();
        aliquotas.setCbs(new BigDecimal(aliquotaCbs));
        final var item = new ItemOperacaoInput();
        item.setNumero(1);
        item.setCst("510");
        item.setCClassTrib("510001");
        item.setBaseCalculo(new BigDecimal(base));
        item.setAliquotasNominais(aliquotas);

        return service.calcularCbsIbs(TributoEnum.CBS, null, null, item,
                new TratamentoClassificacaoDTO(20L, 1L, false, false), BigDecimal.ZERO, false, DATA, null,
                BigDecimal.ZERO);
    }
}
