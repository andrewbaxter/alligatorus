//! `Unique` as a value, plus the `TypeUnique` that types it. Used to expose
//! aspect ids (like `aspect_string_dynamic`) as scope-usable values that can
//! be passed to `.aspect(...)` lookups — both in const-eval and (via
//! `DynamicWrap`) v8.

use crate::r#gen::ScopeEntry;
use crate::r#gen::type_unique_identity;
use {
    crate::{
        r#gen::{AspectBind, AspectConsumeDynamic, AspectInitialBind, AspectOperatorEq, aspect_bind_aspect_id, aspect_consume_dynamic_aspect_id, aspect_initial_bind_aspect_id, aspect_operator_eq_aspect_id},
        code_dynamic::CodeDynamic,
        eval::{
            dynamic_wrap::DynamicWrap,
            from_v8_ref,
            to_v8,
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
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

pub(crate) struct TypeUnique;

impl Value for Unique {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeUnique)
    }
}

impl Aspect for TypeUnique { }

impl Value for TypeUnique {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_any::r#const::TypeAnyConst)
    }
}

impl AspectBind for TypeUnique {
    fn bind(
        &self,
        _xx: &ExprContext,
        _loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((
            cv.code,
            Box::new(super::tuple::scope_entry::ScopeEntryValue { value: cv.value }) as Box<dyn ScopeEntry>,
        ))
    }
}

impl AspectConsumeDynamic for TypeUnique {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectOperatorEq for TypeUnique {
    fn eq(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        let base_const = (base.value.as_ref() as &dyn std::any::Any).downcast_ref::<Unique>().cloned();
        let ref_const = (reference.value.as_ref() as &dyn std::any::Any).downcast_ref::<Unique>().cloned();
        if let (Some(b), Some(r)) = (&base_const, &ref_const) {
            return EvalRes::ok(CodeValue {
                code: Code { loc, code: crate::langhelp::Code_::None },
                value: Rc::new(crate::eval::value::value_bool::r#const::ValueBoolConst(b == r)),
            });
        }
        // At least one side is dynamic — emit a runtime `===` (Unique
        // externals in v8 are reference-identical for equal Uniques because
        // they're cached per-Unique by `to_v8`).
        let base_dyn =
            match crate::r#gen::aspect_consume_dynamic_get(xx, loc, self).into_res() {
                Ok(c) => match c.to_dynamic(xx, loc, base).into_res() {
                    Ok(d) => d,
                    Err(e) => return EvalRes { v: None, errors: Some(e) },
                },
                Err(e) => return EvalRes { v: None, errors: Some(e) },
            };
        let ref_dyn =
            match crate::r#gen::aspect_consume_dynamic_get(xx, loc, self).into_res() {
                Ok(c) => match c.to_dynamic(xx, loc, reference).into_res() {
                    Ok(d) => d,
                    Err(e) => return EvalRes { v: None, errors: Some(e) },
                },
                Err(e) => return EvalRes { v: None, errors: Some(e) },
            };
        let bool_type: Rc<dyn Type> = Rc::new(crate::eval::value::type_bool::dynamic::TypeBoolDynamic);
        let stack = match crate::r#gen::aspect_stack_value_dynamic_get(xx, loc, bool_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        // Emit a `__unique_eq(a, b)` call rather than JS `===`: each Unique
        // reaches v8 as a fresh `v8::External`, so reference equality doesn't
        // hold across distinct conversion sites.
        let unique_eq_fn_code = crate::code_dynamic::CodeDynamic {
            loc,
            type_: crate::code_dynamic::CodeDynamicType::BindingUse(
                crate::code_dynamic::CodeDynamicBindingUse {
                    id: crate::eval::UNIQUE_EQ_FN_SCOPE_ID,
                },
            ),
        };
        EvalRes::ok(stack.to_value(loc, crate::code_dynamic::CodeDynamic {
            loc,
            type_: crate::code_dynamic::CodeDynamicType::Call(
                crate::code_dynamic::CodeDynamicCall {
                    func: Box::new(unique_eq_fn_code),
                    args: vec![base_dyn, ref_dyn],
                },
            ),
        }))
    }
}

impl AspectInitialBind for TypeUnique {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeUnique),
        })
    }
}

impl DynamicWrap for Unique {
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        from_v8_ref::<Unique>(&*value).clone()
    }

    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeUnique)
    }
}

/// Scope entry for a Unique-typed binding that's *also* exposed in v8 (as an
/// External at `dynamic_key`). `use_` returns a `CodeValue` whose `code`
/// references the v8 binding and whose `value` is the actual `Unique` —
/// const-eval consumers (like `TypeAspectLookupFn.call`) can downcast the
/// value, while JS-target consumers see the binding.
pub(crate) struct ScopeEntryUniqueDualMode {
    pub dynamic_key: crate::langhelp::ScopeEntryId,
    pub unique: Unique,
}

impl ScopeEntry for ScopeEntryUniqueDualMode {
    fn use_(&self, _xx: &crate::langhelp::ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: crate::code_dynamic::CodeDynamicType::BindingUse(
                        crate::code_dynamic::CodeDynamicBindingUse { id: self.dynamic_key },
                    ),
                })),
            },
            value: Rc::new(self.unique.clone()),
        }
    }
}


impl Type for TypeUnique {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeUnique)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_unique_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeUnique) as Box<dyn AspectBind>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeUnique) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeUnique) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_operator_eq_aspect_id() {
            return Some(Box::new(Box::new(TypeUnique) as Box<dyn AspectOperatorEq>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &type_unique_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EvalRes {
        // unique → unique is identity.
        EvalRes::ok(cv)
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(cv)
    }

    fn desc(&self) -> String {
        "unique".to_string()
    }
}
