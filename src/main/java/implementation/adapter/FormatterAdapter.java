package implementation.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import formatter.Formatter;
import formatter.FormatterBuilderPS;
import interpreter.PrintScriptFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts PrintScript's Formatter to the TCK's PrintScriptFormatter.
 *
 * The TCK hands the configuration as a JSON stream with kebab-case keys and one rule per file,
 * while FormatterBuilderPS reads a YAML file path and requires every rule to be present. This
 * bridges the two: the keys it understands are translated and the rest are filled with
 * defaults, so a config that only enables one rule still builds.
 *
 * <p>Translation is a stopgap. The TCK also expects the formatter to preserve the spacing the
 * enabled rule does not govern, which an AST pretty-printer cannot do -- that needs the
 * formatter itself to be rewritten, not a richer adapter.
 */
public class FormatterAdapter implements PrintScriptFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        Path rulesFile = null;
        try {
            rulesFile = writeRulesFile(config);
            final Formatter formatter = new FormatterBuilderPS().build(rulesFile.toString(), version);
            final String source = new String(src.readAllBytes(), StandardCharsets.UTF_8);
            writer.write(formatter.format(source));
            writer.flush();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            deleteQuietly(rulesFile);
        }
    }

    /** Translates the TCK's JSON rules into the YAML file FormatterBuilderPS expects. */
    private static Path writeRulesFile(InputStream config) throws IOException {
        final Map<String, Object> tckRules =
                MAPPER.readValue(config, new TypeReference<Map<String, Object>>() {
                });

        final Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("spaceBeforeColon", false);
        rules.put("spaceAfterColon", true);
        rules.put("spaceAroundEquals", true);
        rules.put("lineBreakPrintln", 0);
        rules.put("conditionalIndentation", 4);

        copyBoolean(tckRules, "enforce-spacing-before-colon-in-declaration", rules, "spaceBeforeColon");
        copyBoolean(tckRules, "enforce-spacing-after-colon-in-declaration", rules, "spaceAfterColon");
        copyBoolean(tckRules, "enforce-spacing-around-equals", rules, "spaceAroundEquals");
        copyInt(tckRules, "line-breaks-after-println", rules, "lineBreakPrintln");
        copyInt(tckRules, "indent-inside-if", rules, "conditionalIndentation");

        // Expressed as its own rule by the TCK rather than as "spacing around equals = false".
        if (Boolean.TRUE.equals(tckRules.get("enforce-no-spacing-around-equals"))) {
            rules.put("spaceAroundEquals", false);
        }

        final StringBuilder yaml = new StringBuilder();
        rules.forEach((key, value) -> yaml.append(key).append(": ").append(value).append('\n'));

        final Path file = Files.createTempFile("printscript-tck-rules", ".yaml");
        Files.writeString(file, yaml.toString(), StandardCharsets.UTF_8);
        return file;
    }

    private static void copyBoolean(
            Map<String, Object> from, String fromKey, Map<String, Object> to, String toKey) {
        if (from.get(fromKey) instanceof Boolean value) {
            to.put(toKey, value);
        }
    }

    private static void copyInt(
            Map<String, Object> from, String fromKey, Map<String, Object> to, String toKey) {
        if (from.get(fromKey) instanceof Number value) {
            to.put(toKey, value.intValue());
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // A leftover temp file must not mask the formatting result.
        }
    }
}
