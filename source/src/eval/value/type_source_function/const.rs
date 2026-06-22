use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::{
            definition_set::serialize_definition_set,
            from_v8_ref,
            push::Push,
            to_v8,
            value::{
                tuple::scope_entry::ScopeEntryValue,
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_function::dynamic::dynamic_call,
                type_root::r#const::TypeRoot,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
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
        r#gen::{
            AspectArtifact,
            AspectBind,
            AspectFunction,
            AspectType,
            ScopeEntry,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_function_aspect_id,
            aspect_type_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeSourceFunctionConst {
    pub data: dumpster::unsync::Gc<super::SourceFunctionData>,
}

impl Aspect for TypeSourceFunctionConst { }

impl AspectArtifact for TypeSourceFunctionConst {
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
            xx.vx.definition_sets.borrow_mut().entry(set.def_set_id).or_insert_with(|| set.clone());
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

impl AspectType for TypeSourceFunctionConst {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let f = must_downcast_ref::<TypeSourceFunctionConst>(value);
        Rc::new(TypeSourceFunctionConst { data: f.data.clone() })
    }
}

impl ExporterAny for TypeSourceFunctionConst {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeSourceFunctionConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceFunctionConst { data: self.data.clone() }) })
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
        if *key == aspect_type_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectType>),
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
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceFunctionConst))
    }
}

impl Push for Rc<TypeSourceFunctionConst> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(super::dynamic::TypeSourceFunctionDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_v8_ref::<Rc<TypeSourceFunctionConst>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self).into()
    }
}
