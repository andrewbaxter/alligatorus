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
        eval::{
            Erb,
            value::value_null::r#const::ValueNullConst,
        },
        langhelp::{
            Artifact,
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
};

generate_builtin_artifact!(TypeNullConst, super::artifact::ArtifactDataNull);
pub(crate) struct TypeNullConst;

impl Aspect for TypeNullConst { }

impl AspectArtifact for TypeNullConst {
    fn to_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _loc: QExprId,
        _value: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        EResult::ok(Self::build_artifact(()))
    }

    fn from_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _loc: QExprId,
        data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        match data {
            serde_json::Value::Null => Ok(Rc::new(ValueNullConst) as Rc<dyn Value>),
            _ => Err("Expected null data for null type".to_string()),
        }
    }
}

impl AspectNull for TypeNullConst { }

impl Type for TypeNullConst {
    fn desc(&self) -> String {
        return format!("null");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeNullConst)
    }

    fn type_to_serial(&self, _xx: &ExprContext, _gx: &GlobalContext, _loc: QExprId) -> Option<Artifact> {
        Some(Self::build_artifact(()))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeNullConst) as Box<dyn AspectNull>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeNullConst) as Box<dyn AspectArtifact>));
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
                value: Rc::new(ValueNullConst)
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
                value: Rc::new(ValueNullConst)
            });
        });
    }
}

impl Value for TypeNullConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
