package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.SourceId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.DynCompileErrors;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.SourceInterfaceEmitter;
import compilerxyz.eval.value.source_function.SourceFunctionData;
import compilerxyz.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.util.TList;

@BuiltinPushStruct
public final class DefinitionSetBuilder {
    public final DefinitionSet inner;

    public DefinitionSetBuilder(DefinitionSet inner) {
        this.inner = inner;
    }

    public static Type dynamicType() {
        return GenObjectTypes.definitionSetBuilder();
    }

    public SourceFuncResult func(ExprContext xx, String name) {
        var data = new SourceFunctionData(name, this.inner, DynRuntime.freshUnique(xx));
        data.funcIdx = this.inner.functions.size();
        this.inner.functions.add(data);
        return new SourceFuncResult(new TypeSourceFunctionConst(data));
    }

    public SourceObjectBuilder object(ExprContext xx, String name) {
        var identity = DynRuntime.freshUnique(xx);
        var data = new TypeSourceObjectDynamic(this.inner.defSetId, name, identity, this.inner);
        data.objIdx = this.inner.objects.size();
        this.inner.objects.add(data);
        return new SourceObjectBuilder(data);
    }

    public SourceUnionBuilder union(ExprContext xx) {
        var identity = DynRuntime.freshUnique(xx);
        var data = new TypeSourceUnionDynamic(this.inner, identity);
        data.unionIdx = this.inner.unions.size();
        this.inner.unions.add(data);
        return new SourceUnionBuilder(data);
    }

    public SourceInterfaceResult interface_(ExprContext xx) {
        var identity = DynRuntime.freshUnique(xx);
        var iface = new InterfaceData(this.inner.defSetId, identity, this.inner);
        var ifaceIndex = (long) this.inner.interfaces.size();
        this.inner.interfaces.add(iface);
        iface.compiledBinaryName = SourceInterfaceEmitter.binaryNameFor(this.inner.defSetId, ifaceIndex);
        return new SourceInterfaceResult(new TypeSourceInterfaceDynamic(iface));
    }

    public void finish(ExprContext xx) {
        var _loc = new QExprId(new SourceId(-1L), -1L);
        var _r = this.inner.finish(xx, _loc);
        if (_r.hasErrors()) {
            var _errs = _r.errorsOrNull();
            throw new DynCompileErrors(_errs.optUnwrapOr(new TList<>()));
        }
    }
}
