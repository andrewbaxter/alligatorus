package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.TargetCode;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenType;

public record CodeDynamic(CodeLoc loc, JavaBytecode code) implements TargetCode, JavaBytecode {
    public CodeDynamic(QExprId qExprId, JavaBytecode code) {
        this(new CodeLoc.Some(qExprId), code);
    }

    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    @Override
    public <T> T javaBytecodeDispatch(JavaBytecode.Dispatcher<T> dispatcher) {
        return code.javaBytecodeDispatch(dispatcher);
    }
}
