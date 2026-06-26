use {
    crate::{
        artifact::ExporterAny,
        code_dynamic::{
            CodeDynamic,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_consume_to_dynamic,
                dynamic_stack_to_value,
            },
            value::{
                type_any::r#const::TypeAnyConst,
                type_root::r#const::TypeRoot,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
            },
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
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectStackValueDynamic,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactConstkeyDynamic;
#[derive(Clone)]
pub(crate) struct TypeConstkeyDynamic;

impl Aspect for TypeConstkeyDynamic { }

impl AspectBind for TypeConstkeyDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeConstkeyDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeConstkeyDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeConstkeyDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeConstkeyDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeConstkeyDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeConstkeyDynamic))
    }
}

impl ExporterAny for TypeConstkeyDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeConstkeyDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeConstkeyDynamic));
    }
}

impl Type for TypeConstkeyDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeConstkeyDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeConstkeyDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeConstkeyDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeConstkeyDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeConstkeyDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeConstkeyDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_string_const_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let source_type = erb.must_er(cv.value.type_(xx, loc))?;
            if let Some(aspect_string) = aspect_string_const_get_maybe(xx, source_type.as_ref()) {
                let s = aspect_string.get(cv.value.as_ref());
                let canonical = ConstKey::String(s).to_str();
                return Ok(CodeValue {
                    code: Code {
                        loc,
                        code: Code_::Dyn(Rc::new(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::Literal(serde_json::Value::String(canonical)),
                        })),
                    },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeConstkeyDynamic),
                    }),
                });
            }
            return erb.err_new(format!("Cannot convert {} to constkey", source_type.desc()));
        });
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert constkey".to_string())
    }

    fn desc(&self) -> String {
        "constkey".to_string()
    }
}

impl Value for TypeConstkeyDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeConstkeyDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::ConstkeyDynamic))
    }
}
