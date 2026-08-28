package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.util.Opt;

@BuiltinPushStruct
public final class SourceInterfaceResult {
    public final TypeSourceInterfaceDynamic type_;

    public SourceInterfaceResult(TypeSourceInterfaceDynamic type_) {
        this.type_ = type_;
    }

    public static Type dynamicType() {
        return GenObjectTypes.sourceInterfaceResult();
    }

    public void set_description(ExprContext xx, String description) {
        var ifaceData = this.type_.getInterfaceData();
        if (ifaceData.description.optIsSome()) {
            throw new IllegalStateException("Interface already has a description");
        }
        ifaceData.description = Opt.some(description);
    }

    public SourceInterfaceMethodResult func(ExprContext xx, String name) {
        var ifaceData = this.type_.getInterfaceData();
        var method = new InterfaceMethodData(name);
        ifaceData.methods.add(method);
        return new SourceInterfaceMethodResult(method);
    }

    public void set_is_aspect(ExprContext xx, boolean isAspect) {
        var data = this.type_.getInterfaceData();
        data.isAspect = isAspect;
        if (isAspect && data.aspectId.optIsNone()) {
            data.aspectId = Opt.some(DynRuntime.freshUnique(xx));
        }
    }
}
