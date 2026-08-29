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
    static final long CAST_VALUE_COMPILE_SCOPE_ID = 7024;
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
    private static final ClassName ARGUMENT_RECORD =
            ClassName.get("compilerxyz.main.eval.value.argrecord", "ArgumentRecord");
    private static final ClassName ARGUMENT_RECORD_BINDING =
            ClassName.get("compilerxyz.main.eval.value.argrecord", "ArgumentRecordBinding");
    private static final ClassName TYPE_OBJECT_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectDynamic");
    private static final ClassName TYPE_OBJECT_CONST_CLS =
            ClassName.get("compilerxyz.main.eval.value.object_", "TypeObjectConst");
    public static final CodeBlock TOD_SELF_TYPE_EXPR = CodeBlock.of("$T.constType()", TYPE_OBJECT_DYNAMIC);
    public static final CodeBlock TYPE_WRAPPER_EXPR = CodeBlock.of(
            "$T.metaOf($T.class, $S)", TYPE_OBJECT_CONST_CLS, ClassName.get("compilerxyz.main.eval", "Type"), "type");
    private static final ClassName TYPE_UNIQUE_CONST =
            ClassName.get("compilerxyz.main.eval.value.unique", "TypeUniqueConst");
    public static final CodeBlock TUC_TYPE_EXPR = CodeBlock.of("new $T()", TYPE_UNIQUE_CONST);
    public static final ClassName TYPE_FUNCTION_DYNAMIC =
            ClassName.get("compilerxyz.main.eval.value.function", "TypeFunctionDynamic");
    public static final ClassName IMPORT_STATE_ID = ClassName.get("compilerxyz.main.eval", "ImportStateId");
    public static final ClassName TYPE = ClassName.get("compilerxyz.main.eval", "Type");
    public static final ClassName TLIST = ClassName.get("compilerxyz.prelude.util", "TList");
    public static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.main.eval", "ExprContext");

    public sealed interface NodeSpec {}

    public record Child(String name, NodeSpec node) {}

    public record Branch(TList<Child> children) implements NodeSpec {}

    public record LeafSpec(CodeBlock data, CodeBlock type) implements NodeSpec {

        public static LeafSpec of(CodeBlock data, CodeBlock type) {
            return new LeafSpec(data, type);
        }
    }

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
                    st.body.addStatement(
                            "$N.put(new $T($S), new $T($N, $N))",
                            fields,
                            CONST_KEY_STR,
                            c.name(),
                            ARGUMENT_ENTRY,
                            childVars[0],
                            childVars[1]);
                }
                var record = st.next("record");
                var binding = st.next("binding");
                var type = st.next("type");
                st.body.addStatement("var $N = new $T($N)", record, ARGUMENT_RECORD, fields);
                st.body.addStatement("$T $N = new $T($N)", BINDING, binding, ARGUMENT_RECORD_BINDING, record);
                st.body.addStatement("$T $N = $N", TYPE, type, record);
                yield new String[] {binding, type};
            }
            case LeafSpec ls -> {
                CodeBlock innerExpr = ls.data();
                CodeBlock valueTypeExpr = ls.type();

                var type = st.next("type");
                var wrapper = st.next("wrapper");
                var binding = st.next("binding");
                st.body.addStatement("$T $N = $L", Object.class, type, innerExpr);
                st.body.addStatement("$T $N = ($T) $L", TYPE, wrapper, TYPE, valueTypeExpr);
                st.body.addStatement(
                        "$T $N = new compilerxyz.main.eval.binding.BindingObjectConst($N, $N)",
                        BINDING,
                        binding,
                        wrapper,
                        type);
                yield new String[] {binding, wrapper};
            }
        };
    }

    public static final ClassName JAVA_INTERNAL_NAME =
            ClassName.get("compilerxyz.main.eval.dynamic", "JavaInternalName");

    public static void addSpecial(LinkedHashMap<String, LeafSpec> m, String path, CodeBlock data, CodeBlock type) {
        if (m.put(path, LeafSpec.of(data, type)) != null) {
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
                            "stack",
                            "str_to_json",
                            "constkey_to_str",
                            "gen_binding_name",
                            "new_expr_context",
                            "aspect_function",
                            "aspect_field",
                            "aspect_drop_dynamic",
                            "aspect_drop_dynamic_light",
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
                            "aspect_drop_dynamic_light_id",
                            "aspect_stack_value_dynamic_id",
                            "aspect_field_use_dynamic",
                            "aspect_element_use_dynamic",
                            "aspect_field_use_dynamic_id",
                            "aspect_element_use_dynamic_id",
                            "aspect_initial_bind_id",
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
        var order = BRANCH_ORDER.getOrNull(branchPath);
        var remaining = new LinkedHashMap<>(n.children());
        if (order != null) {
            for (var name : order) {
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
