package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;

@BuiltinLight(uniqueId = 6137L)
public record TypeSourceObjectConst(TypeSourceObjectDynamic dynamic)
        implements Type, AspectArtifact, AspectType, ExporterAny {

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_OBJECT_VALUE_CONST;
    }

    @Override
    public Type aspectTypeType(Value value) {
        if (!(value instanceof ValueObjectConst voc)) {
            throw new IllegalStateException("TypeSourceObjectConst::type_: expected ValueObjectConst, got "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        if (!(voc.data() instanceof Type t)) {
            throw new IllegalStateException("TypeSourceObjectConst::type_: wrapped data is not a Type, got "
                    + (voc.data() == null ? "null" : voc.data().getClass().getName()));
        }
        return t;
    }

    @Override
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(dynamic);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(new TypeTypeSourceObjectConst(dynamic), dynamic));
    }

    @Override
    public String typeDesc() {
        return dynamic.typeDesc();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectType.AspectId)) {
            for (var impl : dynamic.interfaceImpls) {
                if (impl.interface_.builtinKind.optUnwrapOr(null) == BuiltinInterfaceKind.TYPE) {
                    return Opt.some(this);
                }
            }
            return Opt.none();
        }
        return dynamic.typeAspect(xx, key);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_CONST_IDENTITY, TypeSourceObjectConst.class);
        return oOpt.optIsSome() && dynamic.typeIsEqual(xx, oOpt.optUnwrap().dynamic);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return dynamic.typeCanCastFrom(xx, loc, sourceType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return dynamic.typeCastFrom(xx, loc, cv);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return dynamic.typeCanCastTo(xx, loc, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return dynamic.typeCastTo(xx, loc, cv, destType);
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
