package compilerxyz.eval.value.source_union;

import compilerxyz.annotations.BuiltinFull;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Counter;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectUnion;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectUnion;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder;
import compilerxyz.eval.Code;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.MatchDynamic;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectFieldUseDynamic;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6021L)
public final class TypeSourceUnionDynamic
    implements
        Type,
        AspectFieldUseDynamic,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectStackValueDynamic,
        AspectUnion,
        MatchDynamic,
        ExporterAny {

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final Unique identity;
  public final Counter nextTagId;
  public final TList<SourceUnionVariant> variants;
  public final ArtifactIdHolder artifactIdCell =
      new ArtifactIdHolder();

  public Opt<String> compiledBaseBinaryName = Opt.none();
  public Opt<TList<String>> compiledVariantBinaryNames = Opt.none();
  public byte @org.jspecify.annotations.Nullable [] compiledBaseBytecode;
  public Opt<TList<byte[]>> compiledVariantBytecode = Opt.none();

  public TypeSourceUnionDynamic(DefinitionSet set, Unique identity) {
    this.set = set;
    this.identity = identity;
    this.nextTagId = new Counter();
    this.variants = new TList<>();
  }

  @SuppressWarnings("NullAway")
  public TypeSourceUnionDynamic(Unique identity) {
    this.set = (DefinitionSet) null;
    this.identity = identity;
    this.nextTagId = new Counter();
    this.variants = new TList<>();
  }

  public void initSetReflectively(DefinitionSet set) {
    try {
      var f = TypeSourceUnionDynamic.class.getDeclaredField("set");
      f.setAccessible(true);
      f.set(this, set);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "TypeSourceUnionDynamic.initSetReflectively: failed to write final 'set' field", e);
    }
  }

  public EResult<Void> finish(ExprContext xx, QExprId loc, long unionIndex) {
    if (compiledBaseBinaryName.optIsSome()) {
      return EResult.ok(null);
    }
    var _seuBaseBinaryName =
        compilerxyz.eval.dynamic.SourceUnionEmitter.baseBinaryNameFor(set.defSetId, unionIndex);
    var _seuBaseInternalName = _seuBaseBinaryName.replace('.', '/');
    var _seuVariantBinaryNames = new TList<String>(variants.size());
    var _seuVariantInternalNames = new TList<String>(variants.size());
    for (int _seuI = 0; _seuI < variants.size(); _seuI++) {
      var _seuVbn =
          compilerxyz.eval.dynamic.SourceUnionEmitter.variantBinaryNameFor(
              set.defSetId, unionIndex, _seuI);
      _seuVariantBinaryNames.add(_seuVbn);
      _seuVariantInternalNames.add(_seuVbn.replace('.', '/'));
    }
    {
      var _ebCw =
          new ClassWriter(
              ClassWriter.COMPUTE_FRAMES
                  | ClassWriter.COMPUTE_MAXS);
      _ebCw.visit(
          Opcodes.V17,
          Opcodes.ACC_PUBLIC
              | Opcodes.ACC_ABSTRACT
              | Opcodes.ACC_SUPER,
          _seuBaseInternalName,
          null,
          compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT.value(),
          null);
      for (var _ebVin : _seuVariantInternalNames) {
        _ebCw.visitPermittedSubclass(_ebVin);
      }
      var _ebCtor =
          _ebCw.visitMethod(Opcodes.ACC_PROTECTED, "<init>", "()V", null, null);
      _ebCtor.visitCode();
      _ebCtor.visitVarInsn(Opcodes.ALOAD, 0);
      _ebCtor.visitMethodInsn(
          Opcodes.INVOKESPECIAL,
          compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT.value(),
          "<init>",
          "()V",
          false);
      _ebCtor.visitInsn(Opcodes.RETURN);
      _ebCtor.visitMaxs(0, 0);
      _ebCtor.visitEnd();
      _ebCw.visitEnd();
      compiledBaseBytecode = _ebCw.toByteArray();
    }
    var _seuVariantBytecode = new TList<byte[]>(variants.size());
    for (int _seuI = 0; _seuI < variants.size(); _seuI++) {
      var _seuVariant = variants.get(_seuI);
      var _seuPayloadDesc =
          compilerxyz.eval.dynamic.SourceUnionEmitter.payloadDescriptor(xx, _seuVariant.type);
      {
        var _evVariantInternalName = _seuVariantInternalNames.get(_seuI);
        var _evCw =
            new ClassWriter(
                ClassWriter.COMPUTE_FRAMES
                    | ClassWriter.COMPUTE_MAXS);
        _evCw.visit(
            Opcodes.V17,
            Opcodes.ACC_PUBLIC
                | Opcodes.ACC_FINAL
                | Opcodes.ACC_SUPER,
            _evVariantInternalName,
            null,
            _seuBaseInternalName,
            null);
        _evCw
            .visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                compilerxyz.eval.dynamic.SourceUnionEmitter.PAYLOAD_FIELD,
                _seuPayloadDesc.value(),
                null,
                null)
            .visitEnd();
        var _evCtorDesc = "(" + _seuPayloadDesc.value() + ")V";
        var _evCtor =
            _evCw.visitMethod(
                Opcodes.ACC_PUBLIC, "<init>", _evCtorDesc, null, null);
        _evCtor.visitCode();
        _evCtor.visitVarInsn(Opcodes.ALOAD, 0);
        _evCtor.visitMethodInsn(
            Opcodes.INVOKESPECIAL, _seuBaseInternalName, "<init>", "()V", false);
        _evCtor.visitVarInsn(Opcodes.ALOAD, 0);
        int _evLoadOp =
            switch (_seuPayloadDesc.value().charAt(0)) {
              case 'Z', 'B', 'C', 'S', 'I' -> Opcodes.ILOAD;
              case 'J' -> Opcodes.LLOAD;
              case 'F' -> Opcodes.FLOAD;
              case 'D' -> Opcodes.DLOAD;
              default -> Opcodes.ALOAD;
            };
        _evCtor.visitVarInsn(_evLoadOp, 1);
        _evCtor.visitFieldInsn(
            Opcodes.PUTFIELD,
            _evVariantInternalName,
            compilerxyz.eval.dynamic.SourceUnionEmitter.PAYLOAD_FIELD,
            _seuPayloadDesc.value());
        _evCtor.visitInsn(Opcodes.RETURN);
        _evCtor.visitMaxs(0, 0);
        _evCtor.visitEnd();
        _evCw.visitEnd();
        _seuVariantBytecode.add(_evCw.toByteArray());
      }
    }
    compiledBaseBinaryName = Opt.some(_seuBaseBinaryName);
    compiledVariantBinaryNames = Opt.some(_seuVariantBinaryNames);
    compiledVariantBytecode = Opt.some(_seuVariantBytecode);
    return EResult.ok(null);
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }


  @Override
  public String typeDesc() {
    return "union_dyn";
  }

  @Override
  public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
    return new TypeTypeSourceUnionConst(this).artifactableExportArtifact(xx, loc);
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(identity)
        || key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_UNION_DYNAMIC_IDENTITY)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(compilerxyz.gen.GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(compilerxyz.gen.GenAspectFieldUseDynamic.AspectId)
        || key.equals(GenAspectUnion.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx,
      QExprId loc,
      ValueDynamicSource source) {
    return compilerxyz.eval.EResult.ok(
        new CodeValue(
            Code.of(loc, compilerxyz.eval.dynamic.DynUtil.renderSource(xx, loc, source)),
            compilerxyz.eval.EvalOutcome.ofValue(
                new ValueDynamic(source, this))));
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(
      ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().codeDynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeSourceUnionDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public TList<AspectUnion.Variant> aspectUnionVariants() {
    var out = new TList<AspectUnion.Variant>();
    for (var v : variants) {
      out.add(new AspectUnion.Variant(v.tag, v.jsTag, v.type));
    }
    return out;
  }

  private JavaInternalName variantInternal(QExprId loc, int variantIndex) {
    var unionIndex = set.unions.indexOf(this);
    return JavaInternalName.of(
        compilerxyz.eval.dynamic.SourceUnionEmitter.variantBinaryNameFor(
                set.defSetId, unionIndex, variantIndex)
            .replace('.', '/'));
  }

  @Override
  public EResult<Void> matchDynamicPrepare(ExprContext xx, QExprId loc) {
    if (set.unions.indexOf(this) < 0) {
      return EResult.err(loc, "Match: source union not in owning DefinitionSet");
    }
    xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    return EResult.ok(null);
  }

  @Override
  public EResult<CodeDynamic> matchDynamicTest(ExprContext xx, QExprId loc, int variantIndex) {
    return EResult.ok(
        compilerxyz.eval.dynamic.JavaBytecodeUtils.instanceOf(loc, variantInternal(loc, variantIndex)));
  }

  @Override
  public EResult<CodeDynamic> matchDynamicExtract(
      ExprContext xx, QExprId loc, int variantIndex, Type variantType) {
    var internal = variantInternal(loc, variantIndex);
    var payloadDesc =
        compilerxyz.eval.dynamic.SourceUnionEmitter.payloadDescriptor(xx, variantType);
    var seq = new JavaBytecodeSequence();
    seq.add(
        compilerxyz.eval.dynamic.JavaBytecodeUtils.cast(
            loc, JavaDataDescriptor.fromInternalName(internal)));
    seq.add(
        compilerxyz.eval.dynamic.JavaBytecodeUtils.accessField(
            loc, internal, compilerxyz.eval.dynamic.SourceUnionEmitter.PAYLOAD_FIELD, payloadDesc));
    return EResult.ok(new CodeDynamic(loc, seq));
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(new TypeTypeSourceUnionConst(this), this));
  }

  @Override
  public compilerxyz.eval.EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }
  @Override
  public compilerxyz.eval.EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    return compilerxyz.eval.EResult.ok(cv);
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, identity).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, identity).optIsSome();
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType.typeAspect(xx, identity).optIsSome();
  }
  @Override
  public compilerxyz.eval.EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return compilerxyz.eval.EResult.ok(cv);
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }
}
