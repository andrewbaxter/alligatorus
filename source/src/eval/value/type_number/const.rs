use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamicSub,
            AspectConsumeDynamic,
            AspectField,
            AspectNumberConst,
            AspectOperatorAdd,
            AspectOperatorLt,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_sub_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_number_const_aspect_id,
            aspect_number_const_get,
            aspect_number_const_get_maybe,
            aspect_operator_add_aspect_id,
            aspect_operator_lt_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::aspect::ScopeEntryDynamicSub,
        eval::{
            Erb,
            value::{
                type_number::dynamic::TypeNumberDynamic,
                value_number::r#const::ValueNumberConst,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntry,
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
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_sub_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectBindDynamicSub>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberConst) as Box<dyn AspectBind>));
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

impl AspectConsumeDynamic for TypeNumberConst {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        return er_seq!(loc, erb, CodeDynamic, {
            let cv = erb.must_er(TypeNumberDynamic.cast_from(xx, loc, cv))?;
            return erb.must_er(AspectConsumeDynamic::to_dynamic(&TypeNumberDynamic, xx, loc, cv));
        });
    }
}

impl AspectBindDynamicSub for TypeNumberConst {
    fn bind_dynamic_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(CodeDynamic, ScopeEntryDynamicSub)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| (c, ScopeEntryDynamicSub { type_: vtype }))
    }
}

impl AspectBind for TypeNumberConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((Code {
            loc: loc,
            code: crate::langhelp::Code_::None,
        }, Box::new(crate::eval::value::type_tuple::ScopeEntryValue { value: cv.value }) as Box<dyn ScopeEntry>))
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
