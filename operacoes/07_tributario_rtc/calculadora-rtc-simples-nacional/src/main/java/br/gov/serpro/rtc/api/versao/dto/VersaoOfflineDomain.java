package br.gov.serpro.rtc.api.versao.dto;

import java.util.regex.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Saída com o comparativo de versões da aplicação e do banco de dados local em
 * relação às versões remotas para uso offline.
 *
 * <p>O construtor canônico é simples (apenas atribui). Os booleanos
 * {@code *Atualizada} são DERIVADOS dos campos de versão na fábrica estática
 * {@link #de(VersaoDomain, VersaoDomain)} — no estilo de
 * {@code AtividadeSimplesDomain.de(...)} — para que nunca divirjam dos campos
 * de versão que os originam: a comparação da aplicação usa o prefixo semver
 * quando ambos os lados o possuem (ignorando sufixos como {@code -SNAPSHOT});
 * builds sem prefixo semver (ex.: versões de feature branch) e as demais
 * comparações usam igualdade textual.
 */
@Schema(description = "Comparativo das versões locais e remotas da aplicação e do banco de dados")
public record VersaoOfflineDomain(

        @Schema(description = "Indica se a versão da aplicação local está atualizada em relação à remota", example = "true")
        boolean aplicacaoAtualizada,

        @Schema(description = "Indica se a versão do banco de dados local está atualizada em relação à remota", example = "true")
        boolean dbAtualizada,

        @Schema(description = "Indica se a data do banco de dados local está atualizada em relação à remota", example = "true")
        boolean dataDbAtualizada,

        @Schema(description = "Versão da aplicação local", example = "1.0.0")
        String versaoAplicacaoLocal,

        @Schema(description = "Versão da aplicação remota (nula quando indisponível)", example = "1.0.0", nullable = true)
        String versaoAplicacaoRemota,

        @Schema(description = "Versão do banco de dados local", example = "1.0.0")
        String versaoDbLocal,

        @Schema(description = "Versão do banco de dados remoto (nula quando indisponível)", example = "1.0.0", nullable = true)
        String versaoDbRemota,

        @Schema(description = "Data do banco de dados local", example = "2026-01-01")
        String dataDbLocal,

        @Schema(description = "Data do banco de dados remoto (nula quando indisponível)", example = "2026-01-01", nullable = true)
        String dataDbRemota) {

    private static final Pattern SEMVER_PATTERN =
            Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)");

    /**
     * Fábrica que compara as versões local e remota e calcula os booleanos
     * derivados ({@code aplicacaoAtualizada}, {@code dbAtualizada},
     * {@code dataDbAtualizada}). Use-a em vez do construtor canônico para não
     * correr o risco de preencher as flags manualmente.
     *
     * @param local  versões da instalação local (nunca nula)
     * @param remota versões publicadas no serviço online ({@code null} quando a
     *               consulta remota está indisponível — as flags resultam falsas)
     */
    public static VersaoOfflineDomain de(VersaoDomain local, VersaoDomain remota) {
        String versaoAplicacaoLocal = local.versaoApp();
        String versaoDbLocal = local.versaoDb();
        String dataDbLocal = local.dataVersaoDb();

        String versaoAplicacaoRemota = remota != null ? remota.versaoApp() : null;
        String versaoDbRemota = remota != null ? remota.versaoDb() : null;
        String dataDbRemota = remota != null ? remota.dataVersaoDb() : null;

        boolean aplicacaoAtualizada = versoesAplicacaoIguais(versaoAplicacaoLocal, versaoAplicacaoRemota);
        boolean dbAtualizada = versaoDbLocal != null && versaoDbLocal.equals(versaoDbRemota);
        boolean dataDbAtualizada = dataDbLocal != null && dataDbLocal.equals(dataDbRemota);

        return new VersaoOfflineDomain(aplicacaoAtualizada, dbAtualizada, dataDbAtualizada,
                versaoAplicacaoLocal, versaoAplicacaoRemota, versaoDbLocal, versaoDbRemota,
                dataDbLocal, dataDbRemota);
    }

    private static boolean versoesAplicacaoIguais(String local, String remota) {
        if (local == null || remota == null) {
            return false;
        }
        String semverLocal = extrairSemver(local);
        String semverRemota = extrairSemver(remota);
        if (semverLocal != null && semverRemota != null) {
            return semverLocal.equals(semverRemota);
        }
        return local.equals(remota);
    }

    private static String extrairSemver(String versao) {
        if (versao == null) return null;
        var matcher = SEMVER_PATTERN.matcher(versao);
        return matcher.find() ? matcher.group() : null;
    }
}
