use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicType,
        },
        eval::value::{
            tuple::scope_entry::ScopeEntryValue,
            type_any::r#const::TypeAnyConst,
            type_root::r#const::{
                TypeRoot,
                ExporterRootDelegate,
            },
            type_string::{
                artifact::ArtifactStringType,
                dynamic::TypeStringDynamic,
            },
            value_string::r#const::ValueStringConst,
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectArtifact,
            AspectBind,
            AspectConsumeDynamic,
            AspectField,
            AspectStringConst,
            ScopeEntry,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_string_const_aspect_id,
            aspect_string_const_get,
            aspect_string_const_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeStringConst;

impl Aspect for TypeStringConst { }

impl AspectArtifact for TypeStringConst {
    fn exporter(&self) -> std::rc::Rc<dyn crate::artifact::ArtifactExporter> {
        std::rc::Rc::new(crate::eval::value::type_root::r#const::ExporterRoot::StringConst)
    }
}

impl AspectBind for TypeStringConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((Code {
            loc: loc,
            code: Code_::None,
        }, Box::new(ScopeEntryValue { value: cv.value }) as Box<dyn ScopeEntry>))
    }
}

impl AspectConsumeDynamic for TypeStringConst {
    fn to_dynamic(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        let s = must_downcast_ref::<ValueStringConst>(cv.value.as_ref()).0.clone();
        EResult::ok(CodeDynamic {
            loc,
            type_: CodeDynamicType::Literal(serde_json::Value::String(s)),
        })
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

    fn field_type_opt(&self, xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        TypeStringDynamic.field_type_opt(xx, key)
    }
}

impl AspectStringConst for TypeStringConst {
    fn get(&self, value: &dyn Value) -> String {
        return must_downcast_ref::<ValueStringConst>(value).0.clone();
    }
}

impl ExporterAny for TypeStringConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeStringConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeStringConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_string_const_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectStringConst>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectField>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeStringConst) as Box<dyn AspectBind>));
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_string_const_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_string_const_get_maybe(xx, type_).is_some()
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

    fn desc(&self) -> String {
        return format!("string");
    }

}

impl ExporterRootDelegate for TypeStringConst {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        loc: QExprId,
        _id: Option<&crate::artifact::ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        return EResult::from(
            loc,
            serde_json::from_value::<ArtifactStringType>(data).map(|_| Rc::new(Self) as Rc<dyn Any>),
        );
    }

    fn self_to_serial(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn Any>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        return EResult::ok(Artifact::Body(make_body(serde_json::to_value(ArtifactStringType).unwrap())));
    }
}

impl Value for TypeStringConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::StringConst))
    }
}
