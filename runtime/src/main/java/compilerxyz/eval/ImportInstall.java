package compilerxyz.eval;

import compilerxyz.eval.dynamic.Prefixes;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

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
    if (xx.vx.ix.classLoader == null) {
      xx.vx.ix.classLoader =
          new DefinitionSetClassLoader(
              DefinitionSetClassLoader.class.getClassLoader(), depLoaders);
    }
    var loader = xx.vx.ix.classLoader;
    if (!xx.vx.ix.installedDefSets.add(set.defSetId)) {
      return EResult.ok(loader);
    }
    var installResult = installCompiledArtifacts(xx, loc, set, loader);
    if (installResult.hasErrors()) {
      return installResult.reerr();
    }
    return EResult.ok(loader);
  }

  private static EResult<Void> installCompiledArtifacts(
      ExprContext xx, QExprId loc, DefinitionSet set, DefinitionSetClassLoader loader) {

    if (!set.finished) {
      return EResult.err(
          loc,
          "DefinitionSet not finished — call d.finish() in source before using this definition set");
    }

    for (var func : set.functions) {
      var finishResult = func.finish(xx, loc);
      if (finishResult.hasErrors()) {
        return finishResult.reerr();
      }
    }

    for (var iface : set.interfaces) {
      if (iface.compiledBytecode != null && iface.compiledBinaryName != null) {
        loader.registerClass(iface.compiledBinaryName, iface.compiledBytecode);
      }
    }

    for (long objIdx = 0; objIdx < set.objects.size(); objIdx++) {
      var obj = set.objects.get((int) objIdx);
      var underlyingType = obj.underlyingType;
      if (underlyingType == null) {
        return EResult.err(
            loc, "Object '" + obj.name + "' underlying type not finalised before install");
      }
      if (obj.setId == null) {
        throw new AssertionError(
            "ImportInstall: source object '" + obj.name + "' has null setId");
      }
      var objInternalName =
          compilerxyz.eval.dynamic.SourceObjectEmitter.internalNameFor(obj.setId, objIdx);
      for (var method : obj.methods) {
        var nameStr = obj.name != null ? obj.name : "(unnamed)";
        var finishResult =
            method.finish(xx, loc, nameStr, obj, underlyingType, objInternalName);
        if (finishResult.hasErrors()) {
          return finishResult.reerr();
        }
      }
      for (var impl : obj.interfaceImpls) {
        for (var m : impl.methodImpls) {
          var finishResult = m.finish(xx, loc, impl.interface_, obj, underlyingType);
          if (finishResult.hasErrors()) {
            return finishResult.reerr();
          }
        }
      }

      if (obj.compiledBinaryName != null && obj.compiledBytecode != null) {
        loader.registerClass(obj.compiledBinaryName, obj.compiledBytecode);
      }
      var objFinishResult = obj.finish(xx, loc, objIdx, loader);
      if (objFinishResult.hasErrors()) {
        return objFinishResult.reerr();
      }
    }

    for (long unionIdx = 0; unionIdx < set.unions.size(); unionIdx++) {
      var union = set.unions.get((int) unionIdx);
      var unionFinish = union.finish(xx, loc, unionIdx, loader);
      if (unionFinish.hasErrors()) {
        return unionFinish.reerr();
      }
    }

    var emittedFuncs = set.finishFunctions();
    if (emittedFuncs != null) {
      loader.registerClass(emittedFuncs.binaryName(), emittedFuncs.bytecode());
      var funcStaticFields = new TOrderedMap<String, Object>();
      funcStaticFields.putAll(emittedFuncs.staticFieldValues());
      funcStaticFields.put(compilerxyz.eval.dynamic.EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
      loader.registerExternals(emittedFuncs.binaryName(), funcStaticFields);
      try {
        Class.forName(emittedFuncs.binaryName(), true, loader);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(
            "Functions class " + emittedFuncs.binaryName() + " failed to load", e);
      }
    } else if (!set.functions.isEmpty()) {

      String binaryName = null;
      byte[] bytecode = null;
      var mergedStaticFields = new TOrderedMap<String, Object>();
      for (var func : set.functions) {
        if (func.compiledClassName == null || func.compiledBytecode == null) {
          continue;
        }
        if (binaryName == null) {
          binaryName = func.compiledClassName;
          bytecode = func.compiledBytecode;
        }
        if (func.compiledStaticFieldValues != null) {
          mergedStaticFields.putAll(func.compiledStaticFieldValues);
        }
      }
      if (binaryName != null) {
        loader.registerClass(binaryName, bytecode);
        if (!mergedStaticFields.isEmpty()) {
          loader.registerExternals(binaryName, mergedStaticFields);
        }
        try {
          Class.forName(binaryName, true, loader);
        } catch (ClassNotFoundException e) {
          throw new AssertionError(
              "Imported Functions class " + binaryName + " failed to load", e);
        }
      }
    }
    for (var func : set.functions) {
      var binaryName = func.compiledClassName;
      var bytecode = func.compiledBytecode;
      if (binaryName == null || bytecode == null) {
        if (binaryName != null || bytecode != null) {
          throw new AssertionError(
              "function has partially-populated compiled artifact (binaryName="
                  + binaryName
                  + ", bytecodeLen="
                  + (bytecode == null ? "null" : bytecode.length)
                  + ")");
        }
        continue;
      }
    }

    for (var obj : set.objects) {
      for (var method : obj.methods) {
        var binaryName = method.compiledClassName;
        var bytecode = method.compiledBytecode;
        var staticFields = method.compiledStaticFieldValues;
        if (binaryName == null || bytecode == null) {
          if (binaryName != null || bytecode != null) {
            throw new AssertionError(
                "method has partially-populated compiled artifact (binaryName="
                    + binaryName
                    + ", bytecodeLen="
                    + (bytecode == null ? "null" : bytecode.length)
                    + ")");
          }
          continue;
        }
        loader.registerClass(binaryName, bytecode);
        if (staticFields != null && !staticFields.isEmpty()) {
          loader.registerExternals(binaryName, staticFields);
        }
      }
      for (var impl : obj.interfaceImpls) {
        for (var m : impl.methodImpls) {
          var binaryName = m.compiledClassName;
          var bytecode = m.compiledBytecode;
          var staticFields = m.compiledStaticFieldValues;
          if (binaryName == null || bytecode == null) {
            if (binaryName != null || bytecode != null) {
              throw new AssertionError(
                  "iface method has partially-populated compiled artifact (binaryName="
                      + binaryName
                      + ", bytecodeLen="
                      + (bytecode == null ? "null" : bytecode.length)
                      + ")");
            }
            continue;
          }
          loader.registerClass(binaryName, bytecode);
          if (staticFields != null && !staticFields.isEmpty()) {
            loader.registerExternals(binaryName, staticFields);
          }
        }
      }
    }
    return EResult.ok(null);
  }

}
