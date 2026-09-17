package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.TargetCode;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.Opt;

public record CodeDynamic(CodeLoc loc, JavaBytecode code) implements TargetCode, JavaBytecode {
    public CodeDynamic(QExprId qExprId, JavaBytecode code) {
        this(new CodeLoc.Some(qExprId), code);
    }

    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public static EResult<Opt<CodeDynamic>> castOpt(QExprId loc, Code code) {
        return Code.codeCast(loc, code, CodeDynamic.class);
    }

    public static EResult<CodeDynamic> castRequired(QExprId loc, Code code, String message) {
        var res = Code.codeCast(loc, code, CodeDynamic.class);
        if (res.hasErrors()) return res.reerr();
        var opt = res.requireValue("CodeDynamic::castRequired");
        if (opt.optIsNone()) return EResult.err(loc, message);
        return EResult.ok(opt.optUnwrap());
    }
}
