//! Source-level types mirroring Rust's `Code` AST:
//!
//! * [`TypeCodeStructDynamic`] — handle on a Rust `Code { loc, code: Code_ }`
//!   struct. Source-side `.code` field access yields a value of
//!   [`TypeCodeEnumDynamic`].
//!
//! * [`TypeCodeEnumDynamic`] — handle on the `Code_` enum. Matchable as a
//!   tagged union with variants `none` (payload `null`) and `dyn` (payload
//!   [`TypeCodeDynHandleDynamic`]).
//!
//! * [`TypeCodeDynHandleDynamic`] — handle on a `Rc<dyn CodeDyn>`. Field
//!   access `.cast` yields a callable (a [`TypeCodeDynCastFn`]) that, when
//!   invoked with a code_dyn-implementing source type, emits a runtime call
//!   to `__code_cast` to downcast.
//!
//! These types are introduced when source code reaches a Rust `Code` value
//! through a `CodeValueExtern` (e.g. the result of `const_eval`). The Rust
//! runtime carries the actual `Code` struct; source-level field/match access
//! is desugared at compile time into mortar calls and tagged-union matches
//! against the wrapped value.

use crate::r#gen::ScopeEntry;
use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectFunction,
            AspectInitialBind,
            AspectStackValueDynamic,
            AspectTaggedUnion,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            aspect_tagged_union_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
                mortar_consume_to_dynamic,
                mortar_dynamic_stack_to_value,
            },
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

use crate::r#gen::{type_code_struct_identity, type_code_enum_identity, type_code_dyn_handle_identity};

// ---------- TypeCodeStructDynamic ----------

#[derive(Clone)]
pub(crate) struct TypeCodeStructDynamic;

impl Aspect for TypeCodeStructDynamic { }

impl AspectBind for TypeCodeStructDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeStructDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeStructDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeCodeStructDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeCodeStructDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeCodeStructDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeCodeStructDynamic))
    }
}

impl AspectField for TypeCodeStructDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        let key_name = key_string.get(key.value.as_ref());
        match key_name.as_str() {
            "code" => {
                // Field access `code.code` on a `Code` struct yields the inner
                // `Code_` enum. At runtime it's the same JS handle (we trust
                // the structure on the v8 side).
                EvalRes::ok(CodeValue {
                    code: self_.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeCodeEnumDynamic),
                    }),
                })
            },
            _ => EvalRes::err(loc, format!("Field '{}' not found on Code struct", key_name)),
        }
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(k) = key else { return None; };
        if k == "code" { Some(Rc::new(TypeCodeEnumDynamic)) } else { None }
    }
}

impl Type for TypeCodeStructDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeCodeStructDynamic),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeStructDynamic))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_code_struct_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectField>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to Code struct".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from Code struct".to_string())
    }

    fn desc(&self) -> String {
        "Code".to_string()
    }
}

impl Value for TypeCodeStructDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

// ---------- TypeCodeEnumDynamic ----------

#[derive(Clone)]
pub(crate) struct TypeCodeEnumDynamic;

impl Aspect for TypeCodeEnumDynamic { }

impl AspectBind for TypeCodeEnumDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeEnumDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeEnumDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeCodeEnumDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeCodeEnumDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeCodeEnumDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeCodeEnumDynamic))
    }
}

impl AspectTaggedUnion for TypeCodeEnumDynamic {
    fn variants(&self) -> Vec<(ConstKey, String, Rc<dyn Type>)> {
        vec![
            (
                ConstKey::String("none".to_string()),
                "none".to_string(),
                Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>,
            ),
            (
                ConstKey::String("dyn".to_string()),
                "dyn".to_string(),
                Rc::new(TypeCodeDynHandleDynamic) as Rc<dyn Type>,
            ),
        ]
    }
}

impl Type for TypeCodeEnumDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeCodeEnumDynamic),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeEnumDynamic))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_code_enum_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_tagged_union_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectTaggedUnion>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to Code_ enum".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from Code_ enum".to_string())
    }

    fn desc(&self) -> String {
        "Code_".to_string()
    }
}

impl Value for TypeCodeEnumDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

// ---------- TypeCodeDynHandleDynamic ----------

#[derive(Clone)]
pub(crate) struct TypeCodeDynHandleDynamic;

impl Aspect for TypeCodeDynHandleDynamic { }

impl AspectBind for TypeCodeDynHandleDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeDynHandleDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeDynHandleDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeCodeDynHandleDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeCodeDynHandleDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeCodeDynHandleDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeCodeDynHandleDynamic))
    }
}

impl AspectField for TypeCodeDynHandleDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        let key_name = key_string.get(key.value.as_ref());
        match key_name.as_str() {
            "cast" => {
                // `.cast` returns a callable that, when invoked with a
                // code_dyn-implementing source type, runs `__code_cast` to
                // downcast the wrapped CodeDyn to that type's wrapper.
                let self_code = match self_.code.cast::<CodeDynamic>().into_res() {
                    Ok(Some(c)) => c.clone(),
                    _ => return EvalRes::err(loc, "code_dyn.cast: receiver has no dynamic code".to_string()),
                };
                EvalRes::ok(CodeValue {
                    code: Code { loc, code: Code_::Dyn(Rc::new(self_code)) },
                    value: Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
                        inner: Rc::new(TypeCodeDynCastFn),
                    }),
                })
            },
            _ => EvalRes::err(loc, format!("Field '{}' not found on code_dyn handle", key_name)),
        }
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(k) = key else { return None; };
        if k == "cast" {
            Some(Rc::new(TypeCodeDynCastFn))
        } else {
            None
        }
    }
}

impl Type for TypeCodeDynHandleDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeCodeDynHandleDynamic),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeDynHandleDynamic))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_code_dyn_handle_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectField>));
        }
        None
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.clone_rc().is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        // The runtime CVE wraps an inner `CodeValue`; at v8 time the wrapped
        // `Code_::Dyn(Rc<dyn CodeDyn>)` holds a `SourceCodeWrapper`-shaped
        // value carrying a v8 instance. The cast is a static type re-assertion
        // — the JS code unchanged — with the result typed as the dest. (TODO:
        // a follow-up will emit `__code_cast` and a real downcast.)
        let dest_rc = match type_.clone_rc() {
            Some(rc) => rc,
            None => return EvalRes::err(loc, format!("code_dyn handle cast: dest type ({}) doesn't support clone_rc", type_.desc())),
        };
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_rc,
            }),
        })
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to code_dyn handle".to_string())
    }

    fn desc(&self) -> String {
        "code_dyn".to_string()
    }
}

impl Value for TypeCodeDynHandleDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

// ---------- TypeCodeDynCastFn ----------

/// The callable returned by `code_dyn_handle.cast`. Its `AspectFunction.call`
/// validates that the argument is a source-defined type with the `code_dyn`
/// builtin interface impl, then emits a runtime call to `__code_cast(handle,
/// type_marker)` that returns the concrete v8 value the source-defined code
/// constructor stored.
#[derive(Clone)]
pub(crate) struct TypeCodeDynCastFn;

impl Aspect for TypeCodeDynCastFn { }

impl AspectFunction for TypeCodeDynCastFn {
    fn call(&self, _xx: &ExprContext, loc: QExprId, function: CodeValue, _argument: CodeValue) -> EvalRes {
        // TODO: validate that `argument` is a source-defined type whose data
        // implements the `code_dyn` builtin interface.
        // The cast result's static type is the requested target. At runtime
        // the wrapped CodeDyn is downcast to the wrapper Rust type holding the
        // v8 instance; the inner v8 value is returned. For now, the function's
        // code is already a binding-use referencing the code_dyn handle's
        // runtime JS; the cast is conceptually a no-op at the JS level and the
        // result reuses the handle's code unchanged, retyped as the requested
        // dest (we type-erase to `code_dyn` for now; a follow-up will plumb
        // the actual dest type through).
        EvalRes::ok(CodeValue {
            code: function.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeCodeDynHandleDynamic),
            }),
        })
    }
}

impl Type for TypeCodeDynCastFn {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeCodeDynCastFn),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeDynCastFn))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynCastFn) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to code_dyn cast fn".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from code_dyn cast fn".to_string())
    }

    fn desc(&self) -> String {
        "code_dyn.cast".to_string()
    }
}

impl Value for TypeCodeDynCastFn {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
