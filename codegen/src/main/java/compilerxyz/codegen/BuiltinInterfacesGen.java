package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.util.BuiltinMethodEmit;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

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
            Opt<String> nestedSimpleName,
            boolean identityToken,
            boolean lift,
            TList<String> permitted,
            Opt<String> exporter,
            TList<String> superTypes) {
        public BuiltinSpec(
                String simpleName,
                String pkg,
                long uniqueId,
                long identityId,
                boolean isAspect,
                TList<BuiltinMethodEmit.Method> methods,
                TList<DataField> dataFields,
                TList<String> permitted,
                Opt<String> exporter,
                TList<String> superTypes) {
            this(
                    simpleName,
                    pkg,
                    uniqueId,
                    identityId,
                    isAspect,
                    false,
                    true,
                    methods,
                    dataFields,
                    Opt.none(),
                    Opt.none(),
                    false,
                    true,
                    permitted,
                    exporter,
                    superTypes);
        }

        public String fqName() {
            return outerSimpleName.optIsNone()
                    ? pkg + "." + simpleName
                    : pkg + "." + outerSimpleName.optUnwrap() + "." + nestedSimpleName.optUnwrap();
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
    public static final ClassName OBJECT_LIFT_KIND =
            ClassName.get("compilerxyz.main.eval.value.object", "ObjectLiftKind");
    public static final ClassName OBJECT_DROP_KIND =
            ClassName.get("compilerxyz.main.eval.value.object", "ObjectDropKind");
    public static final ClassName OBJECT_ASPECT_SOURCE =
            ClassName.get("compilerxyz.main.eval.value.object", "ObjectAspectSource");
    public static final ClassName OBJECT_ASPECT_SOURCE_STATIC =
            ClassName.get("compilerxyz.main.eval.value.object", "ObjectAspectSource", "Static");
    public static final ClassName JAVA_INTERNAL_NAME =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName");
    public static final ClassName VALUE = ClassName.get("compilerxyz.main.gen", "Value");
    public static final ClassName EXPORTER_ROOT = ClassName.get("compilerxyz.main.artifact", "ExporterRoot");
    public static final ClassName ARTIFACT_EXPORTER = ClassName.get("compilerxyz.main.artifact", "ArtifactExporter");
    public static final ClassName ARTIFACT_EXPORTER_HOST_TYPE =
            ClassName.get("compilerxyz.main.artifact", "ArtifactExporterHostType");
    public static final ClassName ARTIFACT_EXPORTER_HOST_VALUE =
            ClassName.get("compilerxyz.main.artifact", "ArtifactExporterHostValue");

    public static final String HOST_TYPE_FQ = "compilerxyz.main.eval.Type";
    public static final String HOST_VALUE_FQ = "compilerxyz.main.gen.Value";

    public enum HostKind {
        TYPE,
        VALUE
    }

    public static Opt<HostKind> hostKind(BuiltinSpec spec, TMap<String, BuiltinSpec> specsByFq) {
        var fq = spec.fqName();
        if (fq.equals(HOST_TYPE_FQ)) return Opt.some(HostKind.TYPE);
        if (fq.equals(HOST_VALUE_FQ)) return Opt.some(HostKind.VALUE);
        Opt<HostKind> found = Opt.none();
        for (var sup : spec.superTypes()) {
            Opt<HostKind> supKind;
            if (sup.equals(HOST_TYPE_FQ)) {
                supKind = Opt.some(HostKind.TYPE);
            } else if (sup.equals(HOST_VALUE_FQ)) {
                supKind = Opt.some(HostKind.VALUE);
            } else {
                var supSpec = specsByFq.getOpt(sup);
                supKind = supSpec.optIsNone() ? Opt.none() : hostKind(supSpec.optUnwrap(), specsByFq);
            }
            if (supKind.optIsNone()) continue;
            if (supKind.optUnwrap() == HostKind.TYPE) return Opt.some(HostKind.TYPE);
            found = supKind;
        }
        return found;
    }

    public static CodeBlock exporterExpr(BuiltinSpec spec, Opt<HostKind> hostKind) {
        if (spec.exporter().optIsSome()) {
            return CodeBlock.of(
                    "$T.<$T>some($T.$L)",
                    OPT,
                    ARTIFACT_EXPORTER,
                    EXPORTER_ROOT,
                    spec.exporter().optUnwrap());
        }
        if (hostKind.optIsNone()) return CodeBlock.of("$T.<$T>none()", OPT, ARTIFACT_EXPORTER);
        return CodeBlock.of(
                "$T.<$T>some($T.INSTANCE)",
                OPT,
                ARTIFACT_EXPORTER,
                hostKind.optUnwrap() == HostKind.TYPE ? ARTIFACT_EXPORTER_HOST_TYPE : ARTIFACT_EXPORTER_HOST_VALUE);
    }

    public static final ClassName JAVA_DATA_DESCRIPTOR =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaDataDescriptor");

    public static void emitTypeHostRestamp(MethodSpec.Builder b, ClassName interfaceRef) {
        var genTrivial = ClassName.get(GEN_PACKAGE, "GenTrivialIdentityAspects");
        b.beginControlFlow("if ($T.class.isAssignableFrom($T.class))", TYPE, interfaceRef);
        b.addStatement("trivialIdentityAspects.add($T.TYPE_IDENTITY)", genTrivial);
        b.addStatement("castRules.add(new $T.RestampToIdentity($T.TYPE_IDENTITY))", TYPE_OBJECT_CAST_RULE, genTrivial);
        b.endControlFlow();
        b.beginControlFlow("if ($T.class.isAssignableFrom($T.class))", VALUE, interfaceRef);
        b.addStatement("trivialIdentityAspects.add($T.VALUE_IDENTITY)", genTrivial);
        b.addStatement("castRules.add(new $T.RestampToIdentity($T.VALUE_IDENTITY))", TYPE_OBJECT_CAST_RULE, genTrivial);
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
