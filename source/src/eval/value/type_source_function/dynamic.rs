use crate::r#gen::ScopeEntry;
use {
    crate::{
        r#gen::{AspectArtifact, AspectBind, AspectBindDynamic, AspectConsumeDynamic, AspectInitialBind, AspectLiftDynamic, AspectStackValueDynamic, aspect_artifact_aspect_id, aspect_bind_aspect_id, aspect_bind_dynamic_aspect_id, aspect_consume_dynamic_aspect_id, aspect_initial_bind_aspect_id, aspect_lift_dynamic_aspect_id, aspect_stack_value_dynamic_aspect_id},
        code_dynamic::CodeDynamic,
        eval::{
            Erb,
            definition_set::DefinitionSet,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            from_v8_ref,
            value::{
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_data_type::artifact::{
                    ArtifactDataDefSet,
                    ArtifactDataDefSetFunction,
                },
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Artifact,
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
            deserialize_independent_artifact,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        mem,
        rc::Rc,
    },
    super::{
        SourceFunctionData,
        TypeSourceFunctionConst,
    },
};

// --- TypeSourceFunctionDynamic: eval_half type of a function ref ---
pub(crate) struct TypeSourceFunctionDynamic;

impl Aspect for TypeSourceFunctionDynamic { }

impl AspectBind for TypeSourceFunctionDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::r#gen::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceFunctionDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceFunctionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceFunctionDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceFunctionDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeSourceFunctionDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let func_ref: Rc<TypeSourceFunctionConst> = from_v8_ref::<Rc<TypeSourceFunctionConst>>(&value).clone();
        let set = &func_ref.data.set;
        let mut sets = xx.vx.ix.definition_sets.borrow_mut();
        if !sets.iter().any(|s| s.def_set_id == set.def_set_id) {
            sets.push(set.clone());
        }
        let func_type: Rc<dyn crate::langhelp::Type> =
            Rc::new(TypeSourceFunctionConst { data: func_ref.data.clone() });
        Ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner: func_type }))
    }
}

impl crate::r#gen::AspectLowerDynamic for TypeSourceFunctionDynamic {
    fn lower(
        &self,
        xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let func_ref: Rc<TypeSourceFunctionConst> = from_v8_ref::<Rc<TypeSourceFunctionConst>>(&value).clone();
        let set = &func_ref.data.set;
        let mut sets = xx.vx.ix.definition_sets.borrow_mut();
        if !sets.iter().any(|s| s.def_set_id == set.def_set_id) {
            sets.push(set.clone());
        }
        let func_type: Rc<dyn crate::langhelp::Type> =
            Rc::new(TypeSourceFunctionConst { data: func_ref.data.clone() });
        Ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner: func_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceFunctionDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceFunctionDynamic),
            }),
        }
    }
}

impl Type for TypeSourceFunctionDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceFunctionDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectLiftDynamic>));
        }
        if *key == crate::r#gen::aspect_lower_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn crate::r#gen::AspectLowerDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectInitialBind>));
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &crate::r#gen::type_type_identity()).is_none() {
            return EvalRes::err(loc, format!("Cannot convert {} to {}", self.desc(), type_.desc()));
        }
        let source_code = match cv.code.cast::<CodeDynamic>() {
            crate::langhelp::EResult { v: Some(Some(c)), .. } => c.clone(),
            _ => return EvalRes::err(loc, format!("cast {} → {}: source has no runtime code", self.desc(), type_.desc())),
        };
        let call = CodeDynamic {
            loc,
            type_: crate::code_dynamic::CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                func: Box::new(CodeDynamic {
                    loc,
                    type_: crate::code_dynamic::CodeDynamicType::BindingUse(crate::code_dynamic::CodeDynamicBindingUse {
                        id: crate::eval::FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID,
                    }),
                }),
                args: vec![source_code],
            }),
        };
        let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceFunctionDynamic) as Rc<dyn Type>);
        EvalRes::ok(CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(call)) },
            value: Rc::new(ValueDynamic { source: ValueDynamicSource::Expr, type_: dest_rc }),
        })
    }

    fn desc(&self) -> String {
        format!("definition_set_function")
    }
}

impl Value for TypeSourceFunctionDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

pub(crate) struct TypeTypeSourceFunctionDynamic;

impl Aspect for TypeTypeSourceFunctionDynamic { }

impl AspectArtifact for TypeTypeSourceFunctionDynamic {
    fn from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        er_seq_cv!(loc, erb, {
            let serial: ArtifactDataDefSetFunction =
                erb.must_res(
                    serde_json::from_value(data.clone())
                        .map_err(|e| format!("Failed to parse defset function: {}", e)),
                )?;
            let def_set =
                erb.must_res(crate::eval::definition_set::load_def_set(xx, &serial.def_set))?;
            {
                let mut sets = xx.vx.ix.definition_sets.borrow_mut();
                if !sets.iter().any(|s| s.def_set_id == def_set.def_set_id) {
                    sets.push(def_set.clone());
                }
            }
            let functions = def_set.functions.borrow();
            let func_data = functions
                .iter()
                .find(|f| match f.scope_entry_id.get() {
                    Some(crate::langhelp::ScopeEntryId::Def(_, _, id)) => id == serial.entry_id,
                    _ => false,
                })
                .expect("Function not found in definition set")
                .clone();
            drop(functions);
            let func_type: Rc<dyn crate::langhelp::Type> =
                Rc::new(TypeSourceFunctionConst { data: func_data });
            return Ok(CodeValue {
                code: Code { loc, code: Code_::None },
                value: Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
                    inner: func_type,
                }),
            });
        })
    }

    fn to_serial(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn Value>) -> EResult<Artifact> {
        let func = must_downcast_ref::<TypeSourceFunctionConst>(self_.as_ref());
        // Serializing the def set populates each function's artifact_id.
        crate::eval::definition_set::serialize_definition_set(xx, loc, &func.data.set);
        let artifact_id = func
            .data
            .artifact_id
            .borrow()
            .clone()
            .expect("Function artifact ID should be set by serialize_definition_set");
        EResult::ok(Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        })
    }
}

impl Type for TypeTypeSourceFunctionDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeTypeSourceFunctionDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceFunctionDynamic) as Box<dyn AspectArtifact>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_function_type".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from definition_set_function_type".to_string())
    }

    fn desc(&self) -> String {
        "definition_set_function_type".to_string()
    }
}

impl Value for TypeTypeSourceFunctionDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
