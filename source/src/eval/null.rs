use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectNull,
            aspect_artifact_aspect_id,
            aspect_null_aspect_id,
            aspect_null_get,
            aspect_null_get_maybe,
        },
        langhelp::{
            Artifact,
            ArtifactData,
            ArtifactId,
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            GlobalContext,
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

pub(crate) fn null_artifact_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}
pub(crate) struct TypeNull;

impl Value for TypeNull {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeAny)
    }
}

impl Type for TypeNull {
    fn desc(&self) -> String {
        return format!("null");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeNull)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeNull) as Box<dyn AspectNull>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeNull) as Box<dyn AspectArtifact>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            erb.must_er(aspect_null_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueNull)
            });
        });
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            erb.must_er(aspect_null_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueNull)
            });
        });
    }
}

impl Aspect for TypeNull { }

impl AspectNull for TypeNull { }

impl AspectArtifact for TypeNull {
    fn to_serial(&self, gx: &GlobalContext, _value: Rc<dyn Value>) -> EResult<Artifact> {
        let builtin_id = *gx.builtin_type_ids.get(&null_artifact_identity()).unwrap();
        EResult::ok(Artifact {
            type_: ArtifactId::Builtin(builtin_id),
            data: ArtifactData::Null,
        })
    }

    fn from_serial(&self, _gx: &GlobalContext, data: &ArtifactData) -> Result<Rc<dyn Value>, String> {
        match data {
            ArtifactData::Null => Ok(Rc::new(ValueNull) as Rc<dyn Value>),
            _ => Err("Expected null data for null type".to_string()),
        }
    }
}
pub(crate) struct ValueNull;

impl Value for ValueNull {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeNull);
    }
}
