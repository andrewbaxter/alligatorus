package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.QExprId;
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

    public EResult<Void> materialize(QExprId loc, String jvmNamePrefix) {
        if (pendingImpls.isEmpty()) {
            return EResult.ok(null);
        }
        for (var m : interface_.frozen().methods) {
            var key = new ConstKey.Str(m.name);
            var expr = pendingImpls.getOrNull(key);
            if (expr == null) {
                return EResult.err(loc, "Missing implementation for interface method '" + m.name + "'");
            }
            pendingImpls.remove(key);
            var impl = new SourceObjectInterfaceMethodImplData(m, jvmNamePrefix + "_method" + methodImpls.size());
            impl.implExpr = Opt.some(expr);
            methodImpls.add(impl);
        }
        if (!pendingImpls.isEmpty()) {
            var names = new TList<String>();
            for (var k : pendingImpls.keySet()) {
                names.add(k.constKeyToCanonical());
            }
            return EResult.err(loc, "Implementation of methods not on the interface: " + names);
        }
        return EResult.ok(null);
    }
}
