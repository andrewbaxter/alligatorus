use {
    crate::{
        builtin::{
            AspectNull,
            AspectSerial,
            aspect_null_aspect_id,
            aspect_null_get,
            aspect_serial_aspect_id,
        },
        langhelp::{
            Artifacts,
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            SerialValue,
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

pub(crate) struct TypeNull;

impl Value for TypeNull {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeNull);
    }
}

impl Type for TypeNull {
    fn desc(&self) -> String {
        return format!("null");
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeNull) as Box<dyn AspectNull>));
        }
        if *key == aspect_serial_aspect_id() {
            return Some(Box::new(Box::new(TypeNull) as Box<dyn AspectSerial>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value: _value } = cv;
            erb.must_er(aspect_null_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueNull)
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = value.type_(xx, loc);
            erb.must_er(aspect_null_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueNull)
            });
        });
    }
}

impl Aspect for TypeNull { }

impl AspectNull for TypeNull { }

impl AspectSerial for TypeNull {
    fn to_serial(&self, _artifacts: &mut Artifacts, _value: Box<dyn Value>) -> EResult<SerialValue> {
        EResult::ok(SerialValue::Null)
    }
}
pub(crate) struct ValueNull;

impl Value for ValueNull {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeNull);
    }
}
