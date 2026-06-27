use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
        },
        eval::value::{
            type_tuple::binding::BindingValue,
            type_number::dynamic::TypeNumberDynamic,
            type_root::r#const::{
                ExporterRootDelegate,
                TypeRoot,
            },
            value_bool::r#const::ValueBoolConst,
            value_number::r#const::ValueNumberConst,
            value_type_number::r#const::ValueTypeNumberConst,
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
            AspectField,
            AspectNumberConst,
            AspectOperatorAdd,
            AspectOperatorEq,
            AspectOperatorLt,
            Binding,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_field_aspect_id,
            aspect_number_const_aspect_id,
            aspect_number_const_get,
            aspect_number_const_get_maybe,
            aspect_operator_add_aspect_id,
            aspect_operator_eq_aspect_id,
            aspect_operator_lt_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeNumberConst;

impl Aspect for TypeNumberConst { }

impl AspectArtifact for TypeNumberConst {
    fn exporter(&self) -> std::rc::Rc<dyn crate::artifact::ArtifactExporter> {
        std::rc::Rc::new(crate::artifact::ExporterRoot::NumberValueConst)
    }
}

impl AspectBind for TypeNumberConst {
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
            }, Box::new(BindingValue { value }) as Box<dyn Binding>)),
        };
    }
}

impl AspectField for TypeNumberConst {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(TypeNumberDynamic.cast_from(xx, loc, self_))?;
            let v = erb.must_er(AspectField::field(&TypeNumberDynamic, xx, loc, converted, key))?;
            return Ok(v);
        });
    }

    fn field_type_opt(&self, xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        TypeNumberDynamic.field_type_opt(xx, key)
    }
}

impl AspectNumberConst for TypeNumberConst {
    fn get(&self, value: &dyn Value) -> f64 {
        return must_downcast_ref::<ValueNumberConst>(value).0;
    }
}

impl AspectOperatorAdd for TypeNumberConst {
    fn add(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_type = erb.must_er(base.value.type_(xx, loc))?;
            let base_num = erb.must_er(aspect_number_const_get(xx, loc, base_type.as_ref()))?;
            let base_val = base_num.get(base.value.as_ref());
            let ref_type = erb.must_er(reference.value.type_(xx, loc))?;
            let ref_num = erb.must_er(aspect_number_const_get(xx, loc, ref_type.as_ref()))?;
            let ref_val = ref_num.get(reference.value.as_ref());
            return Ok(CodeValue {
                code: base.code.clone(),
                value: Rc::new(ValueNumberConst(base_val + ref_val)),
            });
        });
    }
}

impl AspectOperatorEq for TypeNumberConst {
    fn eq(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_type = erb.must_er(base.value.type_(xx, loc))?;
            let base_num = erb.must_er(aspect_number_const_get(xx, loc, base_type.as_ref()))?;
            let base_val = base_num.get(base.value.as_ref());
            let ref_type = erb.must_er(reference.value.type_(xx, loc))?;
            let ref_num = erb.must_er(aspect_number_const_get(xx, loc, ref_type.as_ref()))?;
            let ref_val = ref_num.get(reference.value.as_ref());
            return Ok(CodeValue {
                code: base.code.clone(),
                value: Rc::new(ValueBoolConst(base_val == ref_val)),
            });
        });
    }
}

impl AspectOperatorLt for TypeNumberConst {
    fn lt(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_type = erb.must_er(base.value.type_(xx, loc))?;
            let base_num = erb.must_er(aspect_number_const_get(xx, loc, base_type.as_ref()))?;
            let base_val = base_num.get(base.value.as_ref());
            let ref_type = erb.must_er(reference.value.type_(xx, loc))?;
            let ref_num = erb.must_er(aspect_number_const_get(xx, loc, ref_type.as_ref()))?;
            let ref_val = ref_num.get(reference.value.as_ref());
            return Ok(CodeValue {
                code: base.code.clone(),
                value: Rc::new(ValueBoolConst(base_val < ref_val)),
            });
        });
    }
}

impl ExporterAny for TypeNumberConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeNumberConst));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(ValueTypeNumberConst));
    }
}

impl ExporterRootDelegate for TypeNumberConst {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _id: Option<&crate::artifact::ArtifactIdSource>,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        return EResult::ok(Rc::new(ValueTypeNumberConst) as Rc<dyn crate::artifact::ExporterAny>);
    }

    fn self_export(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        return EResult::ok(Artifact::Body(make_body(serde_json::Value::Null)));
    }
}

impl Type for TypeNumberConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueTypeNumberConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_number_const_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectNumberConst>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectField>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectBind>));
        }
        if *key == aspect_operator_lt_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectOperatorLt>));
        }
        if *key == aspect_operator_eq_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectOperatorEq>));
        }
        if *key == aspect_operator_add_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectOperatorAdd>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_number_const_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_number_const_get_maybe(xx, type_).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = erb.must_er(value.type_(xx, loc))?;
            let aspect_number = erb.must_er(aspect_number_const_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code,
                value: Rc::new(ValueNumberConst(aspect_number.get(value.as_ref())))
            });
        });
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let aspect_number = erb.must_er(aspect_number_const_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code,
                value: Rc::new(ValueNumberConst(aspect_number.get(value.as_ref())))
            });
        });
    }

    fn desc(&self) -> String {
        return format!("number");
    }
}

impl Value for TypeNumberConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(ValueTypeNumberConst);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::NumberConst))
    }
}
