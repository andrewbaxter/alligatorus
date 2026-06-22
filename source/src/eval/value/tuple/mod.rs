pub mod artifact;
pub mod scope_entry;

use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
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
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_data_type_constructor::dynamic::serialize_type_ref,
                value_empty::{
                    r#const::ValueEmptyConst,
                    scope_entry::ScopeEntryEmpty,
                },
            },
        },
        invariant::must_downcast_box,
        langhelp::{
            Aspect,
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
            ScopeEntryId,
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
            AspectLiftDynamic,
            AspectTuple,
            ScopeEntry,
            TUPLE_TO_TYPE_FN_SCOPE_ID,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
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
                        let saved_value = elem_cv.value.clone();
                        let saved_code = elem_cv.code.clone();
                        match xx.target.maybe_bind_sub(xx, loc, elem_cv).into_res() {
                            Ok(Some(sub)) => {
                                has_target = true;
                                bind_results.push(TupleBindSubResult::Target(sub));
                            },
                            Ok(None) => {
                                extra_codes.push(saved_code);
                                bind_results.push(TupleBindSubResult::Const(CodeValue {
                                    code: Code {
                                        loc: loc,
                                        code: Code_::None,
                                    },
                                    value: saved_value,
                                }));
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
                TupleBindSubResult::Const(_cv) => {
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
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
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
                }, Box::new(ScopeEntryEmpty { type_: tuple_type }) as Box<dyn ScopeEntry>))
            },
            Ok(Some(sub)) => {
                let (code, js_key) = xx.target.bind(xx, loc, sub);
                EResult::ok((code, Box::new(ScopeEntryDynamicBinding {
                    dynamic_key: js_key,
                    type_: tuple_type,
                }) as Box<dyn ScopeEntry>))
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
        let mut js_elements = Vec::new();
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
                        Ok(js) => js_elements.push(js),
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
            type_: CodeDynamicType::NewArray(CodeDynamicNewArray { elements: js_elements }),
        }))
    }
}

impl AspectInitialBind for Tuple {
    fn initial_bind(&self, _dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryEmpty { type_: Rc::new(self.clone_as_type()) as Rc<dyn Type> })
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

impl ExporterAny for Tuple {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
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
        if *key == aspect_lift_dynamic_aspect_id() {
            let element_lifts: Vec<Box<dyn AspectLiftDynamic>> = self.elements.iter().map(|e| {
                let t = match e {
                    TupleElement::Dynamic(t) => t.clone(),
                    TupleElement::EvalRes(_) => panic!("lift_js on non-type tuple"),
                };
                let a =
                    t
                        .aspect(xx, &aspect_lift_dynamic_aspect_id())
                        .expect(&format!("Tuple element type ({}) missing AspectLiftDynamic", t.desc()));
                must_downcast_box::<Box<dyn AspectLiftDynamic>>(a)
            }).collect();
            return Some(Box::new(Box::new(TupleLiftJs { element_lifts }) as Box<dyn AspectLiftDynamic>));
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
                            Some(cv) => cv.value.type_(xx, loc),
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
                        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
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
            let converted_tuple = Tuple { elements: converted_elements };
            let converted_cv = CodeValue {
                code: cv.code,
                value: Rc::new(converted_tuple),
            };
            let tuple_js = erb.must_er(converted_type.to_dynamic(xx, loc, converted_cv))?;
            let call_code = CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: TUPLE_TO_TYPE_FN_SCOPE_ID }),
                    }),
                    args: vec![tuple_js],
                }),
            };
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(call_code)),
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

    fn type_to_serial(&self, xx: &ExprContext, loc: QExprId) -> Artifact {
        let mut elements_serial: Vec<Artifact> = Vec::new();
        for elem in &self.elements {
            let elem_type = match elem {
                TupleElement::Dynamic(t) => t.clone(),
                TupleElement::EvalRes(er) => match &er.v {
                    Some(cv) => cv.value.type_(xx, loc),
                    None => panic!("Tuple::type_to_serial: element has no value"),
                },
            };
            let elem_artifact = serialize_type_ref(xx, loc, elem_type.as_ref());
            elements_serial.push(elem_artifact);
        }
        let payload = artifact::ArtifactTupleType { elements: elements_serial };
        artifact::TypeTupleArtifact::build_artifact(
            serde_json::to_value(&payload).expect("Failed to serialize tuple type"),
        )
    }
}

impl Value for Tuple {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, xx: &ExprContext, loc: QExprId) -> EResult<Rc<dyn Type>> {
        let elements = self.elements.iter().map(|e| {
            TupleElement::Dynamic(tuple_element_type(xx, loc, e))
        }).collect();
        EResult::ok(Rc::new(Tuple { elements: elements }))
    }
}

fn tuple_element_type(xx: &ExprContext, loc: QExprId, e: &TupleElement) -> Rc<dyn Type> {
    match e {
        TupleElement::Dynamic(t) => t.clone(),
        TupleElement::EvalRes(er) => match &er.v {
            Some(cv) => cv.value.type_(xx, loc),
            None => Rc::new(Tuple { elements: vec![] }) as Rc<dyn Type>,
        },
    }
}

enum TupleBindSubResult {
    Const(CodeValue),
    Target(TargetBindSub),
}

pub(crate) enum TupleElement {
    Dynamic(Rc<dyn Type>),
    EvalRes(EvalRes),
}

struct TupleLiftJs {
    element_lifts: Vec<Box<dyn AspectLiftDynamic>>,
}

impl Aspect for TupleLiftJs { }

impl AspectLiftDynamic for TupleLiftJs {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let array = v8::Local::<v8::Array>::try_from(value).expect("Expected JS array for tuple lift");
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
