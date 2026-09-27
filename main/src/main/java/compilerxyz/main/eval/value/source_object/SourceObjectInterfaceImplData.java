package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class SourceObjectInterfaceImplData {
    public final InterfaceCell interface_;
    public final TList<SourceObjectInterfaceMethodImplData> methodImpls;

    private final TOrderedMap<ConstKey, Expr> pendingImpls;

    public SourceObjectInterfaceImplData(InterfaceCell interface_, TOrderedMap<ConstKey, Expr> impls) {
        this.interface_ = interface_;
        this.methodImpls = new TList<>();
        this.pendingImpls = impls;
    }

    public SourceObjectInterfaceImplData(InterfaceCell interface_) {
        this(interface_, new TOrderedMap<>());
    }

    public void materialize(String jvmNamePrefix) {
        if (pendingImpls.isEmpty()) {
            return;
        }
        for (var m : interface_.frozen().methods) {
            var key = new ConstKey.Str(m.name);
            var exprOpt = pendingImpls.getOpt(key);
            if (exprOpt.optIsNone()) {
                throw new AssertionError("Interface method '" + m.name + "' has no implementation (checked at impl)");
            }
            pendingImpls.remove(key);
            var impl = new SourceObjectInterfaceMethodImplData(m, jvmNamePrefix + "_method" + methodImpls.size());
            impl.implExpr = Opt.some(exprOpt.optUnwrap());
            methodImpls.add(impl);
        }
        if (!pendingImpls.isEmpty()) {
            throw new AssertionError("Implementations of methods not on the interface (checked at impl)");
        }
    }
}
