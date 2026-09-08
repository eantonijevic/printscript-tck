package implementation.adapter;

import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;
import linter.BrokenRule;
import linter.Linter;
import linter.LinterOutput;
import linter.LinterVersion;
import token.TokenPosition;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Adapts PrintScript's Linter to the TCK's PrintScriptLinter. */
public class LinterAdapter implements PrintScriptLinter {

    @Override
    public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
        try {
            final LinterVersion linterVersion = LinterVersion.Companion.fromString(version);
            if (linterVersion == null) {
                throw new IllegalArgumentException("Unsupported version: " + version);
            }

            final Linter linter = new Linter(linterVersion);
            linter.readJson(new String(config.readAllBytes(), StandardCharsets.UTF_8));

            final LinterOutput output = linter.check(Pipeline.nodes(src, version));
            for (BrokenRule brokenRule : output.getBrokenRules()) {
                handler.reportError(describe(brokenRule));
            }
        } catch (Throwable throwable) {
            handler.reportError(Pipeline.describe(throwable));
        }
    }

    /** Violations are reported in 1-based coordinates, like the rest of PrintScript's errors. */
    private static String describe(BrokenRule brokenRule) {
        final TokenPosition position = brokenRule.getErrorPosition();
        return brokenRule.getRuleDescription()
                + " (line " + (position.getRow() + 1) + ", column " + (position.getColumn() + 1) + ")";
    }
}
