package compilerxyz.eval.value.bind_result;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;

public record TypeBindResultDynamic() implements Type, AspectConsumeDynamic, ExporterAny {

  @Override
  public TypeId typeId() {
    return GenTypeIds.TYPE_BIND_RESULT_DYNAMIC_TYPE_ID;
  }

  @Override
  public JavaDataDescriptor consumeDynamicJvmClass() {
    return JavaDataDescriptor.fromClass(AspectBind.BindResult.class);
  }

  @Override
  public String typeDesc() {
    return "BindResult";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.BindResultDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, compilerxyz.eval.QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeBindResultDynamic::toDynamic: CodeValue has no dynamic code");
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
