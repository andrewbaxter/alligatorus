use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBind,
            AspectConsumeDynamic,
            AspectField,
            AspectStringConst,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_string_const_aspect_id,
            aspect_string_const_get,
            aspect_string_const_get_maybe,
        },
        code_dynamic::CodeDynamic,
        eval::{
            Erb,
            value::{
                type_dynamic::mortar_bind_to_scope,
                type_string::dynamic::TypeStringDynamic,
                value_string::r#const::ValueStringConst,
            },
        },
        langhelp::{
            Artifact,
            Aspect,
            Code,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            GlobalContext,
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

pub(crate) struct TypeStringConst;

generate_builtin_artifact!(TypeStringConst, super::artifact::ArtifactDataString);

impl Value for TypeStringConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeStringConst {
    fn desc(&self) -> String {
        return format!("string");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeStringConst)
    }

    fn type_to_serial(&self, _xx: &ExprContext, _gx: &GlobalContext, _loc: QExprId) -> Option<Artifact> {
        Some(Self::type_ref_artifact())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_string_const_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectStringConst>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectField>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectBind>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_string_const_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let aspect_string = erb.must_er(aspect_string_const_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code,
                value: Rc::new(ValueStringConst(aspect_string.get(value.as_ref())))
            });
        });
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_string_const_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = value.type_(xx, loc);
            let aspect_string = erb.must_er(aspect_string_const_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code,
                value: Rc::new(ValueStringConst(aspect_string.get(value.as_ref())))
            });
        });
    }
}

impl Aspect for TypeStringConst { }

impl AspectConsumeDynamic for TypeStringConst {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        return er_seq!(loc, erb, CodeDynamic, {
            let cv = erb.must_er(TypeStringDynamic.cast_from(xx, loc, cv))?;
            return erb.must_er(AspectConsumeDynamic::to_dynamic(&TypeStringDynamic, xx, loc, cv));
        });
    }
}

impl AspectBind for TypeStringConst {
    fn bind(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((
            Code {
                loc: loc,
                code: crate::langhelp::Code_::None,
            },
            Box::new(crate::eval::value::type_tuple::ScopeEntryValue { value: cv.value }) as Box<dyn ScopeEntry>,
        ))
    }
}

impl AspectStringConst for TypeStringConst {
    fn get(&self, value: &dyn Value) -> String {
        return <dyn Any>::downcast_ref::<ValueStringConst>(value).unwrap().0.clone();
    }
}

impl AspectField for TypeStringConst {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(TypeStringDynamic.cast_from(xx, loc, self_))?;
            let v = erb.must_er(AspectField::field(&TypeStringDynamic, xx, loc, converted, key))?;
            return Ok(v);
        });
    }

    fn field_opt(&self, xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        TypeStringDynamic.field_opt(xx, key)
    }
}

impl AspectArtifact for TypeStringConst {
    fn to_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _loc: QExprId,
        value: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let s = <dyn Any>::downcast_ref::<ValueStringConst>(value.as_ref()).unwrap().0.clone();
        EResult::ok(Self::build_artifact(s))
    }

    fn from_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _loc: QExprId,
        data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        match data {
            serde_json::Value::String(s) => Ok(Rc::new(ValueStringConst(s.clone())) as Rc<dyn Value>),
            _ => Err("Expected string data for string type".to_string()),
        }
    }
}
