use {
    crate::{
        builtin::{
            AspectBool,
            aspect_bool_aspect_id,
            aspect_bool_get,
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

pub(crate) struct TypeBool;

impl Value for TypeBool {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeBool {
    fn desc(&self) -> String {
        return format!("bool");
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bool_aspect_id() {
            return Some(Box::new(Box::new(TypeBool) as Box<dyn AspectBool>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let aspect_bool = erb.must_er(aspect_bool_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueBool(aspect_bool.get(value.as_ref())))
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = value.type_(xx, loc);
            let aspect_bool = erb.must_er(aspect_bool_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueBool(aspect_bool.get(value.as_ref())))
            });
        });
    }
}

impl Aspect for TypeBool { }

impl AspectBool for TypeBool {
    fn get(&self, value: &dyn Value) -> bool {
        return <dyn Any>::downcast_ref::<ValueBool>(value).unwrap().0;
    }
}

pub(crate) struct ValueBool(pub bool);

impl Value for ValueBool {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeBool);
    }
}
