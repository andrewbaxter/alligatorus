package compilerxyz.eval.value.argrecord;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.util.stream.Collectors;

public record ArgumentRecord(TOrderedMap<ConstKey, Binding> fields)
    implements Type, Value, AspectField, ExporterAny {

  @Override
  public String typeDesc() {
    var keys =
        fields.keySet().stream().map(ConstKey::toCanonical).collect(Collectors.joining(", "));
    return "argument_record(" + keys + ")";
  }

  @Override
  public Type valueTypeOf() {
    return this;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    return key.equals(AspectField.ID) ? Opt.some(this) : Opt.none();
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    if (!(key.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "ArgumentRecord::field: key has no compile-time value");
    }
    var keyType = some.value().valueTypeOf();
    var aspect = AspectStringConst.getMaybe(xx, keyType);
    if (!(aspect instanceof Opt.Some<AspectStringConst> a)) {
      return EResult.err(
          loc,
          "ArgumentRecord::field: key type "
              + keyType.typeDesc()
              + " does not implement AspectStringConst");
    }
    var keyName = a.value().get(some.value());
    var constKey = new ConstKey.Str(keyName);
    var binding = fields.getOrNull(constKey);
    if (binding == null) {
      return EResult.err(loc, "Field '" + keyName + "' not found on argument record");
    }
    return EResult.ok(binding.bindingUse_(xx, loc, Code.none(loc)));
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
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
