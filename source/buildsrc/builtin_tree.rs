use {
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
};

pub enum BuiltinLeafSpec {
    PushFn(&'static str),
    Interface(&'static str),
    AspectId(&'static str),
    AnyConst {
        type_tokens: TokenStream,
    },
    InitialBind {
        dynamic_key_scope_id: usize,
        type_tokens: TokenStream,
    },
    Constructor {
        push_fn_name: &'static str,
        type_tokens: TokenStream,
    },
}

pub enum BuiltinTreeNode {
    Branch(Vec<(&'static str, BuiltinTreeNode)>),
    Leaf(BuiltinLeafSpec),
}

const KEY_LOG: &str = "log";
const KEY_DEFINE: &str = "define";
const KEY_TYPES: &str = "types";
const KEY_LIST: &str = "list";
const KEY_MAP: &str = "map";
const KEY_OPTION: &str = "option";
const KEY_STRING: &str = "string";
const KEY_NUMBER: &str = "number";
const KEY_NULL: &str = "null";
const KEY_BOOL: &str = "bool";
const KEY_ANY: &str = "any";
const KEY_CONSTKEY: &str = "constkey";
const KEY_EXPR: &str = "expr";
const KEY_TYPE: &str = "type";
const KEY_ASPECT: &str = "aspect";
const KEY_CODEDYN: &str = "codedyn";
const KEY_CVE: &str = "cve";
const KEY_EVAL: &str = "eval";
const KEY_ASPECT_LOOKUP: &str = "aspect_lookup";
const KEY_CAST_VALUE: &str = "cast_value";
const KEY_CONST_EVAL: &str = "const_eval";
const KEY_CODEVALUE: &str = "codevalue";
const KEY_STR_TO_JSON: &str = "str_to_json";
const KEY_CONSTKEY_TO_STR: &str = "constkey_to_str";
const KEY_GEN_BINDING_NAME: &str = "gen_binding_name";
const KEY_NEW_EXPR_CONTEXT: &str = "new_expr_context";
const KEY_ASPECT_FUNCTION: &str = "aspect_function";
const KEY_ASPECT_FIELD: &str = "aspect_field";
const KEY_ASPECT_STRING_DYNAMIC: &str = "aspect_string_dynamic";
const KEY_ASPECT_FUNCTION_ID: &str = "aspect_function_id";
const KEY_ASPECT_FIELD_ID: &str = "aspect_field_id";
const KEY_BINDING: &str = "binding";
const KEY_TARGET: &str = "target";
const KEY_CODE_DYN: &str = "code_dyn";

const ASPECT_COMPILE_SCOPE_ID: usize = 7023;
const CAST_VALUE_COMPILE_SCOPE_ID: usize = 7024;

pub fn builtin_tree() -> BuiltinTreeNode {
    use BuiltinLeafSpec::*;
    use BuiltinTreeNode::{
        Branch,
        Leaf,
    };
    Branch(vec![
        (KEY_LOG, Leaf(PushFn(KEY_LOG))),
        (KEY_DEFINE, Leaf(PushFn(KEY_DEFINE))),
        (KEY_TYPES, Branch(vec![
            (KEY_LIST, Leaf(Constructor {
                push_fn_name: KEY_LIST,
                type_tokens: quote!(crate::eval::value::type_list_constructor::r#const::TypeListConstructorConst),
            })),
            (KEY_MAP, Leaf(Constructor {
                push_fn_name: KEY_MAP,
                type_tokens: quote!(crate::eval::value::type_map_constructor::r#const::TypeMapConstructorConst),
            })),
            (KEY_OPTION, Leaf(Constructor {
                push_fn_name: KEY_OPTION,
                type_tokens: quote!(crate::eval::value::type_option_constructor::r#const::TypeOptionConstructorConst),
            })),
            (KEY_STRING, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_string::dynamic::TypeStringDynamic),
            })),
            (KEY_NUMBER, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_number::dynamic::TypeNumberDynamic),
            })),
            (KEY_NULL, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_null::dynamic::TypeNullDynamic),
            })),
            (KEY_BOOL, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_bool::dynamic::TypeBoolDynamic),
            })),
            (KEY_ANY, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
            })),
            (KEY_CONSTKEY, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_constkey::dynamic::TypeConstkeyDynamic),
            })),
            (KEY_EXPR, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_expr::TypeExpr),
            })),
            (KEY_TYPE, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
            })),
            (KEY_ASPECT, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic),
            })),
            (KEY_CODEDYN, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_code_dyn_handle::dynamic::TypeCodeDynHandleDynamic),
            })),
            (KEY_CVE, Leaf(AnyConst {
                type_tokens: quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic),
            })),
        ])),
        (KEY_EVAL, Branch(vec![
            (KEY_ASPECT_LOOKUP, Leaf(InitialBind {
                dynamic_key_scope_id: ASPECT_COMPILE_SCOPE_ID,
                type_tokens: quote!(crate::eval::value::type_function_aspect::dynamic::TypeFunctionAspectDynamic),
            })),
            (KEY_CAST_VALUE, Leaf(InitialBind {
                dynamic_key_scope_id: CAST_VALUE_COMPILE_SCOPE_ID,
                type_tokens: quote!(
                    crate::eval::value::type_function_cast_value::dynamic::TypeFunctionCastValueDynamic
                ),
            })),
            (KEY_CONST_EVAL, Leaf(PushFn(KEY_CONST_EVAL))),
            (KEY_CODEVALUE, Leaf(PushFn(KEY_CODEVALUE))),
            (KEY_STR_TO_JSON, Leaf(PushFn(KEY_STR_TO_JSON))),
            (KEY_CONSTKEY_TO_STR, Leaf(PushFn(KEY_CONSTKEY_TO_STR))),
            (KEY_GEN_BINDING_NAME, Leaf(PushFn(KEY_GEN_BINDING_NAME))),
            (KEY_NEW_EXPR_CONTEXT, Leaf(PushFn(KEY_NEW_EXPR_CONTEXT))),
            (KEY_ASPECT_FUNCTION, Leaf(Interface(KEY_ASPECT_FUNCTION))),
            (KEY_ASPECT_FIELD, Leaf(Interface(KEY_ASPECT_FIELD))),
            (KEY_ASPECT_STRING_DYNAMIC, Leaf(AspectId(KEY_ASPECT_STRING_DYNAMIC))),
            (KEY_ASPECT_FUNCTION_ID, Leaf(AspectId(KEY_ASPECT_FUNCTION_ID))),
            (KEY_ASPECT_FIELD_ID, Leaf(AspectId(KEY_ASPECT_FIELD_ID))),
            (KEY_BINDING, Leaf(Interface(KEY_BINDING))),
            (KEY_TARGET, Leaf(Interface(KEY_TARGET))),
            (KEY_CODE_DYN, Leaf(Interface(KEY_CODE_DYN))),
        ])),
    ])
}

fn emit_node(node: &BuiltinTreeNode) -> TokenStream {
    match node {
        BuiltinTreeNode::Branch(children) => {
            let entries: Vec<TokenStream> = children.iter().map(|(k, child)| {
                let child_tokens = emit_node(child);
                quote!{ (#k, #child_tokens) }
            }).collect();
            quote!{
                crate::eval::builtin_tree::BuiltinNode::Branch(vec![#(#entries),*])
            }
        },
        BuiltinTreeNode::Leaf(leaf) => {
            let leaf_tokens = emit_leaf(leaf);
            quote!{ crate::eval::builtin_tree::BuiltinNode::Leaf(#leaf_tokens) }
        },
    }
}

fn emit_leaf(leaf: &BuiltinLeafSpec) -> TokenStream {
    match leaf {
        BuiltinLeafSpec::PushFn(name) => {
            let fn_name = format_ident!("gen_fn_{}", *name);
            quote!{
                {
                    let (_n, k, t, v) = #fn_name(dynamic_scope);
                    let _ = _n;
                    crate::eval::builtin_tree::BuiltinLeaf::Function {
                        dynamic_key: k,
                        type_: t,
                        dynamic_value: v,
                    }
                }
            }
        },
        BuiltinLeafSpec::Interface(name) => {
            let fn_name = format_ident!("gen_iface_{}", *name);
            quote!{
                {
                    let (k, t, v) = #fn_name(dynamic_scope, &builtin_ifaces);
                    crate::eval::builtin_tree::BuiltinLeaf::Interface {
                        dynamic_key: k,
                        type_: t,
                        dynamic_value: v,
                    }
                }
            }
        },
        BuiltinLeafSpec::AspectId(key) => {
            let fn_name = format_ident!("gen_aspect_id_{}", *key);
            quote!{
                {
                    let (k, u, v) = #fn_name(dynamic_scope);
                    crate::eval::builtin_tree::BuiltinLeaf::AspectId {
                        dynamic_key: k,
                        unique: u,
                        dynamic_value: v,
                    }
                }
            }
        },
        BuiltinLeafSpec::AnyConst { type_tokens } => {
            quote!{
                crate::eval::builtin_tree::BuiltinLeaf::AnyConst {
                    type_: std::rc::Rc::new(#type_tokens) as std::rc::Rc<dyn crate::langhelp::Type>,
                }
            }
        },
        BuiltinLeafSpec::InitialBind { dynamic_key_scope_id, type_tokens } => {
            let id = *dynamic_key_scope_id;
            quote!{
                crate::eval::builtin_tree::BuiltinLeaf::InitialBind {
                    dynamic_key: crate::langhelp::BindingId::Builtin(#id),
                    type_: std::rc::Rc::new(#type_tokens) as std::rc::Rc<dyn crate::langhelp::Type>,
                }
            }
        },
        BuiltinLeafSpec::Constructor { push_fn_name, type_tokens } => {
            let fn_name = format_ident!("gen_fn_{}", *push_fn_name);
            quote!{
                {
                    let (_n, k, _t, v) = #fn_name(dynamic_scope);
                    let _ = _n;
                    let _ = _t;
                    crate::eval::builtin_tree::BuiltinLeaf::Constructor {
                        dynamic_key: k,
                        type_: std::rc::Rc::new(#type_tokens) as std::rc::Rc<dyn crate::langhelp::Type>,
                        dynamic_value: v,
                    }
                }
            }
        },
    }
}

pub fn emit_gen_builtin_tree(out: &mut TokenStream) {
    let tree = builtin_tree();
    let tree_tokens = emit_node(&tree);
    out.extend(quote!{
        pub(crate) fn gen_builtin_tree<'s>(
            dynamic_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            import_state: crate::langhelp::ImportStateId,
        ) -> crate::eval::builtin_tree::BuiltinNode<'s> {
            let builtin_ifaces = crate::r#gen::gen_build_builtin_interfaces(import_state);
            #tree_tokens
        }
    });
}
