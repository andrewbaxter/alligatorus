use {
    super::{
        Erb,
        mortar_string::TypeMortarString,
        mortar_js_value::bind_js_some,
    },
    crate::{
        builtin::{
            AspectBindJs,
            AspectField,
            AspectString,
            aspect_bind_js_aspect_id,
            aspect_field_aspect_id,
            aspect_string_aspect_id,
            aspect_string_get,
            aspect_string_get_maybe,
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

pub(crate) struct TypeString;

impl Value for TypeString {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeString {
    fn desc(&self) -> String {
        return format!("string");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeString)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_string_aspect_id() {
            return Some(Box::new(Box::new(TypeString) as Box<dyn AspectString>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeString) as Box<dyn AspectField>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeString) as Box<dyn AspectBindJs>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_string_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let aspect_string = erb.must_er(aspect_string_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code,
                value: Rc::new(ValueString(aspect_string.get(value.as_ref())))
            });
        });
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_string_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = value.type_(xx, loc);
            let aspect_string = erb.must_er(aspect_string_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code,
                value: Rc::new(ValueString(aspect_string.get(value.as_ref())))
            });
        });
    }
}

impl Aspect for TypeString { }

impl AspectBindJs for TypeString {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        TypeMortarString::cast_from(&TypeMortarString, xx, loc, cv).map(|js| {
            bind_js_some(
                loc,
                js.code.cast::<JsCode>().v.unwrap().unwrap().clone(),
                Rc::new(TypeMortarString) as Rc<dyn Type>,
            )
        })
    }
}

impl AspectString for TypeString {
    fn get(&self, value: &dyn Value) -> String {
        return <dyn Any>::downcast_ref::<ValueString>(value).unwrap().0.clone();
    }
}

impl AspectField for TypeString {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(TypeMortarString.cast_from(xx, loc, self_))?;
            let v = erb.must_er(AspectField::field(&TypeMortarString, xx, loc, converted, key))?;
            return Ok(v);
        });
    }

    fn field_opt(&self, xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        TypeMortarString.field_opt(xx, key)
    }
}

pub(crate) struct ValueString(pub String);

impl Value for ValueString {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeString);
    }
}
