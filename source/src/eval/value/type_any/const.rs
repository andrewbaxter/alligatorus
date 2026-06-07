use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamicSub,
            AspectConsumeDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_sub_aspect_id,
            aspect_consume_dynamic_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
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

pub(crate) fn type_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}
pub(crate) struct TypeAnyConst;

impl Aspect for TypeAnyConst { }

impl AspectBind for TypeAnyConst {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(crate::langhelp::Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        crate::eval::value::type_dynamic::bind_dynamic(xx, loc, cv)
    }
}

impl AspectBindDynamicSub for TypeAnyConst {
    fn bind_dynamic_sub(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(crate::code_dynamic::CodeDynamic, crate::eval::aspect::ScopeEntryDynamicSub)> {
        crate::eval::value::type_dynamic::bind_dynamic_sub_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeAnyConst {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> EResult<CodeDynamic> {
        // If code already has JS (e.g. from Record/Tuple cast_to mortar case or
        // cast_from), pass through.
        if let Some(Some(js)) = cv.code.cast::<CodeDynamic>().v {
            return EResult::ok(js.clone());
        }

        // Compile-time: create external from the value's type
        let type_ = cv.value.type_(xx, loc);
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_)));
        EResult::ok(CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
        })
    }
}

impl Type for TypeAnyConst {
    fn desc(&self) -> String {
        "type".to_string()
    }

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
        if *key == aspect_bind_dynamic_sub_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectBindDynamicSub>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectBind>));
        }
        None
    }

    fn cast_to(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _cv: crate::langhelp::CodeValue,
        _type_: &dyn Type,
    ) -> crate::langhelp::EvalRes {
        crate::langhelp::EvalRes::err(loc, "Cannot convert type".to_string())
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn crate::langhelp::Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> crate::langhelp::EvalRes {
        let type_ = cv.value.type_(xx, loc);
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_.clone())));
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
                })),
            },
            value: type_.as_value(),
        })
    }
}

impl Value for TypeAnyConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
