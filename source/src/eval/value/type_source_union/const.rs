use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
            impl_export_independent_artifact,
        },
        code_dynamic::CodeDynamic,
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
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
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectStackValueDynamic,
            AspectUnion,
            AspectType,
            AspectPostDeserialize,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_union_aspect_id,
            aspect_type_aspect_id,
            aspect_post_deserialize_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::{
        SourceUnionData,
        artifact::{
            ArtifactDataSourceUnion,
            ArtifactDataSourceUnionVariant,
        },
    },
};

#[derive(Clone)]
pub(crate) struct TypeSourceUnionConst {
    pub data: Rc<SourceUnionData>,
}

impl Aspect for TypeSourceUnionConst { }

impl AspectBind for TypeSourceUnionConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceUnionConst {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceUnionConst {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceUnionConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceUnionConst { data: self.data.clone() }),
        })
    }
}

impl AspectStackValueDynamic for TypeSourceUnionConst {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceUnionConst { data: self.data.clone() }),
            }),
        }
    }
}

impl AspectUnion for TypeSourceUnionConst {
    fn variants(&self) -> Vec<(ConstKey, String, Rc<dyn Type>)> {
        self.data.variants.borrow().iter().map(|v| (v.tag.clone(), v.js_tag.clone(), v.type_.clone())).collect()
    }
}

impl AspectType for TypeSourceUnionConst {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeSourceUnionConst { data: self.data.clone() })
    }
}

impl AspectPostDeserialize for TypeSourceUnionConst {
    fn post_deserialize(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeValue> {
        EResult::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(super::dynamic::TypeSourceUnionDynamic { data: self.data.clone() }),
            }),
        })
    }
}

impl ExporterAny for TypeSourceUnionConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeSourceUnionConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&crate::artifact::ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let Some(id) = id else {
            return EResult::err(
                loc,
                "TypeSourceUnionConst requires an artifact id to deserialize".to_string(),
            );
        };
        let id_owned = id.clone();
        let res =
            crate::artifact::impl_import_independent_artifact::<ArtifactDataSourceUnion>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataSourceUnion| {
                    let tu_data = Rc::new(super::SourceUnionData {
                        identity: body.identity.clone(),
                        new_artifact_id: std::cell::RefCell::new(Some(id_owned.clone())),
                        next_tag_id: crate::langhelp::Counter::default(),
                        variants: std::cell::RefCell::new(Vec::new()),
                    });
                    let tu_for_finalizer = tu_data.clone();
                    let variants_serial = body.variants;
                    let deps: Vec<Artifact> = variants_serial.iter().map(|v| v.type_.clone()).collect();
                    let out: Rc<dyn ExporterAny> = Rc::new(crate::artifact::ExporterDataAny(tu_data.clone()));
                    let finalizer: crate::artifact::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            let variants: Vec<super::SourceUnionVariant> = variants_serial.iter().map(|v| {
                                super::SourceUnionVariant {
                                    new_artifact_id: std::cell::RefCell::new(None),
                                    js_tag: v.js_tag.clone(),
                                    tag: v.tag.clone(),
                                    type_: crate::artifact::independent_finalize_import_type_artifact(
                                        xx,
                                        loc,
                                        v.type_.clone(),
                                    )
                                        .v
                                        .expect("Failed to deserialize union variant type"),
                                }
                            }).collect();
                            *tu_for_finalizer.variants.borrow_mut() = variants;
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

    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceUnionConst>().expect("expected TypeSourceUnionConst");
        let errors: std::rc::Rc<std::cell::RefCell<Option<Vec<crate::langhelp::ExprErr>>>> =
            std::rc::Rc::new(std::cell::RefCell::new(None));
        let errors_for_closure = errors.clone();
        let null_art = || {
            Artifact::Body(ArtifactBody {
                data: serde_json::Value::Null,
                exporter: crate::artifact::ArtifactBodyExporter::Root,
            })
        };
        let art = impl_export_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.data.new_artifact_id, || {
            let push_err = |errs: Vec<crate::langhelp::ExprErr>| {
                errors_for_closure.borrow_mut().get_or_insert_default().extend(errs);
            };
            let mut variants_serial: Vec<ArtifactDataSourceUnionVariant> = Vec::new();
            for v in self_.data.variants.borrow().iter() {
                let mut type_art = crate::artifact::export_artifact(xx, loc, v.type_.as_value().as_ref());
                if let Some(errs) = type_art.errors.take() {
                    push_err(errs);
                }
                variants_serial.push(ArtifactDataSourceUnionVariant {
                    tag: v.tag.clone(),
                    js_tag: v.js_tag.clone(),
                    type_: type_art.v.unwrap_or_else(null_art),
                });
            }
            let body = ArtifactDataSourceUnion {
                identity: self_.data.identity.clone(),
                variants: variants_serial,
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

impl Type for TypeSourceUnionConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceUnionConst { data: self.data.clone() }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == self.data.identity {
            return Some(Box::new(true));
        }
        if *key == aspect_union_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionConst { data: self.data.clone() }) as Box<dyn AspectUnion>,
                ),
            );
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionConst { data: self.data.clone() }) as
                        Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionConst { data: self.data.clone() }) as Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceUnionConst { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionConst { data: self.data.clone() }) as
                        Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionConst { data: self.data.clone() }) as Box<dyn AspectInitialBind>,
                ),
            );
        }
        if *key == aspect_type_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceUnionConst { data: self.data.clone() }) as Box<dyn AspectType>),
            );
        }
        if *key == aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceUnionConst { data: self.data.clone() }) as Box<dyn AspectPostDeserialize>),
            );
        }
        return None;
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &self.data.identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceUnionConst { data: self.data.clone() }),
                }),
            });
        }
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceUnionConst { data: self.data.clone() }),
            }),
        })
    }

    fn desc(&self) -> String {
        format!("union")
    }
}

impl Value for TypeSourceUnionConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceUnionConst))
    }
}

impl crate::artifact::ExporterAny for crate::artifact::ExporterDataAny<Rc<super::SourceUnionData>> {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeSourceUnionConst { data: self.0.clone() }))
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        EResult::ok(Rc::new(TypeSourceUnionConst { data: self.0.clone() }))
    }
}
