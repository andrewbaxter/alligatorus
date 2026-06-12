//! `Unique` as a value, plus the `TypeUnique` that types it. Used to expose
//! aspect ids (like `aspect_string_dynamic`) as scope-usable values that can
//! be passed to `.aspect(...)` lookups — both in const-eval and (via
//! `DynamicWrap`) v8.

use {
    crate::{
        builtin::{
            AspectBind,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectOperatorEq,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_operator_eq_aspect_id,
        },
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
            ScopeEntry,
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
    fn eq(&self, _xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        let base_u = crate::invariant::must_downcast_ref::<Unique>(base.value.as_ref()).clone();
        let ref_u = crate::invariant::must_downcast_ref::<Unique>(reference.value.as_ref()).clone();
        EvalRes::ok(CodeValue {
            code: Code { loc, code: crate::langhelp::Code_::None },
            value: Rc::new(crate::eval::value::value_bool::r#const::ValueBoolConst(base_u == ref_u)),
        })
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
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
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

pub(crate) fn type_unique_identity() -> Unique {
    Unique::Builtin {
        file: file!().to_string(),
        line: line!() as usize,
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
