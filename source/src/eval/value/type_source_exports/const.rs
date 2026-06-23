use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
            impl_serialize_independent_artifact,
        },
        eval::{
            definition_set::{
                DefinitionSet,
                serialize_definition_set,
            },
            value::{
                tuple::scope_entry::ScopeEntryValue,
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
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
            DefSetId,
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
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
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

#[derive(serde::Serialize, serde::Deserialize)]
struct ArtifactNewDefSet {
    def_set_id: DefSetId,
    members: Vec<Artifact>,
    dependencies: Vec<Artifact>,
}

impl ExporterRootDelegate for TypeSourceExportsLiftedConst {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        unimplemented!()
    }

    fn self_to_serial(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn Any>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceExportsLiftedConst>().expect("expected TypeSourceExportsLiftedConst");
        let art = impl_serialize_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.set.new_artifact_id, || {
            let mut members: Vec<Artifact> = Vec::new();
            for dt in self_.set.data_types.borrow().iter() {
                let dt_const = Rc::new(super::super::type_source_data_type::r#const::TypeSourceDataTypeConst {
                    data: dt.clone(),
                });
                let inner_make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody> =
                    Box::new(|data| ArtifactBody {
                        data,
                        exporter: crate::artifact::ArtifactBodyExporter::Body(Box::new(ArtifactBody {
                            data: serde_json::to_value(
                                crate::eval::value::type_root::r#const::ExporterRoot::SourceDataTypeConst,
                            ).unwrap(),
                            exporter: crate::artifact::ArtifactBodyExporter::Root,
                        })),
                    });
                if let Some(m) =
                    super::super::type_source_data_type::r#const::TypeSourceDataTypeConst::self_to_serial(
                        xx,
                        loc,
                        dt_const,
                        inner_make_body,
                    ).v {
                    members.push(m);
                }
            }
            for f in self_.set.functions.borrow().iter() {
                let f_const = Rc::new(super::super::type_source_function::r#const::TypeSourceFunctionConst {
                    data: f.clone(),
                });
                let inner_make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody> =
                    Box::new(|data| ArtifactBody {
                        data,
                        exporter: crate::artifact::ArtifactBodyExporter::Body(Box::new(ArtifactBody {
                            data: serde_json::to_value(
                                crate::eval::value::type_root::r#const::ExporterRoot::SourceFunctionConst,
                            ).unwrap(),
                            exporter: crate::artifact::ArtifactBodyExporter::Root,
                        })),
                    });
                if let Some(m) =
                    super::super::type_source_function::r#const::TypeSourceFunctionConst::self_to_serial(
                        xx,
                        loc,
                        f_const,
                        inner_make_body,
                    ).v {
                    members.push(m);
                }
            }
            for i in self_.set.interfaces.borrow().iter() {
                let i_const = Rc::new(super::super::type_source_interface::r#const::TypeSourceInterfaceConst {
                    data: i.clone(),
                });
                let inner_make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody> =
                    Box::new(|data| ArtifactBody {
                        data,
                        exporter: crate::artifact::ArtifactBodyExporter::Body(Box::new(ArtifactBody {
                            data: serde_json::to_value(
                                crate::eval::value::type_root::r#const::ExporterRoot::SourceInterfaceConst,
                            ).unwrap(),
                            exporter: crate::artifact::ArtifactBodyExporter::Root,
                        })),
                    });
                if let Some(m) =
                    super::super::type_source_interface::r#const::TypeSourceInterfaceConst::self_to_serial(
                        xx,
                        loc,
                        i_const,
                        inner_make_body,
                    ).v {
                    members.push(m);
                }
            }
            let body = ArtifactNewDefSet {
                def_set_id: self_.set.def_set_id,
                members,
                dependencies: Vec::new(),
            };
            make_body(serde_json::to_value(&body).unwrap())
        });
        EResult::ok(art)
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
