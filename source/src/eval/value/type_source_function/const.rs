use crate::r#gen::ScopeEntry;
use {
    crate::{
        r#gen::{AspectArtifact, AspectBind, AspectFunction, aspect_artifact_aspect_id, aspect_bind_aspect_id, aspect_function_aspect_id},
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::value::{
            tuple::scope_entry::ScopeEntryValue,
            type_function::dynamic::dynamic_call,
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
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::TypeSourceFunctionConst,
};

impl Aspect for TypeSourceFunctionConst { }

impl AspectArtifact for TypeSourceFunctionConst {
    fn from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        let func_type: Rc<dyn crate::langhelp::Type> =
            Rc::new(TypeSourceFunctionConst { data: self.data.clone() });
        EvalRes::ok(CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
                inner: func_type,
            }),
        })
    }

    fn to_serial(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn Value>) -> EResult<Artifact> {
        let wrapper = must_downcast_ref::<crate::eval::value::type_any::r#const::ValueAnyConst>(self_.as_ref());
        let func = must_downcast_ref::<TypeSourceFunctionConst>(wrapper.inner.as_ref());
        // Ensure the def-set is registered. Bare function references that never
        // get called or lifted skip the usual registration paths, but their
        // serialization still requires the def-set's data types / interface
        // impls to have been compiled in this stage.
        {
            let set = &func.data.set;
            let mut sets = xx.vx.ix.definition_sets.borrow_mut();
            if !sets.iter().any(|s| s.def_set_id == set.def_set_id) {
                sets.push(set.clone());
            }
        }
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

impl AspectBind for TypeSourceFunctionConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((Code {
            loc: loc,
            code: Code_::None,
        }, Box::new(ScopeEntryValue { value: Rc::from(cv.value) }) as Box<dyn ScopeEntry>))
    }
}

impl AspectFunction for TypeSourceFunctionConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        {
            let set = &self.data.set;
            let mut sets = xx.vx.ix.definition_sets.borrow_mut();
            let already = sets.iter().any(|s| s.def_set_id == set.def_set_id);
            if !already {
                sets.push(set.clone());
            }
        }
        let scope_id = self.data.scope_entry_id.get().expect("Definition set function not registered");
        let arg_type = self.data.arg_type.borrow().clone().expect("Definition set function missing arg type");
        let ret_type = self.data.ret_type.borrow().clone().expect("Definition set function missing return type");
        let func_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: scope_id }),
        };
        dynamic_call(xx, loc, func_code, arg_type, ret_type, argument)
    }
}

impl crate::r#gen::AspectType for TypeSourceFunctionConst {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let f = crate::invariant::must_downcast_ref::<TypeSourceFunctionConst>(value);
        Rc::new(TypeSourceFunctionConst { data: f.data.clone() })
    }
}

impl Type for TypeSourceFunctionConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeSourceFunctionConst { data: self.data.clone() }),
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectFunction>),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectArtifact>),
            );
        }
        if *key == crate::r#gen::aspect_type_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as
                        Box<dyn crate::r#gen::AspectType>,
                ),
            );
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_function"),
        }
    }
}

impl Value for TypeSourceFunctionConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
