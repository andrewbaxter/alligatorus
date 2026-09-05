package compilerxyz.main.gen;

import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodBuilding;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.prelude.annotations.BuiltinEvalContext;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6346L)
public final class SourceInterfaceResult {
    public final TypeSourceInterfaceDynamic type;

    public SourceInterfaceResult(TypeSourceInterfaceDynamic type) {
        this.type = type;
    }

    public void setDescription(String description) {
        var building = this.type.cell().building();
        if (building.description.optIsSome()) {
            throw new IllegalStateException("Interface already has a description");
        }
        building.description = Opt.some(description);
    }

    public SourceInterfaceMethodResult func(String name) {
        var method = new InterfaceMethodBuilding(name);
        this.type.cell().building().methods.add(method);
        return new SourceInterfaceMethodResult(method);
    }

    public Unique aspectId() {
        var aspectId = this.type.cell().aspectId();
        if (aspectId.optIsNone()) {
            throw new IllegalStateException("Interface is not an aspect (set_is_aspect was not called)");
        }
        return aspectId.optUnwrap();
    }

    @BuiltinEvalContext
    public void setIsAspect(EvalContext vx, boolean isAspect) {
        var building = this.type.cell().building();
        building.isAspect = isAspect;
        if (isAspect && building.aspectId.optIsNone()) {
            building.aspectId = Opt.some(DynRuntime.freshUnique(vx));
        }
    }
}
