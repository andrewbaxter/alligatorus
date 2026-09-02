package compilerxyz.main.gen;

import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.eval.dynamic.DynCompileErrors;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.dynamic.SourceInterfaceEmitter;
import compilerxyz.main.eval.value.source_function.SourceFunctionData;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.main.eval.value.source_interface.InterfaceData;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6341L)
public final class DefinitionSetBuilder {
    public final DefinitionSet inner;

    public DefinitionSetBuilder(DefinitionSet inner) {
        this.inner = inner;
    }

    public SourceFuncResult func(EvalContext vx, String name) {
        var data = new SourceFunctionData(name, this.inner, DynRuntime.freshUnique(vx));
        data.funcIdx = this.inner.functions.size();
        this.inner.functions.add(data);
        return new SourceFuncResult(new TypeSourceFunctionConst(data));
    }

    public SourceObjectBuilder object(EvalContext vx, String name) {
        var identity = DynRuntime.freshUnique(vx);
        var data = new TypeSourceObjectDynamic(this.inner.defSetId, name, identity, this.inner);
        data.objIdx = this.inner.objects.size();
        this.inner.objects.add(data);
        return new SourceObjectBuilder(data);
    }

    public SourceUnionBuilder union(EvalContext vx) {
        var identity = DynRuntime.freshUnique(vx);
        var data = new TypeSourceUnionDynamic(this.inner, identity);
        data.unionIdx = this.inner.unions.size();
        this.inner.unions.add(data);
        return new SourceUnionBuilder(data);
    }

    public SourceInterfaceResult interface_(EvalContext vx) {
        var identity = DynRuntime.freshUnique(vx);
        var iface = new InterfaceData(this.inner.defSetId, identity, this.inner);
        var ifaceIndex = (long) this.inner.interfaces.size();
        this.inner.interfaces.add(iface);
        iface.compiledBinaryName = SourceInterfaceEmitter.binaryNameFor(this.inner.defSetId, ifaceIndex);
        return new SourceInterfaceResult(new TypeSourceInterfaceDynamic(iface));
    }

    public void finish(EvalContext vx) {
        var _loc = new QExprId(new SourceId(-1L), -1L);
        var _r = this.inner.finish(ExprContext.freshRoot(vx.ix), _loc);
        if (_r.hasErrors()) {
            var _errs = _r.errorsOpt();
            throw new DynCompileErrors(_errs.optUnwrapOr(new TList<>()));
        }
    }
}
