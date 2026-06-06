use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectLiftDynamic,
            AspectNull,
            AspectStackValueDynamic,
            aspect_artifact_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_null_aspect_id,
            aspect_null_get,
            aspect_null_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicType,
        },
        eval::{
            Erb,
            value::{
                type_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                    bind_dynamic_some,
                },
                value_null::r#const::ValueNullConst,
            },
        },
        langhelp::{
            Artifact,
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            GlobalContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

pub(crate) struct TypeNullDynamic;

generate_builtin_artifact!(TypeNullDynamic, super::artifact::ArtifactDataNull);

impl Value for TypeNullDynamic {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeNullDynamic {
    fn desc(&self) -> String {
        return format!("mortar null");
    }

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
        return None;
    }

    fn type_to_serial(&self, _xx: &ExprContext, _gx: &GlobalContext, _loc: QExprId) -> Option<Artifact> {
        Some(Self::build_artifact(()))
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, type_).is_some()
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

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, source_type).is_some()
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
}

impl Aspect for TypeNullDynamic { }

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

impl AspectArtifact for TypeNullDynamic {
    fn to_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _loc: QExprId,
        _value: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        EResult::ok(Self::build_artifact(()))
    }

    fn from_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _loc: QExprId,
        _data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        Ok(Rc::new(ValueNullConst))
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

impl AspectBindDynamic for TypeNullDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| bind_dynamic_some(loc, c, vtype))
    }
}
