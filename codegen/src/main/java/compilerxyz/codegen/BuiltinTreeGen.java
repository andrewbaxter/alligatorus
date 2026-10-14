package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TSet;
import java.util.LinkedHashMap;

public final class BuiltinTreeGen {
    private BuiltinTreeGen() {}

    public static final String GEN_PACKAGE = "compilerxyz.main.gen";

    static final long ASPECT_COMPILE_SCOPE_ID = 7023;
    static final long THROW_COMPILE_SCOPE_ID = 7025;
    static final long PANIC_COMPILE_SCOPE_ID = 7026;

    public static final ClassName BINDING = ClassName.get("compilerxyz.main.gen", "Binding");
    public static final ClassName ERESULT = ClassName.get("compilerxyz.main.eval", "EResult");
    public static final ClassName QEXPR_ID = ClassName.get("compilerxyz.main.eval", "QExprId");
    public static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");
    private static final ClassName CONST_KEY = ClassName.get("compilerxyz.main.eval", "ConstKey");
    private static final ClassName CONST_KEY_STR = ClassName.get("compilerxyz.main.eval", "ConstKey", "Str");
    private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.prelude.util", "TOrderedMap");
    private static final ClassName ARGUMENT_ENTRY =
            ClassName.get("compilerxyz.main.eval.value.argrecord", "ArgumentEntry");
    private static final ClassName VALUE = ClassName.get("compilerxyz.main.gen", "Value");
    private static final ClassName VALUE_EMPTY = ClassName.get("compilerxyz.main.eval.value.empty", "ValueEmpty");
    private static final ClassName VALUE_OBJECT_CONST =
            ClassName.get("compilerxyz.main.eval.value.object", "ValueObjectConst");
    private static final ClassName BINDING_VALUE = ClassName.get("compilerxyz.main.eval.binding", "BindingValue");
    private static final ClassName TYPE_ARGUMENT_RECORD =
            ClassName.get("compilerxyz.main.eval.value.argrecord", "TypeArgumentRecord");
    private static final ClassName ARGUMENT_RECORD_BINDING =
            ClassName.get("compilerxyz.main.eval.value.argrecord", "ArgumentRecordBinding");
    public static final CodeBlock TYPE_OF_TYPES_EXPR =
            CodeBlock.of("$T.plainTypeType()", ClassName.get("compilerxyz.main.eval.value.object", "ValueObjectConst"));
    private static final ClassName TYPE_UNIQUE_CONST =
            ClassName.get("compilerxyz.main.eval.value.unique", "TypeUniqueConst");
    public static final CodeBlock TUC_TYPE_EXPR = CodeBlock.of("new $T()", TYPE_UNIQUE_CONST);
    public static final ClassName TYPE_FUNCTION_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.function", "TypeFunctionConst");
    public static final ClassName IMPORT_STATE_ID = ClassName.get("compilerxyz.main.eval", "ImportStateId");
    public static final ClassName TYPE = ClassName.get("compilerxyz.main.eval", "Type");
    public static final ClassName TLIST = ClassName.get("compilerxyz.prelude.util", "TList");
    public static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.main.eval", "ExprContext");

    public sealed interface NodeSpec {}

    public record Child(String name, NodeSpec node) {}

    public record Branch(TList<Child> children) implements NodeSpec {}

    public record LeafSpec(CodeBlock data, CodeBlock type, boolean function) implements NodeSpec {}

    public static final class EmitState {
        int counter = 0;
        final CodeBlock.Builder body = CodeBlock.builder();

        String next(String prefix) {
            return "_" + prefix + counter++;
        }
    }

    public static String[] emitNode(NodeSpec node, EmitState st) {
        return switch (node) {
            case Branch b -> {
                var fields = st.next("fields");
                st.body.addStatement("var $N = new $T<$T, $T>()", fields, TORDERED_MAP, CONST_KEY, ARGUMENT_ENTRY);
                for (var c : b.children()) {
                    var childVars = emitNode(c.node(), st);
                    if (childVars.length == 1) {
                        st.body.addStatement("$N.put(new $T($S), $N)", fields, CONST_KEY_STR, c.name(), childVars[0]);
                        continue;
                    }
                    st.body.addStatement(
                            "$N.put(new $T($S), new $T($N, $N))",
                            fields,
                            CONST_KEY_STR,
                            c.name(),
                            ARGUMENT_ENTRY,
                            childVars[0],
                            childVars[1]);
                }
                var binding = st.next("binding");
                var type = st.next("type");
                st.body.addStatement("$T $N = new $T($N)", BINDING, binding, ARGUMENT_RECORD_BINDING, fields);
                st.body.addStatement("$T $N = $T.of($N)", TYPE, type, TYPE_ARGUMENT_RECORD, fields);
                yield new String[] {binding, type};
            }
            case LeafSpec ls -> {
                CodeBlock innerExpr = ls.data();
                CodeBlock valueTypeExpr = ls.type();

                var value = st.next("value");
                var entry = st.next("entry");
                if (ls.function()) {
                    st.body.addStatement("$T $N = new $T($L)", VALUE, value, VALUE_EMPTY, innerExpr);
                } else {
                    st.body.addStatement(
                            "$T $N = new $T(($T) $L, $L)",
                            VALUE,
                            value,
                            VALUE_OBJECT_CONST,
                            TYPE,
                            valueTypeExpr,
                            innerExpr);
                }
                st.body.addStatement(
                        "$T $N = new $T(new $T($N), $N.valueType())",
                        ARGUMENT_ENTRY,
                        entry,
                        ARGUMENT_ENTRY,
                        BINDING_VALUE,
                        value,
                        value);
                yield new String[] {entry};
            }
        };
    }

    public static final ClassName JAVA_INTERNAL_NAME =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName");

    public static void addSpecial(LinkedHashMap<String, LeafSpec> m, String path, CodeBlock data, CodeBlock type) {
        if (m.put(path, new LeafSpec(data, type, false)) != null) {
            throw new IllegalStateException("duplicate special path: " + path);
        }
    }

    private static final IMap<String, IList<String>> BRANCH_ORDER = IMap.ofEntries(
            new Pair<>("", IList.of("throw", "panic", "log", "define", "types", "eval")),
            new Pair<>(
                    "types",
                    IList.of(
                            "list",
                            "map",
                            "option",
                            "eresult",
                            "code",
                            "string",
                            "int",
                            "null",
                            "bool",
                            "binding",
                            "bind_result",
                            "constkey",
                            "expr",
                            "type",
                            "aspect",
                            "codedyn",
                            "cve")),
            new Pair<>(
                    "eval",
                    IList.of(
                            "aspect_lookup",
                            "cast_value",
                            "const_eval",
                            "codevalue",
                            "codevalue_dynamic",
                            "str_to_json",
                            "constkey_to_str",
                            "gen_binding_name",
                            "new_expr_context",
                            "aspect_function",
                            "aspect_field",
                            "aspect_drop_dynamic",
                            "aspect_bind_dynamic",
                            "aspect_consume_dynamic",
                            "bind_result",
                            "aspect_bind",
                            "aspect_initial_bind",
                            "aspect_lift",
                            "aspect_string_dynamic",
                            "aspect_function_id",
                            "aspect_field_id",
                            "aspect_bind_id",
                            "aspect_drop_dynamic_id",
                            "aspect_consume_dynamic_id",
                            "aspect_bind_dynamic_id",
                            "aspect_set_id",
                            "aspect_operator_add_id",
                            "aspect_operator_lt_id",
                            "operator_lt",
                            "aspect_field_use_dynamic",
                            "aspect_field_use_dynamic_id",
                            "aspect_initial_bind_id",
                            "aspect_union",
                            "aspect_union_id",
                            "aspect_union_variant",
                            "binding",
                            "target",
                            "code_dyn",
                            "value",
                            "type")));

    public record TreeNode(LinkedHashMap<String, TreeNode> children, Opt<LeafSpec> leaf) {
        TreeNode() {
            this(new LinkedHashMap<>(), Opt.none());
        }

        TreeNode(LeafSpec leaf) {
            this(new LinkedHashMap<>(), Opt.some(leaf));
        }
    }

    public static NodeSpec toNodeSpec(TreeNode n, String branchPath) {
        if (n.leaf().optIsSome()) return n.leaf().optUnwrap();
        var children = new TList<Child>();
        var order = BRANCH_ORDER.getOpt(branchPath);
        var remaining = new LinkedHashMap<>(n.children());
        if (order.optIsSome()) {
            for (var name : order.optUnwrap()) {
                var child = remaining.remove(name);
                if (child == null) continue;
                children.add(new Child(name, toNodeSpec(child, branchPath.isEmpty() ? name : branchPath + "." + name)));
            }
        }
        for (var e : remaining.entrySet()) {
            children.add(new Child(
                    e.getKey(),
                    toNodeSpec(e.getValue(), branchPath.isEmpty() ? e.getKey() : branchPath + "." + e.getKey())));
        }
        return new Branch(children);
    }

    public static void collectAndValidate(NodeSpec node, String path, TSet<String> seenNames, TSet<Long> scopeIds) {
        switch (node) {
            case Branch b -> {
                var seenChildren = new TSet<String>();
                for (var c : b.children()) {
                    if (!seenChildren.add(c.name())) {
                        throw new IllegalStateException(
                                "duplicate child name at " + (path.isEmpty() ? "<root>" : path) + ": " + c.name());
                    }
                    collectAndValidate(
                            c.node(), path.isEmpty() ? c.name() : path + "/" + c.name(), seenNames, scopeIds);
                }
            }
            case LeafSpec ls -> {}
        }
    }
}
