package br.gov.serpro.rtc.api.atividadesimples.service;

import java.util.List;
import java.util.Set;

import br.gov.serpro.rtc.api.atividadesimples.dto.ModoFatorR;

/**
 *
 * Regra pura de derivação da disponibilidade de uma atividade do Simples
 * Nacional frente à configuração de anexos informada pelo usuário.
 *
 * <p>
 *
 * ISOLADO.
 *
 * Vive sob {@code br.gov.serpro.rtc.api.atividadesimples} e não depende do
 * pacote {@code simplesnacional} nem do domínio do regime geral. Classe
 * utilitária sem estado, fácil de testar unitariamente — é o ÚNICO lugar onde
 * a regra de derivação de {@code disponivel}, {@code anexosFaltantes} e
 * {@code modoFatorR} existe.
 *
 * <p>
 *
 * Convenção do parâmetro {@code anexosConfigurados}:
 * {@code null} = sem filtro (todos os anexos I..V configurados);
 * conjunto vazio = nenhum anexo configurado.
 *
 */
public final class DerivacaoAnexosSimples {

    /** Anexos válidos do Simples Nacional, na ordem canônica dos romanos. */
    public static final List<String> ANEXOS_VALIDOS = List.of("I", "II", "III", "IV", "V");

    private DerivacaoAnexosSimples() {
    }

    /**
     * Resultado da derivação: os três campos adicionados ao contrato de saída.
     */
    public record Derivacao(boolean disponivel, List<String> anexosFaltantes, ModoFatorR modoFatorR) {
    }

    /**
     * Deriva {@code disponivel}, {@code anexosFaltantes} e {@code modoFatorR}
     * para uma atividade cujos anexos são {@code anexoSemFatorR} (anexo quando
     * o fator "r" é inferior a 28%, ou o único anexo da atividade) e
     * {@code anexoComFatorR} (anexo quando o fator "r" é igual ou superior a
     * 28%) — ambos anuláveis; ambos nulos = atividade sem anexo (ex.: código
     * 90), sempre disponível. Na carga oficial a atividade 09 tem
     * {@code anexoSemFatorR = "V"} e {@code anexoComFatorR = "III"}; a regra
     * aqui é independente dos valores concretos.
     *
     * @throws IllegalStateException se {@code anexoComFatorR != null} com
     *         {@code anexoSemFatorR == null} — não existe atividade "só com
     *         Fator R"; dado inconsistente no banco deve falhar cedo.
     */
    public static Derivacao derivar(String anexoSemFatorR, String anexoComFatorR, Set<String> anexosConfigurados) {
        if (anexoComFatorR != null && anexoSemFatorR == null) {
            throw new IllegalStateException(
                    "Atividade inconsistente: anexoComFatorR (" + anexoComFatorR
                            + ") preenchido sem anexoSemFatorR. Não existe atividade \"só com Fator R\"; corrija o banco.");
        }

        Set<String> configurados = anexosConfigurados == null ? Set.copyOf(ANEXOS_VALIDOS) : anexosConfigurados;

        List<String> anexosPossiveis = ANEXOS_VALIDOS.stream()
                .filter(anexo -> anexo.equals(anexoSemFatorR) || anexo.equals(anexoComFatorR))
                .toList();

        List<String> anexosFaltantes = anexosPossiveis.stream()
                .filter(anexo -> !configurados.contains(anexo))
                .toList();

        boolean disponivel = anexosPossiveis.isEmpty()
                || anexosPossiveis.stream().anyMatch(configurados::contains);

        ModoFatorR modoFatorR;
        if (anexoComFatorR == null) {
            modoFatorR = ModoFatorR.NAO_APLICAVEL;
        } else if (configurados.contains(anexoComFatorR)) {
            modoFatorR = configurados.contains(anexoSemFatorR) ? ModoFatorR.OPCIONAL : ModoFatorR.OBRIGATORIO;
        } else {
            modoFatorR = ModoFatorR.INDISPONIVEL;
        }

        return new Derivacao(disponivel, anexosFaltantes, modoFatorR);
    }
}
