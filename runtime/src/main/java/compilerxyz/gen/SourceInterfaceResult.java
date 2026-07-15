package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceConst;
import java.lang.Boolean;
import java.lang.IllegalStateException;
import java.lang.String;

@BuiltinPushStruct(uniqueId = 3005L)
public final class SourceInterfaceResult {
    public final TypeSourceInterfaceConst type_;

    public SourceInterfaceResult(TypeSourceInterfaceConst type_) {
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

    public void set_is_aspect(ExprContext xx, Boolean isAspect) {
        this.type_.getInterfaceData().isAspect = isAspect;
    }
}
