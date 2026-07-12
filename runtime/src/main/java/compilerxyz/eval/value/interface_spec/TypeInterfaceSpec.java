package compilerxyz.eval.value.interface_spec;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeInterfaceSpec()
    implements Type, AspectConsumeDynamic, ExporterAny {

  private static final JavaInternalName INTERNAL_NAME =
      JavaInternalName.of("compilerxyz/eval/value/interface_spec/InterfaceSpec");

  @Override
  public TypeId typeId() {
    return GenTypeIds.TYPE_INTERFACE_SPEC_TYPE_ID;
  }

  @Override
  public String typeDesc() {
    return "interface_spec";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.InterfaceSpec();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(INTERNAL_NAME);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType.typeAspect(xx, compilerxyz.gen.GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, compilerxyz.gen.GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()) {
      var sourceCode = cv.code().dynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc, "cast interface_spec → interface_ref: source has no runtime code");
      }
      var call = emitSpecToInterfaceDataCall(loc, sourceCode);
      return EResult.ok(
          new CodeValue(
              compilerxyz.eval.Code.of(loc, call), Opt.some(ValueDynamic.stack(xx, destType))));
    }
    return EResult.err(loc, "Cannot cast interface_spec to " + destType.typeDesc());
  }

  private static CodeDynamic emitSpecToInterfaceDataCall(QExprId loc, CodeDynamic source) {
    var seq = new JavaBytecodeSequence();
    seq.add(source);
    seq.add(
        JavaBytecodeUtils.callStaticMethod(
            loc,
            JavaInternalName.fromClass(DynRuntime.class),
            "specToInterfaceData",
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.fromClass(InterfaceData.class),
                TList.of(JavaDataDescriptor.fromClass(TypeInterfaceSpec.class)))));
    return new CodeDynamic(loc, seq);
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
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
}
