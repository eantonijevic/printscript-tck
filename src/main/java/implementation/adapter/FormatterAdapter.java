package implementation.adapter;

import formatter.Formatter;
import formatter.FormatterBuilderPS;
import interpreter.PrintScriptFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Adapts PrintScript's Formatter to the TCK's PrintScriptFormatter.
 *
 * The formatter reads the TCK's rule names natively and treats an absent rule as "leave it
 * alone", so the configuration stream is handed over untouched.
 */
public class FormatterAdapter implements PrintScriptFormatter {

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        try {
            final Formatter formatter = new FormatterBuilderPS().build(config, version);
            writer.write(formatter.format(new String(src.readAllBytes(), StandardCharsets.UTF_8)));
            writer.flush();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
