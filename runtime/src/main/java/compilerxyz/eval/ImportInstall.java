package compilerxyz.eval;

import compilerxyz.eval.dynamic.Prefixes;
import compilerxyz.eval.value.source_function.SourceFunctionData;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TSet;

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
      var depLoader = depResult.valueOrNull();
      if (depLoader == null) {
        throw new AssertionError(
            "installDefSetToDynamic returned ok with null loader for dependency");
      }
      depLoaders.add(depLoader);
    }
    var existing = DefinitionSetClassLoader.SHARED_CACHE.getIfPresent(set.defSetId);
    if (!xx.vx.ix.installedDefSets.add(set.defSetId)) {
      if (existing == null) {
        throw new AssertionError(
            "definition set " + set.defSetId + " marked installed but loader was GC'd");
      }
      return EResult.ok(existing);
    }
    var loader =
        existing != null
            ? existing
            : new DefinitionSetClassLoader(
                DefinitionSetClassLoader.class.getClassLoader(), depLoaders);
    if (existing == null) {
      DefinitionSetClassLoader.SHARED_CACHE.put(set.defSetId, loader);
    }
    var installResult = installCompiledArtifacts(xx, loc, set, loader);
    if (installResult.hasErrors()) {
      return installResult.reerr();
    }
    return EResult.ok(loader);
  }

  private static EResult<Void> installCompiledArtifacts(
      ExprContext xx, QExprId loc, DefinitionSet set, DefinitionSetClassLoader loader) {

    registerStaticFunctionDefs(xx, set);

    for (var func : set.functions) {
      var finishResult = func.finish(xx, loc);
      if (finishResult.hasErrors()) {
        return finishResult.reerr();
      }
    }

    registerStaticFunctionDefs(xx, set);
    var staticFunctionDefs = xx.vx.ix.staticFunctionDefs;

    for (long objIdx = 0; objIdx < set.objects.size(); objIdx++) {
      var obj = set.objects.get((int) objIdx);
      var underlyingType = obj.underlyingType;
      if (underlyingType == null) {
        return EResult.err(
            loc, "Object '" + obj.name + "' underlying type not finalised before install");
      }
      for (var method : obj.methods) {
        var nameStr = obj.name != null ? obj.name : "(unnamed)";
        var finishResult = method.finish(xx, loc, nameStr, underlyingType, obj.dataField);
        if (finishResult.hasErrors()) {
          return finishResult.reerr();
        }
      }
      for (var impl : obj.interfaceImpls) {
        for (var m : impl.methodImpls) {
          var finishResult = m.finish(xx, loc, impl.interface_, obj, underlyingType, obj.dataField);
          if (finishResult.hasErrors()) {
            return finishResult.reerr();
          }
        }
      }

      if (obj.compiledBinaryName != null && obj.compiledBytecode != null) {
        loader.registerClass(obj.compiledBinaryName, obj.compiledBytecode);
      }
      var objFinishResult =
          obj.finish(loc, objIdx, loader, xx.vx.ix.builtinDynamics, staticFunctionDefs);
      if (objFinishResult.hasErrors()) {
        return objFinishResult.reerr();
      }
    }

    var emittedFuncs = set.finishFunctions(xx.vx.ix.builtinDynamics, staticFunctionDefs);
    if (emittedFuncs != null) {
      loader.registerClass(emittedFuncs.binaryName(), emittedFuncs.bytecode());
      if (!emittedFuncs.staticFieldValues().isEmpty()) {
        loader.registerExternals(emittedFuncs.binaryName(), emittedFuncs.staticFieldValues());
      }
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
      var nameResult = funcBindingKey(loc, func);
      if (nameResult.hasErrors()) {
        return nameResult.reerr();
      }
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
      var instResult = func.instantiate(loc, loader);
      if (instResult.hasErrors()) {
        return instResult.reerr();
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
        var instResult = method.instantiate(loc, loader);
        if (instResult.hasErrors()) {
          return instResult.reerr();
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
          var instResult = m.instantiate(loc, loader);
          if (instResult.hasErrors()) {
            return instResult.reerr();
          }
        }
      }
    }
    return EResult.ok(null);
  }

  public static String bindingIdDynamicKey(BindingId id) {
    return switch (id) {
      case BindingId.Local local -> Prefixes.BINDING_PREFIX + local.index();
      case BindingId.Builtin builtin -> Prefixes.BUILTIN_PREFIX + builtin.index();
      case BindingId.Def def -> {
        var defSet = def.defSet();
        var compileId = defSet.compileId();
        var importState = compileId.importState();
        var source = compileId.source();
        yield Prefixes.DEF_PREFIX
            + importState.value()
            + "_"
            + source.value()
            + "_"
            + defSet.index()
            + "_"
            + def.index();
      }
    };
  }

  private static EResult<String> funcBindingKey(QExprId loc, SourceFunctionData func) {
    var id = func.bindingId;
    if (id == null) {
      return EResult.err(loc, "func missing binding_id");
    }
    return EResult.ok(bindingIdDynamicKey(id));
  }

  public static void registerStaticFunctionDefs(ExprContext xx, DefinitionSet set) {
    var out = xx.vx.ix.staticFunctionDefs;
    var seen = new TSet<DefSetId>();
    walkRegisterStaticFunctionDefs(set, out, seen);

    for (var ds : xx.vx.definitionSets.values()) {
      walkRegisterStaticFunctionDefs(ds, out, seen);
    }
  }

  private static void walkRegisterStaticFunctionDefs(
      DefinitionSet set,
      TSet<BindingId.Def> out,
      TSet<DefSetId> seen) {
    if (!seen.add(set.defSetId)) {
      return;
    }
    for (var func : set.functions) {
      var id = func.bindingId;
      if (id instanceof BindingId.Def def) {
        out.add(def);
      }
    }
    for (var dep : set.dependencies.values()) {
      walkRegisterStaticFunctionDefs(dep, out, seen);
    }
  }

}
