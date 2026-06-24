use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
            impl_serialize_independent_artifact,
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
            AspectTaggedUnion,
            AspectType,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_tagged_union_aspect_id,
            aspect_type_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::SourceTaggedUnionData,
};

pub(crate) struct TypeSourceTaggedUnionConst {
    pub data: Rc<SourceTaggedUnionData>,
}

impl Aspect for TypeSourceTaggedUnionConst { }

impl AspectBind for TypeSourceTaggedUnionConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceTaggedUnionConst {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceTaggedUnionConst {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceTaggedUnionConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() }),
        })
    }
}

impl AspectStackValueDynamic for TypeSourceTaggedUnionConst {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() }),
            }),
        }
    }
}

impl AspectTaggedUnion for TypeSourceTaggedUnionConst {
    fn variants(&self) -> Vec<(ConstKey, String, Rc<dyn Type>)> {
        self.data.variants.borrow().iter().map(|v| (v.tag.clone(), v.js_tag.clone(), v.type_.clone())).collect()
    }
}

impl AspectType for TypeSourceTaggedUnionConst {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() })
    }
}

impl ExporterAny for TypeSourceTaggedUnionConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeSourceTaggedUnionConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == self.data.identity {
            return Some(Box::new(true));
        }
        if *key == aspect_tagged_union_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectTaggedUnion>,
                ),
            );
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as
                        Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as
                        Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectInitialBind>,
                ),
            );
        }
        if *key == aspect_type_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectType>),
            );
        }
        return None;
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &self.data.identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() }),
                }),
            });
        }
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() }),
            }),
        })
    }

    fn desc(&self) -> String {
        format!("tagged_union")
    }

}

impl Value for TypeSourceTaggedUnionConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceTaggedUnionConst))
    }
}

use super::artifact::{
    ArtifactDataSourceTaggedUnion,
    ArtifactDataSourceTaggedUnionVariant,
};

impl ExporterRootDelegate for TypeSourceTaggedUnionConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&crate::artifact::ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        let Some(id) = id else {
            return EResult::err(
                loc,
                "TypeSourceTaggedUnionConst requires an artifact id to deserialize".to_string(),
            );
        };
        let id_owned = id.clone();
        let res =
            crate::artifact::impl_deserialize_independent_artifact::<
                ArtifactDataSourceTaggedUnion,
            >(xx, loc, id, data, move |body: ArtifactDataSourceTaggedUnion| {
                let tu_data = Rc::new(super::SourceTaggedUnionData {
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
                        let variants: Vec<super::SourceTaggedUnionVariant> =
                            variants_serial.iter().map(|v| {
                                super::SourceTaggedUnionVariant {
                                    constructor_artifact_id: std::cell::RefCell::new(None),
                                    js_tag: v.js_tag.clone(),
                                    tag: v.tag.clone(),
                                    type_: crate::artifact::independent_finalize_deserialize_type_artifact(
                                        xx,
                                        loc,
                                        v.type_.clone(),
                                    )
                                        .v
                                        .expect("Failed to deserialize tagged union variant type"),
                                }
                            }).collect();
                        *tu_for_finalizer.variants.borrow_mut() = variants;
                    });
                Ok((out, deps, finalizer))
            });
        let errors = res.errors;
        let Some(any) = res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(Rc::new(any) as Rc<dyn Any>),
        };
    }

    fn self_to_serial(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn Any>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceTaggedUnionConst>().expect("expected TypeSourceTaggedUnionConst");
        let mut errors: Option<Vec<crate::langhelp::ExprErr>> = None;
        let mut variants_serial: Vec<ArtifactDataSourceTaggedUnionVariant> = Vec::new();
        for v in self_.data.variants.borrow().iter() {
            let type_art = crate::artifact::serialize_artifact(xx, loc, v.type_.as_value().as_ref());
            if let Some(errors1) = type_art.errors {
                errors.get_or_insert_default().extend(errors1);
            }
            let Some(type_art) = type_art.v else {
                return EResult {
                    errors,
                    v: None,
                };
            };
            variants_serial.push(ArtifactDataSourceTaggedUnionVariant {
                tag: v.tag.clone(),
                js_tag: v.js_tag.clone(),
                type_: type_art,
            });
        }
        let art =
            impl_serialize_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.data.new_artifact_id, || {
                let body = ArtifactDataSourceTaggedUnion {
                    identity: self_.data.identity.clone(),
                    variants: variants_serial,
                };
                make_body(serde_json::to_value(&body).unwrap())
            });
        return EResult {
            errors,
            v: Some(art),
        };
    }
}
