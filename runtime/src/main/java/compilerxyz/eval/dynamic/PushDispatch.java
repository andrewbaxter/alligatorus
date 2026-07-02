package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.ContextualFunction;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.SourceId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceRegistry;
import compilerxyz.eval.value.source_object_new.TypeSourceObjectNewConst;
import compilerxyz.eval.value.unique.ValueUnique;
import compilerxyz.gen.GenBuiltinInterfaces;
import compilerxyz.gen.OptionPush;
import compilerxyz.gen.TypeRefPush;
import compilerxyz.eval.value.any.TypeAnyDynamic;
import compilerxyz.eval.value.source_interface.SourceInterfaceData;
import compilerxyz.eval.value.source_interface.SourceInterfaceMethodData;
import compilerxyz.eval.value.source_interface.SourceInterfaceRef;
import compilerxyz.eval.value.source_object.SourceDataMethodData;
import compilerxyz.eval.value.source_object.SourceObjectData;
import compilerxyz.eval.value.source_object.SourceObjectInterfaceImplData;
import compilerxyz.eval.value.source_object.SourceObjectInterfaceMethodImplData;
import compilerxyz.eval.value.source_object.SourceObjectRef;
import compilerxyz.eval.value.source_function.SourceFunctionData;
import compilerxyz.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.eval.value.source_union.SourceUnionData;
import compilerxyz.eval.value.source_union.SourceUnionRef;
import compilerxyz.eval.value.source_union.SourceUnionVariant;
import compilerxyz.eval.value.source_union_new.SourceUnionNewRef;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.eval.value.unique.TypeUnique;
import compilerxyz.gen.DefinitionSetBuilder;
import compilerxyz.gen.SourceFuncResult;
import compilerxyz.gen.SourceInterfaceMethodResult;
import compilerxyz.gen.SourceInterfaceResult;
import compilerxyz.gen.SourceObjectMethodResult;
import compilerxyz.gen.SourceObjectResult;
import compilerxyz.gen.SourceUnionBuilder;
import compilerxyz.util.TList;
import java.util.Map;

public final class PushDispatch {
  private PushDispatch() {}

  public static Object fieldGet(Object base, String key) {
    if (base instanceof DefinitionSetBuilder dsb) {
      return dispatchDefinitionSetBuilder(dsb, key);
    }
    if (base instanceof SourceFuncResult sfr) {
      return dispatchSourceFuncResult(sfr, key);
    }
    if (base instanceof SourceInterfaceResult sir) {
      return dispatchSourceInterfaceResult(sir, key);
    }
    if (base instanceof SourceInterfaceMethodResult simr) {
      return dispatchSourceInterfaceMethodResult(simr, key);
    }
    if (base instanceof SourceObjectResult sor) {
      return dispatchSourceObjectResult(sor, key);
    }
    if (base instanceof SourceObjectMethodResult somr) {
      return dispatchSourceObjectMethodResult(somr, key);
    }
    if (base instanceof SourceUnionBuilder sub) {
      return dispatchSourceUnionBuilder(sub, key);
    }
    if (base instanceof TypeRefPush trp) {
      return dispatchTypeRefPush(trp, key);
    }
    if (base instanceof OptionPush opt) {
      return dispatchOptionPush(opt, key);
    }
    return SENTINEL;
  }

  private static Object dispatchTypeRefPush(TypeRefPush trp, String key) {
    return switch (key) {
      case "_0" -> (ContextualFunction) (xx, arg) -> {
        if (!(arg instanceof Unique unique)) {
          throw new AssertionError(
              "PushDispatch.TypeRefPush.aspect: expected Unique, got "
                  + (arg == null ? "null" : arg.getClass()));
        }
        var isSome = trp.type_ref.typeAspect(xx, unique).optIsSome();
        return new OptionPush(isSome);
      };
      default -> throw new AssertionError(
          "PushDispatch.TypeRefPush: unknown field '" + key + "'");
    };
  }

  private static Object dispatchOptionPush(OptionPush opt, String key) {
    return switch (key) {
      case "_0" -> (ContextualFunction) (xx, arg) -> opt.is_some_inner;
      default -> throw new AssertionError(
          "PushDispatch.OptionPush: unknown field '" + key + "'");
    };
  }

  public static final Object SENTINEL = new Object();

  private static Object dispatchDefinitionSetBuilder(DefinitionSetBuilder dsb, String key) {
    return switch (key) {
      case "_0" -> (ContextualFunction) (xx, arg) -> {
        var name = (String) arg;
        var entryId = dsb.inner.nextEntryId.next();
        var data = new SourceFunctionData(dsb.inner.defSetId, name, dsb.inner);
        data.bindingId = new BindingId.Def(dsb.inner.defSetId, entryId);
        dsb.inner.functions.add(data);
        return new SourceFuncResult(new TypeSourceFunctionConst(data));
      };
      case "_1" -> (ContextualFunction) (xx, arg) -> {
        var name = (String) arg;
        var identity = freshUnique(xx);
        var dataField = "_" + dsb.inner.nextFieldId.next();
        var data =
            new SourceObjectData(dsb.inner.defSetId, name, identity, dataField, dsb.inner);
        dsb.inner.objects.add(data);
        return new SourceObjectResult(new SourceObjectRef(data));
      };
      case "_2" -> (ContextualFunction) (xx, arg) -> new DefinitionSetBuilder(dsb.inner);
      case "_3" -> (ContextualFunction) (xx, arg) -> {
        var identity = freshUnique(xx);
        var data = new SourceUnionData(identity);
        return new SourceUnionBuilder(data);
      };
      case "_4" -> (ContextualFunction) (xx, arg) -> {
        var identity = freshUnique(xx);
        var iface = new SourceInterfaceData(dsb.inner.defSetId, identity, null, dsb.inner);
        dsb.inner.interfaces.add(iface);
        return new SourceInterfaceResult(new SourceInterfaceRef(iface));
      };
      case "_5" -> (ContextualFunction) (xx, arg) -> {

        dsb.inner.finished = true;
        return new DefinitionSetBuilder(dsb.inner);
      };
      default -> throw new AssertionError(
          "PushDispatch.DefinitionSetBuilder: unknown field '" + key + "'");
    };
  }

  private static Object dispatchSourceFuncResult(SourceFuncResult sfr, String key) {
    return switch (key) {
      case "_0" -> sfr.func_ref;
      case "_1" -> (ContextualFunction) (xx, arg) -> {
        var description = (String) arg;
        if (sfr.func_ref.data().description != null) {
          throw new IllegalStateException("Function already has a description");
        }
        sfr.func_ref.data().description = description;
        return null;
      };
      case "_2" -> (ContextualFunction) (xx, arg) -> {
        var args = (Object[]) arg;
        var argType = (Type) args[0];
        var retType = (Type) args[1];
        var implCell = (StagedExprCell) args[2];
        var data = sfr.func_ref.data();
        if (data.argType != null) {
          throw new IllegalStateException("Function already has an arg type");
        }
        data.argType = argType;
        if (data.retType != null) {
          throw new IllegalStateException("Function already has a ret type");
        }
        data.retType = retType;
        if (data.implExpr != null) {
          throw new IllegalStateException("Function already has an implementation");
        }
        data.implExpr = (Value) implCell.take();
        return null;
      };
      case "_3" -> (ContextualFunction) (xx, arg) -> {
        var excType = (Type) arg;
        if (sfr.func_ref.data().exceptionType != null) {
          throw new IllegalStateException("Function already has an exception type");
        }
        sfr.func_ref.data().exceptionType = excType;
        return null;
      };
      default -> throw new AssertionError(
          "PushDispatch.SourceFuncResult: unknown field '" + key + "'");
    };
  }

  private static Object dispatchSourceInterfaceResult(SourceInterfaceResult sir, String key) {
    return switch (key) {
      case "_0" -> sir.interface_ref;
      case "_1" -> (ContextualFunction) (xx, arg) -> {
        var description = (String) arg;
        if (sir.interface_ref.data().description != null) {
          throw new IllegalStateException("Interface already has a description");
        }
        sir.interface_ref.data().description = description;
        return null;
      };
      case "_2" -> (ContextualFunction) (xx, arg) -> {
        var name = (String) arg;
        var ifaceData = sir.interface_ref.data();
        var ds = lookupDefSet(xx, ifaceData.setId);
        var fieldName = "_im" + DefinitionSet.nextInterfaceMethodId(ds);
        var method = new SourceInterfaceMethodData(name, fieldName);
        ifaceData.methods.add(method);
        return new SourceInterfaceMethodResult(method);
      };
      case "_3" -> (ContextualFunction) (xx, arg) -> {
        var isAspect = (Boolean) arg;
        sir.interface_ref.data().isAspect = isAspect;
        return null;
      };
      default -> throw new AssertionError(
          "PushDispatch.SourceInterfaceResult: unknown field '" + key + "'");
    };
  }

  private static Object dispatchSourceInterfaceMethodResult(
      SourceInterfaceMethodResult simr, String key) {
    return switch (key) {
      case "_0" -> (ContextualFunction) (xx, arg) -> {
        var args = (Object[]) arg;
        var argType = (Type) args[0];
        var retType = (Type) args[1];
        if (simr.method.argType != null) {
          throw new IllegalStateException("Interface method already has an arg type");
        }
        simr.method.argType = argType;
        if (simr.method.retType != null) {
          throw new IllegalStateException("Interface method already has a ret type");
        }
        simr.method.retType = retType;
        return null;
      };
      default -> throw new AssertionError(
          "PushDispatch.SourceInterfaceMethodResult: unknown field '" + key + "'");
    };
  }

  private static Object dispatchSourceObjectResult(SourceObjectResult sor, String key) {
    return switch (key) {
      case "_0" -> sor.object_ref;
      case "_1" -> new TypeSourceObjectNewConst(
          sor.object_ref.data());
      case "_2" -> (ContextualFunction) (xx, arg) -> {
        var description = (String) arg;
        if (sor.object_ref.data().description != null) {
          throw new IllegalStateException("Object already has a description");
        }
        sor.object_ref.data().description = description;
        return null;
      };
      case "_3" -> (ContextualFunction) (xx, arg) -> {
        var typeRef = (Type) arg;
        if (sor.object_ref.data().underlyingType != null) {
          throw new IllegalStateException("Object already has an inner type");
        }
        sor.object_ref.data().underlyingType = typeRef;
        return null;
      };
      case "_4" -> (ContextualFunction) (xx, arg) -> {
        var name = (String) arg;
        var dtData = sor.object_ref.data();
        var ds = lookupDefSet(xx, dtData.setId);
        var fieldName = "_" + ds.nextFieldId.next();
        var entryId = ds.nextEntryId.next();
        var method = new SourceDataMethodData(name, fieldName);
        method.bindingId = new BindingId.Def(ds.defSetId, entryId);
        dtData.methods.add(method);
        return new SourceObjectMethodResult(method);
      };
      case "_5" -> (ContextualFunction) (xx, arg) -> {
        var args = (Object[]) arg;
        var ifaceRef = (SourceInterfaceRef) args[0];
        var record = recordToConstKeyExprMap(args[1]);
        var ifaceData = ifaceRef.data();
        var dtData = sor.object_ref.data();
        var ds = lookupDefSet(xx, dtData.setId);
        var impl = new SourceObjectInterfaceImplData(ifaceData);
        for (int idx = 0; idx < ifaceData.methods.size(); idx++) {
          var im = ifaceData.methods.get(idx);
          var ckey = new ConstKey.Str(im.name);
          var expr = record.get(ckey);
          if (expr == null) {
            throw new IllegalStateException(
                "Missing implementation for interface method '" + im.name + "'");
          }
          var entryId = ds.nextEntryId.next();
          var mImpl = new SourceObjectInterfaceMethodImplData(idx);
          mImpl.implExpr = expr;
          mImpl.bindingId = new BindingId.Def(ds.defSetId, entryId);
          impl.methodImpls.add(mImpl);
        }
        dtData.interfaceImpls.add(impl);
        return null;
      };
      case "_6" -> (ContextualFunction) (xx, arg) -> {
        var record = recordToConstKeyExprMap(arg);
        var dtData = sor.object_ref.data();
        var ds = lookupDefSet(xx, dtData.setId);
        var identity = freshUnique(xx);
        var ifaceData = new SourceInterfaceData(ds.defSetId, identity, null, ds);
        ds.interfaces.add(ifaceData);
        ifaceData.isAspect = true;
        var methodNames = new TList<String>();
        for (var k : record.keySet()) {
          if (k instanceof ConstKey.Str s) {
            methodNames.add(s.value());
          } else {
            methodNames.add(k.toCanonical());
          }
        }
        for (var imName : methodNames) {
          var fieldName = "_im" + DefinitionSet.nextInterfaceMethodId(ds);
          var im = new SourceInterfaceMethodData(imName, fieldName);
          im.argType = new TypeAnyDynamic();
          im.retType = new TypeAnyDynamic();
          ifaceData.methods.add(im);
        }
        var aspectUniqueField = "_im" + DefinitionSet.nextInterfaceMethodId(ds);
        var aspectIdMethod = new SourceInterfaceMethodData("aspect_id", aspectUniqueField);
        aspectIdMethod.argType = new Tuple(new TList<>());
        aspectIdMethod.retType = new TypeUnique();
        ifaceData.methods.add(aspectIdMethod);
        var impl = new SourceObjectInterfaceImplData(ifaceData);
        var allMethodNames = new TList<String>();
        for (var m : ifaceData.methods) {
          allMethodNames.add(m.name);
        }
        for (int idx = 0; idx < allMethodNames.size(); idx++) {
          var imName = allMethodNames.get(idx);
          if (imName.equals("aspect_id")) {
            var aspectIdEntryId = ds.nextEntryId.next();
            var uniqueLiteral = new Expr(
                new QExprId(new SourceId(0), 0L),
                new Expr.Literal(Expr.LitValue.shared(
                    new ValueUnique(identity))));
            var mImpl = new SourceObjectInterfaceMethodImplData(idx);
            mImpl.implExpr = uniqueLiteral;
            mImpl.bindingId = new BindingId.Def(ds.defSetId, aspectIdEntryId);
            impl.methodImpls.add(mImpl);
            continue;
          }
          var ckey = new ConstKey.Str(imName);
          var expr = record.get(ckey);
          if (expr == null) {
            throw new IllegalStateException(
                "Missing implementation for aspect method '" + imName + "'");
          }
          var entryId = ds.nextEntryId.next();
          var mImpl = new SourceObjectInterfaceMethodImplData(idx);
          mImpl.implExpr = expr;
          mImpl.bindingId = new BindingId.Def(ds.defSetId, entryId);
          impl.methodImpls.add(mImpl);
        }
        dtData.interfaceImpls.add(impl);
        return new SourceInterfaceRef(ifaceData);
      };
      case "_7" -> (ContextualFunction) (xx, arg) ->
          implBuiltinInterface(xx, sor, arg, "value");
      case "_8" -> (ContextualFunction) (xx, arg) ->
          implBuiltinInterface(xx, sor, arg, "type_");
      default -> throw new AssertionError(
          "PushDispatch.SourceObjectResult: unknown field '" + key + "'");
    };
  }

  private static Object implBuiltinInterface(
      ExprContext xx, SourceObjectResult sor, Object arg, String ifaceField) {
    var record = recordToConstKeyExprMap(arg);
    var dtData = sor.object_ref.data();
    var ds = lookupDefSet(xx, dtData.setId);
    var vx = xx.vx;
    var builtinIfaces =
        BuiltinInterfaceRegistry.bundle(
            vx.ix.importState);
    SourceInterfaceData ifaceData;
    try {
      ifaceData =
          (SourceInterfaceData)
              GenBuiltinInterfaces.class.getField(ifaceField).get(builtinIfaces);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
          "PushDispatch.implBuiltinInterface: missing builtin interface field '"
              + ifaceField
              + "'",
          e);
    }
    var impl = new SourceObjectInterfaceImplData(ifaceData);
    for (int idx = 0; idx < ifaceData.methods.size(); idx++) {
      var im = ifaceData.methods.get(idx);
      var ckey = new ConstKey.Str(im.name);
      var expr = record.get(ckey);
      if (expr == null) {
        throw new IllegalStateException(
            "Missing implementation for builtin interface method '" + im.name + "'");
      }
      var entryId = ds.nextEntryId.next();
      var mImpl = new SourceObjectInterfaceMethodImplData(idx);
      mImpl.implExpr = expr;
      mImpl.bindingId = new BindingId.Def(ds.defSetId, entryId);
      impl.methodImpls.add(mImpl);
    }
    dtData.interfaceImpls.add(impl);
    return null;
  }

  private static Object dispatchSourceObjectMethodResult(
      SourceObjectMethodResult somr, String key) {
    return switch (key) {
      case "_0" -> (ContextualFunction) (xx, arg) -> {
        var args = (Object[]) arg;
        var argType = (Type) args[0];
        var retType = (Type) args[1];
        var implCell = (StagedExprCell) args[2];
        var method = somr.method;
        if (method.argType != null) {
          throw new IllegalStateException("Method already has an arg type");
        }
        method.argType = argType;
        if (method.retType != null) {
          throw new IllegalStateException("Method already has a ret type");
        }
        method.retType = retType;
        if (method.implExpr != null) {
          throw new IllegalStateException("Method already has an implementation");
        }
        method.implExpr = (Value) implCell.take();
        return null;
      };
      default -> throw new AssertionError(
          "PushDispatch.SourceObjectMethodResult: unknown field '" + key + "'");
    };
  }

  private static Object dispatchSourceUnionBuilder(SourceUnionBuilder sub, String key) {
    return switch (key) {
      case "_0" -> new SourceUnionRef(sub.data);
      case "_1" -> (ContextualFunction) (xx, arg) -> {
        var args = (Object[]) arg;
        var tag = (String) args[0];
        var typeRef = (Type) args[1];
        var jsTag = Prefixes.TAG_PREFIX + sub.data.nextTagId.next();
        var variantIndex = sub.data.variants.size();
        sub.data.variants.add(new SourceUnionVariant(new ConstKey.Str(tag), jsTag, typeRef));
        return new SourceUnionNewRef(sub.data, variantIndex);
      };
      default -> throw new AssertionError(
          "PushDispatch.SourceUnionBuilder: unknown field '" + key + "'");
    };
  }

  private static Map<ConstKey, Expr> recordToConstKeyExprMap(Object arg) {
    if (!(arg instanceof Map<?, ?> rawMap)) {
      throw new IllegalStateException(
          "PushDispatch.recordToConstKeyExprMap: expected Map, got "
              + (arg == null ? "null" : arg.getClass()));
    }
    var out = new java.util.LinkedHashMap<ConstKey, Expr>();
    for (var entry : rawMap.entrySet()) {
      ConstKey ckey;
      var k = entry.getKey();
      if (k instanceof ConstKey ck) {
        ckey = ck;
      } else if (k instanceof String s) {
        ckey = parseConstKeyCanonical(s);
      } else {
        throw new IllegalStateException(
            "PushDispatch.recordToConstKeyExprMap: unsupported key type "
                + (k == null ? "null" : k.getClass()));
      }
      var v = entry.getValue();
      Expr expr;
      if (v instanceof Expr e) {
        expr = e;
      } else if (v instanceof StagedExprCell cell) {

        var inner = cell.take();
        if (!(inner instanceof Expr e)) {
          throw new IllegalStateException(
              "PushDispatch.recordToConstKeyExprMap: StagedExprCell did not contain Expr, got "
                  + (inner == null ? "null" : inner.getClass()));
        }
        expr = e;
      } else {
        throw new IllegalStateException(
            "PushDispatch.recordToConstKeyExprMap: expected Expr or StagedExprCell value, got "
                + (v == null ? "null" : v.getClass()));
      }
      out.put(ckey, expr);
    }
    return out;
  }

  private static ConstKey parseConstKeyCanonical(String s) {
    if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
      var inner = s.substring(1, s.length() - 1);
      var sb = new StringBuilder(inner.length());
      for (int i = 0; i < inner.length(); i++) {
        var c = inner.charAt(i);
        if (c == '\\' && i + 1 < inner.length()) {
          sb.append(inner.charAt(++i));
        } else {
          sb.append(c);
        }
      }
      return new ConstKey.Str(sb.toString());
    }
    return new ConstKey.Uint(Long.parseUnsignedLong(s));
  }

  private static Unique freshUnique(ExprContext xx) {
    var vx = xx.vx;
    var compileId = new CompileId(vx.ix.importState, vx.ix.sourceId);
    return new Unique.Source(compileId, vx.uniqueCounter.next());
  }

  private static DefinitionSet lookupDefSet(ExprContext xx, DefSetId id) {
    if (id == null) {
      throw new IllegalStateException("PushDispatch: definition set id is null");
    }
    var vx = xx.vx;
    var ds = vx.definitionSets.get(id);
    if (ds == null) {
      throw new IllegalStateException(
          "PushDispatch: definition set " + id + " not found in EvalContext.definitionSets");
    }
    return ds;
  }
}
