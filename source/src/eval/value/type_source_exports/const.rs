use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
        },
        eval::{
            definition_set::{
                DefinitionSet,
                serialize_definition_set,
            },
            value::{
                tuple::scope_entry::ScopeEntryValue,
                type_any::r#const::ValueAnyConst,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
                type_source_function::r#const::TypeSourceFunctionConst,
            },
        },
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
            AspectField,
            ScopeEntry,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_field_aspect_id,
            aspect_string_const_get,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeSourceExportsLiftedConst {
    pub set: Gc<DefinitionSet>,
}

impl Aspect for TypeSourceExportsLiftedConst { }

impl AspectArtifact for TypeSourceExportsLiftedConst {
    fn exporter(&self) -> std::rc::Rc<dyn crate::artifact::ArtifactExporter> {
        std::rc::Rc::new(crate::eval::value::type_root::r#const::ExporterRoot::SourceExportsLiftedConst)
    }
}

impl AspectBind for TypeSourceExportsLiftedConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((Code {
            loc: loc,
            code: Code_::None,
        }, Box::new(ScopeEntryValue { value: Rc::from(cv.value) }) as Box<dyn ScopeEntry>))
    }
}

impl AspectField for TypeSourceExportsLiftedConst {
    fn field(&self, xx: &ExprContext, loc: QExprId, _self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            {
                let functions = self.set.functions.borrow();
                for func_data in functions.iter() {
                    if func_data.name.as_deref() == Some(&key_name) {
                        xx
                            .vx
                            .definition_sets
                            .borrow_mut()
                            .entry(self.set.def_set_id)
                            .or_insert_with(|| self.set.clone());
                        let func_type: Rc<dyn Type> = Rc::new(TypeSourceFunctionConst { data: func_data.clone() });
                        return Ok(CodeValue {
                            code: Code {
                                loc,
                                code: Code_::None,
                            },
                            value: Rc::new(ValueAnyConst { inner: func_type, }),
                        });
                    }
                }
            }
            {
                let data_types = self.set.data_types.borrow();
                for dt_data in data_types.iter() {
                    if dt_data.name.as_deref() == Some(&key_name) {
                        let ctor_type: Rc<dyn Type> =
                            Rc::new(TypeSourceDataTypeConstructorConst { data: dt_data.clone() });
                        return Ok(CodeValue {
                            code: Code {
                                loc,
                                code: Code_::None,
                            },
                            value: Rc::new(ValueAnyConst { inner: ctor_type, }),
                        });
                    }
                }
            }
            return erb.err_new(format!("'{}' not found in definition set", key_name));
        });
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(key) = key else {
            return None;
        };
        {
            let functions = self.set.functions.borrow();
            for func_data in functions.iter() {
                if func_data.name.as_deref() == Some(key.as_str()) {
                    return Some(Rc::new(TypeSourceFunctionConst { data: func_data.clone() }));
                }
            }
        }
        {
            let data_types = self.set.data_types.borrow();
            for dt_data in data_types.iter() {
                if dt_data.name.as_deref() == Some(key.as_str()) {
                    return Some(Rc::new(TypeSourceDataTypeConstructorConst { data: dt_data.clone() }));
                }
            }
        }
        None
    }
}

impl ExporterAny for TypeSourceExportsLiftedConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeSourceExportsLiftedConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceExportsLiftedConst { set: self.set.clone() }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceExportsLiftedConst { set: self.set.clone() }) as Box<dyn AspectField>),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceExportsLiftedConst { set: self.set.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceExportsLiftedConst { set: self.set.clone() }) as Box<dyn AspectArtifact>,
                ),
            );
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_exports"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_exports"))
    }

    fn desc(&self) -> String {
        format!("definition_set_exports")
    }
}

impl ExporterRootDelegate for TypeSourceExportsLiftedConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&crate::artifact::ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        // Delegate to the def-set deserializer; wrap the resulting Gc<DefinitionSet>
        // into a TypeSourceExportsLiftedConst.
        let any =
            crate::eval::definition_set::definition_set_from_artifact_data(xx, loc, id, data);
        let mut errors = any.errors;
        let Some(any) = any.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let wrapped = match any.downcast::<crate::artifact::ExporterDataAny<Gc<DefinitionSet>>>() {
            Ok(v) => v,
            Err(_) => {
                return EResult::err(
                    loc,
                    "TypeSourceExportsLiftedConst: def_set deserializer returned wrong type".to_string(),
                );
            },
        };
        let out: Rc<dyn Any> =
            Rc::new(crate::artifact::ExporterDataAny(TypeSourceExportsLiftedConst { set: wrapped.0.clone() }));
        let _ = &mut errors;
        return EResult {
            errors,
            v: Some(out),
        };
    }

    fn self_to_serial(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn Any>,
        _make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceExportsLiftedConst>().expect("expected TypeSourceExportsLiftedConst");
        EResult::ok(serialize_definition_set(xx, loc, &self_.set))
    }
}

impl Value for TypeSourceExportsLiftedConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceExportsLiftedConst))
    }
}
