package compilerxyz.main.gen;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.source_interface.InterfaceData;
import compilerxyz.main.eval.value.source_object.SourceDataMethodData;
import compilerxyz.main.eval.value.source_object.SourceObjectInterfaceImplData;
import compilerxyz.main.eval.value.source_object.SourceObjectInterfaceMethodImplData;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_object_new.TypeSourceObjectNewDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.LinkedHashMap;

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
        var method = new SourceDataMethodData(name);
        method.methodIdx = dtData.methods.size();
        dtData.methods.add(method);
        return new SourceObjectMethodResult(method);
    }

    public void impl_interface(InterfaceData ifaceRef, TOrderedMap<ConstKey, StagedExprCell> record) {
        var recordMap = new LinkedHashMap<ConstKey, Expr>();
        for (var entry : record.entrySet()) {
            recordMap.put(entry.getKey(), mustCast(Expr.class, entry.getValue().value()));
        }
        var dtData = this.type_;
        var impl = new SourceObjectInterfaceImplData(ifaceRef);
        impl.implIdx = dtData.interfaceImpls.size();
        for (int idx = 0; idx < ifaceRef.methods.size(); idx++) {
            var im = ifaceRef.methods.get(idx);
            var ckey = new ConstKey.Str(im.name);
            var implExpr = recordMap.get(ckey);
            if (implExpr == null) {
                throw new IllegalStateException("Missing implementation for interface method '" + im.name + "'");
            }
            var mImpl = new SourceObjectInterfaceMethodImplData(idx);
            mImpl.implExpr = Opt.some((Value) implExpr);
            mImpl.methodImplIdx = impl.methodImpls.size();
            impl.methodImpls.add(mImpl);
        }
        dtData.interfaceImpls.add(impl);
    }
}
