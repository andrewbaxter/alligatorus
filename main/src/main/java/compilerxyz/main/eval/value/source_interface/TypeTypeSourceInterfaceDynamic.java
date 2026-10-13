package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Imported;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectArtifactObjectConst;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactDataSourceInterfaceTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6196L)
public record TypeTypeSourceInterfaceDynamic(Opt<TypeSourceInterfaceDynamic> type) implements Aspect, Type {

    public static final TypeTypeSourceInterfaceDynamic GENERIC = new TypeTypeSourceInterfaceDynamic(Opt.none());

    TypeSourceInterfaceDynamic require(String op) {
        if (type.optIsSome()) return type.optUnwrap();
        throw new IllegalStateException(
                "TypeTypeSourceInterfaceDynamic::" + op + " on the generic (interface-agnostic) descriptor");
    }

    @Override
    public String typeDesc() {
        if (type.optIsSome()) return type.optUnwrap().typeDesc();
        return "interface";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindTypeSourceInterfaceDynamic(this));
        }
        if (key.equals(GenAspectType.AspectId)) {
            return Opt.some(new AspectTypeTypeSourceInterfaceDynamic(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftTypeSourceInterfaceDynamic.INSTANCE);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldTypeSourceInterfaceDynamic(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicTypeSourceInterfaceDynamic(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactObjectConst.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY)
                || key.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)
                || key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(this);
        }
        if (type.optIsSome() && key.equals(type.optUnwrap().cell().identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_CELL_IDENTITY)
                        .optIsSome()
                || destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY)
                        .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            return EResult.ok(DynUtil.dynamicCastKeepingSource(cv, destType));
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_CELL_IDENTITY)
                .optIsSome()) {
            var sourceRes = DynUtil.materialize(xx, loc, cv);
            if (sourceRes.hasErrors()) return sourceRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out = b.invokeVirtual(
                    GenCodegenDescriptors.TYPE_SOURCE_INTERFACE_DYNAMIC_GET_INTERFACE_CELL,
                    b.reg(sourceRes.requireValue("sourceCode")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, destType));
        }
        return EResult.err(loc, "Cannot cast interface to " + destType.typeDesc());
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY,
                TypeTypeSourceInterfaceDynamic.class);
        if (oOpt.optIsNone()) {
            return false;
        }
        if (type.optIsSome() && oOpt.optUnwrap().type.optIsSome()) {
            return type.optUnwrap().typeIsEqual(xx, oOpt.optUnwrap().type.optUnwrap());
        }
        return type.optIsNone() && oOpt.optUnwrap().type.optIsNone();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.SOURCE_INTERFACE_TYPE_DYNAMIC);
    }

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx,
                QExprId loc,
                Opt<Type> ofType,
                Object self,
                Function<JsonValue, ArtifactBody> makeBody) {
            var tt = MustCast.mustCast(TypeTypeSourceInterfaceDynamic.class, self);
            Opt<Artifact> ifaceArt = Opt.none();
            if (tt.type.optIsSome()) {
                var iRes = Artifactable.exportArtifact(xx, loc, tt.type.optUnwrap());
                if (iRes.hasErrors()) return iRes.reerr();
                ifaceArt = Opt.some(iRes.requireValue("TypeTypeSourceInterfaceDynamic::export interface"));
            }
            var body = new ArtifactDataSourceInterfaceType(ifaceArt);
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactDataSourceInterfaceTypeCodec.toValue(body))));
        }

        @Override
        public EResult<Imported> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactDataSourceInterfaceType body;
            try {
                body = ArtifactDataSourceInterfaceTypeCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceInterfaceDynamic data: failed to parse ArtifactDataSourceInterfaceType: "
                                + e.getMessage());
            }
            if (body.interface_().optIsNone()) return Imported.ok(GENERIC);
            var iRes = ArtifactSupport.importArtifact(xx, loc, body.interface_().optUnwrap());
            if (iRes.hasErrors()) return iRes.reerr();
            return Imported.ok(new TypeTypeSourceInterfaceDynamic(Opt.some(MustCast.mustCast(
                    TypeSourceInterfaceDynamic.class,
                    iRes.requireValue("TypeTypeSourceInterfaceDynamic::import interface")
                            .data()))));
        }
    };
}
