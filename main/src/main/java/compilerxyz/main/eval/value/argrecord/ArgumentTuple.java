package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.stream.Collectors;

@BuiltinLight(uniqueId = 6101L)
public record ArgumentTuple(TList<Binding> elements, TList<Type> elementTypes) implements Type, Value, Aspect {

    @Override
    public String typeDesc() {
        var inner = elementTypes.stream().map(Type::typeDesc).collect(Collectors.joining(", "));
        return "argument_tuple(" + inner + ")";
    }

    public Type valueType() {
        return this;
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.ARGUMENT_TUPLE_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(AspectFieldArgumentTuple.INSTANCE);
        }
        if (key.equals(GenAspectTuple.AspectId)) {
            return Opt.some(new AspectTupleArgumentTuple(this));
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.ARGUMENT_TUPLE_IDENTITY, ArgumentTuple.class);
        if (oOpt.optIsNone()) return false;
        var o = oOpt.optUnwrap();
        if (elementTypes.size() != o.elementTypes.size()) return false;
        for (int i = 0; i < elementTypes.size(); i++) {
            if (!elementTypes.get(i).typeIsEqual(xx, o.elementTypes.get(i))) return false;
        }
        return true;
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.none();
    }

    @Override
    public Opt<ArtifactExporter> valueArtifactExporter(ExprContext xx) {
        return typeArtifactExporter(xx);
    }
}
