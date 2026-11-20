package br.gov.serpro.rtc.testesunitarios.nomenclatura;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.serpro.rtc.domain.model.enumeration.ModoValidacaoNomenclatura;
import br.gov.serpro.rtc.domain.model.enumeration.SiglasDFeEnum;
import br.gov.serpro.rtc.domain.service.NbsAplicavelService;
import br.gov.serpro.rtc.domain.service.NcmAplicavelService;
import br.gov.serpro.rtc.domain.service.NomenclaturaService;
import br.gov.serpro.rtc.domain.service.ValidacaoNomenclaturaService;
import br.gov.serpro.rtc.domain.service.exception.NbsCompletoNaoInformadoException;
import br.gov.serpro.rtc.domain.service.exception.NcmCompletoNaoInformadoException;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaIncompativelTipoDfeException;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaNaoPermitidaTipoDfeException;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaObrigatoriaNaoInformadaException;

@ExtendWith(MockitoExtension.class)
class Teste_ValidacaoNomenclaturaService {
    private static final LocalDate DATA = LocalDate.of(2026, 1, 1);
    private static final long ID = 7L;
    private static final String CODIGO = "200044";
    private static final String TRIBUTOS = "CBS e IBS";

    @Mock private NomenclaturaService nomenclatura;
    @Mock private NcmAplicavelService ncm;
    @Mock private NbsAplicavelService nbs;
    private ValidacaoNomenclaturaService service;

    @BeforeEach void configurar() { service = new ValidacaoNomenclaturaService(nomenclatura, ncm, nbs); }

    private void resultado(String valor) {
        when(nomenclatura.determinarNomenclatura(anyString(), eq(CODIGO), eq(DATA))).thenReturn(valor);
    }
    private void validar(SiglasDFeEnum dfe, String codigo, String valorNcm, String valorNbs,
            ModoValidacaoNomenclatura modo) {
        service.validar(dfe, ID, codigo, valorNcm, valorNbs, DATA, TRIBUTOS, modo);
    }

    @Test void ncmObrigatorioEValidaContraAnexo() {
        resultado("NCM");
        assertThatCode(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, "87100000", null,
                ModoValidacaoNomenclatura.PERMISSIVO)).doesNotThrowAnyException();
        verify(ncm).criticarNcmContraAnexo("87100000", ID, CODIGO, DATA, TRIBUTOS);
        verifyNoInteractions(nbs);
    }

    @Test void nomenclaturaNcmRejeitaAusenciaOuNbsIncompativel() {
        resultado("NCM");
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, null, null,
                ModoValidacaoNomenclatura.PERMISSIVO)).isInstanceOf(NcmCompletoNaoInformadoException.class);
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, "87100000", "115012000",
                ModoValidacaoNomenclatura.PERMISSIVO)).isInstanceOf(NomenclaturaIncompativelTipoDfeException.class);
    }

    @Test void nbsObrigatorioERejeitaNcmOuCodigoIncompleto() {
        resultado("NBS");
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, null, null,
                ModoValidacaoNomenclatura.PERMISSIVO)).isInstanceOf(NbsCompletoNaoInformadoException.class);
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, "87100000", "115012000",
                ModoValidacaoNomenclatura.PERMISSIVO)).isInstanceOf(NomenclaturaIncompativelTipoDfeException.class);
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, null, "123",
                ModoValidacaoNomenclatura.PERMISSIVO)).isInstanceOf(NbsCompletoNaoInformadoException.class);
    }

    @Test void mistoAceitaUmaDasNomenclaturasEExigeUmaQuandoAmbasVazias() {
        resultado("MISTO");
        assertThatCode(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, "87100000", null,
                ModoValidacaoNomenclatura.PERMISSIVO)).doesNotThrowAnyException();
        assertThatCode(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, null, "115012000",
                ModoValidacaoNomenclatura.PERMISSIVO)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, "", " ",
                ModoValidacaoNomenclatura.PERMISSIVO)).isInstanceOf(NomenclaturaObrigatoriaNaoInformadaException.class);
        verify(ncm).criticarNcmContraAnexo("87100000", ID, CODIGO, DATA, TRIBUTOS);
        verify(nbs).criticarNbsContraAnexo("115012000", ID, CODIGO, DATA, TRIBUTOS);
    }

    @Test void semNoTrioAceitaAusenciaENomenclaturaNaturalMasRejeitaContraria() {
        resultado("SEM");
        assertThatCode(() -> validar(SiglasDFeEnum.NFE, CODIGO, null, null,
                ModoValidacaoNomenclatura.RIGIDO)).doesNotThrowAnyException();
        assertThatCode(() -> validar(SiglasDFeEnum.NFE, CODIGO, "87100000", null,
                ModoValidacaoNomenclatura.RIGIDO)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFE, CODIGO, null, "115012000",
                ModoValidacaoNomenclatura.PERMISSIVO)).isInstanceOf(NomenclaturaIncompativelTipoDfeException.class);
        assertThatCode(() -> validar(SiglasDFeEnum.NFSE, CODIGO, null, "115012000",
                ModoValidacaoNomenclatura.RIGIDO)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFSE, CODIGO, "87100000", null,
                ModoValidacaoNomenclatura.PERMISSIVO)).isInstanceOf(NomenclaturaIncompativelTipoDfeException.class);
    }

    @Test void semForaDoTrioDistingueModoRigidoEPermissivo() {
        resultado("SEM");
        assertThatThrownBy(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, "87100000", null,
                ModoValidacaoNomenclatura.RIGIDO)).isInstanceOf(NomenclaturaNaoPermitidaTipoDfeException.class);
        assertThatCode(() -> validar(SiglasDFeEnum.NFCOM, CODIGO, "87100000", null,
                ModoValidacaoNomenclatura.PERMISSIVO)).doesNotThrowAnyException();
        verify(ncm).validarNcmAplicavel("87100000", ID, CODIGO, DATA, TRIBUTOS);
    }
}
