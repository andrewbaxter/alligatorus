use {
    crate::{
        artifact::ExporterAny,
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
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
            AspectBoolDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectStackValueDynamic,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_bool_const_get_maybe,
            aspect_bool_dynamic_aspect_id,
            aspect_bool_dynamic_get_maybe,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeBoolDynamic;

impl Aspect for TypeBoolDynamic { }

impl AspectBind for TypeBoolDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeBoolDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectBoolDynamic for TypeBoolDynamic { }

impl AspectConsumeDynamic for TypeBoolDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeBoolDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeBoolDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeBoolDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeBoolDynamic))
    }
}

impl ExporterAny for TypeBoolDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeBoolDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeBoolDynamic));
    }
}

impl Type for TypeBoolDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeBoolDynamic)
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bool_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectBoolDynamic>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectInitialBind>));
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_bool_const_get_maybe(xx, source_type).is_some() ||
            aspect_bool_dynamic_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_bool_dynamic_get_maybe(xx, type_).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = erb.must_er(cv.value.type_(xx, loc))?;
            if let Some(aspect_bool) = aspect_bool_const_get_maybe(xx, type_.as_ref()) {
                let CodeValue { code, value } = cv;
                let mut seq = vec![];
                if let Some(c) = erb.must_er(code.cast::<CodeDynamic>())? {
                    seq.push(c.clone());
                };
                let bool_val = aspect_bool.get(value.as_ref());
                seq.push(CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::Literal(serde_json::Value::Bool(bool_val))
                });
                return Ok(CodeValue {
                    code: Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(CodeDynamic {
                            loc: loc,
                            type_: CodeDynamicType::Block(
                                CodeDynamicBlock { entries: seq.into_iter().map(CodeDynamicBlockEntry::Code).collect() }
                            )
                        }))
                    },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeBoolDynamic),
                    }),
                });
            } else {
                return Ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeBoolDynamic),
                    })
                });
            }
        });
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeBoolDynamic),
            }),
        })
    }

    fn desc(&self) -> String {
        return format!("dynamic bool");
    }
}

impl Value for TypeBoolDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeBoolDynamic);
    }

    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::BoolDynamic))
    }
}
