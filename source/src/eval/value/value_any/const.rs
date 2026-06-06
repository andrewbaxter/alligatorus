use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
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

pub(crate) struct TypeAnyConst;

pub(crate) fn type_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for TypeAnyConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
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
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectBindDynamic>));
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

impl Aspect for TypeAnyConst { }

impl AspectConsumeDynamic for TypeAnyConst {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> EResult<CodeDynamic> {
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

impl AspectBindDynamic for TypeAnyConst {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<
        (Option<CodeDynamic>, Box<dyn crate::eval::value::value_scope_entry::dynamic::DynamicScopeEntry>),
    > {
        self.to_js(xx, loc, cv).map(|js| {
            crate::eval::value::value_dynamic::bind_js_some(loc, js, Rc::new(TypeAnyDynamic))
        })
    }
}

// TypeMortarType — the mortar (v8) representation of a type value. In v8, types
// are always Rc`<dyn Type>` externals.
pub(crate) struct TypeAnyDynamic;

impl Value for TypeAnyDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}

impl Type for TypeAnyDynamic {
    fn desc(&self) -> String {
        "mortar type".to_string()
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeAnyDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectLiftDynamic>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> crate::langhelp::EvalRes {
        crate::langhelp::EvalRes::err(loc, "Cannot convert mortar type".to_string())
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> crate::langhelp::EvalRes {
        let type_ = cv.value.type_(xx, loc);
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
            value: Rc::new(crate::eval::value::value_dynamic::ValueDynamic {
                source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
                type_: Rc::new(TypeAnyDynamic),
            }),
        })
    }
}

impl Aspect for TypeAnyDynamic { }

impl AspectConsumeDynamic for TypeAnyDynamic {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        crate::eval::value::value_dynamic::mortar_consume_js_to_js(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeAnyDynamic {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<
        (Option<CodeDynamic>, Box<dyn crate::eval::value::value_scope_entry::dynamic::DynamicScopeEntry>),
    > {
        crate::eval::value::value_dynamic::mortar_bind_js(xx, loc, cv)
    }
}

impl AspectStackValueDynamic for TypeAnyDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        crate::eval::value::value_dynamic::mortar_js_stack_to_value(loc, value, Rc::new(TypeAnyDynamic))
    }
}

impl AspectLiftDynamic for TypeAnyDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_ = crate::eval::rc_type_from_v8(&*value);
        Ok(type_.as_value())
    }
}
