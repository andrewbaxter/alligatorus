package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.prelude.util.Opt;

public record AspectDropDynamicTuple(Tuple type) implements AspectDropDynamic {

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        var res = type.accessOrIndex(xx, loc, cv, Opt.none());
        if (res.hasErrors()) return res.reerr();
        return EResult.ok(res.requireValue("Tuple::drop").code());
    }
}
