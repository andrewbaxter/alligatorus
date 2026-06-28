use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
        },
        eval::{
            dynamic::code::{
                CodeDynamic,
                CodeDynamicType,
            },
            value::{
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_string::dynamic::TypeStringDynamic,
                type_tuple::binding::BindingValue,
                value_string::r#const::ValueStringConst,
                value_type_string::r#const::ValueTypeStringConst,
            },
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
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectArtifact,
            AspectBind,
            AspectConsumeDynamic,
            AspectField,
            AspectStringConst,
            Binding,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_string_const_aspect_id,
            aspect_string_const_get,
            aspect_string_const_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeStringConst;

impl Aspect for TypeStringConst { }

impl AspectArtifact for TypeStringConst {
    fn exporter(&self) -> std::rc::Rc<dyn crate::artifact::ArtifactExporter> {
        std::rc::Rc::new(crate::artifact::ExporterRoot::StringValueConst)
    }
}

impl AspectBind for TypeStringConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        let mut value_res = crate::langhelp::EResult::ok(cv.value.clone());
        let mut errors = None;
        value_res.push_errors(&mut errors);
        let Some(value) = value_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some((Code {
                loc: loc,
                code: Code_::None,
            }, Box::new(BindingValue { value: value.unwrap() }) as Box<dyn Binding>)),
        };
    }
}

impl AspectConsumeDynamic for TypeStringConst {
    fn to_dynamic(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        let s = must_downcast_ref::<ValueStringConst>(cv.value.as_deref().unwrap()).0.clone();
        EResult::ok(CodeDynamic {
            loc,
            type_: CodeDynamicType::Literal(serde_json::Value::String(s)),
        })
    }
}

impl AspectField for TypeStringConst {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(TypeStringDynamic.cast_from(xx, loc, self_))?;
            let v = erb.must_er(AspectField::field(&TypeStringDynamic, xx, loc, converted, key))?;
            return Ok(v);
        });
    }

    fn field_keys(&self, _xx: &crate::langhelp::ExprContext) -> Vec<crate::langhelp::ConstKey> {
        Vec::new()
    }

    fn field_type_opt(&self, xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        TypeStringDynamic.field_type_opt(xx, key)
    }
}

impl AspectStringConst for TypeStringConst {
    fn get(&self, value: &dyn Value) -> String {
        return must_downcast_ref::<ValueStringConst>(value).0.clone();
    }
}

impl ExporterAny for TypeStringConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeStringConst));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(ValueTypeStringConst));
    }
}

impl ExporterRootDelegate for TypeStringConst {
    fn self_export(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        return EResult::ok(Artifact::Body(make_body(serde_json::Value::Null)));
    }

    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _id: Option<&crate::artifact::ArtifactIdSource>,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        return EResult::ok(Rc::new(ValueTypeStringConst) as Rc<dyn crate::artifact::ExporterAny>);
    }
}

impl Type for TypeStringConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueTypeStringConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_string_const_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectStringConst>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectField>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_string_const_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_string_const_get_maybe(xx, type_).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let value = value.unwrap();
            let type_ = erb.must_er(value.type_(xx, loc))?;
            let aspect_string = erb.must_er(aspect_string_const_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code,
                value: Some(Rc::new(ValueStringConst(aspect_string.get(value.as_ref()))))
            });
        });
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let value = value.unwrap();
            let aspect_string = erb.must_er(aspect_string_const_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code,
                value: Some(Rc::new(ValueStringConst(aspect_string.get(value.as_ref()))))
            });
        });
    }

    fn desc(&self) -> String {
        return format!("string");
    }
}

impl Value for TypeStringConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(ValueTypeStringConst);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::StringConst))
    }
}
