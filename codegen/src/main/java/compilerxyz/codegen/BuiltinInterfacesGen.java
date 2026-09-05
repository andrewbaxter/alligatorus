package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.util.BuiltinMethodEmit;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class BuiltinInterfacesGen {
    private BuiltinInterfacesGen() {}

    public record BuiltinSpec(
            String simpleName,
            String pkg,
            long uniqueId,
            long identityId,
            boolean isAspect,
            boolean isRecord,
            boolean isInterface,
            TList<BuiltinMethodEmit.Method> methods,
            TList<DataField> dataFields,
            Opt<String> outerSimpleName,
            Opt<String> nestedSimpleName) {
        public BuiltinSpec(
                String simpleName,
                String pkg,
                long uniqueId,
                long identityId,
                boolean isAspect,
                boolean isInterface,
                TList<BuiltinMethodEmit.Method> methods,
                TList<DataField> dataFields) {
            this(
                    simpleName,
                    pkg,
                    uniqueId,
                    identityId,
                    isAspect,
                    false,
                    isInterface,
                    methods,
                    dataFields,
                    Opt.none(),
                    Opt.none());
        }
    }

    public record DataField(String langName, String jvmFieldName, TypeName fieldJavaType) {}

    public static final String GEN_PACKAGE = "compilerxyz.main.gen";

    public static final ClassName TYPE = ClassName.get("compilerxyz.main.eval", "Type");
    public static final ClassName UNIQUE = ClassName.get("compilerxyz.main.eval", "Unique");
    public static final ClassName UNIQUE_BUILTIN = ClassName.get("compilerxyz.main.eval", "Unique", "Builtin");
    public static final ClassName ERESULT = ClassName.get("compilerxyz.main.eval", "EResult");
    public static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");
    public static final ClassName TLIST = ClassName.get("compilerxyz.prelude.util", "TList");
    public static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.main.eval", "ExprContext");
    public static final ClassName QEXPR_ID = ClassName.get("compilerxyz.main.eval", "QExprId");
    public static final ClassName ASPECT_LOOKUP = ClassName.get("compilerxyz.main.eval.aspect", "AspectLookup");

    public static final ClassName INTERFACE_DATA =
            ClassName.get("compilerxyz.main.eval.value.source_interface", "InterfaceData");
    public static final ClassName TYPE_OBJECT_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.object", "TypeObjectDynamic");
    public static final ClassName ILIST = ClassName.get("compilerxyz.prelude.util", "IList");
    public static final ClassName TYPE_OBJECT_META =
            ClassName.get("compilerxyz.main.eval.value.object", "TypeObjectMeta");
    public static final ClassName TYPE_OBJECT_CAST_RULE =
            ClassName.get("compilerxyz.main.eval.value.object", "TypeObjectCastRule");
    public static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
            ClassName.get("compilerxyz.main.eval.value.object", "TypeObjectCastFromRule");
    public static final ClassName OBJECT_METHOD = ClassName.get("compilerxyz.main.eval.value.object", "ObjectMethod");
    public static final ClassName OBJECT_DATA_FIELD =
            ClassName.get("compilerxyz.main.eval.value.object", "ObjectDataField");
    public static final ClassName OBJECT_DROP_KIND =
            ClassName.get("compilerxyz.main.eval.value.object", "ObjectDropKind");
    public static final ClassName OBJECT_ASPECT_SOURCE =
            ClassName.get("compilerxyz.main.eval.value.object", "ObjectAspectSource");
    public static final ClassName OBJECT_ASPECT_SOURCE_STATIC =
            ClassName.get("compilerxyz.main.eval.value.object", "ObjectAspectSource", "Static");
    public static final ClassName JAVA_INTERNAL_NAME =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName");
    public static final ClassName JAVA_DATA_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaDataDescriptor");

    public static void emitTypeHostRestamp(MethodSpec.Builder b, ClassName interfaceRef) {
        var genTrivial = ClassName.get(GEN_PACKAGE, "GenTrivialIdentityAspects");
        b.beginControlFlow("if ($T.class.isAssignableFrom($T.class))", TYPE, interfaceRef);
        b.addStatement("trivialIdentityAspects.add($T.TYPE_IDENTITY)", genTrivial);
        b.addStatement("castRules.add(new $T.RestampToIdentity($T.TYPE_IDENTITY))", TYPE_OBJECT_CAST_RULE, genTrivial);
        b.endControlFlow();
        if (interfaceRef.equals(TYPE)) {
            b.addStatement(
                    "castRules.add(new $T.InvokeVirtualByDestIdentity($T.INTERFACE_CELL_IDENTITY,"
                            + " $T.TYPE_OBJECT_DYNAMIC_GET_INTERFACE_CELL))",
                    TYPE_OBJECT_CAST_RULE,
                    genTrivial,
                    ClassName.get(GEN_PACKAGE, "GenCodegenDescriptors"));
        }
    }

    public static String snake(String camel) {
        var out = new StringBuilder();
        for (var i = 0; i < camel.length(); i++) {
            var c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) out.append('_');
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }
}
