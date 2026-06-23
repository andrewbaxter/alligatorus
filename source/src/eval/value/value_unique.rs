use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::{
            from_v8_ref,
            push::Push,
            to_v8,
            value::{
                type_any::r#const::TypeAnyConst,
                type_bool::dynamic::TypeBoolDynamic,
                type_root::r#const::{
                    TypeRoot,
                    ExporterRootDelegate,
                },
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_bool::r#const::ValueBoolConst,
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
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectOperatorEq,
            ScopeEntry,
            UNIQUE_EQ_FN_SCOPE_ID,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_initial_bind_aspect_id,
            aspect_operator_eq_aspect_id,
            aspect_stack_value_dynamic_get,
            type_unique_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct ScopeEntryUniqueDualMode {
    pub dynamic_key: ScopeEntryId,
    pub unique: Unique,
}

impl ScopeEntry for ScopeEntryUniqueDualMode {
    fn use_(&self, _xx: &ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: self.dynamic_key }),
                })),
            },
            value: Rc::new(self.unique),
        }
    }
}
pub(crate) struct TypeUnique;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactUnique;

impl Aspect for TypeUnique { }

impl AspectBind for TypeUnique {
    fn bind(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok(
            (
                cv.code,
                Box::new(super::tuple::scope_entry::ScopeEntryValue { value: cv.value }) as Box<dyn ScopeEntry>,
            ),
        )
    }
}

impl AspectConsumeDynamic for TypeUnique {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
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

impl AspectOperatorEq for TypeUnique {
    fn eq(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        let base_const = (base.value.as_ref() as &dyn std::any::Any).downcast_ref::<Unique>().cloned();
        let ref_const = (reference.value.as_ref() as &dyn std::any::Any).downcast_ref::<Unique>().cloned();
        if let (Some(b), Some(r)) = (&base_const, &ref_const) {
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Rc::new(ValueBoolConst(b == r)),
            });
        }
        let base_dyn = match aspect_consume_dynamic_get(xx, loc, self).into_res() {
            Ok(c) => match c.to_dynamic(xx, loc, base).into_res() {
                Ok(d) => d,
                Err(e) => return EvalRes {
                    v: None,
                    errors: Some(e),
                },
            },
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let ref_dyn = match aspect_consume_dynamic_get(xx, loc, self).into_res() {
            Ok(c) => match c.to_dynamic(xx, loc, reference).into_res() {
                Ok(d) => d,
                Err(e) => return EvalRes {
                    v: None,
                    errors: Some(e),
                },
            },
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let bool_type: Rc<dyn Type> = Rc::new(TypeBoolDynamic);
        let stack = match aspect_stack_value_dynamic_get(xx, loc, bool_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let unique_eq_fn_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: UNIQUE_EQ_FN_SCOPE_ID }),
        };
        EvalRes::ok(stack.to_value(loc, CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(unique_eq_fn_code),
                args: vec![base_dyn, ref_dyn],
            }),
        }))
    }
}

impl ExporterAny for TypeUnique {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
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
        EvalRes::ok(cv)
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(cv)
    }

    fn desc(&self) -> String {
        "unique".to_string()
    }
}

impl ExporterRootDelegate for TypeUnique {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        loc: QExprId,
        _id: Option<&crate::artifact::ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        return EResult::from(
            loc,
            serde_json::from_value::<ArtifactUnique>(data).map(|_| Rc::new(Self) as Rc<dyn Any>),
        );
    }

    fn self_to_serial(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn Any>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        return EResult::ok(Artifact::Body(make_body(serde_json::to_value(ArtifactUnique).unwrap())));
    }
}

impl Value for TypeUnique {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::Unique))
    }
}

impl ExporterAny for Unique {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Push for Unique {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeUnique)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        *from_v8_ref::<Unique>(&*value)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self).into()
    }
}

impl Value for Unique {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeUnique))
    }
}
