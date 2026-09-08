package implementation;

import implementation.adapter.FormatterAdapter;
import implementation.adapter.InterpreterAdapter;
import implementation.adapter.LinterAdapter;
import interpreter.PrintScriptFormatter;
import interpreter.PrintScriptInterpreter;
import interpreter.PrintScriptLinter;

public class CustomImplementationFactory implements PrintScriptFactory {

    @Override
    public PrintScriptInterpreter interpreter() {
        return new InterpreterAdapter();
    }

    @Override
    public PrintScriptFormatter formatter() {
        return new FormatterAdapter();
    }

    @Override
    public PrintScriptLinter linter() {
        return new LinterAdapter();
    }
}
