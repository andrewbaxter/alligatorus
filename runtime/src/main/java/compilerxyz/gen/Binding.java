package compilerxyz.gen;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
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
import compilerxyz.eval.value.object_.TypeObjectMeta;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@compilerxyz.annotations.BuiltinTrait(
    uniqueId = 2005L,
    scopeId = 3005)
@compilerxyz.annotations.BuiltinPath("eval.binding")
@compilerxyz.annotations.BuiltinTypeId(7025L)
public interface Binding {
    @compilerxyz.annotations.SourceExposed
    CodeValue bindingUse(ExprContext xx, QExprId loc, Code code);

    default EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        var useCv = bindingUse(xx, loc, Code.none(loc));
        return xx.target.targetDrop(xx, loc, useCv);
    }

    Binding bindingFork();

    EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others);

    static Type dynamicType() {
        return DynamicType.INSTANCE;
    }

    final class DynamicType {
        static final TypeObjectDynamic INSTANCE = build();

        private DynamicType() {}

        private static TypeObjectDynamic build() {
            var trivialIdentityAspects = new TList<Unique>();
            trivialIdentityAspects.add(GenBuiltinInterfaces.builtinBindingIfaceIdentity());
            return new TypeObjectDynamic(
                    new TypeObjectMeta(GenTypeIds.BINDING_TYPE_ID, "binding", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
                    JavaInternalName.fromClass(Binding.class),
                    JavaDataDescriptor.fromClass(Binding.class),
                    Binding.class,
                    new TList<ObjectDataField>(),
                    new TList<ObjectMethod>(),
                    ObjectDropKind.DYNAMIC_LIGHT,
                    Opt.<JavaInternalName>none());
        }
    }
}
