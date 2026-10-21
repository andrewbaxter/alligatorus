package compilerxyz.main.gen;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.main.eval.value.source_object.SourceDataMethodData;
import compilerxyz.main.eval.value.source_object.SourceObjectInterfaceImplData;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_object_new.TypeSourceObjectNewDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

@BuiltinFull(uniqueId = 6347L)
public final class SourceObjectBuilder {
    private final TypeSourceObjectDynamic object;

    public final Type type;

    public final Value new_;

    public SourceObjectBuilder(TypeSourceObjectDynamic type) {
        this.object = type;
        this.type = type;
        this.new_ = new ValueEmpty(new TypeSourceObjectNewDynamic(type));
    }

    public void impl(Type inner, Opt<String> description) {
        if (this.object.underlyingType.optIsSome()) {
            throw new IllegalStateException("Object already has an impl");
        }
        this.object.underlyingType = Opt.some(inner);
        this.object.description = description;
    }

    public SourceObjectMethodResult func(String name) {
        var dtData = this.object;
        var method = new SourceDataMethodData(name, "_" + dtData.methods.size());
        dtData.methods.add(method);
        return new SourceObjectMethodResult(method);
    }

    public void implInterface(InterfaceCell ifaceRef, TOrderedMap<ConstKey, StagedExprCell> record) {
        var impls = new TOrderedMap<ConstKey, Expr>();
        for (var entry : record.entrySet()) {
            impls.put(entry.getKey(), mustCast(Expr.class, entry.getValue().value()));
        }
        var names = new TList<ConstKey>();
        for (var name : ifaceRef.methodNames()) {
            var key = new ConstKey.Str(name);
            if (impls.getOpt(key).optIsNone()) {
                throw new IllegalStateException("Missing implementation for interface method '" + name + "'");
            }
            names.add(key);
        }
        var extra = new TList<String>();
        for (var key : impls.keySet()) {
            if (!names.contains(key)) extra.add(key.constKeyToCanonical());
        }
        if (!extra.isEmpty()) {
            throw new IllegalStateException("Implementation of methods not on the interface: " + extra);
        }
        ifaceRef.markImplemented();
        this.object.interfaceImpls.add(new SourceObjectInterfaceImplData(ifaceRef, impls));
    }
}
