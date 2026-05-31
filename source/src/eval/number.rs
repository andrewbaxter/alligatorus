use {
    super::{
        Erb,
        mortar_number::TypeMortarNumber,
        mortar_js_value::bind_js_some,
    },
    crate::{
        builtin::{
            AspectBindJs,
            AspectField,
            AspectNumber,
            AspectOperatorAdd,
            AspectOperatorLt,
            aspect_bind_js_aspect_id,
            aspect_field_aspect_id,
            aspect_number_aspect_id,
            aspect_number_get,
            aspect_operator_add_aspect_id,
            aspect_operator_lt_aspect_id,
        },
        jscode::JsCode,
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
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

pub(crate) struct TypeNumber;

impl Value for TypeNumber {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeNumber {
    fn desc(&self) -> String {
        return format!("number");
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_number_aspect_id() {
            return Some(Box::new(Box::new(TypeNumber) as Box<dyn AspectNumber>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeNumber) as Box<dyn AspectField>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeNumber) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_operator_lt_aspect_id() {
            return Some(Box::new(Box::new(TypeNumber) as Box<dyn AspectOperatorLt>));
        }
        if *key == aspect_operator_add_aspect_id() {
            return Some(Box::new(Box::new(TypeNumber) as Box<dyn AspectOperatorAdd>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let aspect_number = erb.must_er(aspect_number_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueNumber(aspect_number.get(value.as_ref())))
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = value.type_(xx, loc);
            let aspect_number = erb.must_er(aspect_number_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueNumber(aspect_number.get(value.as_ref())))
            });
        });
    }
}

impl Aspect for TypeNumber { }

impl AspectBindJs for TypeNumber {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        TypeMortarNumber::cast_from(&TypeMortarNumber, xx, loc, &cv).map(|js| {
            bind_js_some(
                loc,
                js.code.cast::<JsCode>().v.unwrap().unwrap().clone(),
                Rc::new(TypeMortarNumber) as Rc<dyn Type>,
            )
        })
    }
}

impl AspectNumber for TypeNumber {
    fn get(&self, value: &dyn Value) -> f64 {
        return <dyn Any>::downcast_ref::<ValueNumber>(value).unwrap().0;
    }
}

impl AspectField for TypeNumber {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(TypeMortarNumber.cast_from(xx, loc, &self_))?;
            let v = erb.must_er(AspectField::field(&TypeMortarNumber, xx, loc, converted, key))?;
            return Ok(v);
        });
    }

    fn field_opt(&self, xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        TypeMortarNumber.field_opt(xx, key)
    }
}

impl AspectOperatorLt for TypeNumber {
    fn lt(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_type = base.value.type_(xx, loc);
            let base_num = erb.must_er(aspect_number_get(xx, loc, base_type.as_ref()))?;
            let base_val = base_num.get(base.value.as_ref());
            let ref_type = reference.value.type_(xx, loc);
            let ref_num = erb.must_er(aspect_number_get(xx, loc, ref_type.as_ref()))?;
            let ref_val = ref_num.get(reference.value.as_ref());
            return Ok(CodeValue {
                code: base.code.clone(),
                value: Box::new(super::bool::ValueBool(base_val < ref_val)),
            });
        });
    }
}

impl AspectOperatorAdd for TypeNumber {
    fn add(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_type = base.value.type_(xx, loc);
            let base_num = erb.must_er(aspect_number_get(xx, loc, base_type.as_ref()))?;
            let base_val = base_num.get(base.value.as_ref());
            let ref_type = reference.value.type_(xx, loc);
            let ref_num = erb.must_er(aspect_number_get(xx, loc, ref_type.as_ref()))?;
            let ref_val = ref_num.get(reference.value.as_ref());
            return Ok(CodeValue {
                code: base.code.clone(),
                value: Box::new(ValueNumber(base_val + ref_val)),
            });
        });
    }
}

pub(crate) struct ValueNumber(pub f64);

impl Value for ValueNumber {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeNumber);
    }
}
