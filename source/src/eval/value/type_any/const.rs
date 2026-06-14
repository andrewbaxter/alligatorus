use crate::r#gen::ScopeEntry;
use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectType,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_type_aspect_id,
            aspect_type_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
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
use crate::r#gen::type_type_identity;

pub(crate) struct TypeAnyConst;

/// Canonical value carrier for `TypeAnyConst`: a type-of-types value carries
/// an `Rc<dyn Type>` explicitly so `TypeAnyConst::AspectType::type_` can do
/// an infallible downcast and read out the inner type without RTTI over the
/// full list of type singletons. Per "one type = one representation": the
/// `TypeListDynamic` etc. structs are reserved for their list/map/option
/// runtime representations and aren't reused as values of `TypeAnyConst`.
pub(crate) struct ValueAnyConst {
    pub inner: Rc<dyn Type>,
}

impl Value for ValueAnyConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}

impl Aspect for TypeAnyConst { }

impl AspectType for TypeAnyConst {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let wrapper = crate::invariant::must_downcast_ref::<ValueAnyConst>(value);
        wrapper.inner.clone()
    }
}

/// Field access on a type-of-types value (`list(string).new`, `option.some`,
/// etc.) — extract the inner type via `AspectType` and delegate to its
/// `AspectField`. Receiver passed down is the original `cv` (so the inner
/// type's field handler sees the same wrapper-carrying value its dispatch
/// expects, and `cv.value.type_()` still reports `TypeAnyConst`).
impl crate::builtin::AspectField for TypeAnyConst {
    fn field(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_: CodeValue,
        key: CodeValue,
    ) -> crate::langhelp::EvalRes {
        let inner = {
            let wrapper = crate::invariant::must_downcast_ref::<ValueAnyConst>(self_.value.as_ref());
            wrapper.inner.clone()
        };
        let field = match crate::builtin::aspect_field_get(xx, loc, inner.as_ref()).into_res() {
            Ok(f) => f,
            Err(e) => return crate::langhelp::EvalRes { v: None, errors: Some(e) },
        };
        field.field(xx, loc, self_, key)
    }

    fn field_type_opt(
        &self,
        _xx: &ExprContext,
        _key: &crate::langhelp::ConstKey,
    ) -> Option<Rc<dyn Type>> {
        None
    }
}

/// Same delegation for calls (`list(string)`, `option(expr)`, etc.).
impl crate::builtin::AspectFunction for TypeAnyConst {
    fn call(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_: CodeValue,
        argument: CodeValue,
    ) -> crate::langhelp::EvalRes {
        let inner = {
            let wrapper = crate::invariant::must_downcast_ref::<ValueAnyConst>(self_.value.as_ref());
            wrapper.inner.clone()
        };
        let func = match crate::builtin::aspect_function_get(xx, loc, inner.as_ref()).into_res() {
            Ok(f) => f,
            Err(e) => return crate::langhelp::EvalRes { v: None, errors: Some(e) },
        };
        func.call(xx, loc, self_, argument)
    }
}

impl AspectBind for TypeAnyConst {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeAnyConst {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeAnyConst {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> EResult<CodeDynamic> {
        // If code already has JS (e.g. from Record/Tuple cast_to mortar case or
        // cast_from), pass through.
        if let Some(Some(js)) = cv.code.cast::<CodeDynamic>().v {
            return EResult::ok(js.clone());
        }

        // Compile-time: extract the actual `Rc<dyn Type>` via `AspectType` on
        // the value's meta-type, then push it as an `External::Type` and emit
        // a `BindingUse` to reach it at runtime.
        let meta_type = cv.value.type_(xx, loc);
        let aspect_type = match aspect_type_get(xx, loc, meta_type.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return EResult { v: None, errors: Some(e) },
        };
        let type_ = aspect_type.type_(cv.value.as_ref());
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_)));
        EResult::ok(CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
        })
    }
}

impl AspectInitialBind for TypeAnyConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeAnyConst),
        })
    }
}

impl Type for TypeAnyConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeAnyConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectBind>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectType>));
        }
        if *key == crate::builtin::aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn crate::builtin::AspectField>));
        }
        if *key == crate::builtin::aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn crate::builtin::AspectFunction>));
        }
        None
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, source_type: &dyn crate::langhelp::Type) -> bool {
        // Identity only; other types reach `TypeAny*` via their own `cast_to`.
        source_type.aspect(_xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, _loc: QExprId, cv: crate::langhelp::CodeValue) -> crate::langhelp::EvalRes {
        crate::langhelp::EvalRes::ok(cv)
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        // Lower a const type-value into a dynamic type-value (Rc<dyn Type> external).
        type_.aspect(_xx, &type_type_identity()).is_some()
    }

    fn cast_to(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: crate::langhelp::CodeValue,
        _type_: &dyn Type,
    ) -> crate::langhelp::EvalRes {
        let meta_type = cv.value.type_(xx, loc);
        let aspect_type = match aspect_type_get(xx, loc, meta_type.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return crate::langhelp::EvalRes { v: None, errors: Some(e) },
        };
        let type_ = aspect_type.type_(cv.value.as_ref());
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_)));
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
                })),
            },
            value: Rc::new(super::dynamic::TypeAnyDynamic),
        })
    }

    fn desc(&self) -> String {
        "type".to_string()
    }

    fn type_to_serial(
        &self,
        _xx: &ExprContext,
        _loc: QExprId,
    ) -> Option<crate::langhelp::Artifact> {
        Some(Self::build_type_artifact())
    }
}

generate_builtin_artifact_type!(TypeAnyConst, serde_json::Value);

impl Value for TypeAnyConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
