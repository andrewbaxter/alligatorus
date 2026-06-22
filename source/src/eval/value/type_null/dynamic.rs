use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            value::{
                type_any::r#const::TypeAnyConst,
                type_null::artifact::ArtifactNull,
                type_root::r#const::{
                    TypeRoot,
                    TypeRootExporter,
                },
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                value_null::r#const::ValueNullConst,
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
            AspectArtifact,
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectNull,
            AspectStackValueDynamic,
            AspectType,
            ScopeEntry,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_null_aspect_id,
            aspect_null_get,
            aspect_null_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
            aspect_type_aspect_id,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeNullDynamic;

impl Aspect for TypeNullDynamic { }

impl AspectArtifact for TypeNullDynamic {
    fn from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: Code {
                loc: loc,
                code: Code_::None,
            },
            value: Rc::new(ValueNullConst),
        })
    }

    fn to_serial(&self, _xx: &ExprContext, _loc: QExprId, _value: Rc<dyn Value>) -> EResult<Artifact> {
        EResult::ok(Self::build_artifact(()))
    }
}

impl AspectBind for TypeNullDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeNullDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeNullDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        match cv.code.code {
            Code_::Dyn(_) => cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone()),
            Code_::None => EResult::ok(CodeDynamic {
                loc,
                type_: CodeDynamicType::Literal(serde_json::Value::Null),
            }),
        }
    }
}

impl AspectInitialBind for TypeNullDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeNullDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeNullDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        _value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        Ok(Rc::new(ValueNullConst))
    }
}

impl AspectNull for TypeNullDynamic { }

impl AspectStackValueDynamic for TypeNullDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeNullDynamic),
            }),
        }
    }
}

impl AspectType for TypeNullDynamic {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeNullDynamic)
    }
}

impl ExporterAny for TypeNullDynamic {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeNullDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeNullDynamic)
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectLiftDynamic>));
        }
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectNull>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeNullDynamic) as Box<dyn AspectType>));
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, type_).is_some() || type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, ex: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(ex, loc);
            erb.must_er(aspect_null_get(ex, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeNullDynamic),
                }),
            });
        });
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeNullDynamic),
            }),
        })
    }

    fn desc(&self) -> String {
        return format!("dynamic null");
    }

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Artifact {
        Self::build_type_artifact()
    }
}

impl TypeRootExporter for TypeNullDynamic {
    fn self_from_artifact_data(_xx: &ExprContext, loc: QExprId, data: serde_json::Value) -> EResult<Rc<dyn Any>> {
        return EResult::from(
            loc,
            serde_json::from_value::<ArtifactNull>(data).map(|_| Rc::new(Self) as Rc<dyn Any>),
        );
    }

    fn self_to_artifact_data(_xx: &ExprContext, _loc: QExprId, _self_: Rc<dyn Any>) -> EResult<serde_json::Value> {
        return EResult::ok(serde_json::to_value(ArtifactNull).unwrap());
    }
}

impl Value for TypeNullDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::NullDynamic))
    }
}
