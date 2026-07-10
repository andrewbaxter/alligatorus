package compilerxyz.eval;

import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.value.argrecord.ArgumentEntry;
import compilerxyz.eval.value.argrecord.ArgumentRecord;
import compilerxyz.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.eval.value.unique.BindingUniqueDualMode;
import compilerxyz.eval.value.unique.TypeUnique;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public sealed interface BuiltinTree {

  record WalkResult(Binding binding, Type type) {}

  EResult<WalkResult> build(ExprContext xx, QExprId loc);

  void registerDynamic(ExprContext xx);

  record Branch(TList<Child> children) implements BuiltinTree {
    @Override
    public EResult<WalkResult> build(ExprContext xx, QExprId loc) {
      var fields = new TOrderedMap<ConstKey, ArgumentEntry>();
      for (var child : children) {
        var childRes = child.node().build(xx, loc);
        if (childRes.hasErrors()) return childRes.reerr();
        var childResult = childRes.requireValue("BuiltinTree.Branch::build child");
        fields.put(
            new ConstKey.Str(child.name()),
            new ArgumentEntry(childResult.binding(), childResult.type()));
      }
      var argRecord = new ArgumentRecord(fields);
      return EResult.ok(new WalkResult(new ArgumentRecordBinding(argRecord), argRecord));
    }

    @Override
    public void registerDynamic(ExprContext xx) {
      for (var child : children) child.node().registerDynamic(xx);
    }
  }

  record Leaf(BuiltinLeaf leaf) implements BuiltinTree {
    @Override
    public EResult<WalkResult> build(ExprContext xx, QExprId loc) {
      return leaf.build(xx, loc);
    }

    @Override
    public void registerDynamic(ExprContext xx) {
      leaf.registerDynamic(xx);
    }
  }

  record Child(String name, BuiltinTree node) {}

  sealed interface BuiltinLeaf {

    EResult<WalkResult> build(ExprContext xx, QExprId loc);

    default void registerDynamic(ExprContext xx) {}

    record AnyConst(Type type) implements BuiltinLeaf {
      @Override
      public EResult<WalkResult> build(ExprContext xx, QExprId loc) {
        var anyConstType = compilerxyz.eval.value.object_.ObjectConstOfType.dynamicType();
        var cv = new CodeValue(Code.none(loc), Opt.some(new ValueAnyConst(type)));
        var bindRes = AspectBind.get(xx, loc, anyConstType);
        if (bindRes.hasErrors()) return bindRes.reerr();
        var br = bindRes.requireValue("AnyConst::build bind aspect").bind(xx, loc, cv);
        if (br.hasErrors()) return br.reerr();
        return EResult.ok(
            new WalkResult(
                br.requireValue("AnyConst::build bind result").binding(), anyConstType));
      }
    }

    record AspectId(BindingId dynamicKey, Unique unique, External dynamicValue)
        implements BuiltinLeaf {
      @Override
      public EResult<WalkResult> build(ExprContext xx, QExprId loc) {
        return EResult.ok(
            new WalkResult(new BindingUniqueDualMode(dynamicKey, unique), new TypeUnique()));
      }

      @Override
      public void registerDynamic(ExprContext xx) {
        xx.vx.ix.builtinDynamics.put(
            ImportInstall.bindingIdDynamicKey(dynamicKey), dynamicValue);
      }
    }

    record Constructor(BindingId dynamicKey, Type type) implements BuiltinLeaf {
      @Override
      public EResult<WalkResult> build(ExprContext xx, QExprId loc) {
        return initialBindLeafBuild(xx, loc, type, dynamicKey);
      }
    }

    record Function(BindingId dynamicKey, Type type) implements BuiltinLeaf {
      @Override
      public EResult<WalkResult> build(ExprContext xx, QExprId loc) {
        return initialBindLeafBuild(xx, loc, type, dynamicKey);
      }
    }

    record InitialBind(BindingId dynamicKey, Type type) implements BuiltinLeaf {
      @Override
      public EResult<WalkResult> build(ExprContext xx, QExprId loc) {
        return initialBindLeafBuild(xx, loc, type, dynamicKey);
      }
    }

    record Interface(BindingId dynamicKey, Type type, External dynamicValue)
        implements BuiltinLeaf {
      @Override
      public EResult<WalkResult> build(ExprContext xx, QExprId loc) {
        return initialBindLeafBuild(xx, loc, type, dynamicKey);
      }

      @Override
      public void registerDynamic(ExprContext xx) {
        xx.vx.ix.builtinDynamics.put(
            ImportInstall.bindingIdDynamicKey(dynamicKey), dynamicValue);
      }
    }

    private static EResult<WalkResult> initialBindLeafBuild(
        ExprContext xx, QExprId loc, Type type, BindingId dynamicKey) {
      var ibRes = AspectInitialBind.get(xx, loc, type);
      if (ibRes.hasErrors()) return ibRes.reerr();
      var binding = ibRes.requireValue("BuiltinLeaf::build AspectInitialBind").initialBind(dynamicKey);
      return EResult.ok(new WalkResult(binding, type));
    }
  }
}
