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
import compilerxyz.util.TOrderedMap;

/**
 * Runtime helpers used by the codegen-emitted installRoot method. Each leaf
 * category (AnyConst, InitialBind-like, AspectId) has a small install-*
 * function that produces a WalkResult from its data. installBranch composes
 * a sequence of named children into an ArgumentRecord+ArgumentRecordBinding.
 * Dynamic registration is a plain side-effect helper. BuiltinTree/BuiltinLeaf
 * are no longer needed as data types once the codegen emits calls to these.
 */
public final class BuiltinInstall {
  private BuiltinInstall() {}

  public record WalkResult(Binding binding, Type type) {}

  @FunctionalInterface
  public interface Installer {
    EResult<WalkResult> install();
  }

  public record NamedChild(String name, Installer installer) {}

  public static EResult<WalkResult> installAnyConst(
      ExprContext xx, QExprId loc, Type type) {
    var wrapperType = compilerxyz.eval.value.object_.ObjectConstOfType.dynamicType();
    var cv = new CodeValue(Code.none(loc), Opt.some(new ValueAnyConst(type)));
    var bindRes = AspectBind.get(xx, loc, wrapperType);
    if (bindRes.hasErrors()) return bindRes.reerr();
    var br =
        bindRes.requireValue("BuiltinInstall::installAnyConst bind aspect").bind(xx, loc, cv);
    if (br.hasErrors()) return br.reerr();
    return EResult.ok(
        new WalkResult(
            br.requireValue("BuiltinInstall::installAnyConst bind result").binding(),
            wrapperType));
  }

  public static EResult<WalkResult> installInitialBind(
      ExprContext xx, QExprId loc, Type type, BindingId dynamicKey) {
    var ibRes = AspectInitialBind.get(xx, loc, type);
    if (ibRes.hasErrors()) return ibRes.reerr();
    var binding =
        ibRes.requireValue("BuiltinInstall::installInitialBind AspectInitialBind")
            .initialBind(dynamicKey);
    return EResult.ok(new WalkResult(binding, type));
  }

  public static EResult<WalkResult> installAspectId(BindingId dynamicKey, Unique unique) {
    return EResult.ok(
        new WalkResult(new BindingUniqueDualMode(dynamicKey, unique), new TypeUnique()));
  }

  public static EResult<WalkResult> installBranch(NamedChild... children) {
    var fields = new TOrderedMap<ConstKey, ArgumentEntry>();
    for (var c : children) {
      var childRes = c.installer().install();
      if (childRes.hasErrors()) return childRes.reerr();
      var result = childRes.requireValue("BuiltinInstall::installBranch child");
      fields.put(
          new ConstKey.Str(c.name()),
          new ArgumentEntry(result.binding(), result.type()));
    }
    var record = new ArgumentRecord(fields);
    return EResult.ok(new WalkResult(new ArgumentRecordBinding(record), record));
  }

  public static void registerDynamic(
      ExprContext xx, BindingId dynamicKey, External dynamicValue) {
    xx.vx.ix.builtinDynamics.put(
        ImportInstall.bindingIdDynamicKey(dynamicKey), dynamicValue);
  }
}
