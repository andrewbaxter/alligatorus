package compilerxyz.gen;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
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
    uniqueId = 2005L,
    scopeId = 3005,
    fieldPrefix = "se",
    typeIdConstant = "BINDING_TYPE_ID",
    path = "eval.binding")
@compilerxyz.annotations.BuiltinPath("types.binding")
@compilerxyz.annotations.BuiltinTypeId(7025L)
public interface Binding {
    @compilerxyz.annotations.SourceExposed(name = "use")
    CodeValue bindingUse(ExprContext xx, QExprId loc, Code code);

    static Type dynamicType() {
        return DynamicType.INSTANCE;
    }

    final class DynamicType {
        static final TypeObjectDynamic INSTANCE = build();

        private DynamicType() {}

        private static TypeObjectDynamic build() {
            var identityAspects = new TList<Unique>();
            identityAspects.add(GenBuiltinInterfaces.builtinBindingIfaceIdentity());
            return new TypeObjectDynamic(
                    GenTypeIds.BINDING_TYPE_ID,
                    "binding",
                    JavaInternalName.of("compilerxyz/gen/Binding"),
                    JavaDataDescriptor.fromClass(Binding.class),
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
