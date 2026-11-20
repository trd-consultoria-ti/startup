package br.gov.serpro.rtc.api.simplesnacional.adapter;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import br.gov.serpro.rtc.api.simplesnacional.collector.ItemCollectorSimples;
import br.gov.serpro.rtc.api.simplesnacional.entity.ClassificacaoTributariaSimples;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;
import br.gov.serpro.rtc.domain.model.enumeration.ModoValidacaoNomenclatura;
import br.gov.serpro.rtc.domain.model.enumeration.SiglasDFeEnum;
import br.gov.serpro.rtc.domain.service.NbsService;
import br.gov.serpro.rtc.domain.service.NcmService;
import br.gov.serpro.rtc.domain.service.ValidacaoNomenclaturaService;
import br.gov.serpro.rtc.domain.service.exception.NbsCompletoNaoInformadoException;
import br.gov.serpro.rtc.domain.service.exception.NbsNaoEncontradaException;
import br.gov.serpro.rtc.domain.service.exception.NbsNaoVinculadaException;
import br.gov.serpro.rtc.domain.service.exception.NcmCompletoNaoInformadoException;
import br.gov.serpro.rtc.domain.service.exception.NcmNaoEncontradaException;
import br.gov.serpro.rtc.domain.service.exception.NcmNbsSimultaneasException;
import br.gov.serpro.rtc.domain.service.exception.NcmNaoVinculadaException;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaIncompativelTipoDfeException;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaNaoPermitidaTipoDfeException;
import br.gov.serpro.rtc.domain.service.exception.NomenclaturaObrigatoriaNaoInformadaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Adapta as validações de nomenclatura ao coletor e contrato de itens do Simples Nacional. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidacaoNomenclaturaAdapter {

    private final NcmService ncmService;
    private final NbsService nbsService;
    private final ValidacaoNomenclaturaService validacaoNomenclaturaService;

    /** Validação estrutural de origem; false indica falha já registrada no item. */
    public boolean validarEstrutural(String ncm, String nbs, LocalDate data, ItemCollectorSimples collector,
            int index) {
        try {
            validarNcmNbs(ncm, nbs, data);
            return true;
        } catch (RuntimeException ex) {
            registrar(ex, ncm, nbs, collector, index);
            return false;
        }
    }

    /** Validação dirigida pela classificação CBS/IBS, depois da resolução do cClassTrib. */
    public void validarPorClassificacao(String ncm, String nbs, LocalDate data, String siglaDfe,
            ClassificacaoTributariaSimples classificacao, boolean estruturaValida, ItemCollectorSimples collector,
            int index) {
        if (!estruturaValida || collector.hasErros() || classificacao == null) {
            return;
        }
        try {
            if (siglaDfe == null) {
                throw new IllegalStateException("Sigla TIPO_DFE ausente para o documento habilitado na data");
            }
            validacaoNomenclaturaService.validar(SiglasDFeEnum.getPorSiglaNormalizada(siglaDfe),
                    classificacao.getId(), classificacao.getCodigo(), ncm, nbs, data, "CBS e IBS",
                    ModoValidacaoNomenclatura.PERMISSIVO);
        } catch (RuntimeException ex) {
            registrar(ex, ncm, nbs, collector, index);
        }
    }

    private void registrar(RuntimeException ex, String ncm, String nbs, ItemCollectorSimples collector, int index) {
        if (!traduzirErroConhecido(ex, ncm, nbs, collector)) {
            log.error("Falha técnica na validação de nomenclatura do item {}", index, ex);
            collector.addFalhaTecnica(ex);
        }
    }

    /** Traduz as dez exceções de validação conhecidas, conservando a mensagem original. */
    private static boolean traduzirErroConhecido(RuntimeException ex, String ncm, String nbs,
            ItemCollectorSimples collector) {
        CodigoValidacaoSimples codigo;
        String campo = null;
        if (ex instanceof NcmNbsSimultaneasException) {
            codigo = CodigoValidacaoSimples.NCM_NBS_SIMULTANEOS;
            campo = "ncm";
        } else if (ex instanceof NcmNaoEncontradaException) {
            codigo = CodigoValidacaoSimples.NCM_NAO_ENCONTRADO;
            campo = "ncm";
        } else if (ex instanceof NbsNaoEncontradaException) {
            codigo = CodigoValidacaoSimples.NBS_NAO_ENCONTRADO;
            campo = "nbs";
        } else if (ex instanceof NcmCompletoNaoInformadoException) {
            codigo = CodigoValidacaoSimples.NCM_INCOMPLETO;
            campo = "ncm";
        } else if (ex instanceof NbsCompletoNaoInformadoException) {
            codigo = CodigoValidacaoSimples.NBS_INCOMPLETO;
            campo = "nbs";
        } else if (ex instanceof NomenclaturaObrigatoriaNaoInformadaException) {
            codigo = CodigoValidacaoSimples.NOMENCLATURA_OBRIGATORIA;
        } else if (ex instanceof NomenclaturaIncompativelTipoDfeException) {
            codigo = CodigoValidacaoSimples.NOMENCLATURA_INCOMPATIVEL;
            campo = !isNullOrEmpty(ncm) ? "ncm" : !isNullOrEmpty(nbs) ? "nbs" : null;
        } else if (ex instanceof NomenclaturaNaoPermitidaTipoDfeException) {
            codigo = CodigoValidacaoSimples.NOMENCLATURA_NAO_PERMITIDA;
        } else if (ex instanceof NcmNaoVinculadaException) {
            codigo = CodigoValidacaoSimples.NCM_NAO_APLICAVEL;
            campo = "ncm";
        } else if (ex instanceof NbsNaoVinculadaException) {
            codigo = CodigoValidacaoSimples.NBS_NAO_APLICAVEL;
            campo = "nbs";
        } else if (ex instanceof FormatoNomenclaturaInvalidoException) {
            codigo = CodigoValidacaoSimples.NOMENCLATURA_FORMATO_INVALIDO;
            campo = ((FormatoNomenclaturaInvalidoException) ex).campo;
        } else {
            return false;
        }
        collector.addErroValidacao(codigo, campo, ex.getMessage());
        return true;
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private void validarNcmNbs(String ncm, String nbs, LocalDate data) {
        // sob demanda da plataforma
        // if (isNullOrEmpty(ncm) && isNullOrEmpty(nbs)) {
        //     throw new NcmNbsNaoInformadasException();
        // }
        if (!isNullOrEmpty(ncm) && !isNullOrEmpty(nbs)) {
            throw new NcmNbsSimultaneasException();
        }
        if (!isNullOrEmpty(ncm) && !ncm.matches("[0-9]{8}")) {
            throw new FormatoNomenclaturaInvalidoException("ncm");
        }
        if (!isNullOrEmpty(nbs) && !nbs.matches("[0-9]{9}")) {
            throw new FormatoNomenclaturaInvalidoException("nbs");
        }
        if (!isNullOrEmpty(ncm) && !ncmService.existeNcm(ncm, data)) {
            throw new NcmNaoEncontradaException(ncm, data);
        }
        if (!isNullOrEmpty(nbs) && !nbsService.existeNbs(nbs, data)) {
            throw new NbsNaoEncontradaException(nbs, data);
        }
    }

    private static final class FormatoNomenclaturaInvalidoException extends RuntimeException {
        private final String campo;

        private FormatoNomenclaturaInvalidoException(String campo) {
            super("O " + campo.toUpperCase() + " deve conter exclusivamente " + ("ncm".equals(campo) ? 8 : 9)
                    + " dígitos ASCII, sem espaços ou símbolos.");
            this.campo = campo;
        }
    }
}
