package compilerxyz.main.gen;

import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodBuilding;
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
        var building = this.type_.cell().building();
        if (building.description.optIsSome()) {
            throw new IllegalStateException("Interface already has a description");
        }
        building.description = Opt.some(description);
    }

    public SourceInterfaceMethodResult func(String name) {
        var method = new InterfaceMethodBuilding(name);
        this.type_.cell().building().methods.add(method);
        return new SourceInterfaceMethodResult(method);
    }

    public Unique aspect_id() {
        var aspectId = this.type_.cell().aspectId();
        if (aspectId.optIsNone()) {
            throw new IllegalStateException("Interface is not an aspect (set_is_aspect was not called)");
        }
        return aspectId.optUnwrap();
    }

    public void set_is_aspect(EvalContext vx, boolean isAspect) {
        var building = this.type_.cell().building();
        building.isAspect = isAspect;
        if (isAspect && building.aspectId.optIsNone()) {
            building.aspectId = Opt.some(DynRuntime.freshUnique(vx));
        }
    }
}
