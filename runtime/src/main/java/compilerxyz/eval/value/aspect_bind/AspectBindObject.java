package compilerxyz.eval.value.aspect_bind;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.bind_result.BindResultType;
import compilerxyz.eval.value.code_value_extern.TypeCodeValueExternDynamic;
import compilerxyz.eval.value.eresult.TypeEResultDynamic;
import compilerxyz.eval.value.function.TypeMethodDynamic;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectLift;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public final class AspectBindObject {

  private AspectBindObject() {}

  public static TypeObjectDynamic dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectDynamic INSTANCE = build();

    private static TypeObjectDynamic build() {
      var jvmInterface = JavaInternalName.of("compilerxyz/gen/AspectBind");
      var jvmInterfaceDesc = JavaDataDescriptor.fromInternalName(jvmInterface);

      var methods = new TList<ObjectMethod>();
      var castRules = new TList<TypeObjectCastRule>();
      castRules.add(new TypeObjectCastRule.CallToInterfaceSpec(DynRuntime.OBJECT_TO_INTERFACE_SPEC));
      castRules.add(
          new TypeObjectCastRule.CallByDestIdentity(
              GenIdentities.INTERFACE_REF_TYPE_IDENTITY, DynRuntime.OBJECT_TO_INTERFACE_DATA));

      var instance =
          new TypeObjectDynamic(
              new TypeId.Builtin(9101L),
              "aspect_bind",
              jvmInterface,
              jvmInterfaceDesc,
              new TList<ObjectDataField>(),
              methods,
              ObjectDropKind.DYNAMIC,
              new TList<Unique>(),
              castRules,
              new TList<TypeObjectCastFromRule>(),
              Opt.<TypeObjectLift>none(),
              Opt.<ObjectAspectSource>some(new ObjectAspectSource.Static(AspectBind.ID)),
              Opt.some(jvmInterface));

      var bindDesc =
          JavaMethodDescriptor.fromParts(
              JavaDataDescriptor.fromClass(EResult.class),
              TList.of(
                  JavaDataDescriptor.fromClass(ExprContext.class),
                  JavaDataDescriptor.fromClass(QExprId.class),
                  JavaDataDescriptor.fromClass(CodeValue.class)));
      var argRecordFields = new TOrderedMap<compilerxyz.eval.ConstKey, RecordField>();
      argRecordFields.put(
          new compilerxyz.eval.ConstKey.Str("xx"),
          new RecordField.Dynamic(ExprContext.dynamicType()));
      argRecordFields.put(
          new compilerxyz.eval.ConstKey.Str("loc"),
          new RecordField.Dynamic(QExprId.dynamicType()));
      argRecordFields.put(
          new compilerxyz.eval.ConstKey.Str("cv"),
          new RecordField.Dynamic(new TypeCodeValueExternDynamic()));
      var argRecord = new Record(Opt.none(), argRecordFields);
      var retType = new TypeEResultDynamic(BindResultType.dynamicType());
      var bindMethod =
          new TypeMethodDynamic(
              instance, argRecord, retType, jvmInterface, "bind", bindDesc, true);
      methods.add(new ObjectMethod("bind", bindMethod));

      return instance;
    }
  }
}
