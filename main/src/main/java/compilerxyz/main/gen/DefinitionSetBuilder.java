package compilerxyz.main.gen;

import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.eval.dynamic.DynCompileErrors;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.source_function.SourceFunctionData;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.main.eval.value.source_interface.InterfaceBuilding;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.prelude.annotations.BuiltinEvalContext;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

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
        var loc = new QExprId(new SourceId(-1L), -1L);
        var finishVx = new EvalContext(vx.ix);
        var r = this.inner.finish(
                new ExprContext(
                        new Scope(finishVx),
                        new TargetDynamic(),
                        finishVx,
                        vx.ix.importState,
                        Opt.some(new TypeStringConst())),
                loc);
        if (r.hasErrors()) {
            var errs = r.errorsOpt();
            throw new DynCompileErrors(errs.optUnwrapOr(new TList<>()));
        }
    }
}
