use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            export_artifact,
            import_type_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_set,
            },
            from_dynamic_ref,
            push::Push,
            to_dynamic_ext,
            typecheck_convert,
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_binding::dynamic::BindingDynamicBinding,
                type_null::dynamic::TypeNullDynamic,
                type_option::artifact::ArtifactOptionType,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
            },
        },
        langhelp::{
            Aspect,
            BindingId,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectFunction,
            AspectInitialBind,
            AspectSet,
            AspectStackValueDynamic,
            AspectUnion,
            Binding,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_set_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_stack_value_dynamic_get,
            aspect_string_const_get,
            aspect_union_aspect_id,
            type_option_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeOptionDynamic {
    pub(crate) element_type: Rc<dyn Type>,
}

impl Aspect for TypeOptionDynamic { }

impl AspectBind for TypeOptionDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeOptionDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeOptionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectField for TypeOptionDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, _self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = match key.value.as_ref().unwrap().type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_deref().unwrap());
        let make = |is_some: bool| -> EvalRes {
            let var_type: Rc<dyn Type> = Rc::new(TypeOptionVariantConstructorDynamic {
                element_type: self.element_type.clone(),
                is_some,
            });
            EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Some(Rc::new(ValueAnyConst { inner: var_type })),
            })
        };
        match key_name.as_str() {
            "some" => make(true),
            "none" => make(false),
            _ => EvalRes::err(loc, format!("Field '{}' not found on option", key_name)),
        }
    }

    fn field_keys(&self, _xx: &crate::langhelp::ExprContext) -> Vec<crate::langhelp::ConstKey> {
        Vec::new()
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(k) = key else {
            return None;
        };
        match k.as_str() {
            "some" => Some(Rc::new(TypeOptionVariantConstructorDynamic {
                element_type: self.element_type.clone(),
                is_some: true,
            })),
            "none" => Some(Rc::new(TypeOptionVariantConstructorDynamic {
                element_type: self.element_type.clone(),
                is_some: false,
            })),
            _ => None,
        }
    }
}

impl AspectInitialBind for TypeOptionDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(self.clone()),
        })
    }
}

impl AspectSet for TypeOptionDynamic {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        dynamic_set(xx, loc, self_, value)
    }
}

impl AspectStackValueDynamic for TypeOptionDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(self.clone()),
            })),
        }
    }
}

impl AspectUnion for TypeOptionDynamic {
    fn variants(&self) -> Vec<(ConstKey, String, Rc<dyn Type>)> {
        vec![
            (ConstKey::String("some".to_string()), "some".to_string(), self.element_type.clone()),
            (ConstKey::String("none".to_string()), "none".to_string(), Rc::new(TypeNullDynamic) as Rc<dyn Type>),
        ]
    }
}

impl ExporterAny for TypeOptionDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeOptionDynamic {
    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = match self_.downcast::<TypeOptionDynamic>() {
            Ok(v) => v,
            Err(_) => return EResult::err(loc, "TypeOptionDynamic::self_export: type mismatch".to_string()),
        };
        let element_art = export_artifact(xx, loc, self_.element_type.as_ref());
        let errors = element_art.errors;
        let Some(element_artifact) = element_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(
                Artifact::Body(
                    make_body(serde_json::to_value(&ArtifactOptionType { element_type: element_artifact }).unwrap()),
                ),
            ),
        };
    }

    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        let body: ArtifactOptionType = match serde_json::from_value(data) {
            Ok(v) => v,
            Err(e) => return EResult::err(loc, e.to_string()),
        };
        let element_res = import_type_artifact(xx, loc, body.element_type);
        let errors = element_res.errors;
        let Some(element_type) = element_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(Rc::new(TypeOptionDynamic { element_type }) as Rc<dyn ExporterAny>),
        };
    }
}

impl Push for TypeOptionDynamic {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }

    fn from_dynamic<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let rc: Rc<TypeOptionDynamic> = from_dynamic_ref::<Rc<TypeOptionDynamic>>(&*value).clone();
        (*rc).clone()
    }

    fn into_dynamic<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_dynamic_ext(scope, Rc::new(self)).into()
    }
}

impl Type for TypeOptionDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(self.clone())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_option_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_union_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectUnion>));
        }
        if *key == aspect_set_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectSet>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &type_option_identity()).is_some()
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.as_ref().unwrap().type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &type_option_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(self.clone()),
                })),
            });
        }
        EvalRes::err(loc, "Cannot convert to option".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from option".to_string())
    }

    fn desc(&self) -> String {
        format!("option({})", self.element_type.desc())
    }
}

impl Value for TypeOptionDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::OptionDynamic))
    }
}

#[derive(Clone)]
pub(crate) struct TypeOptionVariantConstructorDynamic {
    pub(crate) element_type: Rc<dyn Type>,
    pub(crate) is_some: bool,
}

impl Aspect for TypeOptionVariantConstructorDynamic { }

impl AspectFunction for TypeOptionVariantConstructorDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let underlying_type: Rc<dyn Type> = if self.is_some {
                self.element_type.clone()
            } else {
                Rc::new(TypeNullDynamic)
            };
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
            let js_tag = if self.is_some {
                "some".to_string()
            } else {
                "none".to_string()
            };
            let result_type = Rc::new(TypeOptionDynamic { element_type: self.element_type.clone() });
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(
                    CodeDynamicNewObject { fields: vec![(ConstKey::String(js_tag).to_str(), underlying_js)], }
                ),
            }));
        });
    }

    fn exception_type(&self) -> Option<std::rc::Rc<dyn crate::langhelp::Type>> {
        None
    }
}

impl ExporterAny for TypeOptionVariantConstructorDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeOptionVariantConstructorDynamic {
    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = match self_.downcast::<TypeOptionVariantConstructorDynamic>() {
            Ok(v) => v,
            Err(_) => return EResult::err(
                loc,
                "TypeOptionVariantConstructorDynamic::self_export: type mismatch".to_string(),
            ),
        };
        let element_art = export_artifact(xx, loc, self_.element_type.as_ref());
        let errors = element_art.errors;
        let Some(element_artifact) = element_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(Artifact::Body(make_body(serde_json::to_value(&super::artifact::ArtifactOptionVariantConstructor {
                element_type: element_artifact,
                is_some: self_.is_some,
            }).unwrap()))),
        };
    }

    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        let body: super::artifact::ArtifactOptionVariantConstructor = match serde_json::from_value(data) {
            Ok(v) => v,
            Err(e) => return EResult::err(loc, e.to_string()),
        };
        let element_res = import_type_artifact(xx, loc, body.element_type);
        let errors = element_res.errors;
        let Some(element_type) = element_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(Rc::new(TypeOptionVariantConstructorDynamic {
                element_type,
                is_some: body.is_some,
            }) as Rc<dyn ExporterAny>),
        };
    }
}

impl Type for TypeOptionVariantConstructorDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(self.clone())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to option variant constructor".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert option variant constructor".to_string())
    }

    fn desc(&self) -> String {
        let tag = if self.is_some {
            "some"
        } else {
            "none"
        };
        format!("option({}).{}", self.element_type.desc(), tag)
    }
}

impl Value for TypeOptionVariantConstructorDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::OptionVariantConstructorDynamic))
    }
}
