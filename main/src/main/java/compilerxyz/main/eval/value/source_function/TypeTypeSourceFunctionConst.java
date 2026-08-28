package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.empty_.ValueEmpty;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;

@BuiltinLight(uniqueId = 6135L)
public final class TypeTypeSourceFunctionConst {
    private TypeTypeSourceFunctionConst() {}

    public static Type dynamicType() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final Impl INSTANCE = new Impl();
    }

    private static final class Impl
            implements Type,
                    AspectLift,
                    AspectConsumeDynamic,
                    AspectBind,
                    AspectBindDynamic,
                    AspectInitialBind,
                    AspectDropDynamicLight {

        @Override
        public String typeDesc() {
            return "definition_set_function";
        }

        @Override
        public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
            if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_FUNCTION_CONST_IDENTITY)) {
                return Opt.some(this);
            }
            if (key.equals(GenAspectLift.AspectId)
                    || key.equals(GenAspectConsumeDynamic.AspectId)
                    || key.equals(GenAspectBind.AspectId)
                    || key.equals(GenAspectBindDynamic.AspectId)
                    || key.equals(GenAspectInitialBind.AspectId)
                    || key.equals(GenAspectDropDynamicLight.AspectId)) {
                return Opt.some(this);
            }
            return Opt.none();
        }

        @Override
        public EResult<AspectBind.BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
            return DynUtil.bindViaTarget(xx, loc, cv);
        }

        @Override
        public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
            return DynUtil.bindDynamicDefault(xx, loc, cv);
        }

        @Override
        public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
            return new BindingDynamicBinding(dynamicKey, this);
        }

        @Override
        public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
            return DynUtil.dropDefault(xx, loc, cv);
        }

        @Override
        public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
            return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
        }

        @Override
        public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
            return JavaDataDescriptor.fromClass(TypeSourceFunctionConst.class);
        }

        @Override
        public boolean typeIsEqual(ExprContext xx, Type other) {
            return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_FUNCTION_CONST_IDENTITY)
                    .optIsSome();
        }

        @Override
        public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
            return false;
        }

        @Override
        public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
            return EResult.err(loc, typeDesc() + "::typeCastFrom: not supported");
        }

        @Override
        public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
            return false;
        }

        @Override
        public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
            return EResult.err(loc, typeDesc() + "::typeCastTo: not supported");
        }

        @Override
        public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
            if (externalHostValue.optIsNone()) {
                return EResult.err(loc, typeDesc() + "::lift: unexpected host null");
            }
            var rawHostValue = externalHostValue.optUnwrap();
            if (rawHostValue instanceof ValueObjectConst voc && voc.data() instanceof TypeSourceFunctionConst func) {
                return EResult.ok(new ValueEmpty(func));
            }
            if (rawHostValue instanceof TypeSourceFunctionConst func) {
                return EResult.ok(new ValueEmpty(func));
            }
            return EResult.err(
                    loc,
                    typeDesc() + "::lift: unexpected host "
                            + rawHostValue.getClass().getName());
        }

        @Override
        public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
            return Type.exportArtifact(xx, loc, this);
        }
    }
}
