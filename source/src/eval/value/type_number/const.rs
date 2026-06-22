use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
        },
        eval::value::{
            tuple::scope_entry::ScopeEntryValue,
            type_any::r#const::TypeAnyConst,
            type_number::{
                artifact::ArtifactNumberType,
                dynamic::TypeNumberDynamic,
            },
            type_root::r#const::{
                TypeRoot,
                ExporterRootDelegate,
            },
            value_bool::r#const::ValueBoolConst,
            value_number::r#const::ValueNumberConst,
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
            ScopeEntry,
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
    fn from_serial(&self, _xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        match data.as_f64() {
            Some(n) => EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Rc::new(ValueNumberConst(n)) as Rc<dyn Value>,
            }),
            None => EvalRes::err(loc, "Expected number data for number type".to_string()),
        }
    }

    fn to_serial(&self, _xx: &ExprContext, _loc: QExprId, value: Rc<dyn Value>) -> EResult<Artifact> {
        let n = must_downcast_ref::<ValueNumberConst>(value.as_ref()).0;
        EResult::ok(Self::build_artifact(n))
    }
}

impl AspectBind for TypeNumberConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((Code {
            loc: loc,
            code: Code_::None,
        }, Box::new(ScopeEntryValue { value: cv.value }) as Box<dyn ScopeEntry>))
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
            let base_type = base.value.type_(xx, loc);
            let base_num = erb.must_er(aspect_number_const_get(xx, loc, base_type.as_ref()))?;
            let base_val = base_num.get(base.value.as_ref());
            let ref_type = reference.value.type_(xx, loc);
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
            let base_type = base.value.type_(xx, loc);
            let base_num = erb.must_er(aspect_number_const_get(xx, loc, base_type.as_ref()))?;
            let base_val = base_num.get(base.value.as_ref());
            let ref_type = reference.value.type_(xx, loc);
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
            let base_type = base.value.type_(xx, loc);
            let base_num = erb.must_er(aspect_number_const_get(xx, loc, base_type.as_ref()))?;
            let base_val = base_num.get(base.value.as_ref());
            let ref_type = reference.value.type_(xx, loc);
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
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeNumberConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeNumberConst)
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
            let type_ = value.type_(xx, loc);
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

impl ExporterRootDelegate for TypeNumberConst {
    fn self_from_artifact_data(_xx: &ExprContext, loc: QExprId, data: serde_json::Value) -> EResult<Rc<dyn Any>> {
        return EResult::from(
            loc,
            serde_json::from_value::<ArtifactNumberType>(data).map(|_| Rc::new(Self) as Rc<dyn Any>),
        );
    }

    fn self_to_artifact_data(_xx: &ExprContext, _loc: QExprId, _self_: Rc<dyn Any>) -> EResult<serde_json::Value> {
        return EResult::ok(serde_json::to_value(ArtifactNumberType).unwrap());
    }
}

impl Value for TypeNumberConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::NumberConst))
    }
}
