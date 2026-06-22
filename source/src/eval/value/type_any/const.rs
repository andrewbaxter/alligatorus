use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
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
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            External,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectArtifact,
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectFunction,
            AspectInitialBind,
            AspectType,
            ScopeEntry,
            aspect_artifact_aspect_id,
            aspect_artifact_get,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_field_get,
            aspect_function_aspect_id,
            aspect_function_get,
            aspect_initial_bind_aspect_id,
            aspect_type_aspect_id,
            aspect_type_get,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeAnyConst;

impl Aspect for TypeAnyConst { }

impl AspectArtifact for TypeAnyConst {
}

impl AspectBind for TypeAnyConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeAnyConst {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeAnyConst {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        if let Some(Some(js)) = cv.code.cast::<CodeDynamic>().v {
            return EResult::ok(js.clone());
        }
        let meta_type = cv.value.type_(xx, loc);
        let aspect_type = match aspect_type_get(xx, loc, meta_type.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return EResult {
                v: None,
                errors: Some(e),
            },
        };
        let type_ = aspect_type.type_(cv.value.as_ref());
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, External::Type(type_)));
        EResult::ok(CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
        })
    }
}

impl AspectField for TypeAnyConst {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let inner = {
            let wrapper = must_downcast_ref::<ValueAnyConst>(self_.value.as_ref());
            wrapper.inner.clone()
        };
        let field = match aspect_field_get(xx, loc, inner.as_ref()).into_res() {
            Ok(f) => f,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        field.field(xx, loc, self_, key)
    }

    fn field_type_opt(&self, _xx: &ExprContext, _key: &ConstKey) -> Option<Rc<dyn Type>> {
        None
    }
}

impl AspectFunction for TypeAnyConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes {
        let inner = {
            let wrapper = must_downcast_ref::<ValueAnyConst>(self_.value.as_ref());
            wrapper.inner.clone()
        };
        let func = match aspect_function_get(xx, loc, inner.as_ref()).into_res() {
            Ok(f) => f,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        func.call(xx, loc, self_, argument)
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

impl AspectType for TypeAnyConst {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let wrapper = must_downcast_ref::<ValueAnyConst>(value);
        wrapper.inner.clone()
    }
}

impl ExporterAny for TypeAnyConst {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
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
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectField>));
        }
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectFunction>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyConst) as Box<dyn AspectArtifact>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &type_type_identity()).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EvalRes {
        EvalRes::ok(cv)
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        let meta_type = cv.value.type_(xx, loc);
        let aspect_type = match aspect_type_get(xx, loc, meta_type.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let type_ = aspect_type.type_(cv.value.as_ref());
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, External::Type(type_)));
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
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

}

impl Value for TypeAnyConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeAnyConst))
    }
}

pub(crate) struct ValueAnyConst {
    pub inner: Rc<dyn Type>,
}

impl ExporterAny for ValueAnyConst {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for ValueAnyConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeAnyConst))
    }
}
