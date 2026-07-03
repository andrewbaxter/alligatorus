package compilerxyz.eval.value.list;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
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
import compilerxyz.eval.value.any.ValueAnyConst;
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

public record TypeListDynamic(Type elementType)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectField,
        AspectInitialBind,
        AspectIterable,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> drop(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public JavaDataDescriptor consumeDynamicJvmClass() {
    return JavaDataDescriptor.fromInternalName(JavaInternalName.of("compilerxyz/util/TList"));
  }

  @Override
  public String typeDesc() {
    return "list(" + elementType.typeDesc() + ")";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.ListDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectIterable.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
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
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
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
              new CodeValue(Code.none(loc), Opt.some(new ValueAnyConst(f.type()))));
      case FieldSpec.Method m ->
          EResult.ok(
              new CodeValue(
                  self.code(),
                  Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), m.type()))));
    };
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs(xx).keySet());
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    var spec = fieldSpecs(xx).getOrNull(key);
    return spec == null ? Opt.none() : Opt.some(spec.fieldType());
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> bindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      throw new IllegalStateException(
          "TypeListDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
    }
    return EResult.ok(dyn);
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public Type elementType() {
    return elementType;
  }

  @Override
  public EResult<CodeValue> foreach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
    var baseConsumeRes = AspectConsumeDynamic.get(xx, loc, this);
    if (baseConsumeRes.hasErrors()) return baseConsumeRes.reerr();
    var baseDynRes =
        baseConsumeRes.requireValue("AspectConsumeDynamic.get").toDynamic(xx, loc, base);
    if (baseDynRes.hasErrors()) return baseDynRes.reerr();
    var baseDyn = baseDynRes.requireValue("toDynamic(base)");

    var iterableName = JavaInternalName.of("java/lang/Iterable");
    var iteratorName = JavaInternalName.of("java/util/Iterator");
    var descObject = JavaDataDescriptor.fromClass(Object.class);
    var descBool = JavaDataDescriptor.DESC_BOOL;
    var iterMethod =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
    var hasNextMethod = JavaMethodDescriptor.fromParts(descBool, TList.of());
    var nextMethod = JavaMethodDescriptor.fromParts(descObject, TList.of());

    var loopLabel = new LabelNode();
    var doneLabel = new LabelNode();
    var seq = new JavaBytecodeSequence();
    seq.add(baseDyn);
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iterableName, "iterator", iterMethod));
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(loopLabel)));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iteratorName, "hasNext", hasNextMethod));
    seq.add(
        new CodeDynamic(
            loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.IFEQ, doneLabel))));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iteratorName, "next", nextMethod));
    if (!(elementScopeId instanceof BindingId.Local localBinding)) {
      throw new IllegalStateException(
          "TypeListDynamic::foreach: element binding must be BindingId.Local");
    }
    // Iterator.next() returns Object; cast to the element type's declared JVM
    // class so the binding slot is typed as promised.
    if (elementType instanceof AspectConsumeDynamic elemConsume) {
      var elemDesc = elemConsume.consumeDynamicJvmClass();
      if (!elemDesc.value().equals(compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT.value())) {
        seq.add(JavaBytecodeUtils.cast(loc, elemDesc));
      }
    }
    seq.add(JavaBytecodeUtils.storeObj(loc, localBinding.key()));
    var bodyDyn = body.code().dynamic();
    if (bodyDyn != null) {
      seq.add(bodyDyn);
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    }
    seq.add(
        new CodeDynamic(
            loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.GOTO, loopLabel))));
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(doneLabel)));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    seq.add(JavaBytecodeUtils.literalNull(loc));

    var stackRes = AspectStackValueDynamic.get(xx, loc, new TypeNullDynamic());
    if (stackRes.hasErrors()) return stackRes.reerr();
    return EResult.ok(
        stackRes.requireValue("AspectStackValueDynamic.get").toValue(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
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
}
