package compilerxyz.main.eval.value.array;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Imported;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.AspectSetDynamicDefault;
import compilerxyz.main.eval.binding.BindingIteratorIndexed;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.AspectDynamicConstOf;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import java.util.function.Function;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6461L)
public final class TypeArrayDynamic<T extends Type> implements Aspect, Type {

    public final T elementType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeArrayDynamic(T elementType) {
        this.elementType = elementType;
    }

    @Override
    public String typeDesc() {
        return "array(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectDynamicConst.AspectId)) {
            return Opt.some(new AspectDynamicConstOf(new TypeArrayConst<>(elementType)));
        }
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindArrayDynamic<>(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_ARRAY_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectDropDynamic.AspectId)) {
            return GenAspectDropDynamic.getMaybe(xx, elementType).optIsSome()
                    ? Opt.some(new AspectDropDynamicArrayDynamic<>(this))
                    : Opt.none();
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicArrayDynamic<>(this));
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableArrayDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldArrayDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicArrayDynamic<>(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(new AspectBindDynamicArrayDynamic<>(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return switch (GenAspectLift.getMaybe(xx, elementType)) {
                case Opt.Some<AspectLift> elementLift -> Opt.some(new AspectLiftArrayDynamic<>(this));
                case Opt.None<AspectLift> none -> Opt.none();
            };
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindArrayDynamic<>(this));
        }
        return Opt.none();
    }

    sealed interface FieldSpec {
        Type fieldSpecFieldType();

        record Length() implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return new TypeIntDynamic();
            }
        }

        record Method(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }
    }

    IMap<ConstKey, FieldSpec> fieldSpecs() {
        return IMap.ofEntries(
                new Pair<>(new ConstKey.Str("len"), new FieldSpec.Length()),
                new Pair<>(new ConstKey.Str("get"), new FieldSpec.Method(new TypeArrayGetDynamic(this))));
    }

    JavaDataDescriptor descriptor(ExprContext xx) {
        return JvmSort.descriptor(xx, elementType).arrayOf();
    }

    JavaBytecodeReg length(JavaBytecodeBuilder b, JavaBytecodeReg array) {
        return b.op(Opcodes.ARRAYLENGTH, JavaDataDescriptor.DESC_INT, array);
    }

    JavaBytecodeReg element(ExprContext xx, JavaBytecodeBuilder b, JavaBytecodeReg array, JavaBytecodeReg index) {
        var elementDesc = JvmSort.descriptor(xx, elementType);
        return b.op(elementDesc.arrayLoadOpcode(), elementDesc, array, index);
    }

    EResult<CodeValue> foreach(
            ExprContext xx,
            QExprId loc,
            JavaBytecodeBuilder b,
            JavaBytecodeReg base,
            BindingIteratorIndexed slots,
            CodeDynamic elementBind,
            Code body) {
        var arrayDesc = descriptor(xx);
        var loopKey = new JumpKey();
        var doneKey = new JumpKey();
        b.store(slots.baseSlot(), base);
        b.store(slots.indexSlot(), b.constInt(0));
        b.land(loopKey);
        var index = b.load(slots.indexSlot(), JavaDataDescriptor.DESC_INT);
        var size = length(b, b.load(slots.baseSlot(), arrayDesc));
        b.branch(Opcodes.IF_ICMPGE, doneKey, index, size);
        b.add(elementBind);
        var bodyRes = b.addCode(loc, body);
        if (bodyRes.optIsSome()) return EResult.errs(bodyRes.optUnwrap());
        var next = b.op(
                Opcodes.IADD,
                JavaDataDescriptor.DESC_INT,
                b.load(slots.indexSlot(), JavaDataDescriptor.DESC_INT),
                b.constInt(1));
        b.store(slots.indexSlot(), next);
        b.jump(loopKey);
        b.land(doneKey);
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }

    JavaBytecodeReg loopElement(ExprContext xx, JavaBytecodeBuilder b, BindingIteratorIndexed slots) {
        return element(
                xx,
                b,
                b.load(slots.baseSlot(), descriptor(xx)),
                b.load(slots.indexSlot(), JavaDataDescriptor.DESC_INT));
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_ARRAY_DYNAMIC_IDENTITY, TypeArrayDynamic.class);
        return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return typeIsEqual(xx, sourceType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.ARRAY_DYNAMIC);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx,
                QExprId loc,
                Opt<Type> ofType,
                Object self,
                Function<JsonValue, ArtifactBody> makeBody) {
            var array = MustCast.mustCast(TypeArrayDynamic.class, self);
            return TypeArrayConst.exportType(xx, loc, array.artifactIdCell, array.elementType, makeBody);
        }

        @Override
        public EResult<Imported> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return TypeArrayConst.importType(xx, loc, id, data, (elementType, idValue) -> {
                var out = new TypeArrayDynamic<>(elementType);
                out.artifactIdCell.set(idValue);
                return out;
            });
        }
    };
}
