pub mod artifact;
pub mod binding;

use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            export_artifact,
            impl_export_independent_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicNewArray,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::wrap_with_carry,
            typecheck_convert,
            util::er_seq_cv_inline,
            value::{
                type_any::dynamic::TypeAnyDynamic,
                type_binding::dynamic::BindingDynamicBinding,
                type_root::r#const::ExporterRootDelegate,
                type_tuple::artifact::ArtifactTupleType,
                value_empty::{
                    binding::BindingEmpty,
                    r#const::ValueEmptyConst,
                },
            },
        },
        invariant::must_downcast_box,
        langhelp::{
            Aspect,
            BindingId,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            External,
            QExprId,
            SourceId,
            Type,
            Unique,
            Value,
            downcast_value,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLift,
            AspectPostDeserialize,
            AspectTuple,
            Binding,
            TUPLE_TO_TYPE_FN_SCOPE_ID,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_initial_bind_aspect_id,
            aspect_lift_aspect_id,
            aspect_post_deserialize_aspect_id,
            aspect_post_deserialize_get,
            aspect_tuple_aspect_id,
            aspect_tuple_get_maybe,
            type_type_identity,
        },
        syntax::ExprId,
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct Tuple {
    pub elements: Vec<TupleElement>,
}

impl Tuple {
    fn bind_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        let tuple: Tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(cv.value)).ok().expect("Tuple bind_sub: Rc not unique");
        let mut errors = vec![];
        let mut bind_results: Vec<TupleBindSubResult> = Vec::new();
        let mut has_target = false;
        let mut extra_codes: Vec<Code> = vec![cv.code];
        for elem in tuple.elements.into_iter() {
            match elem {
                TupleElement::Dynamic(_) => {
                    errors.push(ExprErr {
                        loc: loc,
                        errors: vec![
                            ExprSubErr::Opaque(format!("Tuple element is dynamic-only, cannot bind directly")),
                        ],
                    });
                },
                TupleElement::EvalRes(elem_res) => {
                    if let Some(errs) = elem_res.errors {
                        errors.extend(errs);
                    }
                    if let Some(elem_cv) = elem_res.v {
                        let saved_code = elem_cv.code.clone();
                        match xx.target.maybe_bind_sub(xx, loc, elem_cv).into_res() {
                            Ok(Some(sub)) => {
                                has_target = true;
                                bind_results.push(TupleBindSubResult::Target(sub));
                            },
                            Ok(None) => {
                                extra_codes.push(saved_code);
                                bind_results.push(TupleBindSubResult::Const);
                            },
                            Err(e) => errors.extend(e),
                        }
                    }
                },
            }
        }
        if !errors.is_empty() {
            return EResult {
                v: None,
                errors: Some(errors),
            };
        }
        let carry = match xx.target.seq(loc, extra_codes).into_res() {
            Ok(c) => c,
            Err(e) => return EResult {
                v: None,
                errors: Some(e),
            },
        };
        if !has_target {
            if matches!(carry.code, Code_::None) {
                return EResult::ok(None);
            }
            let tuple_type = Rc::new(self.clone_as_type()) as Rc<dyn Type>;
            return xx.target.code_to_bind_sub(loc, carry, tuple_type).map(|sub| Some(sub));
        }
        let mut target_elements = Vec::new();
        for result in bind_results {
            match result {
                TupleBindSubResult::Target(sub) => {
                    target_elements.push(sub);
                },
                TupleBindSubResult::Const => {
                    errors.push(ExprErr {
                        loc: loc,
                        errors: vec![
                            ExprSubErr::Opaque(
                                format!("Tuple element is const but other elements require target binding"),
                            ),
                        ],
                    });
                },
            }
        }
        if !errors.is_empty() {
            return EResult {
                v: None,
                errors: Some(errors),
            };
        }
        let combined = xx.target.bind_sub_tuple(loc, carry, target_elements);
        EResult::ok(Some(combined))
    }

    pub fn clone_as_type(&self) -> Tuple {
        Tuple { elements: self.elements.iter().map(|e| {
            match e {
                TupleElement::Dynamic(t) => TupleElement::Dynamic(t.clone()),
                TupleElement::EvalRes(_) => panic!("clone_as_type on non-type tuple"),
            }
        }).collect() }
    }
}

impl Aspect for Tuple { }

impl AspectBind for Tuple {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        let tuple_type = Rc::new(self.clone_as_type()) as Rc<dyn Type>;
        match self.bind_sub(xx, loc, cv).into_res() {
            Err(errs) => EResult {
                v: None,
                errors: Some(errs),
            },
            Ok(None) => {
                EResult::ok((Code {
                    loc: loc,
                    code: Code_::None,
                }, Box::new(BindingEmpty { type_: tuple_type }) as Box<dyn Binding>))
            },
            Ok(Some(sub)) => {
                let (code, js_key) = xx.target.bind(xx, loc, sub);
                EResult::ok((code, Box::new(BindingDynamicBinding {
                    dynamic_key: js_key,
                    type_: tuple_type,
                }) as Box<dyn Binding>))
            },
        }
    }
}

impl AspectBindDynamic for Tuple {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        self.bind_sub(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for Tuple {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        let carry = cv.code;
        let tuple: Tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(cv.value)).ok().expect("Tuple to_dynamic: Rc not unique");
        let types: Vec<Rc<dyn Type>> = self.elements.iter().map(|e| {
            match e {
                TupleElement::Dynamic(t) => t.clone(),
                TupleElement::EvalRes(_) => panic!("to_dynamic on non-type tuple"),
            }
        }).collect();
        let mut errors = vec![];
        let mut dynamic_elements = Vec::new();
        for (elem, elem_type) in tuple.elements.into_iter().zip(types.iter()) {
            let elem_res = match elem {
                TupleElement::EvalRes(er) => er,
                TupleElement::Dynamic(_) => panic!("to_dynamic on dynamic-only tuple element"),
            };
            if let Some(errs) = elem_res.errors {
                errors.extend(errs);
            }
            if let Some(elem_cv) = elem_res.v {
                match aspect_consume_dynamic_get(xx, loc, elem_type.as_ref()).into_res() {
                    Ok(to_dynamic) => match to_dynamic.to_dynamic(xx, loc, elem_cv).into_res() {
                        Ok(dynamic_code) => dynamic_elements.push(dynamic_code),
                        Err(e) => errors.extend(e),
                    },
                    Err(e) => errors.extend(e),
                }
            }
        }
        if !errors.is_empty() {
            return EResult {
                v: None,
                errors: Some(errors),
            };
        }
        EResult::ok(wrap_with_carry(loc, carry, CodeDynamic {
            loc,
            type_: CodeDynamicType::NewArray(CodeDynamicNewArray { elements: dynamic_elements }),
        }))
    }
}

impl AspectInitialBind for Tuple {
    fn initial_bind(&self, _dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingEmpty { type_: Rc::new(self.clone_as_type()) as Rc<dyn Type> })
    }
}

impl AspectPostDeserialize for Tuple {
    fn post_deserialize(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeValue> {
        let tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(cv.value))
                .ok()
                .expect("Tuple post_deserialize: Rc not unique");
        let mut new_elements = Vec::with_capacity(tuple.elements.len());
        let mut errors = None;
        for elem in tuple.elements.into_iter() {
            let new_elem = match elem {
                TupleElement::Dynamic(t) => TupleElement::Dynamic(t),
                TupleElement::EvalRes(eval_res) => {
                    let mut elem_res = eval_res;
                    elem_res.push_errors(&mut errors);
                    let Some(elem_cv) = elem_res.v else {
                        new_elements.push(TupleElement::EvalRes(EResult {
                            errors: None,
                            v: None,
                        }));
                        continue;
                    };
                    let mut elem_type_res = elem_cv.value.type_(xx, loc);
                    elem_type_res.push_errors(&mut errors);
                    let Some(elem_type) = elem_type_res.v else {
                        continue;
                    };
                    let mut post_deserialize_res = aspect_post_deserialize_get(xx, loc, elem_type.as_ref());
                    post_deserialize_res.push_errors(&mut errors);
                    let Some(u) = post_deserialize_res.v else {
                        continue;
                    };
                    let mut r = u.post_deserialize(xx, loc, elem_cv);
                    r.push_errors(&mut errors);
                    let Some(new_cv) = r.v else {
                        continue;
                    };
                    TupleElement::EvalRes(EResult::ok(new_cv))
                },
            };
            new_elements.push(new_elem);
        }
        EResult {
            errors,
            v: Some(CodeValue {
                code: cv.code,
                value: Rc::new(Tuple { elements: new_elements }),
            }),
        }
    }
}

impl AspectTuple for Tuple {
    fn element_types(&self) -> Vec<Rc<dyn Type>> {
        self.elements.iter().map(|e| {
            match e {
                TupleElement::Dynamic(t) => t.clone(),
                TupleElement::EvalRes(_) => panic!("element_types on non-type tuple"),
            }
        }).collect()
    }

    fn take_elements(&self, value: Rc<dyn Value>) -> Vec<EvalRes> {
        let tuple: Tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(value)).ok().expect("take_elements: Rc not unique");
        tuple.elements.into_iter().map(|e| {
            match e {
                TupleElement::EvalRes(er) => er,
                TupleElement::Dynamic(_) => panic!("take_elements encountered Mortar element"),
            }
        }).collect()
    }
}

impl crate::r#gen::AspectArtifact for Tuple {
    fn exporter(&self) -> Rc<dyn crate::artifact::ArtifactExporter> {
        Rc::new(crate::artifact::ExporterRoot::Tuple)
    }
}

impl ExporterAny for Tuple {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for Tuple {
    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<Tuple>().expect("expected Tuple");
        let errors: std::rc::Rc<std::cell::RefCell<Option<Vec<crate::langhelp::ExprErr>>>> =
            std::rc::Rc::new(std::cell::RefCell::new(None));
        let errors_for_closure = errors.clone();
        let cell = std::cell::RefCell::new(None);
        let art = impl_export_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &cell, || {
            let push_err = |errs: Vec<crate::langhelp::ExprErr>| {
                errors_for_closure.borrow_mut().get_or_insert_default().extend(errs);
            };
            let null_art = || Artifact::Body(ArtifactBody {
                data: serde_json::Value::Null,
                exporter: crate::artifact::ArtifactBodyExporter::Root,
            });
            let mut elements: Vec<Artifact> = Vec::new();
            for e in self_.elements.iter() {
                let t = match e {
                    TupleElement::Dynamic(t) => t.clone(),
                    TupleElement::EvalRes(er) => match &er.v {
                        Some(cv) => match cv.value.type_(xx, loc).v {
                            Some(t) => t,
                            None => {
                                push_err(vec![crate::langhelp::ExprErr {
                                    loc,
                                    errors: vec![
                                        crate::langhelp::ExprSubErr::Opaque(
                                            "Tuple element type unavailable".to_string(),
                                        ),
                                    ],
                                }]);
                                elements.push(null_art());
                                continue;
                            },
                        },
                        None => {
                            push_err(vec![crate::langhelp::ExprErr {
                                loc,
                                errors: vec![
                                    crate::langhelp::ExprSubErr::Opaque(
                                        "Tuple element has no value to serialize".to_string(),
                                    ),
                                ],
                            }]);
                            elements.push(null_art());
                            continue;
                        },
                    },
                };
                let mut art = export_artifact(xx, loc, t.as_value().as_ref());
                if let Some(errs) = art.errors.take() {
                    push_err(errs);
                }
                elements.push(art.v.unwrap_or_else(null_art));
            }
            make_body(serde_json::to_value(&ArtifactTupleType { elements }).unwrap())
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
            return EResult::err(loc, "Tuple requires an artifact id to deserialize".to_string());
        };
        let holder: Rc<std::cell::RefCell<Option<Tuple>>> = Rc::new(std::cell::RefCell::new(None));
        let holder_for_finalizer = holder.clone();
        return crate::artifact::impl_import_independent_artifact::<ArtifactTupleType>(
            xx,
            loc,
            id,
            data,
            move |body: ArtifactTupleType| {
                let element_arts = body.elements;
                let deps: Vec<Artifact> = element_arts.clone();
                let finalizer: crate::artifact::ArtifactFinalizer = Box::new(move |xx: &ExprContext, loc: QExprId| {
                    let mut elements = Vec::with_capacity(element_arts.len());
                    for elem_art in element_arts {
                        elements.push(
                            TupleElement::Dynamic(
                                crate::artifact::independent_finalize_import_type_artifact(xx, loc, elem_art)
                                    .v
                                    .expect("Failed to deserialize tuple element type"),
                            ),
                        );
                    }
                    *holder_for_finalizer.borrow_mut() = Some(Tuple { elements });
                });
                Ok(
                    (
                        Rc::new(crate::artifact::ExporterDataAny(holder.clone())) as
                            Rc<dyn crate::artifact::ExporterAny>,
                        deps,
                        finalizer,
                    ),
                )
            },
        );
    }
}

impl Type for Tuple {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(self.clone_as_type())
    }

    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_tuple_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectTuple>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectBind>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn crate::r#gen::AspectArtifact>));
        }
        if *key == aspect_lift_aspect_id() {
            let element_lifts: Vec<Box<dyn AspectLift>> = self.elements.iter().map(|e| {
                let t = match e {
                    TupleElement::Dynamic(t) => t.clone(),
                    TupleElement::EvalRes(_) => panic!("lift_js on non-type tuple"),
                };
                let a =
                    t
                        .aspect(xx, &aspect_lift_aspect_id())
                        .expect(&format!("Tuple element type ({}) missing AspectLift", t.desc()));
                must_downcast_box::<Box<dyn AspectLift>>(a)
            }).collect();
            return Some(Box::new(Box::new(TupleLiftJs { element_lifts }) as Box<dyn AspectLift>));
        }
        if *key == aspect_post_deserialize_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectPostDeserialize>));
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        let Some(source_tuple) = aspect_tuple_get_maybe(xx, source_type) else {
            return false;
        };
        source_tuple.element_types().len() == self.elements.len()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let code = cv.code;
        let tuple: Tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(cv.value)).ok().expect("Tuple cast_from: Rc not unique");
        return er_seq_cv!(loc, erb, {
            let mut converted_elements: Vec<TupleElement> = Vec::new();
            for (i, elem) in tuple.elements.into_iter().enumerate() {
                let dest_elem_type = match self.elements.get(i) {
                    Some(TupleElement::Dynamic(t)) => t.clone(),
                    Some(TupleElement::EvalRes(er)) => {
                        match &er.v {
                            Some(cv) => erb.must_er(cv.value.type_(xx, loc))?,
                            None => return erb.err_new(format!("Tuple cast_from: dest element {} missing", i)),
                        }
                    },
                    None => return erb.err_new(format!("Tuple cast_from: dest has no element {}", i)),
                };
                let converted = match elem {
                    TupleElement::EvalRes(elem_res) => {
                        if let Some(elem_cv) = elem_res.v {
                            let conv = erb.must_er(typecheck_convert(xx, loc, dest_elem_type.as_ref(), elem_cv),)?;
                            TupleElement::EvalRes(EvalRes::ok(conv))
                        } else {
                            TupleElement::EvalRes(EvalRes {
                                v: None,
                                errors: elem_res.errors,
                            })
                        }
                    },
                    TupleElement::Dynamic(_) => TupleElement::Dynamic(dest_elem_type),
                };
                converted_elements.push(converted);
            }
            return Ok(CodeValue {
                code: code,
                value: Rc::new(Tuple { elements: converted_elements, }),
            });
        });
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        let cv_value: Rc<dyn Value> = if (cv.value.as_ref() as &dyn std::any::Any).is::<ValueEmptyConst>() {
            Rc::new(self.clone_as_type())
        } else {
            cv.value
        };
        let tuple: Tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(cv_value)).ok().expect("Tuple cast_to: Rc not unique");
        return er_seq_cv_inline(loc, |erb| {
            let any_type: Rc<dyn Type> = Rc::new(TypeAnyDynamic);
            let mut converted_elements = Vec::new();
            let mut converted_type = self.clone_as_type();
            for (i, elem) in tuple.elements.into_iter().enumerate() {
                let converted_cv = match elem {
                    TupleElement::Dynamic(t) => {
                        let id = BindingId::Local(xx.vx.next_local_id.next());
                        xx.vx.externals.borrow_mut().push((id, External::Type(t)));
                        CodeValue {
                            code: Code {
                                loc,
                                code: Code_::Dyn(Rc::new(CodeDynamic {
                                    loc,
                                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
                                })),
                            },
                            value: any_type.clone().as_value(),
                        }
                    },
                    TupleElement::EvalRes(er) => {
                        let entry_cv = erb.must_er(er)?;
                        erb.must_er(typecheck_convert(xx, loc, any_type.as_ref(), entry_cv))?
                    },
                };
                converted_type.elements[i] = TupleElement::Dynamic(any_type.clone());
                converted_elements.push(TupleElement::EvalRes(EvalRes::ok(converted_cv)));
            }
            let tuple_js = erb.must_er(converted_type.to_dynamic(xx, loc, CodeValue {
                code: cv.code,
                value: Rc::new(Tuple { elements: converted_elements }),
            }))?;
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::Call(CodeDynamicCall {
                            func: Box::new(CodeDynamic {
                                loc,
                                type_: CodeDynamicType::BindingUse(
                                    CodeDynamicBindingUse { id: TUPLE_TO_TYPE_FN_SCOPE_ID },
                                ),
                            }),
                            args: vec![tuple_js],
                        }),
                    })),
                },
                value: any_type.as_value(),
            });
        });
    }

    fn desc(&self) -> String {
        format!("tuple({})", self.elements.iter().map(|e| {
            match e {
                TupleElement::Dynamic(t) => t.desc(),
                TupleElement::EvalRes(_) => "?".to_string(),
            }
        }).collect::<Vec<_>>().join(", "))
    }
}

impl Value for Tuple {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, xx: &ExprContext, loc: QExprId) -> EResult<Rc<dyn Type>> {
        let elements = self.elements.iter().map(|e| {
            TupleElement::Dynamic(match e {
                TupleElement::Dynamic(t) => t.clone(),
                TupleElement::EvalRes(er) => match &er.v {
                    Some(cv) => cv.value.type_(xx, loc).v.expect("Tuple element type unavailable"),
                    None => Rc::new(Tuple { elements: vec![] }) as Rc<dyn Type>,
                },
            })
        }).collect();
        EResult::ok(Rc::new(Tuple { elements: elements }))
    }
}

enum TupleBindSubResult {
    Const,
    Target(TargetBindSub),
}

#[derive(Clone)]
pub(crate) enum TupleElement {
    Dynamic(Rc<dyn Type>),
    EvalRes(EvalRes),
}

struct TupleLiftJs {
    element_lifts: Vec<Box<dyn AspectLift>>,
}

impl Aspect for TupleLiftJs { }

impl AspectLift for TupleLiftJs {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let array = v8::Local::<v8::Array>::try_from(value).expect("Expected dynamic array for tuple lift");
        let mut elements = Vec::new();
        for (i, elem_lift) in self.element_lifts.iter().enumerate() {
            let elem_v8 = array.get_index(scope, i as u32).expect("Array index out of bounds in tuple lift");
            let lifted = elem_lift.lift(xx, scope, elem_v8)?;
            elements.push(TupleElement::EvalRes(EvalRes::ok(CodeValue {
                code: Code {
                    loc: (SourceId(0), ExprId(0)),
                    code: Code_::None,
                },
                value: lifted,
            })));
        }
        Ok(Rc::new(Tuple { elements: elements }))
    }
}

impl ExporterAny for crate::artifact::ExporterDataAny<Rc<std::cell::RefCell<Option<Tuple>>>> {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(self.0.borrow().as_ref().expect("Tuple holder not populated").clone()))
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        EResult::ok(Rc::new(self.0.borrow().as_ref().expect("Tuple holder not populated").clone()))
    }
}
