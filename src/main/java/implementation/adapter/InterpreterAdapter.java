package implementation.adapter;

import ast.ASTNode;
import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.Interpreter;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Adapts PrintScript's Interpreter to the TCK's PrintScriptInterpreter.
 *
 * The TCK reports errors instead of propagating them, so every failure -- lexing, parsing and
 * evaluation alike -- is caught here and handed to the ErrorHandler.
 */
public class InterpreterAdapter implements PrintScriptInterpreter {

    @Override
    public void execute(
            InputStream src,
            String version,
            PrintEmitter emitter,
            ErrorHandler handler,
            InputProvider provider
    ) {
        try {
            // Printer and Reader are single-method Kotlin interfaces, so the TCK's collaborators
            // adapt to them directly.
            final Interpreter interpreter =
                    Interpreter.Companion.forVersion(version, emitter::print, provider::input);

            final Iterator<ASTNode> statements = Pipeline.statements(src, version);
            while (statements.hasNext()) {
                interpreter.execute(statements.next());
            }
        } catch (Throwable throwable) {
            // Throwable, not Exception: the large-file test expects OutOfMemoryError to be
            // reported as an error rather than to escape.
            handler.reportError(Pipeline.describe(throwable));
        }
    }
}
