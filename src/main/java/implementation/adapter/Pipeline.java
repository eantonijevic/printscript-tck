package implementation.adapter;

import ast.ASTNode;
import diagnostics.PrintScriptException;
import kotlin.sequences.Sequence;
import lexer.Lexer;
import lexer.TokenMapper;
import parser.Parser;
import token.Token;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Lexer + Parser wiring shared by the three adapters.
 *
 * The chain stays lazy: the source is read as the consumer asks for the next statement, which
 * is what lets the large-file test run inside the TCK's 7 MB heap.
 */
final class Pipeline {

    private Pipeline() {
    }

    static Sequence<ASTNode> nodes(InputStream src, String version) {
        final Sequence<Token> tokens = new Lexer(new TokenMapper(version)).convertToTokens(src);
        return Parser.Companion.forVersion(version).execute(tokens);
    }

    static Iterator<ASTNode> statements(InputStream src, String version) {
        return nodes(src, version).iterator();
    }

    /**
     * Message reported to the TCK's ErrorHandler.
     *
     * PrintScript errors carry their position, so they are described with it. Everything else
     * falls back to the throwable's own message: notably OutOfMemoryError, whose "Java heap
     * space" is exactly what InterpreterLargeFileTest expects to be reported.
     */
    static String describe(Throwable throwable) {
        if (throwable instanceof PrintScriptException) {
            return ((PrintScriptException) throwable).describe();
        }
        final String message = throwable.getMessage();
        return (message == null || message.isBlank()) ? throwable.getClass().getSimpleName() : message;
    }
}
