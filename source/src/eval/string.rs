use {
    crate::{
        builtin::{
            AspectString,
            aspect_string_aspect_id,
            aspect_string_get,
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
    super::Erb,
};

pub(crate) struct TypeString;

impl Value for TypeString {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        unimplemented!();
    }
}

impl Type for TypeString {
    fn desc(&self) -> String {
        return format!("string");
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_string_aspect_id() {
            return Some(Box::new(Box::new(TypeString) as Box<dyn AspectString>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let aspect_string = erb.must_er(aspect_string_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueString(aspect_string.get(value.as_ref())))
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = value.type_(xx, loc);
            let aspect_string = erb.must_er(aspect_string_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueString(aspect_string.get(value.as_ref())))
            });
        });
    }
}

impl Aspect for TypeString { }

impl AspectString for TypeString {
    fn get(&self, value: &dyn Value) -> String {
        return <dyn Any>::downcast_ref::<ValueString>(value).unwrap().0.clone();
    }
}

pub(crate) struct ValueString(pub String);

impl Value for ValueString {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeString);
    }
}
