package compilerxyz.eval.value.source_interface;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
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
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenIdentities;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.TypeId;

public record TypeInterfaceConst(InterfaceData data)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectType,
        ExporterAny {

  @Override
  public TypeId typeId() {
    return new TypeId.DefsetMember(data.set.defSetId, uniqueId(data.identity));
  }

  private static long uniqueId(Unique u) {
    return switch (u) {
      case Unique.Builtin b -> b.id();
      case Unique.Source s -> s.id();
    };
  }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(
        compilerxyz.eval.value.source_interface.InterfaceData.class);
  }

  @Override
  public String typeDesc() {
    var d = data.description;
    if (d != null) return d;
    return "interface";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceInterfaceConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(data.identity)
        || key.equals(GenIdentities.INTERFACE_CONST_IDENTITY)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectType.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.SOURCE_INTERFACE_CONST;
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
              "TypeInterfaceConst::exporterRootDelegateSelfExport: recursive export_artifact + "
                  + "type_source_exports port deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeInterfaceConst::exporterRootDelegateSelfFromArtifactData: recursive "
                  + "import_independent_artifact + type_source_exports port deferred");
        }
      };

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeInterfaceConst::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str keyStr)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on interface");
    }
    String keyName = keyStr.value();
    InterfaceMethodData match = null;
    for (var m : data.methods) {
      if (m.name.equals(keyName)) {
        match = m;
        break;
      }
    }
    if (match == null) {
      return EResult.err(loc, "Field '" + keyName + "' not found on interface");
    }
    var argType = match.argType;
    var retType = match.retType;
    if (argType == null || retType == null) {
      return EResult.err(
          loc, "Interface method '" + keyName + "' is missing arg or return type");
    }
    var descObjObj =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
            TList.of(
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class)));
    var fieldType =
        new compilerxyz.eval.value.function.TypeFunctionDynamic(
            argType,
            retType,
            compilerxyz.eval.dynamic.JavaInternalName.of("PENDING_source_interface_class"),
            match.fieldName,
            descObjObj,
            false);
    return EResult.ok(
        new CodeValue(
            self.code(),
            Opt.some(ValueDynamic.stack(fieldType))));
  }

  @Override
  public TList<ConstKey> aspectFieldFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldFieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    for (var m : data.methods) {
      if (m.name.equals(s.value())) {
        var argType = m.argType;
        var retType = m.retType;
        if (argType != null && retType != null) {
          var descObjObj =
              compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                  compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
                  TList.of(
                      compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
                      compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class)));
          return Opt.some(
              new compilerxyz.eval.value.function.TypeFunctionDynamic(
                  argType,
                  retType,
                  compilerxyz.eval.dynamic.JavaInternalName.of("PENDING_source_interface_class"),
                  m.fieldName,
                  descObjObj,
                  false));
        }
        return Opt.none();
      }
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeInterfaceConst::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType instanceof compilerxyz.eval.value.interface_spec.TypeInterfaceSpec
        || destType.typeAspect(xx, compilerxyz.gen.GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()
        || destType.typeAspect(xx, compilerxyz.gen.GenIdentities.TYPE_TYPE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType instanceof compilerxyz.eval.value.interface_spec.TypeInterfaceSpec) {
      var sourceCode = cv.code().dynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc, "cast interface_const → interface_spec: source has no runtime code");
      }
      var args = new TList<CodeDynamic>();
      args.add(sourceCode);
      var call =
          compilerxyz.gen.GenDynMethods.INTERFACE_REF_TO_INTERFACE_SPEC.emitInvocation(
              loc, xx.vx, args);
      return EResult.ok(
          new CodeValue(
              Code.of(loc, call),
              Opt.some(
                  ValueDynamic.stack(
                      new compilerxyz.eval.value.interface_spec.TypeInterfaceSpec()))));
    }
    if (destType.typeAspect(xx, compilerxyz.gen.GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()
        || destType.typeAspect(xx, compilerxyz.gen.GenIdentities.TYPE_TYPE_IDENTITY).optIsSome()) {
      return EResult.ok(
          new CodeValue(cv.code(), Opt.some(ValueDynamic.stack(destType))));
    }
    return EResult.err(loc, "Cannot cast interface_const to " + destType.typeDesc());
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeInterfaceConst(data));
  }

  @Override
  public Type type_(Value value) {
    return InterfaceData.dynamicType();
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
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
