package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.util.Opt;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_object.SourceDataMethodData;
import compilerxyz.eval.value.source_object.SourceObjectInterfaceImplData;
import compilerxyz.eval.value.source_object.SourceObjectInterfaceMethodImplData;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.value.source_object_new.TypeSourceObjectNewDynamic;
import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.util.Map;

@BuiltinPushStruct
public final class SourceObjectBuilder {
    public final TypeSourceObjectDynamic type_;

    public final TypeSourceObjectNewDynamic new_;

    public SourceObjectBuilder(TypeSourceObjectDynamic type_) {
        this.type_ = type_;
        this.new_ = new TypeSourceObjectNewDynamic(type_);
    }

    public static Type dynamicType() {
        return GenObjectTypes.sourceObjectBuilder();
    }

    public void set_description(ExprContext xx, String description) {
        if (this.type_.description.optIsSome()) {
            throw new IllegalStateException("Object already has a description");
        }
        this.type_.description = Opt.some(description);
    }

    public void set_inner(ExprContext xx, Type typeRef) {
        if (this.type_.underlyingType.optIsSome()) {
            throw new IllegalStateException("Object already has an inner type");
        }
        this.type_.underlyingType = Opt.some(typeRef);
    }

    public SourceObjectMethodResult func(ExprContext xx, String name) {
        var dtData = this.type_;
        var method = new SourceDataMethodData(name);
        method.methodIdx = dtData.methods.size();
        dtData.methods.add(method);
        return new SourceObjectMethodResult(method);
    }

    public void impl_interface(ExprContext xx, InterfaceData ifaceRef, Map<Object, Object> record) {
        var recordMap = DynRuntime.recordToConstKeyExprMap(record);
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
