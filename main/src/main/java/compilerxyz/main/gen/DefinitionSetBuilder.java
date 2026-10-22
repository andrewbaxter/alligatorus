package compilerxyz.main.gen;

import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalContextInternal;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynCompileErrors;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.source_function.SourceFunctionData;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.main.eval.value.source_interface.InterfaceBuilding;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.eval.value.source_macro.SourceMacroData;
import compilerxyz.main.eval.value.source_macro.TypeSourceMacroConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.prelude.annotations.BuiltinEvalContext;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6341L)
public final class DefinitionSetBuilder {
    public final DefinitionSet inner;

    public DefinitionSetBuilder(DefinitionSet inner) {
        this.inner = inner;
    }

    @BuiltinEvalContext
    public SourceFuncResult func(EvalContext vx, String name) {
        var data =
                new SourceFunctionData(name, this.inner, DynRuntime.freshUnique(vx), "_" + this.inner.functions.size());
        this.inner.functions.add(data);
        return new SourceFuncResult(new TypeSourceFunctionConst(data));
    }

    @BuiltinEvalContext
    public SourceMacroResult constFunc(EvalContext vx, String name) {
        var function =
                new SourceFunctionData(name, this.inner, DynRuntime.freshUnique(vx), "_" + this.inner.functions.size());
        this.inner.functions.add(function);
        return new SourceMacroResult(
                new TypeSourceMacroConst(new SourceMacroData(DynRuntime.freshUnique(vx), function)));
    }

    @BuiltinEvalContext
    public SourceObjectBuilder object(EvalContext vx, String name) {
        var identity = DynRuntime.freshUnique(vx);
        var data = new TypeSourceObjectDynamic(
                this.inner.defSetId,
                name,
                identity,
                this.inner,
                "SourceObject$" + setNameSuffix() + this.inner.objects.size());
        this.inner.objects.add(data);
        return new SourceObjectBuilder(data);
    }

    @BuiltinEvalContext
    public SourceUnionBuilder union(EvalContext vx) {
        var identity = DynRuntime.freshUnique(vx);
        var data = new TypeSourceUnionDynamic(
                this.inner, identity, "SourceUnion$" + setNameSuffix() + this.inner.unions.size());
        this.inner.unions.add(data);
        return new SourceUnionBuilder(data);
    }

    @BuiltinEvalContext
    public SourceInterfaceResult interface_(EvalContext vx) {
        var identity = DynRuntime.freshUnique(vx);
        var iface = new InterfaceCell(
                Opt.some(this.inner),
                identity,
                "compilerxyz.main.eval.gen.Interface$" + setNameSuffix() + this.inner.interfaces.size(),
                new InterfaceCell.State.Building(new InterfaceBuilding()));
        this.inner.interfaces.add(iface);
        return new SourceInterfaceResult(new TypeSourceInterfaceDynamic(iface));
    }

    private String setNameSuffix() {
        var compileId = this.inner.defSetId.compileId();
        return compileId.importState().value() + "_" + compileId.source().value() + "_" + this.inner.defSetId.index()
                + "_";
    }

    @BuiltinEvalContext
    public void finish(EvalContext vx) {
        var incomplete = this.inner.incomplete();
        if (incomplete.optIsSome()) {
            throw new IllegalStateException(incomplete.optUnwrap());
        }
        var r = this.inner.finish(builderContext(vx));
        if (r.optIsSome()) {
            throw new DynCompileErrors(r.optUnwrap());
        }
    }

    static ExprContext builderContext(EvalContext vx) {
        var builderVx = new EvalContext(EvalContextInternal.ix(vx));
        return new ExprContext(
                new Scope(builderVx),
                new TargetDynamic(Opt.none()),
                builderVx,
                EvalContextInternal.ix(vx).importState,
                Opt.some(new TypeStringConst()),
                Opt.none());
    }

    static void requireArgBindable(ExprContext xx, Type arg, String what) {
        var tupleOpt = GenAspectTuple.getMaybe(xx, arg);
        var recordOpt = GenAspectRecord.getMaybe(xx, arg);
        if (tupleOpt.optIsSome()) {
            for (var et : tupleOpt.optUnwrap().aspectTupleElementTypes()) {
                if (GenAspectInitialBind.getMaybe(xx, et).optIsNone()) {
                    throw new IllegalStateException(
                            what + ": arg tuple element type " + et.typeDesc() + " cannot be bound dynamically");
                }
            }
        } else if (recordOpt.optIsSome()) {
            for (var entry : recordOpt.optUnwrap().aspectRecordFieldTypes().entrySet()) {
                if (GenAspectInitialBind.getMaybe(xx, entry.getValue()).optIsNone()) {
                    throw new IllegalStateException(what + ": arg record field '"
                            + entry.getKey().constKeyToCanonical() + "' type "
                            + entry.getValue().typeDesc()
                            + " cannot be bound dynamically");
                }
            }
        } else if (GenAspectInitialBind.getMaybe(xx, arg).optIsNone()) {
            throw new IllegalStateException(what + ": arg type " + arg.typeDesc() + " cannot be bound dynamically");
        }
    }
}
