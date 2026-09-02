package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinPushStruct;

@BuiltinPushStruct
public final class IfBranch {
    public final CodeValue condition;
    public final CodeValue body;

    public IfBranch(CodeValue condition, CodeValue body) {
        this.condition = condition;
        this.body = body;
    }

    public static Type dynamicType() {
        return GenObjectTypes.ifBranch();
    }
}
