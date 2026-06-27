use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            ExporterDataAny,
            export_artifact,
            impl_export_independent_artifact,
            impl_import_independent_artifact,
            independent_finalize_import_artifact,
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
                type_any::r#const::ValueAnyConst,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_source_union::{
                    SourceUnionData,
                    r#const::TypeSourceUnionConst,
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
            Binding,
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
    super::artifact::ArtifactDataSourceUnionNew,
};

#[derive(Clone)]
pub(crate) struct TypeSourceUnionNewConst {
    pub data: Rc<SourceUnionData>,
    pub variant_index: usize,
}

impl Aspect for TypeSourceUnionNewConst { }

impl AspectBind for TypeSourceUnionNewConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectFunction for TypeSourceUnionNewConst {
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
            let result_type = Rc::new(TypeSourceUnionConst { data: self.data.clone() });
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

impl ExporterAny for TypeSourceUnionNewConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeSourceUnionNewConst {
    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceUnionNewConst>().expect("expected TypeSourceUnionNewConst");
        let errors: std::rc::Rc<std::cell::RefCell<Option<Vec<crate::langhelp::ExprErr>>>> =
            std::rc::Rc::new(std::cell::RefCell::new(None));
        let errors_for_closure = errors.clone();
        let variant_index = self_.variant_index;
        let variants = self_.data.variants.borrow();
        let Some(variant) = variants.get(variant_index) else {
            return EResult::err(loc, format!("union variant index {} out of range", variant_index));
        };
        let art = impl_export_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &variant.new_artifact_id, || {
            let tu_const = TypeSourceUnionConst { data: self_.data.clone() };
            let mut tu_art = export_artifact(xx, loc, &tu_const as &dyn Value);
            if let Some(errs) = tu_art.errors.take() {
                errors_for_closure.borrow_mut().get_or_insert_default().extend(errs);
            }
            let tu_art = tu_art.v.unwrap_or(Artifact::Body(ArtifactBody {
                data: serde_json::Value::Null,
                exporter: crate::artifact::ArtifactBodyExporter::Root,
            }));
            let body = ArtifactDataSourceUnionNew {
                union: tu_art,
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

    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let Some(id) = id else {
            return EResult::err(loc, "TypeSourceUnionNewConst requires an artifact id to deserialize".to_string());
        };
        let id_owned = id.clone();
        let holder: Rc<std::cell::RefCell<Option<(Rc<SourceUnionData>, usize)>>> =
            Rc::new(std::cell::RefCell::new(None));
        let holder_for_finalizer = holder.clone();
        let res =
            impl_import_independent_artifact::<ArtifactDataSourceUnionNew>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataSourceUnionNew| {
                    let tu_art = body.union;
                    let variant_index = body.variant_index;
                    let deps = vec![tu_art.clone()];
                    let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(holder.clone()));
                    let finalizer: crate::artifact::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            if let Some(parent_any) = independent_finalize_import_artifact(xx, loc, tu_art).v {
                                let rc_any: Rc<dyn Any> = parent_any;
                                if let Ok(wrapped) = rc_any.downcast::<ExporterDataAny<Rc<SourceUnionData>>>() {
                                    let tu_data = wrapped.0.clone();
                                    if let Some(variant) = tu_data.variants.borrow().get(variant_index) {
                                        *variant.new_artifact_id.borrow_mut() = Some(id_owned.clone());
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
}

impl Type for TypeSourceUnionNewConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceUnionNewConst {
            data: self.data.clone(),
            variant_index: self.variant_index,
        }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceUnionNewConst {
                data: self.data.clone(),
                variant_index: self.variant_index,
            }) as Box<dyn AspectBind>));
        }
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceUnionNewConst {
                data: self.data.clone(),
                variant_index: self.variant_index,
            }) as Box<dyn AspectFunction>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to union_new"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert union_new"))
    }

    fn desc(&self) -> String {
        let variants = self.data.variants.borrow();
        let tag = &variants[self.variant_index].tag;
        format!("union_new({:?})", tag)
    }
}

impl Value for TypeSourceUnionNewConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceUnionNewConst))
    }
}

impl crate
::artifact
::ExporterAny for crate::artifact::ExporterDataAny<Rc<std::cell::RefCell<Option<(Rc<SourceUnionData>, usize)>>>> {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        let borrowed = self.0.borrow();
        let (data, variant_index) = borrowed.as_ref().expect("constructor holder not populated").clone();
        EResult::ok(Rc::new(TypeSourceUnionNewConst {
            data,
            variant_index,
        }))
    }

    fn as_value(&self) -> EResult<Rc<dyn crate::langhelp::Value>> {
        let borrowed = self.0.borrow();
        let (data, variant_index) = borrowed.as_ref().expect("constructor holder not populated").clone();
        let inner: Rc<dyn Type> = Rc::new(TypeSourceUnionNewConst {
            data,
            variant_index,
        });
        EResult::ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner }))
    }
}
