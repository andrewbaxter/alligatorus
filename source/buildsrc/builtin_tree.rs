use {
    crate::buildsrc::{
        aspects::{
            ASPECT_ID_FIELD_ID_NAME,
            ASPECT_ID_FUNCTION_ID_NAME,
            ASPECT_ID_STRING_DYNAMIC_NAME,
            IFACE_ASPECT_FIELD_NAME,
            IFACE_ASPECT_FUNCTION_NAME,
        },
        push_fns::{
            PUSHFN_CODEVALUE_NAME,
            PUSHFN_CONSTKEY_TO_STR_NAME,
            PUSHFN_CONST_EVAL_NAME,
            PUSHFN_DEFINE_NAME,
            PUSHFN_GEN_BINDING_NAME_NAME,
            PUSHFN_LIST_NAME,
            PUSHFN_LOG_NAME,
            PUSHFN_MAP_NAME,
            PUSHFN_NEW_EXPR_CONTEXT_NAME,
            PUSHFN_OPTION_NAME,
            PUSHFN_STR_TO_JSON_NAME,
        },
        traits::{
            IFACE_BINDING_NAME,
            IFACE_CODE_DYN_NAME,
            IFACE_TARGET_NAME,
        },
    },
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

const ASPECT_COMPILE_SCOPE_ID: usize = 7023;
const CAST_VALUE_COMPILE_SCOPE_ID: usize = 7024;

pub fn builtin_tree() -> BuiltinTreeNode {
    use BuiltinLeafSpec::*;
    use BuiltinTreeNode::{
        Branch,
        Leaf,
    };
    Branch(vec![
        ("log", Leaf(PushFn(PUSHFN_LOG_NAME))),
        ("define", Leaf(PushFn(PUSHFN_DEFINE_NAME))),
        ("types", Branch(vec![
            ("list", Leaf(Constructor {
                push_fn_name: PUSHFN_LIST_NAME,
                type_tokens: quote!(crate::eval::value::type_list_constructor::r#const::TypeListConstructorConst),
            })),
            ("map", Leaf(Constructor {
                push_fn_name: PUSHFN_MAP_NAME,
                type_tokens: quote!(crate::eval::value::type_map_constructor::r#const::TypeMapConstructorConst),
            })),
            ("option", Leaf(Constructor {
                push_fn_name: PUSHFN_OPTION_NAME,
                type_tokens: quote!(crate::eval::value::type_option_constructor::r#const::TypeOptionConstructorConst),
            })),
            ("string", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_string::dynamic::TypeStringDynamic),
            })),
            ("number", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_number::dynamic::TypeNumberDynamic),
            })),
            ("null", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_null::dynamic::TypeNullDynamic),
            })),
            ("bool", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_bool::dynamic::TypeBoolDynamic),
            })),
            ("any", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
            })),
            ("constkey", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_constkey::dynamic::TypeConstkeyDynamic),
            })),
            ("expr", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_expr::TypeExpr),
            })),
            ("type", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
            })),
            ("aspect", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic),
            })),
            ("codedyn", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::value::type_code_dyn_handle::dynamic::TypeCodeDynHandleDynamic),
            })),
            ("cve", Leaf(AnyConst {
                type_tokens: quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic),
            })),
        ])),
        ("eval", Branch(vec![
            ("aspect_lookup", Leaf(InitialBind {
                dynamic_key_scope_id: ASPECT_COMPILE_SCOPE_ID,
                type_tokens: quote!(crate::eval::value::type_function_aspect::dynamic::TypeFunctionAspectDynamic),
            })),
            ("cast_value", Leaf(InitialBind {
                dynamic_key_scope_id: CAST_VALUE_COMPILE_SCOPE_ID,
                type_tokens: quote!(
                    crate::eval::value::type_function_cast_value::dynamic::TypeFunctionCastValueDynamic
                ),
            })),
            ("const_eval", Leaf(PushFn(PUSHFN_CONST_EVAL_NAME))),
            ("codevalue", Leaf(PushFn(PUSHFN_CODEVALUE_NAME))),
            ("str_to_json", Leaf(PushFn(PUSHFN_STR_TO_JSON_NAME))),
            ("constkey_to_str", Leaf(PushFn(PUSHFN_CONSTKEY_TO_STR_NAME))),
            ("gen_binding_name", Leaf(PushFn(PUSHFN_GEN_BINDING_NAME_NAME))),
            ("new_expr_context", Leaf(PushFn(PUSHFN_NEW_EXPR_CONTEXT_NAME))),
            ("aspect_function", Leaf(Interface(IFACE_ASPECT_FUNCTION_NAME))),
            ("aspect_field", Leaf(Interface(IFACE_ASPECT_FIELD_NAME))),
            ("aspect_string_dynamic", Leaf(AspectId(ASPECT_ID_STRING_DYNAMIC_NAME))),
            ("aspect_function_id", Leaf(AspectId(ASPECT_ID_FUNCTION_ID_NAME))),
            ("aspect_field_id", Leaf(AspectId(ASPECT_ID_FIELD_ID_NAME))),
            ("binding", Leaf(Interface(IFACE_BINDING_NAME))),
            ("target", Leaf(Interface(IFACE_TARGET_NAME))),
            ("code_dyn", Leaf(Interface(IFACE_CODE_DYN_NAME))),
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
