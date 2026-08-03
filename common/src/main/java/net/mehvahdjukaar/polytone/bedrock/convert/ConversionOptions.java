package net.mehvahdjukaar.polytone.bedrock.convert;

import net.mehvahdjukaar.polytone.bedrock.model.BedrockDescription;
import net.mehvahdjukaar.polytone.bedrock.molang.MolangTranslator;

import java.util.Locale;

/**
 * @param namespace  namespace of the generated particles, also the pack folder they land in
 * @param path       file name of the generated particle, without extension
 * @param translator how Molang source becomes a Polytone expression
 * @param validate   run the generated json back through {@code CustomParticleType.CODEC}
 */
public record ConversionOptions(String namespace, String path, MolangTranslator translator, boolean validate) {

    public static ConversionOptions of(String namespace, String path) {
        return new ConversionOptions(sanitize(namespace), sanitize(path), MolangTranslator.PASSTHROUGH, true);
    }

    /** Reuses the effect's own identifier, which is already namespaced the same way ours are. */
    public static ConversionOptions from(BedrockDescription description) {
        String namespace = description.namespace();
        return of(namespace.isEmpty() ? "bedrock" : namespace, description.name());
    }

    public ConversionOptions withTranslator(MolangTranslator translator) {
        return new ConversionOptions(namespace, path, translator, validate);
    }

    public ConversionOptions withValidation(boolean validate) {
        return new ConversionOptions(namespace, path, translator, validate);
    }

    /** Same sanitising, for effect ids referenced from elsewhere (event targets, mostly). */
    public static String identifierOf(String bedrockId) {
        int colon = bedrockId.indexOf(':');
        String namespace = colon < 0 ? "bedrock" : bedrockId.substring(0, colon);
        String path = colon < 0 ? bedrockId : bedrockId.substring(colon + 1);
        return sanitize(namespace) + ":" + sanitize(path);
    }

    public String particleId() {
        return namespace + ":" + path;
    }

    public String emitterId() {
        return namespace + ":" + path + "_emitter";
    }

    /** Identifiers only accept a narrow character set, and Bedrock names are not held to it. */
    private static String sanitize(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        for (char c : raw.toLowerCase(Locale.ROOT).toCharArray()) {
            out.append(switch (c) {
                case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                     'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                     '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '.', '-', '/' -> c;
                default -> '_';
            });
        }
        return out.isEmpty() ? "unnamed" : out.toString();
    }
}
