package br.gov.serpro.rtc.api.simplesnacional.exceptionhandler.enumeration;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;

/**
 * Catálogo próprio de códigos de validação do Simples Nacional,
 * com prefixo {@code VSN-} (Validação Simples Nacional), alinhado à
 * NT 2026.005 (Reforma Tributária do Consumo — Simples Nacional).
 *
 * <p>
 * POLÍTICA DE APOSENTADORIA DE CÓDIGOS: os códigos {@code VSN-008} e
 * {@code VSN-009} (origem de alíquota ANEXO/REGIME_GERAL do contrato antigo)
 * foram APOSENTADOS junto com o conceito de "origem" e seus números NÃO são
 * reutilizados, para que consultas históricas ao catálogo nunca retornem um
 * significado diferente do que foi emitido à época. Novos erros recebem
 * números a partir de {@code VSN-014}, sempre o próximo livre (os últimos
 * atribuídos foram {@code VSN-030} a {@code VSN-040}, dos endpoints não
 * assistido e assistido para todos os documentos).
 *
 * <p>
 * Duplicado deliberadamente do análogo {@code CodigoValidacao} do regime geral:
 * os catálogos de erro dos dois cálculos são independentes (códigos, mensagens e
 * ciclos de vida próprios) e a duplicação preserva o isolamento entre os pacotes.
 *
 */
@Getter
public enum CodigoValidacaoSimples {

    BASE_AUSENTE("VSN-001",
            "Base de cálculo do Simples ausente",
            "A base de cálculo do Simples Nacional é obrigatória e não foi informada."),
    BASE_INVALIDA("VSN-002",
            "Base de cálculo do Simples inválida (negativa/fora do formato)",
            "A base de cálculo informada é negativa ou não está no formato 13v2 (até 13 dígitos inteiros e até "
                    + "2 casas decimais, sem notação científica)."),
    IS_INVALIDO("VSN-003",
            "Valor do Imposto Seletivo inválido (negativo/fora do formato)",
            "O valor do Imposto Seletivo (vIS) informado é negativo ou não está no formato 13v2 (até 13 dígitos "
                    + "inteiros e até 2 casas decimais, sem notação científica)."),
    ALIQUOTA_CBS_AUSENTE("VSN-004",
            "Alíquota pCBSSN ausente",
            "A alíquota efetiva de CBS do Simples Nacional (pCBSSN) é exigida para o cálculo do item e não foi informada."),
    ALIQUOTA_CBS_INVALIDA("VSN-005",
            "Alíquota pCBSSN inválida (negativa/fora do formato)",
            "A alíquota efetiva de CBS do Simples Nacional (pCBSSN) informada é negativa ou não está no formato "
                    + "aceito (até 3 dígitos inteiros e até 34 casas decimais, sem notação científica)."),
    ALIQUOTA_IBS_AUSENTE("VSN-006",
            "Alíquota pIBSSN ausente",
            "A alíquota efetiva de IBS do Simples Nacional (pIBSSN) é exigida para o cálculo do item e não foi informada."),
    ALIQUOTA_IBS_INVALIDA("VSN-007",
            "Alíquota pIBSSN inválida (negativa/fora do formato)",
            "A alíquota efetiva de IBS do Simples Nacional (pIBSSN) informada é negativa ou não está no formato "
                    + "aceito (até 3 dígitos inteiros e até 34 casas decimais, sem notação científica)."),
    // VSN-008 e VSN-009 (origem de alíquota): APOSENTADOS — números não reutilizados (ver Javadoc da classe).
    NUMERO_AUSENTE("VSN-010",
            "Número do item ausente",
            "O número (identificador) do item é obrigatório e não foi informado."),
    ITENS_VAZIOS("VSN-011",
            "Lista de itens vazia",
            "A lista de itens da operação é obrigatória e não pode estar vazia."),
    DATA_INVALIDA("VSN-012",
            "Data do fato gerador ausente/inválida",
            "A data do fato gerador é obrigatória e deve estar no formato de data (YYYY-MM-DD)."),
    DADO_GLOBAL_AUSENTE("VSN-013",
            "Dado global obrigatório ausente",
            "Um campo global obrigatório da operação (id ou versao) não foi informado."),
    TPDOC_AUSENTE("VSN-014",
            "Tipo de documento fiscal (tpDoc) ausente",
            "O tipo de documento fiscal (tpDoc) é obrigatório e não foi informado."),
    TPDOC_NAO_SUPORTADO("VSN-015",
            "Tipo de documento fiscal (tpDoc) não suportado",
            "O tipo de documento fiscal (tpDoc) informado não está habilitado para o cálculo do Simples "
                    + "Nacional na data do fato gerador. A habilitação de documentos é parametrizada em banco "
                    + "de dados (tabela de tipos de DFe do cálculo do Simples), conforme as NTs publicadas."),
    CRT_INVALIDO("VSN-016",
            "Código de Regime Tributário (CRT) ausente/inválido",
            "O Código de Regime Tributário (CRT) é obrigatório e deve ser 1, 2, 3 ou 4."),
    TPREGIMEIBSCBS_INVALIDO("VSN-017",
            "Tipo de regime do IBS/CBS (tpRegimeIBSCBS) inválido",
            "O tipo de regime de apuração do IBS/CBS (tpRegimeIBSCBS), quando informado, deve ser 1 ou 2."),
    TPREGIMEIBSCBS_INCOMPATIVEL("VSN-018",
            "tpRegimeIBSCBS incompatível com o CRT",
            "A combinação de CRT e tpRegimeIBSCBS informada é incoerente (regras C21a-10/20/30 da NT 2026.005): "
                    + "CRT 1, 2 ou 4 não admite tpRegimeIBSCBS=2; CRT 3 não admite tpRegimeIBSCBS=1; "
                    + "CRT 4 não admite tpRegimeIBSCBS algum."),
    OPERACAO_FORA_ESCOPO("VSN-019",
            "Operação fora do escopo do cálculo do Simples Nacional",
            "A operação não é calculada por este endpoint: optante pelo regime regular do IBS/CBS "
                    + "(tpRegimeIBSCBS=1), MEI (CRT=4) ou regime normal puro (CRT=3 sem tpRegimeIBSCBS=2)."),
    CCLASSTRIB_AUSENTE("VSN-020",
            "Código de Classificação Tributária (cClassTrib) ausente",
            "O código de classificação tributária do IBS/CBS (cClassTrib) é obrigatório e não foi informado."),
    CCLASSTRIB_DESCONHECIDO("VSN-021",
            "cClassTrib desconhecido ou fora de vigência",
            "O cClassTrib informado não existe ou não está vigente na data do fato gerador: "
                    + "o Tipo de Receita Bruta do Simples Nacional (tpRBSN) não pôde ser determinado."),
    CCLASSTRIB_INCOMPATIVEL_TPDOC("VSN-022",
            "cClassTrib incompatível com o tpDoc",
            "O cClassTrib informado não é admissível para o tipo de documento fiscal (tpDoc) da operação."),
    CATIVSN_INCOMPATIVEL_CCLASSTRIB("VSN-023",
            "Código de Atividade do Simples Nacional incompatível com cClassTrib",
            "O código de atividade cAtivSN='90' (Operações não Tributáveis) é incompatível com cClassTrib "
                    + "cujo tpRBSN esteja entre 1 e 5 (regra UB14-90 da NT 2026.005)."),
    TPENTEGOV_INVALIDO("VSN-024",
            "Tipo de ente governamental (tpEnteGov) inválido",
            "O tipo de ente governamental (tpEnteGov), quando informado, deve ser 1 (União), 2 (Estado), "
                    + "3 (Distrito Federal) ou 4 (Município). O campo é da operação — espelha a tag "
                    + "gCompraGov/tpEnteGov da identificação da nota (grupo B/ide do leiaute do DF-e) — e "
                    + "sua invalidade rejeita a operação inteira."),
    COMPRA_GOV_SEM_VIGENCIA("VSN-025",
            "Parametrização de compra governamental indisponível para a data",
            "A data do fato gerador está fora de todas as vigências do parâmetro de transição de alíquotas "
                    + "de compra governamental do Simples Nacional (NT 2026.005, seção 5.3.3; arts. 372 e 473 "
                    + "da LC 214/2025). Como o tpEnteGov é dado da operação, a falha rejeita a operação "
                    + "inteira."),
    PARAMETRIZACAO_INDISPONIVEL("VSN-026",
            "Parametrização do cálculo do Simples Nacional indisponível para a data",
            "Não há linha vigente, na data do fato gerador, em uma tabela de parametrização do cálculo do "
                    + "Simples Nacional (efeito do tipo de receita bruta ou matriz de regime de apuração). "
                    + "O cálculo nunca assume comportamento implícito: a parametrização deve ser corrigida "
                    + "por script de migração."),
    CCLASSTRIB_NAO_IMPLEMENTADO("VSN-027",
            "cClassTrib sem tratamento implementado no cálculo do Simples Nacional",
            "A classificação tributária (cClassTrib) informada está vinculada, na data do fato gerador, a "
                    + "tratamento tributário marcado como ainda não implementado na Calculadora (indicador de "
                    + "ajuste do tratamento tributário). O item é reportado com o estado NAO_IMPLEMENTADO, "
                    + "sem cálculo e fora dos totais; a funcionalidade está prevista no roadmap e será "
                    + "disponibilizada em versão futura."),
    NI_CONTRIBUINTE_INVALIDO("VSN-028",
            "Identificação do contribuinte (niContribuinte) ausente ou inválida",
            "A identificação do contribuinte emitente (niContribuinte) é obrigatória no cálculo assistido e "
                    + "deve ser um CNPJ alfanumérico válido: 14 caracteres (12 caracteres [A-Z0-9] e 2 dígitos "
                    + "verificadores), aceito com ou sem máscara. É por ela e pelo período de apuração da data "
                    + "do fato gerador que as alíquotas efetivas são obtidas na Tabela de Alíquotas Efetivas do "
                    + "Simples Nacional (NT 2026.005, seção 5.3.3). A falha rejeita a operação inteira."),
    ALIQUOTAS_EFETIVAS_INDISPONIVEIS("VSN-029",
            "Alíquotas efetivas do Simples Nacional indisponíveis para o contribuinte no período de apuração",
            "Não há registro na Tabela de Alíquotas Efetivas do Simples Nacional para o contribuinte "
                    + "(niContribuinte) no período de apuração (AAAAMM) derivado da data do fato gerador. Sem as "
                    + "alíquotas efetivas o cálculo assistido não pode ser realizado; a operação inteira é "
                    + "rejeitada. Alternativa: o endpoint clássico, informando as alíquotas por item."),
    OPSIMPNAC_INVALIDO("VSN-030",
            "Situação perante o Simples Nacional (opSimpNac) ausente ou inválida",
            "Na NFS-e, a situação do prestador perante o Simples Nacional (opSimpNac, DPS prest/regTrib) é "
                    + "obrigatória e deve ser 1 (não optante), 2 (MEI), 3 (ME/EPP) ou 4 (opção pendente)."),
    REGAPIBSCBSSN_INVALIDO("VSN-031",
            "Regime de apuração do IBS e da CBS no Simples (regApIBSCBSSN) incoerente ou inválido",
            "O regApIBSCBSSN (DPS prest/regTrib) deve ser 1 (IBS e CBS pelo Simples), 2 (CBS pelo Simples e IBS "
                    + "pelo regime regular) ou 3 (os dois pelo regime regular). É obrigatório com opSimpNac 3 ou "
                    + "4 (regra 313 da NFS-e) e não pode ser informado com opSimpNac 1 ou 2."),
    REGAPTRIBSN_INVALIDO("VSN-032",
            "Regime de apuração dos tributos no Simples (regApTribSN) incoerente ou inválido",
            "O regApTribSN (DPS prest/regTrib) deve ser 1 (tributos federais e ISS pelo Simples), 2 (federais "
                    + "pelo Simples e ISS fora dele) ou 3 (federais e ISS fora). É obrigatório com opSimpNac 3 ou 4 "
                    + "(regra E0166, estendida pela NT 009) e não pode ser informado com opSimpNac 1 ou 2 (E0162)."),
    CAMPO_REGIME_PROIBIDO("VSN-033",
            "Campo de regime não pertence ao documento",
            "O regime do emitente é informado no vocabulário do próprio documento: CRT e tpRegimeIBSCBS nos "
                    + "documentos com CRT (NF-e, NFC-e, CT-e, CT-e OS, BP-e, BP-e TM e NFCom); opSimpNac, "
                    + "regApIBSCBSSN e regApTribSN na NFS-e. Campo do outro vocabulário não pode ser informado."),
    QUANTIDADE_ITENS_INVALIDA("VSN-034",
            "Número de itens não admitido pelo documento",
            "O documento admite um número máximo de itens (parametrizado em banco). A NFS-e tem exatamente 1 "
                    + "item: a DPS tem um só serviço, com um cTribNac e um cClassTrib."),
    IS_NAO_ADMITIDO("VSN-035",
            "Imposto Seletivo não admitido pelo documento",
            "O documento não tem Imposto Seletivo (NFS-e): o valorImpostoSeletivo deve estar ausente ou ser zero."),
    TRIBISSQN_INVALIDO("VSN-036",
            "Tributação do ISSQN (tribISSQN) ausente ou inválida",
            "Na NFS-e, a tributação do ISSQN (tribISSQN) é obrigatória quando a atividade tem ISS no Simples e o "
                    + "regime calcula tributos, e deve ser 1 (operação tributável), 2 (imunidade), 3 (exportação "
                    + "de serviço) ou 4 (não incidência)."),
    TPRETISSQN_INVALIDO("VSN-037",
            "Retenção do ISSQN (tpRetISSQN) ausente ou inválida",
            "Na NFS-e, o tipo de retenção do ISSQN (tpRetISSQN) é obrigatório quando tribISSQN = 1, nas atividades "
                    + "com ISS no Simples, e deve ser 1 (não retido), 2 (retido pelo tomador) ou 3 (retido pelo "
                    + "intermediário)."),
    TPBM_INVALIDO("VSN-038",
            "Tipo de benefício municipal (tpBM) inválido",
            "Na NFS-e, o tipo de benefício municipal do ISS (tpBM), quando informado, deve ser 1 (isenção), 2 "
                    + "(redução da base em %), 3 (redução da base em R$) ou 4 (alíquota diferenciada). Ausente = "
                    + "sem benefício municipal."),
    ALIQUOTAS_EFETIVAS_AUSENTES("VSN-039",
            "Alíquotas efetivas do contribuinte (aliquotasEfetivas) ausentes",
            "No cálculo não assistido, a linha de alíquotas efetivas do contribuinte no período "
                    + "(aliquotasEfetivas) é obrigatória, exceto quando o regime não calcula tributos "
                    + "(SOMENTE_RECEITA). A falha rejeita a operação inteira."),
    FATOR_R_AUSENTE("VSN-040",
            "Fator \"r\" (fatorR) ausente",
            "O fatorR da linha de alíquotas é exigido quando o item tem atividade sujeita ao fator \"r\" (a 09): "
                    + "é ele que decide entre o Anexo III (fator r igual ou superior a 28%) e o Anexo V."),
    NCM_NBS_SIMULTANEOS("VSN-041", "NCM e NBS simultâneos", "Informe NCM ou NBS, nunca ambos."),
    NCM_INCOMPLETO("VSN-042", "NCM incompleto", "A classificação tributária exige um NCM completo com 8 caracteres."),
    NBS_INCOMPLETO("VSN-043", "NBS incompleto", "A classificação tributária exige um NBS completo com 9 caracteres."),
    NOMENCLATURA_OBRIGATORIA("VSN-044", "Nomenclatura obrigatória", "Informe NCM ou NBS completo conforme exigido pela classificação tributária."),
    NOMENCLATURA_INCOMPATIVEL("VSN-045", "Nomenclatura incompatível com o documento", "A nomenclatura informada é incompatível com o tipo de documento fiscal ou a classificação tributária."),
    NOMENCLATURA_NAO_PERMITIDA("VSN-046", "Nomenclatura não permitida", "A nomenclatura informada não é permitida para o tipo de documento fiscal."),
    NCM_NAO_APLICAVEL("VSN-047", "NCM não aplicável", "O NCM informado não está vinculado à classificação tributária."),
    NBS_NAO_APLICAVEL("VSN-048", "NBS não aplicável", "O NBS informado não está vinculado à classificação tributária."),
    NCM_NAO_ENCONTRADO("VSN-049", "NCM desconhecido", "O NCM informado não existe ou não está vigente na data."),
    NBS_NAO_ENCONTRADO("VSN-050", "NBS desconhecido", "O NBS informado não existe ou não está vigente na data."),
    NOMENCLATURA_FORMATO_INVALIDO("VSN-051", "Formato de nomenclatura inválido",
            "NCM deve conter 8 ou NBS deve conter 9 dígitos ASCII, sem espaços ou símbolos."),
    DESCONHECIDO("VSN-000",
            "Erro de validação não categorizado",
            "O tipo de validação aplicado não possui uma categoria específica."),
    FALHA_TECNICA("VSN-999",
            "Falha técnica no cálculo do item",
            "Ocorreu uma falha técnica inesperada durante o cálculo do item.");

    private final String code;
    private final String titulo;
    private final String descricao;

    private static final Map<String, CodigoValidacaoSimples> CODE_INDEX =
            Stream.of(values())
                    .collect(Collectors.toUnmodifiableMap(CodigoValidacaoSimples::getCode, Function.identity()));

    CodigoValidacaoSimples(String code, String titulo, String descricao) {
        this.code = code;
        this.titulo = titulo;
        this.descricao = descricao;
    }

    public static Optional<CodigoValidacaoSimples> fromCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(CODE_INDEX.get(codigo));
    }
}
