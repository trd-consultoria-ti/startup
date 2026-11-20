package br.gov.serpro.rtc.api.model.roc;

import static br.gov.serpro.rtc.api.model.input.calculadora.enumeration.TipoEnteGovernamental.COMITE_GESTOR_IBS;
import static br.gov.serpro.rtc.api.model.input.calculadora.enumeration.TipoEnteGovernamental.CONSORCIO_PUBLICO;
import static br.gov.serpro.rtc.api.model.input.calculadora.enumeration.TipoEnteGovernamental.DISTRITO_FEDERAL;
import static br.gov.serpro.rtc.api.model.input.calculadora.enumeration.TipoEnteGovernamental.ESTADO;
import static br.gov.serpro.rtc.api.model.input.calculadora.enumeration.TipoEnteGovernamental.MUNICIPIO;
import static br.gov.serpro.rtc.api.model.input.calculadora.enumeration.TipoEnteGovernamental.UNIAO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class TributacaoCompraGovernamentalDomainTest {

	/** Base de cálculo do domínio de teste: as alíquotas de 20%, 10% e 5% valem 200, 100 e 50. */
	private static final BigDecimal VBC = new BigDecimal("1000.00");

	@Test
	void compraDaUniao_valorPelaAliquotaPublicada_naoPelaSomaDoDetalhe() {
		// T5: base 12,00; CBS 8,8% (1,06), IBS 0,05% + 0,05% (0,01 + 0,01). O detalhe soma 1,08;
		// o grupo principal é 12,00 × 8,90% = 1,068 → 1,07.
		var detalhe = TributacaoCompraGovernamentalDomain.builder()
				.pAliqCBS(new BigDecimal("8.8000")).vTribCBS(new BigDecimal("1.06"))
				.pAliqIBSUF(new BigDecimal("0.0500")).vTribIBSUF(new BigDecimal("0.01"))
				.pAliqIBSMun(new BigDecimal("0.0500")).vTribIBSMun(new BigDecimal("0.01"))
				.build();

		var resultado = detalhe.getValoresEfetivos(UNIAO, ZERO, LocalDate.of(2027, 3, 15), new BigDecimal("12.00"));

		assertResultado(resultado, new BigDecimal("8.90"), new BigDecimal("1.07"), ZERO, ZERO, ZERO, ZERO);
		assertThat(resultado.getVTribCBS().scale()).isEqualTo(2);
	}

	@Test
	void transferenciaDaCbs_quemCedeArredonda_quemRecebeFechaPelaDiferenca() {
		// CBS 1,2345% com transferência de 10%: fica 1,11105 → 1,1110 (meio para o par). A parte
		// transferida, 0,12345, arredondada por conta própria daria 0,1234 e a soma cairia para
		// 1,2344; pela diferença, quem recebe fica com 0,1235 e a soma das alíquotas se preserva.
		var detalhe = TributacaoCompraGovernamentalDomain.builder()
				.pAliqCBS(new BigDecimal("1.2345"))
				.pAliqIBSUF(ZERO)
				.pAliqIBSMun(ZERO)
				.build();

		var estado = detalhe.getValoresEfetivos(ESTADO, new BigDecimal("10"), LocalDate.of(2029, 1, 1), VBC);
		assertResultado(estado, new BigDecimal("1.1110"), new BigDecimal("11.11"), new BigDecimal("0.1235"),
				new BigDecimal("1.24"), ZERO, ZERO);
		assertThat(somarPercentuais(estado)).isEqualByComparingTo("1.2345");

		var municipio = detalhe.getValoresEfetivos(MUNICIPIO, new BigDecimal("10"), LocalDate.of(2029, 1, 1), VBC);
		assertResultado(municipio, new BigDecimal("1.1110"), new BigDecimal("11.11"), ZERO, ZERO,
				new BigDecimal("0.1235"), new BigDecimal("1.24"));
		assertThat(somarPercentuais(municipio)).isEqualByComparingTo("1.2345");
	}

	@Test
	void transferenciaDaCbs_estadoRecebeTambemOIbsDoMunicipio() {
		// detalhe com redução de 60%: CBS 3,5249; IBS UF 2,9333; IBS Mun 0,4938; base 1.234,57
		var detalhe = TributacaoCompraGovernamentalDomain.builder()
				.pAliqCBS(new BigDecimal("3.5249"))
				.pAliqIBSUF(new BigDecimal("2.9333"))
				.pAliqIBSMun(new BigDecimal("0.4938"))
				.build();

		var resultado = detalhe.getValoresEfetivos(ESTADO, new BigDecimal("10"), LocalDate.of(2029, 1, 1),
				new BigDecimal("1234.57"));

		// CBS: 3,5249 × 0,9 = 3,17241 → 3,1724; IBS UF: 6,9520 − 3,1724 = 3,7796
		assertResultado(resultado, new BigDecimal("3.1724"), new BigDecimal("39.17"), new BigDecimal("3.7796"),
				new BigDecimal("46.66"), ZERO, ZERO);
		assertThat(somarPercentuais(resultado)).isEqualByComparingTo(somarPercentuais(detalhe));
	}

	@Test
	void deveRetornarNuloQuandoTipoEnteForNulo() {
		var domain = criarDomainBase();

		var resultado = domain.getValoresEfetivos(null, new BigDecimal("25"), LocalDate.of(2027, 1, 1), VBC);

		assertThat(resultado).isNull();
	}

	@Test
	void deveLancarExcecaoQuandoDataFatoGeradorForNula() {
		var domain = criarDomainBase();

		assertThatNullPointerException()
				.isThrownBy(() -> domain.getValoresEfetivos(ESTADO, new BigDecimal("25"), null, VBC))
				.withMessage("Data do fato gerador obrigatória");
	}

	@Test
	void deveLancarExcecaoQuandoPercentualTransferenciaForNulo() {
		var domain = criarDomainBase();

		assertThatNullPointerException()
				.isThrownBy(() -> domain.getValoresEfetivos(ESTADO, null, LocalDate.of(2027, 1, 1), VBC))
				.withMessage("Percentual de transferência da CBS obrigatória");
	}

	@Test
	void deveRetornarMesmaInstanciaAntesDe2027() {
		var domain = criarDomainBase();

		var resultado = domain.getValoresEfetivos(ESTADO, new BigDecimal("25"), LocalDate.of(2026, 12, 31), VBC);

		assertThat(resultado).isSameAs(domain);
	}

	@ParameterizedTest(name = "União - ano={0}, transferencia={1}%")
	@MethodSource("cenariosUniaoPorAno")
	void deveConcentrarTudoNaCbsQuandoUniaoCompraAPartirDe2027(int ano, BigDecimal pTransferenciaCBS,
			BigDecimal esperadoPAliqCBS, BigDecimal esperadoVTribCBS, BigDecimal esperadoPAliqIBSUF,
			BigDecimal esperadoVTribIBSUF, BigDecimal esperadoPAliqIBSMun, BigDecimal esperadoVTribIBSMun) {
		var domain = criarDomainBase();

		var resultado = domain.getValoresEfetivos(UNIAO, pTransferenciaCBS, LocalDate.of(ano, 1, 1), VBC);

		assertSomasConservadas(domain, resultado);

		assertResultado(resultado, esperadoPAliqCBS, esperadoVTribCBS, esperadoPAliqIBSUF, esperadoVTribIBSUF,
				esperadoPAliqIBSMun, esperadoVTribIBSMun);
	}

	@ParameterizedTest(name = "Estado - ano={0}, transferencia={1}%")
	@MethodSource("cenariosEstadoPorAno")
	void deveTransferirParaUfQuandoEstadoCompraAPartirDe2027(int ano, BigDecimal pTransferenciaCBS,
			BigDecimal esperadoPAliqCBS, BigDecimal esperadoVTribCBS, BigDecimal esperadoPAliqIBSUF,
			BigDecimal esperadoVTribIBSUF, BigDecimal esperadoPAliqIBSMun, BigDecimal esperadoVTribIBSMun) {
		var domain = criarDomainBase();

		var resultado = domain.getValoresEfetivos(ESTADO, pTransferenciaCBS, LocalDate.of(ano, 1, 1), VBC);

		assertSomasConservadas(domain, resultado);

		assertResultado(resultado, esperadoPAliqCBS, esperadoVTribCBS, esperadoPAliqIBSUF, esperadoVTribIBSUF,
				esperadoPAliqIBSMun, esperadoVTribIBSMun);
	}

	@ParameterizedTest(name = "Município - ano={0}, transferencia={1}%")
	@MethodSource("cenariosMunicipioPorAno")
	void deveTransferirParaMunicipioQuandoMunicipioCompraAPartirDe2027(int ano, BigDecimal pTransferenciaCBS,
			BigDecimal esperadoPAliqCBS, BigDecimal esperadoVTribCBS, BigDecimal esperadoPAliqIBSUF,
			BigDecimal esperadoVTribIBSUF, BigDecimal esperadoPAliqIBSMun, BigDecimal esperadoVTribIBSMun) {
		var domain = criarDomainBase();

		var resultado = domain.getValoresEfetivos(MUNICIPIO, pTransferenciaCBS, LocalDate.of(ano, 1, 1), VBC);

		assertSomasConservadas(domain, resultado);

		assertResultado(resultado, esperadoPAliqCBS, esperadoVTribCBS, esperadoPAliqIBSUF, esperadoVTribIBSUF,
				esperadoPAliqIBSMun, esperadoVTribIBSMun);
	}

	@ParameterizedTest(name = "Distrito Federal - ano={0}, transferencia={1}%")
	@MethodSource("cenariosEstadoPorAno")
	void deveTransferirParaMunicipioQuandoDistritoFederalCompraAPartirDe2027(int ano, BigDecimal pTransferenciaCBS,
			BigDecimal esperadoPAliqCBS, BigDecimal esperadoVTribCBS, BigDecimal esperadoPAliqIBSUF,
			BigDecimal esperadoVTribIBSUF, BigDecimal esperadoPAliqIBSMun, BigDecimal esperadoVTribIBSMun) {
		var domain = criarDomainBase();

		var resultado = domain.getValoresEfetivos(DISTRITO_FEDERAL, pTransferenciaCBS, LocalDate.of(ano, 1, 1), VBC);

		assertSomasConservadas(domain, resultado);

		assertResultado(resultado, esperadoPAliqCBS, esperadoVTribCBS, esperadoPAliqIBSUF, esperadoVTribIBSUF,
				esperadoPAliqIBSMun, esperadoVTribIBSMun);
	}
	
	@ParameterizedTest(name = "Consórcio Público - ano={0}, transferencia={1}%")
	@MethodSource("cenariosMunicipioPorAno")
	void deveTransferirParaMunicipioQuandoConsorcioPublicoCompraAPartirDe2027(int ano, BigDecimal pTransferenciaCBS,
			BigDecimal esperadoPAliqCBS, BigDecimal esperadoVTribCBS, BigDecimal esperadoPAliqIBSUF,
			BigDecimal esperadoVTribIBSUF, BigDecimal esperadoPAliqIBSMun, BigDecimal esperadoVTribIBSMun) {
		var domain = criarDomainBase();

		var resultado = domain.getValoresEfetivos(CONSORCIO_PUBLICO, pTransferenciaCBS, LocalDate.of(ano, 1, 1), VBC);

		assertSomasConservadas(domain, resultado);

		assertResultado(resultado, esperadoPAliqCBS, esperadoVTribCBS, esperadoPAliqIBSUF, esperadoVTribIBSUF,
				esperadoPAliqIBSMun, esperadoVTribIBSMun);
	}
	
	@ParameterizedTest(name = "Comitê Gestor IBS - ano={0}, transferencia={1}%")
	@MethodSource("cenariosMunicipioPorAno")
	void deveTransferirParaMunicipioQuandoComiteGestorIbsCompraAPartirDe2027(int ano, BigDecimal pTransferenciaCBS,
			BigDecimal esperadoPAliqCBS, BigDecimal esperadoVTribCBS, BigDecimal esperadoPAliqIBSUF,
			BigDecimal esperadoVTribIBSUF, BigDecimal esperadoPAliqIBSMun, BigDecimal esperadoVTribIBSMun) {
		var domain = criarDomainBase();

		var resultado = domain.getValoresEfetivos(COMITE_GESTOR_IBS, pTransferenciaCBS, LocalDate.of(ano, 1, 1), VBC);

		assertSomasConservadas(domain, resultado);

		assertResultado(resultado, esperadoPAliqCBS, esperadoVTribCBS, esperadoPAliqIBSUF, esperadoVTribIBSUF,
				esperadoPAliqIBSMun, esperadoVTribIBSMun);
	}

	private static Stream<Arguments> cenariosUniaoPorAno() {
		return Stream.of(
				Arguments.of(2027, new BigDecimal("0"), new BigDecimal("35.00"), new BigDecimal("350.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2028, new BigDecimal("0"), new BigDecimal("35.00"), new BigDecimal("350.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2029, new BigDecimal("10"), new BigDecimal("35.00"), new BigDecimal("350.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2030, new BigDecimal("20"), new BigDecimal("35.00"), new BigDecimal("350.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2031, new BigDecimal("30"), new BigDecimal("35.00"), new BigDecimal("350.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2032, new BigDecimal("40"), new BigDecimal("35.00"), new BigDecimal("350.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2033, new BigDecimal("100"), new BigDecimal("35.00"), new BigDecimal("350.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")));
	}

	private static Stream<Arguments> cenariosEstadoPorAno() {
		return Stream.of(
				Arguments.of(2027, new BigDecimal("0"), new BigDecimal("20.00"), new BigDecimal("200.00"), new BigDecimal("15.00"), new BigDecimal("150.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2028, new BigDecimal("0"), new BigDecimal("20.00"), new BigDecimal("200.00"), new BigDecimal("15.00"), new BigDecimal("150.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2029, new BigDecimal("10"), new BigDecimal("18.00"), new BigDecimal("180.00"), new BigDecimal("17.00"), new BigDecimal("170.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2030, new BigDecimal("20"), new BigDecimal("16.00"), new BigDecimal("160.00"), new BigDecimal("19.00"), new BigDecimal("190.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2031, new BigDecimal("30"), new BigDecimal("14.00"), new BigDecimal("140.00"), new BigDecimal("21.00"), new BigDecimal("210.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2032, new BigDecimal("40"), new BigDecimal("12.00"), new BigDecimal("120.00"), new BigDecimal("23.00"), new BigDecimal("230.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
				Arguments.of(2033, new BigDecimal("100"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("35.00"), new BigDecimal("350.00"), new BigDecimal("0.00"), new BigDecimal("0.00")));
	}

	private static Stream<Arguments> cenariosMunicipioPorAno() {
		return Stream.of(
				Arguments.of(2027, new BigDecimal("0"), new BigDecimal("20.00"), new BigDecimal("200.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("15.00"), new BigDecimal("150.00")),
				Arguments.of(2028, new BigDecimal("0"), new BigDecimal("20.00"), new BigDecimal("200.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("15.00"), new BigDecimal("150.00")),
				Arguments.of(2029, new BigDecimal("10"), new BigDecimal("18.00"), new BigDecimal("180.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("17.00"), new BigDecimal("170.00")),
				Arguments.of(2030, new BigDecimal("20"), new BigDecimal("16.00"), new BigDecimal("160.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("19.00"), new BigDecimal("190.00")),
				Arguments.of(2031, new BigDecimal("30"), new BigDecimal("14.00"), new BigDecimal("140.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("21.00"), new BigDecimal("210.00")),
				Arguments.of(2032, new BigDecimal("40"), new BigDecimal("12.00"), new BigDecimal("120.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("23.00"), new BigDecimal("230.00")),
				Arguments.of(2033, new BigDecimal("100"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("35.00"), new BigDecimal("350.00")));
	}

	private void assertResultado(TributacaoCompraGovernamentalDomain resultado, BigDecimal esperadoPAliqCBS,
			BigDecimal esperadoVTribCBS, BigDecimal esperadoPAliqIBSUF, BigDecimal esperadoVTribIBSUF,
			BigDecimal esperadoPAliqIBSMun, BigDecimal esperadoVTribIBSMun) {
		assertThat(resultado.getPAliqCBS()).isEqualByComparingTo(esperadoPAliqCBS);
		assertThat(resultado.getVTribCBS()).isEqualByComparingTo(esperadoVTribCBS);
		assertThat(resultado.getPAliqIBSUF()).isEqualByComparingTo(esperadoPAliqIBSUF);
		assertThat(resultado.getVTribIBSUF()).isEqualByComparingTo(esperadoVTribIBSUF);
		assertThat(resultado.getPAliqIBSMun()).isEqualByComparingTo(esperadoPAliqIBSMun);
		assertThat(resultado.getVTribIBSMun()).isEqualByComparingTo(esperadoVTribIBSMun);
	}

	private void assertSomasConservadas(TributacaoCompraGovernamentalDomain original,
			TributacaoCompraGovernamentalDomain resultado) {
		assertThat(somarPercentuais(resultado)).isEqualByComparingTo(somarPercentuais(original));
		assertThat(somarValores(resultado)).isEqualByComparingTo(somarValores(original));
	}

	private BigDecimal somarPercentuais(TributacaoCompraGovernamentalDomain domain) {
		return valorOuZero(domain.getPAliqCBS()).add(valorOuZero(domain.getPAliqIBSUF()))
				.add(valorOuZero(domain.getPAliqIBSMun()));
	}

	private BigDecimal somarValores(TributacaoCompraGovernamentalDomain domain) {
		return valorOuZero(domain.getVTribCBS()).add(valorOuZero(domain.getVTribIBSUF()))
				.add(valorOuZero(domain.getVTribIBSMun()));
	}

	private BigDecimal valorOuZero(BigDecimal valor) {
		return valor == null ? ZERO : valor;
	}

	private TributacaoCompraGovernamentalDomain criarDomainBase() {
		return TributacaoCompraGovernamentalDomain.builder()
				.pAliqCBS(new BigDecimal("20.00")).vTribCBS(new BigDecimal("200.00"))
				.pAliqIBSUF(new BigDecimal("10.00")).vTribIBSUF(new BigDecimal("100.00"))
				.pAliqIBSMun(new BigDecimal("5.00")).vTribIBSMun(new BigDecimal("50.00"))
				.build();
	}
}