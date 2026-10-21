package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstEval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodBuilding;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodData;
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

    @BuiltinEvalContext
    public void impl(EvalContext vx, boolean isAspect, Opt<String> description) {
        var building = this.type.cell().building();
        if (building.implProvided) {
            throw new IllegalStateException("Interface already has an impl");
        }
        building.implProvided = true;
        building.isAspect = isAspect;
        if (isAspect) {
            building.aspectId = Opt.some(DynRuntime.freshUnique(vx));
        }
        building.description = description;
    }

    @BuiltinEvalContext
    public void func(EvalContext vx, String name, Type arg, Opt<Type> ret) {
        var building = this.type.cell().building();
        if (building.implemented) {
            throw new IllegalStateException(
                    "Cannot add method '" + name + "' to an interface that has already been implemented");
        }
        var what = "Interface method '" + name + "'";
        var builderXx = DefinitionSetBuilder.builderContext(vx);
        DefinitionSetBuilder.requireArgBindable(builderXx, arg, what);
        var recordOpt = GenAspectRecord.getMaybe(builderXx, arg);
        if (recordOpt.optIsSome()
                && recordOpt
                        .optUnwrap()
                        .aspectRecordFieldTypes()
                        .getOpt(ConstEval.PIPE_SELF_KEY)
                        .optIsSome()) {
            throw new IllegalStateException(
                    what + ": arg record field 'self' is reserved for the receiver of a method ref call");
        }
        building.methods.add(new InterfaceMethodBuilding(
                name, arg, ret, InterfaceMethodData.deriveDescriptor(builderXx, arg, ret, what)));
    }

    public Unique aspectId() {
        var aspectId = this.type.cell().aspectId();
        if (aspectId.optIsNone()) {
            throw new IllegalStateException("Interface is not an aspect (impl was called with is_aspect false)");
        }
        return aspectId.optUnwrap();
    }
}
