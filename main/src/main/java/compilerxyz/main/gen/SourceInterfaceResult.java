package compilerxyz.main.gen;

import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6346L)
public final class SourceInterfaceResult {
    public final TypeSourceInterfaceDynamic type_;

    public SourceInterfaceResult(TypeSourceInterfaceDynamic type_) {
        this.type_ = type_;
    }

    public void set_description(String description) {
        var ifaceData = this.type_.getInterfaceData();
        if (ifaceData.description.optIsSome()) {
            throw new IllegalStateException("Interface already has a description");
        }
        ifaceData.description = Opt.some(description);
    }

    public SourceInterfaceMethodResult func(String name) {
        var ifaceData = this.type_.getInterfaceData();
        var method = new InterfaceMethodData(name);
        ifaceData.methods.add(method);
        return new SourceInterfaceMethodResult(method);
    }

    public Unique aspect_id() {
        var data = this.type_.getInterfaceData();
        if (data.aspectId.optIsNone()) {
            throw new IllegalStateException("Interface is not an aspect (set_is_aspect was not called)");
        }
        return data.aspectId.optUnwrap();
    }

    public void set_is_aspect(EvalContext vx, boolean isAspect) {
        var data = this.type_.getInterfaceData();
        data.isAspect = isAspect;
        if (isAspect && data.aspectId.optIsNone()) {
            data.aspectId = Opt.some(DynRuntime.freshUnique(vx));
        }
    }
}
