use {
    crate::{
        builtin::{
            AspectBind,
            AspectFunction,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_stack_value_dynamic_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            Erb,
            definition_set::DefinitionSet,
            typecheck_convert,
            value::{
                type_source_data::TypeSourceDataTypeConst,
                type_type_source_data_type_constructor::dynamic::TypeTypeSourceDataTypeConstructorDynamic,
            },
        },
        langhelp::{
            Artifact,
            ArtifactId,
            Aspect,
            Code,
            CodeValue,
            Code_,
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
    super::{
        TypeSourceDataTypeConstructorConst,
        artifact::{
            ArtifactDataDefSetCtorData,
            ArtifactDataDefSetCtorMethod,
        },
        dynamic::{
            serialize_type_ref,
            unique_to_serial,
        },
    },
};

impl TypeSourceDataTypeConstructorConst {
    pub(crate) fn serialize_ctor(&self, xx: &ExprContext, gx: &GlobalContext, loc: QExprId) -> ArtifactId {
        if let Some(existing_id) = self.data.artifact_id.borrow().clone() {
            return existing_id;
        }
        let artifact_id = gx.artifacts.reserve_id();
        *self.data.artifact_id.borrow_mut() = Some(artifact_id.clone());
        let serial_data = {
            let underlying_type = self.data.underlying_type.borrow();
            let underlying_type = underlying_type.as_ref().expect("Data type missing underlying type");
            let methods: Vec<ArtifactDataDefSetCtorMethod> = self.data.methods.borrow().iter().map(|m| {
                let compiled_js = m.compiled_js.borrow();
                let compiled_js = compiled_js.as_ref().expect("Method missing compiled JS");
                let ret_type = m.ret_type.borrow().clone().expect("Method missing ret type");
                ArtifactDataDefSetCtorMethod {
                    name: m.name.clone(),
                    field_name: m.field_name.clone(),
                    ret_type: serialize_type_ref(xx, gx, loc, ret_type.as_ref()),
                    compiled_js: compiled_js.clone(),
                }
            }).collect();
            ArtifactDataDefSetCtorData {
                name: self.data.name.clone(),
                data_field: self.data.data_field.clone(),
                underlying_type: serialize_type_ref(xx, gx, loc, underlying_type.as_ref()),
                identity: unique_to_serial(&self.data.identity),
                methods,
                import_id: self.data.set.import_id,
                def_set_id: self.data.set.def_set_id,
            }
        };
        let json = serde_json::to_value(&serial_data).expect("Failed to serialize defset ctor data");
        gx.artifacts.set(artifact_id.clone(), Artifact {
            type_: ArtifactId::Builtin(TypeTypeSourceDataTypeConstructorDynamic::artifact_id()),
            data: json,
        });
        artifact_id
    }
}

impl Aspect for TypeSourceDataTypeConstructorConst { }

impl AspectBind for TypeSourceDataTypeConstructorConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok(
            (
                Code {
                    loc: loc,
                    code: Code_::None,
                },
                Box::new(crate::eval::value::type_tuple::ScopeEntryValue { value: Rc::from(cv.value) }) as
                    Box<dyn ScopeEntry>,
            ),
        )
    }
}

impl AspectFunction for TypeSourceDataTypeConstructorConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        {
            let set = &self.data.set;
            let mut sets = xx.vx.definition_sets.borrow_mut();
            let already =
                sets.iter().any(|s| std::ptr::eq(&**s as *const DefinitionSet, &**set as *const DefinitionSet));
            if !already {
                sets.push(set.clone());
            }
        }
        let underlying_type =
            self.data.underlying_type.borrow().as_ref().expect("Data type missing underlying type").clone();
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
            let mut object_fields = vec![(self.data.data_field.clone(), underlying_js),];
            for method in self.data.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
                object_fields.push((method.field_name.clone(), CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
                }));
            }
            for impl_data in self.data.interface_impls.borrow().iter() {
                let iface_methods = impl_data.interface.methods.borrow();
                for method_impl in &impl_data.method_impls {
                    let iface_method = &iface_methods[method_impl.interface_method_index];
                    let method_id =
                        method_impl.scope_entry_id.get().expect("Interface method impl scope entry ID not allocated");
                    object_fields.push((iface_method.field_name.clone(), CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
                    }));
                }
            }
            let result_type = Rc::new(TypeSourceDataTypeConst { data: self.data.clone(), });
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: object_fields }),
            }));
        });
    }
}

impl Type for TypeSourceDataTypeConstructorConst {
    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_data_type_constructor"),
        }
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceDataTypeConstructorConst { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConstructorConst { data: self.data.clone() }) as
                        Box<dyn AspectFunction>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConstructorConst { data: self.data.clone() }) as Box<dyn AspectBind>,
                ),
            );
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }
}

impl Value for TypeSourceDataTypeConstructorConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeTypeSourceDataTypeConstructorDynamic)
    }
}
