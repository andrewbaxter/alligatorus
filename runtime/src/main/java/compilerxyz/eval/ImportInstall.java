package compilerxyz.eval;

import compilerxyz.eval.dynamic.Prefixes;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.eval.dynamic.EvalRootEmitter;

public final class ImportInstall {
  private ImportInstall() {}

  public static EResult<DefinitionSetClassLoader> installDefSetToDynamic(
      ExprContext xx, QExprId loc, DefinitionSet set) {
    var depLoaders = new TList<DefinitionSetClassLoader>();
    for (var dep : set.dependencies.values()) {
      var depResult = installDefSetToDynamic(xx, loc, dep);
      if (depResult.hasErrors()) {
        return depResult.reerr();
      }
      var depLoader =
          depResult.requireValue("ImportInstall::installDefSetToDynamic dependency loader");
      depLoaders.add(depLoader);
    }
    if (xx.vx.ix.classLoader.optIsNone()) {
      xx.vx.ix.classLoader =
          Opt.some(new DefinitionSetClassLoader(
              xx.vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), depLoaders));
    }
    var loader = xx.vx.ix.classLoader.optUnwrap();
    if (!xx.vx.ix.installedDefSets.add(set.defSetId)) {
      return EResult.ok(loader);
    }
    if (!set.finished) {
      return EResult.err(
          loc,
          "DefinitionSet not finished — call d.finish() in source before using this definition set");
    }
    for (var iface : set.interfaces) {
      if (iface.compiledBytecode.optIsSome() && iface.compiledBinaryName != null) {
        loader.registerClass(iface.compiledBinaryName, iface.compiledBytecode.optUnwrap());
      }
    }
    for (long objIdx = 0; objIdx < set.objects.size(); objIdx++) {
      var obj = set.objects.get((int) objIdx);
      if (obj.underlyingType.optIsNone()) {
        return EResult.err(
            loc, "Object '" + obj.name.optUnwrapOr("(unnamed)") + "' underlying type not finalised before install");
      }
      if (obj.setId.optIsNone()) {
        throw new AssertionError(
            "ImportInstall: source object '" + obj.name.optUnwrapOr("(unnamed)") + "' has null setId");
      }
      if (obj.compiledBinaryName.optIsSome() && obj.compiledBytecode.optIsSome()) {
        var binaryName = obj.compiledBinaryName.optUnwrap();
        loader.registerClass(binaryName, obj.compiledBytecode.optUnwrap());
        if (obj.compiledStaticFieldValues.optIsSome()) {
          var fields = new TOrderedMap<String, Object>();
          fields.putAll(obj.compiledStaticFieldValues.optUnwrap());
          fields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
          loader.registerExternals(binaryName, fields);
        }
      }
    }
    for (long unionIdx = 0; unionIdx < set.unions.size(); unionIdx++) {
      var union = set.unions.get((int) unionIdx);
      if (union.compiledBaseBinaryName.optIsSome() && union.compiledBaseBytecode.optIsSome()) {
        loader.registerClass(
            union.compiledBaseBinaryName.optUnwrap(), union.compiledBaseBytecode.optUnwrap());
        if (union.compiledVariantBinaryNames.optIsSome() && union.compiledVariantBytecode.optIsSome()) {
          var variantNames = union.compiledVariantBinaryNames.optUnwrap();
          var variantBytes = union.compiledVariantBytecode.optUnwrap();
          for (int i = 0; i < variantNames.size(); i++) {
            loader.registerClass(variantNames.get(i), variantBytes.get(i));
          }
        }
      }
    }
    var emittedFuncsOpt = set.finishFunctions();
    if (emittedFuncsOpt.optIsSome()) {
      var emittedFuncs = emittedFuncsOpt.optUnwrap();
      loader.registerClass(emittedFuncs.binaryName(), emittedFuncs.bytecode());
      var funcStaticFields = new TOrderedMap<String, Object>();
      funcStaticFields.putAll(emittedFuncs.staticFieldValues());
      funcStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
      loader.registerExternals(emittedFuncs.binaryName(), funcStaticFields);
      try {
        Class.forName(emittedFuncs.binaryName(), true, loader);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(
            "Functions class " + emittedFuncs.binaryName() + " failed to load", e);
      }
    } else if (!set.functions.isEmpty()) {
      String icaBinaryName = null;
      byte[] icaBytecode = null;
      var mergedStaticFields = new TOrderedMap<String, Object>();
      for (var func : set.functions) {
        if (func.compiledClassName.optIsNone() || func.compiledBytecode.optIsNone()) {
          continue;
        }
        if (icaBinaryName == null) {
          icaBinaryName = func.compiledClassName.optUnwrap();
          icaBytecode = func.compiledBytecode.optUnwrap();
        }
        if (func.compiledStaticFieldValues.optIsSome()) {
          mergedStaticFields.putAll(func.compiledStaticFieldValues.optUnwrap());
        }
      }
      if (icaBinaryName != null) {
        loader.registerClass(icaBinaryName, icaBytecode);
        mergedStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
        loader.registerExternals(icaBinaryName, mergedStaticFields);
        try {
          Class.forName(icaBinaryName, true, loader);
        } catch (ClassNotFoundException e) {
          throw new AssertionError(
              "Imported Functions class " + icaBinaryName + " failed to load", e);
        }
      }
    }
    for (var func : set.functions) {
      var compiledClassName = func.compiledClassName;
      var compiledBytecode = func.compiledBytecode;
      if (compiledClassName.optIsNone() || compiledBytecode.optIsNone()) {
        if (compiledClassName.optIsSome() || compiledBytecode.optIsSome()) {
          throw new AssertionError(
              "function has partially-populated compiled artifact (binaryName="
                  + compiledClassName.optUnwrapOr(null)
                  + ", bytecodeLen="
                  + compiledBytecode.optUnwrapOr(null)
                  + ")");
        }
        continue;
      }
    }
    for (var obj : set.objects) {
      for (var method : obj.methods) {
        if (method.compiledClassName.optIsNone() || method.compiledBytecode.optIsNone()) {
          if (method.compiledClassName.optIsSome() || method.compiledBytecode.optIsSome()) {
            throw new AssertionError(
                "method has partially-populated compiled artifact (binaryName="
                    + method.compiledClassName.optUnwrapOr(null)
                    + ", bytecodeLen="
                    + method.compiledBytecode.optUnwrapOr(null)
                    + ")");
          }
          continue;
        }
        var binaryName = method.compiledClassName.optUnwrap();
        var bytecode = method.compiledBytecode.optUnwrap();
        loader.registerClass(binaryName, bytecode);
        if (method.compiledStaticFieldValues.optIsSome()
            && !method.compiledStaticFieldValues.optUnwrap().isEmpty()) {
          loader.registerExternals(binaryName, method.compiledStaticFieldValues.optUnwrap());
        }
      }
      for (var impl : obj.interfaceImpls) {
        for (var m : impl.methodImpls) {
          if (m.compiledClassName.optIsNone() || m.compiledBytecode.optIsNone()) {
            if (m.compiledClassName.optIsSome() || m.compiledBytecode.optIsSome()) {
              throw new AssertionError(
                  "iface method has partially-populated compiled artifact (binaryName="
                      + m.compiledClassName.optUnwrapOr(null)
                      + ", bytecodeLen="
                      + m.compiledBytecode.optUnwrapOr(null)
                      + ")");
            }
            continue;
          }
          var binaryName = m.compiledClassName.optUnwrap();
          var bytecode = m.compiledBytecode.optUnwrap();
          loader.registerClass(binaryName, bytecode);
          if (m.compiledStaticFieldValues.optIsSome()
              && !m.compiledStaticFieldValues.optUnwrap().isEmpty()) {
            loader.registerExternals(binaryName, m.compiledStaticFieldValues.optUnwrap());
          }
        }
      }
    }
    return EResult.ok(loader);
  }

}
