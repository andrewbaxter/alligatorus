package compilerxyz.gen;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectLift;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@compilerxyz.annotations.BuiltinTrait(
    uniqueId = 2001L,
    scopeId = 3001,
    fieldPrefix = "v",
    typeIdConstant = "VALUE_TYPE_ID",
    path = "eval.value")
@compilerxyz.annotations.BuiltinTypeId(7109L)
public interface Value {
    @compilerxyz.annotations.SourceExposed(name = "type")
    Type valueType();

    static Type dynamicType() {
        return DynamicType.INSTANCE;
    }

    final class DynamicType {
        static final TypeObjectDynamic INSTANCE = build();

        private DynamicType() {}

        private static TypeObjectDynamic build() {
            var identityAspects = new TList<Unique>();
            identityAspects.add(GenBuiltinInterfaces.builtinValueIfaceIdentity());
            return new TypeObjectDynamic(
                    GenTypeIds.VALUE_TYPE_ID,
                    "value",
                    JavaInternalName.of("compilerxyz/gen/Value"),
                    JavaDataDescriptor.fromClass(Value.class),
                    new TList<ObjectDataField>(),
                    new TList<ObjectMethod>(),
                    ObjectDropKind.DYNAMIC_LIGHT,
                    identityAspects,
                    new TList<TypeObjectCastRule>(),
                    new TList<TypeObjectCastFromRule>(),
                    Opt.<TypeObjectLift>none(),
                    Opt.<ObjectAspectSource>none(),
                    Opt.<JavaInternalName>none());
        }
    }
}
