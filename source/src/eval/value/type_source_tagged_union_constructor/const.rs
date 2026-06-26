use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            ExporterDataAny,
            impl_deserialize_independent_artifact,
            impl_serialize_independent_artifact,
            independent_finalize_deserialize_artifact,
            serialize_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            EResult,
            dynamic_util::bind_via_target,
            typecheck_convert,
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_source_tagged_union::{
                    SourceTaggedUnionData,
                    r#const::TypeSourceTaggedUnionConst,
                },
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            ConstKey,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectFunction,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_stack_value_dynamic_get,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::artifact::ArtifactDataSourceTaggedUnionConstructor,
};

#[derive(Clone)]
pub(crate) struct TypeSourceTaggedUnionConstructorConst {
    pub data: Rc<SourceTaggedUnionData>,
    pub variant_index: usize,
}

impl Aspect for TypeSourceTaggedUnionConstructorConst { }

impl AspectBind for TypeSourceTaggedUnionConstructorConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectFunction for TypeSourceTaggedUnionConstructorConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let variants = self.data.variants.borrow();
        let variant = &variants[self.variant_index];
        let js_tag = variant.js_tag.clone();
        let underlying_type = variant.type_.clone();
        drop(variants);
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
            let result_type = Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() });
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(
                    CodeDynamicNewObject { fields: vec![(ConstKey::String(js_tag).to_str(), underlying_js)], }
                ),
            }));
        });
    }
}

impl ExporterAny for TypeSourceTaggedUnionConstructorConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeSourceTaggedUnionConstructorConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let Some(id) = id else {
            return EResult::err(
                loc,
                "TypeSourceTaggedUnionConstructorConst requires an artifact id to deserialize".to_string(),
            );
        };
        let id_owned = id.clone();
        let holder: Rc<std::cell::RefCell<Option<(Rc<SourceTaggedUnionData>, usize)>>> =
            Rc::new(std::cell::RefCell::new(None));
        let holder_for_finalizer = holder.clone();
        let res =
            impl_deserialize_independent_artifact::<ArtifactDataSourceTaggedUnionConstructor>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataSourceTaggedUnionConstructor| {
                    let tu_art = body.tagged_union;
                    let variant_index = body.variant_index;
                    let deps = vec![tu_art.clone()];
                    let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(holder.clone()));
                    let finalizer: crate::artifact::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            if let Some(parent_any) = independent_finalize_deserialize_artifact(xx, loc, tu_art).v {
                                let rc_any: Rc<dyn Any> = parent_any;
                                if let Ok(wrapped) =
                                    rc_any.downcast::<ExporterDataAny<Rc<SourceTaggedUnionData>>>() {
                                    let tu_data = wrapped.0.clone();
                                    if let Some(variant) = tu_data.variants.borrow().get(variant_index) {
                                        *variant.constructor_artifact_id.borrow_mut() = Some(id_owned.clone());
                                    }
                                    *holder_for_finalizer.borrow_mut() = Some((tu_data, variant_index));
                                }
                            }
                        });
                    Ok((out, deps, finalizer))
                },
            );
        let errors = res.errors;
        let Some(any) = res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(any),
        };
    }

    fn self_to_serial(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ =
            self_
                .downcast::<TypeSourceTaggedUnionConstructorConst>()
                .expect("expected TypeSourceTaggedUnionConstructorConst");
        let errors: std::rc::Rc<std::cell::RefCell<Option<Vec<crate::langhelp::ExprErr>>>> =
            std::rc::Rc::new(std::cell::RefCell::new(None));
        let errors_for_closure = errors.clone();
        let variant_index = self_.variant_index;
        let variants = self_.data.variants.borrow();
        let Some(variant) = variants.get(variant_index) else {
            return EResult::err(loc, format!("tagged union variant index {} out of range", variant_index));
        };
        let art =
            impl_serialize_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &variant.constructor_artifact_id, || {
                let tu_const = TypeSourceTaggedUnionConst { data: self_.data.clone() };
                let mut tu_art = serialize_artifact(xx, loc, &tu_const as &dyn Value);
                if let Some(errs) = tu_art.errors.take() {
                    errors_for_closure.borrow_mut().get_or_insert_default().extend(errs);
                }
                let tu_art = tu_art.v.unwrap_or(Artifact::Body(ArtifactBody {
                    data: serde_json::Value::Null,
                    exporter: crate::artifact::ArtifactBodyExporter::Root,
                }));
                let body = ArtifactDataSourceTaggedUnionConstructor {
                    tagged_union: tu_art,
                    variant_index,
                };
                make_body(serde_json::to_value(&body).unwrap())
            });
        let errors = errors.borrow_mut().take();
        return EResult {
            errors,
            v: Some(art),
        };
    }
}

impl Type for TypeSourceTaggedUnionConstructorConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceTaggedUnionConstructorConst {
            data: self.data.clone(),
            variant_index: self.variant_index,
        }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionConstructorConst {
                data: self.data.clone(),
                variant_index: self.variant_index,
            }) as Box<dyn AspectBind>));
        }
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionConstructorConst {
                data: self.data.clone(),
                variant_index: self.variant_index,
            }) as Box<dyn AspectFunction>));
        }
        if *key == crate::r#gen::aspect_unlift_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::UnliftIdentity) as Box<dyn crate::r#gen::AspectUnlift>),
            );
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union_constructor"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union_constructor"))
    }

    fn desc(&self) -> String {
        let variants = self.data.variants.borrow();
        let tag = &variants[self.variant_index].tag;
        format!("tagged_union_constructor({:?})", tag)
    }
}

impl Value for TypeSourceTaggedUnionConstructorConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceTaggedUnionConstructorConst))
    }
}

impl crate
::artifact
::ExporterAny for crate
::artifact
::ExporterDataAny<Rc<std::cell::RefCell<Option<(Rc<SourceTaggedUnionData>, usize)>>>> {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!(
            "ExporterDataAny<Rc<RefCell<Option<(Rc<SourceTaggedUnionData>, usize)>>>>::as_type not meaningful"
        )
    }

    fn as_value(&self) -> EResult<Rc<dyn crate::langhelp::Value>> {
        unimplemented!(
            "ExporterDataAny<Rc<RefCell<Option<(Rc<SourceTaggedUnionData>, usize)>>>>::as_value not meaningful"
        )
    }
}
