use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
        },
        eval::{
            aspect::AspectPull,
            definition_set::serialize_definition_set,
            pull_type_dispatch::{
                pull_aspect_v8,
                pull_can_cast_from,
                pull_can_cast_from_v8,
                pull_cast_from,
                pull_cast_from_v8,
                pull_desc,
            },
            value::{
                type_any::r#const::TypeAnyConst,
                type_root::r#const::TypeRoot,
                type_source_data_type::SourceDataTypeData,
                type_source_interface::SourceInterfaceRef,
                value_pull::aspect::TypePullAspect,
            },
        },
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            SourceId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectField,
            AspectFunction,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
        },
        syntax::ExprId,
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypePull {
    pub bundle: Option<v8::Global<v8::Object>>,
    pub data: Gc<SourceDataTypeData>,
}

impl Aspect for TypePull { }

impl ExporterAny for TypePull {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypePull {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypePull {
            data: self.data.clone(),
            bundle: self.bundle.clone(),
        })
    }

    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        let bundle_opt = self.bundle.as_ref();
        if let Some(bundle) = bundle_opt {
            let interface_impls = self.data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if *key == impl_data.interface.identity && impl_data.interface.is_aspect.get() {
                    return Some(Box::new(Box::new(TypePullAspect {
                        iface_ref: Rc::new(SourceInterfaceRef { data: impl_data.interface.clone() }),
                        bundle: Some(bundle.clone()),
                    }) as Box<dyn AspectField>));
                }
            }
        }
        let bundle = bundle_opt?;
        let dummy_loc: QExprId = (SourceId(0), ExprId(0));
        let aspect = pull_aspect_v8(&self.data, bundle, xx, dummy_loc, key)?;
        let aspect_pull = (&*aspect as &dyn std::any::Any).downcast_ref::<AspectPull>().unwrap().clone();
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(aspect_pull) as Box<dyn AspectField>));
        }
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(aspect_pull) as Box<dyn AspectFunction>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, source_type: &dyn Type) -> bool {
        if let Some(bundle) = &self.bundle {
            return pull_can_cast_from_v8(&self.data, bundle, xx, loc, source_type);
        }
        pull_can_cast_from(&self.data, xx, loc, source_type)
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        if let Some(bundle) = &self.bundle {
            return pull_cast_from_v8(&self.data, bundle, xx, loc, cv);
        }
        pull_cast_from(&self.data, xx, loc, cv)
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn desc(&self) -> String {
        pull_desc(&self.data)
    }
}

impl Value for TypePull {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::Pull))
    }
}
