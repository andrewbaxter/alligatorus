package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.any.TypeAnyDynamic;
import compilerxyz.eval.value.aspect.TypeAspectDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.binding.TypeAnyBindingDynamic;
import compilerxyz.eval.value.binding.TypeAnyTargetDynamic;
import compilerxyz.eval.value.code.TypeCodeDynHandleDynamic;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceConst;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.value_source_object.ValueSourceObjectConst;

public record TypeSourceObjectDynamic(SourceObjectData data)
    implements
        Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectField,
        AspectInitialBind,
        AspectLift,
        ExporterAny {

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> drop(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.dynamic.JavaInternalName consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT;
  }

  @Override
  public String typeDesc() {
    var d = data.description;
    if (d != null) return d;
    return "object(" + (data.name != null ? data.name : "?") + ")";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceObjectDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(data.identity)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectLift.ID)) {
      return Opt.some(this);
    }

    for (var impl : data.interfaceImpls) {
      if (key.equals(impl.interface_.identity)) {
        return Opt.some(this);
      }
    }
    return Opt.none();
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
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeSourceObjectDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeSourceObjectDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str keyStr)) {
      return EResult.err(
          loc,
          "Field "
              + ck.toCanonical()
              + " not found on object (underlying-type Record fallback deferred)");
    }
    String keyName = keyStr.value();
    String fieldName = null;
    Type argType = null;
    Type retType = null;
    for (var m : data.methods) {
      if (m.name.equals(keyName)) {
        if (m.retType == null) {
          throw new IllegalStateException("Data type method missing return type");
        }
        retType = m.retType;
        argType = m.argType != null ? m.argType : new TypeNullDynamic();
        fieldName = m.fieldName;
        break;
      }
    }
    if (fieldName == null) {
      outer:
      for (var impl : data.interfaceImpls) {
        for (var method : impl.interface_.methods) {
          if (method.name.equals(keyName)) {
            if (method.argType == null) {
              throw new IllegalStateException("Interface method missing arg type");
            }
            if (method.retType == null) {
              throw new IllegalStateException("Interface method missing return type");
            }
            argType = method.argType;
            retType = method.retType;
            fieldName = method.fieldName;
            break outer;
          }
        }
      }
    }
    if (fieldName == null) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic::field: field '"
              + keyName
              + "' not found on object (underlying-type Record fallback deferred)");
    }
    if (!(self.value() instanceof Opt.Some<Value> selfSome)
        || !(selfSome.value() instanceof ValueDynamic selfVd)) {
      return EResult.err(
          loc, "TypeSourceObjectDynamic::field: receiver is not a ValueDynamic");
    }

    if (data.set != null) {
      xx.vx.definitionSets.putIfAbsent(data.set.defSetId, data.set);
    }
    var descObjObj =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
            TList.of(
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class)));
    var fieldType =
        new compilerxyz.eval.value.function.TypeMethodDynamic(
            this,
            argType,
            retType,
            compilerxyz.eval.dynamic.JavaInternalName.of("PENDING_source_object_class"),
            fieldName,
            descObjObj,
            false);
    return EResult.ok(
        new CodeValue(
            self.code(),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), fieldType))));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return Opt.none();
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeSourceObjectDynamic(data));
  }

  @Override
  public EResult<Value> lift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    var underlyingType = data.underlyingType;
    if (underlyingType == null) {
      return EResult.err(
          loc, "TypeSourceObjectDynamic::lift: object underlying type not finalised");
    }
    var underlyingLiftOpt = AspectLift.getMaybe(xx, underlyingType);
    if (underlyingLiftOpt.optIsNone()) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic::lift: underlying type ("
              + underlyingType.typeDesc()
              + ") missing AspectLift");
    }
    var underlyingLift = underlyingLiftOpt.optUnwrap();
    Object underlyingHost;

    if (externalHostValue instanceof SourceObjectInstance) {
      try {
        var field = externalHostValue.getClass().getField(SourceObjectEmitter.DATA_INSTANCE_FIELD);
        underlyingHost = field.get(externalHostValue);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new AssertionError(
            "TypeSourceObjectDynamic::lift: source-object instance "
                + externalHostValue.getClass().getName()
                + " missing "
                + SourceObjectEmitter.DATA_INSTANCE_FIELD
                + " field",
            e);
      }
    } else {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic::lift: expected SourceObjectInstance, got "
              + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
    }
    return underlyingLift.lift(xx, loc, underlyingHost)
        .map(
            underlying ->
                new ValueSourceObjectConst(
                    data, underlying));
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    if (destType.typeAspect(xx, data.identity).optIsSome()) {
      return true;
    }
    for (var impl : data.interfaceImpls) {
      if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
        return true;
      }
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_TYPE_IDENTITY).optIsSome()) {
      for (var impl : data.interfaceImpls) {
        if (impl.interface_.builtinKind == BuiltinInterfaceKind.TYPE) {
          return true;
        }
      }
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_ASPECT_IDENTITY).optIsSome()) {
      return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, data.identity).optIsSome()) {
      return EResult.ok(
          new CodeValue(
              cv.code(),
              Opt.some(
                  new ValueDynamic(
                      new ValueDynamic.Source.Expr(), new TypeSourceObjectDynamic(data)))));
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_TYPE_IDENTITY).optIsSome()) {
      boolean hasTypeImpl = false;
      for (var impl : data.interfaceImpls) {
        if (impl.interface_.builtinKind == BuiltinInterfaceKind.TYPE) {
          hasTypeImpl = true;
          break;
        }
      }
      if (hasTypeImpl) {
        var sourceCode = cv.code().dynamic();
        if (sourceCode == null) {
          return EResult.err(
              loc, "cast " + typeDesc() + " -> any: source has no runtime code");
        }
        return EResult.ok(
            emitBuiltinCast(
                xx,
                loc,
                sourceCode,
                compilerxyz.eval.dynamic.DynRuntime.DATA_INSTANCE_TO_TYPE_TYPE,
                new TypeAnyDynamic()));
      }
    }
    for (var impl : data.interfaceImpls) {
      if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
        var builtinKind = impl.interface_.builtinKind;
        compilerxyz.eval.value.function.TypeFunctionDynamic runtimeFn = null;
        Type resultType = null;
        if (builtinKind != null) {
          switch (builtinKind) {
            case TYPE -> {
              runtimeFn = compilerxyz.eval.dynamic.DynRuntime.DATA_INSTANCE_TO_TYPE_TYPE;
              resultType = new TypeAnyDynamic();
            }
            case TARGET -> {
              runtimeFn = compilerxyz.eval.dynamic.DynRuntime.DATA_INSTANCE_TO_TARGET_TYPE;
              resultType = new TypeAnyTargetDynamic();
            }
            case BINDING -> {
              runtimeFn = compilerxyz.eval.dynamic.DynRuntime.DATA_INSTANCE_TO_BINDING_TYPE;
              resultType = new TypeAnyBindingDynamic();
            }
            case CODE_DYN -> {
              return EResult.err(
                  loc,
                  "TypeSourceObjectDynamic::typeCastTo: BuiltinInterfaceKind.CODE_DYN's "
                      + "invisible builtin data_instance_to_code is not yet ported");
            }
            case VALUE -> {
              return EResult.err(
                  loc,
                  "TypeSourceObjectDynamic::typeCastTo: BuiltinInterfaceKind.VALUE lowers through "
                      + "DATA_INSTANCE_TO_VALUE_FN to TypeValueDynamic; TypeValueDynamic is not "
                      + "yet ported on the JVM side (deferred)");
            }
            default -> {

            }
          }
        }
        if (runtimeFn != null) {
          var sourceCode = cv.code().dynamic();
          if (sourceCode == null) {
            return EResult.err(
                loc, "cast " + typeDesc() + " -> interface: source has no runtime code");
          }
          return EResult.ok(emitBuiltinCast(xx, loc, sourceCode, runtimeFn, resultType));
        }
        return EResult.ok(
            new CodeValue(
                cv.code(),
                Opt.some(
                    new ValueDynamic(
                        new ValueDynamic.Source.Expr(),
                        new TypeSourceInterfaceConst(impl.interface_)))));
      }
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_ASPECT_IDENTITY).optIsSome()) {
      var sourceCode = cv.code().dynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc, "cast " + typeDesc() + " -> aspect: source has no runtime code");
      }
      var args = new TList<CodeDynamic>();
      args.add(sourceCode);
      var call =
          compilerxyz.eval.dynamic.DynRuntime.DATA_INSTANCE_TO_ASPECT_TYPE.emitInvocation(
              loc, xx.vx, args);
      return EResult.ok(
          new CodeValue(
              Code.of(loc, call),
              Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), new TypeAspectDynamic()))));
    }
    return EResult.err(loc, "Cannot convert " + typeDesc());
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, data.identity).optIsSome();
  }

  private CodeValue emitBuiltinCast(
      ExprContext xx,
      QExprId loc,
      CodeDynamic sourceCode,
      compilerxyz.eval.value.function.TypeFunctionDynamic runtimeFn,
      Type resultType) {
    Type template = new TypeSourceObjectConst(data);
    var templateKey = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    var templateId = new BindingId.Local(templateKey);
    xx.vx.externals.add(new EvalContext.ExternalEntry(
        templateKey, new compilerxyz.eval.value.any.TypeAnyDynamic(), new External.TypeExternal(template)));
    var templateCode = BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, templateId);
    var args = new TList<CodeDynamic>();
    args.add(templateCode);
    args.add(sourceCode);
    var call = runtimeFn.emitInvocation(loc, xx.vx, args);
    return new CodeValue(
        Code.of(loc, call),
        Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), resultType)));
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
