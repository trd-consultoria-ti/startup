package br.gov.serpro.rtc.api.atividadesimples.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * Contrato de saída de uma atividade do Simples Nacional e seu(s) anexo(s).
 *
 * <p>
 *
 * Vive sob {@code br.gov.serpro.rtc.api.atividadesimples}
 * e NÃO depende do domínio do regime geral.
 *
 * <p>
 *
 * Escopo deliberado: resolve apenas <b>atividade → anexo(s)</b> e as marcações
 * de disponibilidade. NÃO devolve alíquotas — a relação anexo → alíquota
 * continua sendo realizada externamente.
 *
 * <p>
 *
 * O {@code codigo} é o código oficial de Atividade do Simples Nacional (tag {@code cAtivSN}
 * dos leiautes de DF-e, NT 2026.005; ex.: {@code "09"}, {@code "90"}).
 *
 * <p>
 *
 * Os campos {@code anexoSemFatorR} e {@code anexoComFatorR} são numerais romanos puros ({@code "I".."V"}), sem ressalvas.
 * As ressalvas ("menos ICMS", etc.) ficam apenas em {@code rotuloAnexo}. Semântica (LC 123/2006, art. 18,
 * §§ 5º-J e 5º-M; Resolução CGSN 190/2026): {@code anexoComFatorR} é o anexo quando o fator "r" é <b>igual ou
 * superior a 28%</b> (para a 09, o Anexo III); {@code anexoSemFatorR} é o anexo quando o fator "r" é
 * <b>inferior a 28%</b> (para a 09, o Anexo V) — ou o único anexo, quando a atividade não depende do fator "r".
 * <b>Ambos são anuláveis</b>: {@code anexoComFatorR} só é preenchido para a atividade sujeita ao Fator R (a 09), e
 * {@code anexoSemFatorR} é nulo na linha do código 90 (Operações não Tributáveis, NT 2026.005), que não possui
 * anexo de apuração e aparece na listagem como item normal (decisão deliberada).
 * {@code sujeitaFatorR} é derivado de {@code anexoComFatorR != null} no próprio construtor,
 * para que as duas informações nunca divirjam (para o 90, ambos nulos → {@code false}).
 *
 * <p>
 *
 * Os campos {@code disponivel}, {@code anexosFaltantes} e {@code modoFatorR} são derivados
 * da configuração de anexos informada na consulta. A regra de derivação fica concentrada
 * em {@code DerivacaoAnexosSimples} (chamada pelo service); este record apenas transporta o resultado.
 *
 * <p>
 *
 * Os campos de admissibilidade por documento fiscal (associativa
 * {@code TIPO_DFE_ATIVIDADE_SIMPLES_NACIONAL}, semântica de whitelist — mundo
 * fechado) são aditivos: {@code tpDocsAdmissiveis} é SEMPRE presente e lista
 * exatamente os documentos com vínculo vigente na data (lista vazia = nenhum
 * documento publicado para a atividade — inadmissível em todos), e
 * {@code admissivelNoDocumento} só aparece quando o query param {@code tpDoc}
 * foi informado (inclusão {@code NON_NULL} no campo).
 *
 * <p>
 *
 * O campo {@code compativelComClassTrib} só aparece quando o query param
 * {@code cClassTrib} foi informado (inclusão {@code NON_NULL}): {@code false}
 * quando o tpRBSN da classificação declara receita vedada para atividades não
 * tributáveis (espelho da regra UB14-90 da NT 2026.005 — hoje alcança apenas a
 * linha do código 90 com tpRBSN 1 a 5).
 *
 * <p>
 *
 * {@code anexosFaltantes} e {@code modoFatorR} continuam derivados
 * EXCLUSIVAMENTE dos anexos; {@code disponivel} agrega todos os eixos
 * informados na consulta (anexos, admissibilidade no documento e
 * compatibilidade com a classificação) — os motivos são distinguíveis e
 * combináveis pelos campos individuais.
 *
 */
@JsonInclude(ALWAYS)
@JsonPropertyOrder({ "codigo", "ordem", "descricao", "anexoSemFatorR", "anexoComFatorR", "sujeitaFatorR",
        "rotuloAnexo", "rotuloCurtoAnexo", "disponivel", "anexosFaltantes", "modoFatorR", "tpDocsAdmissiveis",
        "admissivelNoDocumento", "compativelComClassTrib" })
@Schema(description = "Atividade do Simples Nacional e o(s) anexo(s) aplicável(is). Não inclui alíquotas.")
public record AtividadeSimplesDomain(

        @Schema(description = "Código oficial de Atividade do Simples Nacional do leiaute DF-e "
                + "(tag cAtivSN, NT 2026.005): 2 caracteres com zero à esquerda ('01'..'14' e '90'). Único entre "
                + "as linhas vigentes do catálogo", example = "09")
        String codigo,

        @Schema(description = "Ordem de exibição (1..15; a 15 é a linha do código 90)", example = "9")
        int ordem,

        @Schema(description = "Descrição longa da atividade",
                example = "Prestação de serviços — Sujeitos ao Fator \"R\"")
        String descricao,

        @Schema(description = "Anexo (romano puro) quando o fator r é INFERIOR a 28%, ou quando a atividade não "
                + "depende do fator r (é então o único anexo). Para a atividade 09 é o Anexo V (LC 123/2006, "
                + "art. 18, § 5º-M). Null na linha do código 90 (Operações não Tributáveis), que não possui anexo "
                + "de apuração",
                example = "V", nullable = true)
        String anexoSemFatorR,

        @Schema(description = "Anexo (romano puro) quando o fator r é IGUAL OU SUPERIOR a 28% (fator r atingido). "
                + "Para a atividade 09 é o Anexo III (LC 123/2006, art. 18, § 5º-J). Null quando a atividade não "
                + "depende do fator r",
                example = "III", nullable = true)
        String anexoComFatorR,

        @Schema(description = "Indica se a atividade depende do Fator R (derivado: anexoComFatorR != null)", example = "true")
        boolean sujeitaFatorR,

        @Schema(description = "Rótulo informativo completo do anexo (badge), com ressalvas", example = "III ou V")
        String rotuloAnexo,

        @Schema(description = "Rótulo curto do anexo (badge), apenas o(s) numeral(is) romano(s), sem ressalvas",
                example = "III ou V")
        String rotuloCurtoAnexo,

        @Schema(description = "Indica se a atividade pode ser selecionada com os filtros informados na consulta "
                + "(anexos configurados, documento fiscal e classificação tributária). A atividade sem anexo é "
                + "disponível pelo eixo dos anexos.", example = "true")
        boolean disponivel,

        @ArraySchema(arraySchema = @Schema(description = "Anexos do conjunto possível da atividade que NÃO estão na "
                + "configuração informada, ordenados por romano (I..V). Nunca null; [] quando nada falta."),
                schema = @Schema(example = "III"))
        List<String> anexosFaltantes,

        @Schema(description = "Estado da decisão de Fator R da atividade, dada a configuração de anexos informada",
                example = "OBRIGATORIO")
        ModoFatorR modoFatorR,

        @ArraySchema(arraySchema = @Schema(description = "Tipos de documento fiscal (TPDF_TIPO: \"55\", \"65\", "
                + "\"91\", ...) com vínculo vigente na data na associativa da atividade (whitelist). SEMPRE "
                + "presente, tenha ou não sido informado o tpDoc na consulta. Lista vazia = nenhum documento "
                + "publicado para a atividade — inadmissível em todos."),
                schema = @Schema(example = "55"))
        List<String> tpDocsAdmissiveis,

        @JsonInclude(NON_NULL)
        @Schema(description = "Indica se a atividade é admissível no documento fiscal informado no query param "
                + "tpDoc. Presente SOMENTE quando tpDoc foi informado: true se e somente se o tpDoc está em "
                + "tpDocsAdmissiveis (whitelist — sem vínculo vigente, não há permissão).",
                example = "true", nullable = true)
        Boolean admissivelNoDocumento,

        @JsonInclude(NON_NULL)
        @Schema(description = "Indica se a atividade é compatível com a classificação tributária informada no "
                + "query param cClassTrib. Presente SOMENTE quando cClassTrib foi informado: false quando o tpRBSN "
                + "da classificação declara receita vedada para atividades não tributáveis (espelho da regra "
                + "UB14-90 da NT 2026.005, o mesmo erro VSN-023 do cálculo).",
                example = "true", nullable = true)
        Boolean compativelComClassTrib) {

    /**
     *
     * Construtor canônico: força {@code sujeitaFatorR} a ser derivado de {@code anexoComFatorR},
     * ignorando qualquer valor informado, para garantir coerência entre os dois campos.
     * Também garante que {@code anexosFaltantes} e {@code tpDocsAdmissiveis} nunca sejam
     * null (contrato: lista vazia); quando presentes, são copiados para lista imutável.
     *
     */
    public AtividadeSimplesDomain {
        sujeitaFatorR = anexoComFatorR != null;
        anexosFaltantes = anexosFaltantes == null ? List.of() : List.copyOf(anexosFaltantes);
        tpDocsAdmissiveis = tpDocsAdmissiveis == null ? List.of() : List.copyOf(tpDocsAdmissiveis);
    }

    /**
     *
     * Fábrica que dispensa o parâmetro {@code sujeitaFatorR} (sempre derivado).
     * Use-a no catálogo para não correr o risco de preencher a flag manualmente.
     *
     */
    public static AtividadeSimplesDomain de(
            String codigo,
            int ordem,
            String descricao,
            String anexoSemFatorR,
            String anexoComFatorR,
            String rotuloAnexo,
            String rotuloCurtoAnexo,
            boolean disponivel,
            List<String> anexosFaltantes,
            ModoFatorR modoFatorR,
            List<String> tpDocsAdmissiveis,
            Boolean admissivelNoDocumento,
            Boolean compativelComClassTrib) {
        return new AtividadeSimplesDomain(
                codigo, ordem, descricao, anexoSemFatorR, anexoComFatorR, anexoComFatorR != null, rotuloAnexo,
                rotuloCurtoAnexo, disponivel, anexosFaltantes, modoFatorR, tpDocsAdmissiveis, admissivelNoDocumento,
                compativelComClassTrib);
    }
}
