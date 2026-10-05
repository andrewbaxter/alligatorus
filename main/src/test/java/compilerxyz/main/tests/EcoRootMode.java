package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;

public enum EcoRootMode {
    APP,
    IR;

    public SourceExpr ecoRootModeFinish(EcoMb e, SourceExpr llvm, SourceExpr ll) {
        return switch (this) {
            case APP -> e.ret(e.call(e.fld(llvm, "build"), e.callM0(ll, "render")));
            case IR -> e.call(e.mb.log(), e.fld(e.callM0(ll, "render"), "ir"));
        };
    }
}
