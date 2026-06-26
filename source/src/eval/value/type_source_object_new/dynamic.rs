use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            definition_set::DefinitionSet,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            from_dynamic_ref,
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_root::r#const::TypeRoot,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_object::{
                    SourceDataMethodData,
                    SourceObjectData,
                },
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
            },
        },
        invariant::DeserialLazy,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            Counter,
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
            AspectLift,
            AspectStackValueDynamic,
            DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        cell::Cell,
        rc::Rc,
    },
    super::TypeSourceObjectNewConst,
};

pub(crate) struct TypeSourceObjectNewDynamic;

impl Aspect for TypeSourceObjectNewDynamic { }

impl AspectBind for TypeSourceObjectNewDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceObjectNewDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceObjectNewDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceObjectNewDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceObjectNewDynamic),
        })
    }
}

impl AspectLift for TypeSourceObjectNewDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let ctor_ref: Rc<TypeSourceObjectNewConst> =
            from_dynamic_ref::<Rc<TypeSourceObjectNewConst>>(&value).clone();
        let ctor_type: Rc<dyn Type> = Rc::new(TypeSourceObjectNewConst { data: ctor_ref.data.clone() });
        Ok(Rc::new(ValueAnyConst { inner: ctor_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceObjectNewDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceObjectNewDynamic),
            }),
        }
    }
}

impl ExporterAny for TypeSourceObjectNewDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeSourceObjectNewDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeSourceObjectNewDynamic));
    }
}

impl Type for TypeSourceObjectNewDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceObjectNewDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceObjectNewDynamic) as Box<dyn AspectConsumeDynamic>),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceObjectNewDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceObjectNewDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceObjectNewDynamic) as Box<dyn AspectStackValueDynamic>),
            );
        }
        if *key == aspect_lift_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceObjectNewDynamic) as Box<dyn AspectLift>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceObjectNewDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to object_new"))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &type_type_identity()).is_none() {
            return EvalRes::err(loc, format!("Cannot convert {} to {}", self.desc(), type_.desc()));
        }
        let source_code = match cv.code.cast::<CodeDynamic>() {
            EResult { v: Some(Some(c)), .. } => c.clone(),
            _ => return EvalRes::err(
                loc,
                format!("cast {} → {}: source has no runtime code", self.desc(), type_.desc()),
            ),
        };
        let call = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(
                        CodeDynamicBindingUse { id: DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID },
                    ),
                }),
                args: vec![source_code],
            }),
        };
        let dest_rc =
            type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceObjectNewDynamic) as Rc<dyn Type>);
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(call)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_rc,
            }),
        })
    }

    fn desc(&self) -> String {
        format!("definition_set_object_new")
    }
}

impl Value for TypeSourceObjectNewDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeSourceObjectNewDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceObjectNewDynamic))
    }
}
