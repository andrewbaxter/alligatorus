package compilerxyz.main.gen;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.main.eval.value.source_object.SourceDataMethodData;
import compilerxyz.main.eval.value.source_object.SourceObjectInterfaceImplData;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_object_new.TypeSourceObjectNewDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

@BuiltinFull(uniqueId = 6347L)
public final class SourceObjectBuilder {
    public final TypeSourceObjectDynamic type_;

    public final TypeSourceObjectNewDynamic new_;

    public SourceObjectBuilder(TypeSourceObjectDynamic type_) {
        this.type_ = type_;
        this.new_ = new TypeSourceObjectNewDynamic(type_);
    }

    public void set_description(String description) {
        if (this.type_.description.optIsSome()) {
            throw new IllegalStateException("Object already has a description");
        }
        this.type_.description = Opt.some(description);
    }

    public void set_inner(Type typeRef) {
        if (this.type_.underlyingType.optIsSome()) {
            throw new IllegalStateException("Object already has an inner type");
        }
        this.type_.underlyingType = Opt.some(typeRef);
    }

    public SourceObjectMethodResult func(String name) {
        var dtData = this.type_;
        var method = new SourceDataMethodData(name, "_" + dtData.methods.size());
        dtData.methods.add(method);
        return new SourceObjectMethodResult(method);
    }

    public void impl_interface(InterfaceCell ifaceRef, TOrderedMap<ConstKey, StagedExprCell> record) {
        var impls = new TOrderedMap<ConstKey, Expr>();
        for (var entry : record.entrySet()) {
            impls.put(entry.getKey(), mustCast(Expr.class, entry.getValue().value()));
        }
        for (var name : ifaceRef.methodNames()) {
            if (impls.getOrNull(new ConstKey.Str(name)) == null) {
                throw new IllegalStateException("Missing implementation for interface method '" + name + "'");
            }
        }
        this.type_.interfaceImpls.add(new SourceObjectInterfaceImplData(ifaceRef, impls));
    }
}
