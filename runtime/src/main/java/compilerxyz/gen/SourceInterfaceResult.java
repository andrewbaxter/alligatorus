package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import java.lang.Boolean;
import java.lang.IllegalStateException;
import java.lang.String;

@BuiltinPushStruct
public final class SourceInterfaceResult {
    public final TypeSourceInterfaceDynamic type_;

    public SourceInterfaceResult(TypeSourceInterfaceDynamic type_) {
        this.type_ = type_;
    }

    public static Type dynamicType() {
        return GenObjectTypes.sourceInterfaceResult();
    }

    public InterfaceData get_ref() {
        return this.type_.getInterfaceData();
    }

    public void set_description(ExprContext xx, String description) {
        var ifaceData = this.type_.getInterfaceData();
        if (ifaceData.description != null) {
            throw new IllegalStateException("Interface already has a description");
        }
        ifaceData.description = description;
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
        if (isAspect && data.aspectId == null) {
            data.aspectId = compilerxyz.eval.dynamic.DynRuntime.freshUnique(xx);
        }
    }
}
