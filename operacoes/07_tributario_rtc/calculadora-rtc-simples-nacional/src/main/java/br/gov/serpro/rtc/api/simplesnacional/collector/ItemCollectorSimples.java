package br.gov.serpro.rtc.api.simplesnacional.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.gov.serpro.rtc.api.simplesnacional.enumeration.EstadoItemSimplesEnum;
import br.gov.serpro.rtc.shared.exceptionhandler.dto.ErroDetalhe;
import br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration.CodigoValidacaoSimples;

/**
 * Coletor de erros por item do Simples Nacional.
 *
 * Duplicado deliberadamente da classe análoga {@code ItemCollector} do regime geral,
 * para preservar a independência dos códigos de erro ({@code VSN-}) e do endpoint de
 * catálogo específicos do Simples Nacional.
 */
public class ItemCollectorSimples {

    private final int index;
    private final String baseURL;
    private final List<ErroDetalhe> erros = new ArrayList<>();
    private EstadoItemSimplesEnum estado = EstadoItemSimplesEnum.CALCULADO;

    public ItemCollectorSimples(int index, String baseURL) {
        this.index = index;
        this.baseURL = baseURL;
    }

    /**
     * Acumula um erro de validação de entrada no item, promovendo o estado para
     * {@code INCONSISTENCIA_ENTRADA} (se ainda estiver {@code CALCULADO}).
     *
     * @param codigo código {@code VSN-} catalogado
     * @param campo  nome do campo (relativo ao item) para compor o JSON Pointer
     * @param detail mensagem de detalhe
     */
    public void addErroValidacao(CodigoValidacaoSimples codigo, String campo, String detail) {
        if (estado == EstadoItemSimplesEnum.CALCULADO) {
            estado = EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA;
        }
        String pointer = buildPointer(campo);
        String type = buildTypeURI(codigo.getCode());
        erros.add(new ErroDetalhe(type, codigo.getTitulo(), codigo.getCode(), pointer, detail));
    }

    /**
     * Acumula um erro de validação de entrada do item cujo campo fica FORA do
     * item — por exemplo, a alíquota exigida pelo item e ausente na linha de
     * alíquotas da operação ({@code #/aliquotasEfetivas/anexoIII/pCBSSN}).
     * Mesmo efeito de {@link #addErroValidacao} sobre o estado.
     *
     * @param codigo   código {@code VSN-} catalogado
     * @param ponteiro JSON Pointer completo do campo (começa com {@code #/})
     * @param detail   mensagem de detalhe
     */
    public void addErroValidacaoNoPonteiro(CodigoValidacaoSimples codigo, String ponteiro, String detail) {
        if (estado == EstadoItemSimplesEnum.CALCULADO) {
            estado = EstadoItemSimplesEnum.INCONSISTENCIA_ENTRADA;
        }
        erros.add(new ErroDetalhe(buildTypeURI(codigo.getCode()), codigo.getTitulo(), codigo.getCode(), ponteiro,
                detail));
    }

    /** Índice do item na lista de entrada (base dos ponteiros {@code #/itens/N}). */
    public int getIndex() {
        return index;
    }

    /**
     * Registra que o item não pode ser calculado por falta de implementação na
     * Calculadora (cClassTrib vinculado, na data do fato gerador, a tratamento
     * tributário marcado como "sem implementação"), definindo o estado como
     * {@code NAO_IMPLEMENTADO}.
     *
     * <p>Diferente de {@link #addErroValidacao}, o estado é atribuído
     * INCONDICIONALMENTE: a falta de implementação tem prioridade sobre
     * inconsistências de entrada eventualmente já acumuladas — o mesmo
     * comportamento do coletor do regime geral, em que o estado do código de
     * erro catalogado prevalece.
     *
     * @param codigo código {@code VSN-} catalogado
     * @param campo  nome do campo (relativo ao item) para compor o JSON Pointer
     * @param detail mensagem de detalhe
     */
    public void addNaoImplementado(CodigoValidacaoSimples codigo, String campo, String detail) {
        estado = EstadoItemSimplesEnum.NAO_IMPLEMENTADO;
        String pointer = buildPointer(campo);
        String type = buildTypeURI(codigo.getCode());
        erros.add(new ErroDetalhe(type, codigo.getTitulo(), codigo.getCode(), pointer, detail));
    }

    /**
     * Acumula uma falha técnica inesperada de cálculo, definindo o estado como
     * {@code FALHA_TECNICA}.
     */
    public void addFalhaTecnica(Exception ex) {
        estado = EstadoItemSimplesEnum.FALHA_TECNICA;
        CodigoValidacaoSimples codigo = CodigoValidacaoSimples.FALHA_TECNICA;
        String pointer = "#/itens/" + index;
        String type = buildTypeURI(codigo.getCode());
        erros.add(new ErroDetalhe(type, codigo.getTitulo(), codigo.getCode(), pointer, ex.getMessage()));
    }

    private String buildTypeURI(String codigo) {
        return String.format("%s/calculadora/observabilidade/simples-nacional/erros?codigo=%s", baseURL, codigo);
    }

    private String buildPointer(String campo) {
        if (campo == null) {
            return "#/itens/" + index;
        }
        return "#/itens/" + index + "/" + campo;
    }

    public EstadoItemSimplesEnum getEstado() {
        return estado;
    }

    public List<ErroDetalhe> getErros() {
        return Collections.unmodifiableList(erros);
    }

    public boolean hasErros() {
        return !erros.isEmpty();
    }
}
