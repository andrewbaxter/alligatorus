use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectField,
            AspectNumberConst,
            AspectOperatorAdd,
            AspectOperatorLt,
            aspect_bind_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_number_const_aspect_id,
            aspect_number_const_get,
            aspect_number_const_get_maybe,
            aspect_operator_add_aspect_id,
            aspect_operator_lt_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            Erb,
            value::{
                type_dynamic::bind_dynamic_some,
                type_number::dynamic::TypeNumberDynamic,
                value_bool::r#const::ValueBoolConst,
                value_number::r#const::ValueNumberConst,
            },
        },
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

pub(crate) struct TypeNumberConst;

impl Value for TypeNumberConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeNumberConst {
    fn desc(&self) -> String {
        return format!("number");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeNumberConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_number_const_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectNumberConst>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectField>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_operator_lt_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectOperatorLt>));
        }
        if *key == aspect_operator_add_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectOperatorAdd>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_number_const_get_maybe(xx, type_).is_some()
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

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_number_const_get_maybe(xx, source_type).is_some()
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
}

impl Aspect for TypeNumberConst { }

impl AspectBindDynamic for TypeNumberConst {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        TypeNumberDynamic::cast_from(&TypeNumberDynamic, xx, loc, cv).map(|js| {
            bind_dynamic_some(
                loc,
                js.code.cast::<CodeDynamic>().v.unwrap().unwrap().clone(),
                Rc::new(TypeNumberDynamic) as Rc<dyn Type>,
            )
        })
    }
}

impl AspectNumberConst for TypeNumberConst {
    fn get(&self, value: &dyn Value) -> f64 {
        return <dyn Any>::downcast_ref::<ValueNumberConst>(value).unwrap().0;
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

    fn field_opt(&self, xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        TypeNumberDynamic.field_opt(xx, key)
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
                value: Rc::new(crate::eval::value::value_bool::r#const::ValueBoolConst(base_val < ref_val)),
            });
        });
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
