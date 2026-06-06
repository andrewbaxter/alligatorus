use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBindJs,
            AspectConsumeJs,
            AspectJsStackValue,
            AspectLiftJs,
            AspectNull,
            aspect_artifact_aspect_id,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
            aspect_null_aspect_id,
            aspect_null_get,
            aspect_null_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeType,
        },
        langhelp::{
            Artifact,
            ArtifactId,
            ArtifactIdBuiltin,
            Aspect,
            Code,
            CodeValue,
            Code_,
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
    super::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
            bind_js_some,
        },
        null::ValueNull,
    },
};

pub(crate) fn mortar_null_artifact_identity() -> ArtifactIdBuiltin {
    ArtifactIdBuiltin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}
pub(crate) struct TypeMortarNull;

impl Value for TypeMortarNull {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeAny)
    }
}

impl Type for TypeMortarNull {
    fn desc(&self) -> String {
        return format!("mortar null");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeMortarNull)
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectLiftJs>));
        }
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectNull>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectBindJs>));
        }
        return None;
    }

    fn type_to_serial(&self, _xx: &ExprContext, _gx: &GlobalContext, _loc: QExprId) -> Option<Artifact> {
        Some(Artifact {
            type_: ArtifactId::Builtin(mortar_null_artifact_identity()),
            data: serde_json::Value::Null,
        })
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueNull),
        })
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, ex: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(ex, loc);
            erb.must_er(aspect_null_get(ex, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueNull),
            });
        });
    }
}

impl Aspect for TypeMortarNull { }

impl AspectJsStackValue for TypeMortarNull {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarNull),
            }),
        }
    }
}

impl AspectArtifact for TypeMortarNull {
    fn to_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _loc: QExprId,
        _value: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        EResult::ok(Artifact {
            type_: ArtifactId::Builtin(mortar_null_artifact_identity()),
            data: serde_json::Value::Null,
        })
    }

    fn from_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _loc: QExprId,
        _data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        Ok(Rc::new(ValueNull))
    }
}

impl AspectLiftJs for TypeMortarNull {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        _value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        Ok(Rc::new(ValueNull))
    }
}

impl AspectNull for TypeMortarNull { }

impl AspectConsumeJs for TypeMortarNull {
    fn to_js(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        match cv.code.code {
            Code_::Dyn(_) => cv.code.cast::<JsCode>().map(|x| x.unwrap().clone()),
            Code_::None => EResult::ok(JsCode {
                loc,
                type_: JsCodeType::Literal(serde_json::Value::Null),
            }),
        }
    }
}

impl AspectBindJs for TypeMortarNull {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}
