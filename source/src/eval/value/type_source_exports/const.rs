use {
    crate::{
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
                type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
                type_source_function::r#const::TypeSourceFunctionConst,
            },
        },
        langhelp::{
            Artifact,
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
    fn from_serial(&self, xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        {
            let mut sets = xx.vx.ix.definition_sets.borrow_mut();
            let already = sets.iter().any(|s| s.def_set_id == self.set.def_set_id);
            if !already {
                sets.push(self.set.clone());
            }
        }
        let exports_type: Rc<dyn Type> =
            Rc::new(TypeSourceExportsLiftedConst { set: self.set.clone() });
        EvalRes::ok(CodeValue {
            code: Code {
                loc: loc,
                code: Code_::None,
            },
            value: Rc::new(ValueAnyConst { inner: exports_type }),
        })
    }

    fn to_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        _self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let artifact_id = serialize_definition_set(xx, loc, &self.set);
        EResult::ok(Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        })
    }
}

impl AspectBind for TypeSourceExportsLiftedConst {
    fn bind(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
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
                        {
                            let mut sets = xx.vx.ix.definition_sets.borrow_mut();
                            if !sets.iter().any(|s| s.def_set_id == self.set.def_set_id) {
                                sets.push(self.set.clone());
                            }
                        }
                        let func_type: Rc<dyn Type> =
                            Rc::new(TypeSourceFunctionConst { data: func_data.clone() });
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

impl Type for TypeSourceExportsLiftedConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(
            ValueAnyConst {
                inner: Rc::new(TypeSourceExportsLiftedConst { set: self.set.clone() }),
            },
        )
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

impl Value for TypeSourceExportsLiftedConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
