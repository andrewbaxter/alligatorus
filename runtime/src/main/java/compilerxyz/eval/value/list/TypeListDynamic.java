package compilerxyz.eval.value.list;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectIterable;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.Code;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeInstructionObj;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.gen.AspectStringDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.function.Function;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeListDynamic(Type elementType)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectIterable,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public TypeId typeId() { return new TypeId.Composite(GenTypeIds.TYPE_LIST_DYNAMIC_TYPE_ID, TList.of(elementType.typeId())); }


  @Override
  public JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass() {
    return JavaDataDescriptor.fromInternalName(JavaInternalName.of("compilerxyz/util/TList"));
  }

  @Override
  public String typeDesc() {
    return "list(" + elementType.typeDesc() + ")";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.ListDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectDropDynamic.ID)) {
      return AspectDropDynamic.getMaybe(xx, elementType).optIsSome()
          ? Opt.some(this)
          : Opt.none();
    }
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectIterable.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.LIST_DYNAMIC;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          return EResult.err(
              loc,
              "TypeListDynamic::exporterRootDelegateSelfExport: recursive export_artifact (eval/artifact.rs) port"
                  + " deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeListDynamic::exporterRootDelegateSelfFromArtifactData: recursive import_type_artifact"
                  + " (eval/artifact.rs) port deferred");
        }
      };

  private sealed interface FieldSpec {
    Type fieldType();

    record Factory(Type type) implements FieldSpec {
      @Override
      public Type fieldType() {
        return type;
      }
    }

    record Method(Type type) implements FieldSpec {
      @Override
      public Type fieldType() {
        return type;
      }
    }
  }

  private static final JavaInternalName DYN_RUNTIME = JavaInternalName.fromClass(DynRuntime.class);
  private static final JavaMethodDescriptor DESC_LIST_PUSH =
      JavaMethodDescriptor.fromParts(
          JavaDataDescriptor.fromClass(Object.class),
          TList.of(
              JavaDataDescriptor.fromInternalName(
                  JavaInternalName.of("compilerxyz/util/TList")),
              JavaDataDescriptor.fromClass(Object.class)));
  private static final JavaMethodDescriptor DESC_LIST_JOIN =
      JavaMethodDescriptor.fromParts(
          JavaDataDescriptor.fromClass(String.class),
          TList.of(
              JavaDataDescriptor.fromInternalName(
                  JavaInternalName.of("compilerxyz/util/TList")),
              JavaDataDescriptor.fromClass(String.class)));

  private TMap<ConstKey, FieldSpec> fieldSpecs(ExprContext xx) {
    var m = TMap.<ConstKey, FieldSpec>of();
    m.put(new ConstKey.Str("new"), new FieldSpec.Factory(new TypeListNewDynamic(elementType)));
    m.put(
        new ConstKey.Str("push"),
        new FieldSpec.Method(
            new compilerxyz.eval.value.function.TypeFunctionDynamic(
                elementType,
                new TypeNullDynamic(),
                DYN_RUNTIME,
                "listPush",
                DESC_LIST_PUSH,
                false)));
    if (AspectStringDynamic.getMaybe(xx, elementType).optIsSome()) {
      m.put(
          new ConstKey.Str("join"),
          new FieldSpec.Method(
              new compilerxyz.eval.value.function.TypeFunctionDynamic(
                  new TypeStringDynamic(),
                  new TypeStringDynamic(),
                  DYN_RUNTIME,
                  "listJoin",
                  DESC_LIST_JOIN,
                  false)));
    }
    return m;
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeListDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    var spec = fieldSpecs(xx).getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on list");
    }
    return switch (spec) {
      case FieldSpec.Factory f ->
          EResult.ok(
              new CodeValue(Code.none(loc), Opt.some(new ValueObjectConst(f.type()))));
      case FieldSpec.Method m ->
          EResult.ok(
              new CodeValue(
                  self.code(),
                  Opt.some(ValueDynamic.stack(m.type()))));
    };
  }

  @Override
  public TList<ConstKey> aspectFieldFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs(xx).keySet());
  }

  @Override
  public Opt<Type> aspectFieldFieldTypeOpt(ExprContext xx, ConstKey key) {
    var spec = fieldSpecs(xx).getOrNull(key);
    return spec == null ? Opt.none() : Opt.some(spec.fieldType());
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      throw new IllegalStateException(
          "TypeListDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
    }
    return EResult.ok(dyn);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public Type aspectIterableElementType() {
    return elementType;
  }

  @Override
  public EResult<CodeValue> aspectIterableForeach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
    var baseConsumeRes = AspectConsumeDynamic.get(xx, loc, this);
    if (baseConsumeRes.hasErrors()) return baseConsumeRes.reerr();
    var baseDynRes =
        baseConsumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, base);
    if (baseDynRes.hasErrors()) return baseDynRes.reerr();
    var baseDyn = baseDynRes.requireValue("aspectConsumeDynamicToDynamic(base)");

    var iterableName = JavaInternalName.of("java/lang/Iterable");
    var iteratorName = JavaInternalName.of("java/util/Iterator");
    var iterMethod =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
    var setupSeq = new JavaBytecodeSequence();
    setupSeq.add(baseDyn);
    setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iterableName, "iterator", iterMethod));
    var iteratorSetup = new CodeDynamic(loc, setupSeq);

    if (!(elementScopeId instanceof BindingId.Local localBinding)) {
      throw new IllegalStateException(
          "TypeListDynamic::foreach: element binding must be BindingId.Local");
    }

    var elementDecode = elementDecodeCast(xx, loc);
    return EResult.ok(
        DynUtil.foreachSkeleton(
            loc, iteratorSetup, elementDecode, localBinding, Opcodes.ASTORE, body.code()));
  }

  private CodeDynamic elementDecodeCast(ExprContext xx, QExprId loc) {
    var decodeSeq = new JavaBytecodeSequence();
    var elemConsumeOpt = AspectConsumeDynamic.getMaybe(xx, elementType);
    if (elemConsumeOpt instanceof Opt.Some<AspectConsumeDynamic> elemConsumeSome) {
      var elemDesc = elemConsumeSome.value().aspectConsumeDynamicConsumeDynamicJvmClass();
      var d = elemDesc.value();
      if (!d.equals(compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT.value())) {
        char c = d.charAt(0);
        if (c == 'L' || c == '[') {
          decodeSeq.add(JavaBytecodeUtils.cast(loc, elemDesc));
        }
      }
    }
    return new CodeDynamic(loc, decodeSeq);
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    var elemDropRes = AspectDropDynamic.get(xx, loc, elementType);
    if (elemDropRes.hasErrors()) return elemDropRes.reerr();
    var elemDrop = elemDropRes.requireValue("TypeListDynamic::drop element AspectDropDynamic");
    if (!(cv.value() instanceof Opt.Some<Value> some)
        || !(some.value() instanceof compilerxyz.eval.value.ValueDynamic vd)) {
      return EResult.ok(Code.none(loc));
    }
    var elementSlot =
        new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var loadElement =
        new CodeDynamic(
            loc,
            new compilerxyz.eval.dynamic.JavaBytecodeStoreLoad(Opcodes.ALOAD, elementSlot.key()));
    var elementCv =
        new CodeValue(
            Code.of(loc, loadElement),
            Opt.some(compilerxyz.eval.value.ValueDynamic.stack(elementType)));
    var elemDropCodeRes = elemDrop.aspectDropDynamicDrop(xx, loc, elementCv);
    if (elemDropCodeRes.hasErrors()) return elemDropCodeRes.reerr();
    var elemDropCode = elemDropCodeRes.requireValue("TypeListDynamic::drop element drop");

    var iterableName = JavaInternalName.of("java/lang/Iterable");
    var iteratorName = JavaInternalName.of("java/util/Iterator");
    var iterMethod =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
    var setupSeq = new JavaBytecodeSequence();
    setupSeq.add(vd.source().realize(xx, loc));
    setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iterableName, "iterator", iterMethod));
    var iteratorSetup = new CodeDynamic(loc, setupSeq);

    var elementDecode = elementDecodeCast(xx, loc);

    var loop =
        DynUtil.foreachSkeleton(
            loc, iteratorSetup, elementDecode, elementSlot, Opcodes.ASTORE, elemDropCode);
    var loopCode = loop.code().dynamic();
    var finalSeq = new JavaBytecodeSequence();
    if (loopCode != null) finalSeq.add(loopCode);
    finalSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, finalSeq)));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
