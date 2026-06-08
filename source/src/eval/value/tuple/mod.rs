pub mod scope_entry;

pub(crate) use scope_entry::{
    ScopeEntryStatic,
    ScopeEntryValue,
};
use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectLiftDynamic,
            AspectTuple,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_lift_dynamic_aspect_id,
            aspect_tuple_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicNewArray,
            CodeDynamicType,
        },
        eval::TargetBindSub,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntry,
            Type,
            Unique,
            Value,
            downcast_value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct Tuple {
    /// Ordered list of element entries.
    pub elements: Vec<TupleElement>,
    /// Optional mortar source — a JS array that elements can be accessed from at
    /// runtime.
    pub source: Option<super::dynamic_util::ValueDynamicSource>,
}

impl Tuple {
    fn bind_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        let tuple: Tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(cv.value).unwrap())
                .ok()
                .expect("Tuple bind_sub: Rc not unique");
        let is_sourceless =
            matches!(cv.code.code, Code_::None) && tuple.source.is_none() && tuple.elements.iter().all(|e| match e {
                TupleElement::EvalRes(er) => {
                    er.v.as_ref().map_or(true, |cv| matches!(cv.code.code, Code_::None))
                },
                TupleElement::Mortar(_) => false,
            });
        if is_sourceless {
            return EResult::ok(None);
        }
        let carry = cv.code;
        if tuple.source.is_some() {
            let tuple_type = Rc::new(self.clone_as_type()) as Rc<dyn Type>;
            return xx.target.code_to_bind_sub(loc, carry, tuple_type).map(|sub| Some(sub));
        }
        let mut errors = vec![];
        let mut bind_results: Vec<TupleBindSubResult> = Vec::new();
        let mut has_target = false;
        for elem in tuple.elements.into_iter() {
            match elem {
                TupleElement::Mortar(_) => {
                    errors.push(crate::langhelp::ExprErr {
                        loc: loc,
                        errors: vec![
                            crate::langhelp::ExprSubErr::Opaque(
                                format!("Tuple element is mortar-only, cannot bind directly"),
                            ),
                        ],
                    });
                },
                TupleElement::EvalRes(elem_res) => {
                    if let Some(errs) = elem_res.errors {
                        errors.extend(errs);
                    }
                    if let Some(elem_cv) = elem_res.v {
                        let saved_value = elem_cv.value.clone();
                        match xx.target.maybe_bind_sub(xx, loc, elem_cv).into_res() {
                            Ok(Some(sub)) => {
                                has_target = true;
                                bind_results.push(TupleBindSubResult::Target(sub));
                            },
                            Ok(None) => {
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
        if !has_target {
            return EResult::ok(None);
        }
        let mut target_elements = Vec::new();
        for result in bind_results {
            match result {
                TupleBindSubResult::Target(sub) => {
                    target_elements.push(sub);
                },
                TupleBindSubResult::Const(_cv) => {
                    errors.push(crate::langhelp::ExprErr {
                        loc: loc,
                        errors: vec![
                            crate::langhelp::ExprSubErr::Opaque(
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

    /// Clone this type-Tuple. Panics if not a type.
    pub fn clone_as_type(&self) -> Tuple {
        Tuple {
            source: None,
            elements: self.elements.iter().map(|e| {
                match e {
                    TupleElement::Mortar(t) => TupleElement::Mortar(t.clone()),
                    TupleElement::EvalRes(_) => panic!("clone_as_type on non-type tuple"),
                }
            }).collect(),
        }
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
                }, Box::new(ScopeEntryStatic { type_: tuple_type }) as Box<dyn ScopeEntry>))
            },
            Ok(Some(sub)) => {
                let (code, js_key) = xx.target.bind(xx, loc, sub);
                EResult::ok((code, Box::new(crate::eval::value::type_scope_entry::dynamic::ScopeEntryDynamicBinding {
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
            Rc::try_unwrap(downcast_value::<Tuple>(cv.value).unwrap())
                .ok()
                .expect("Tuple to_dynamic: Rc not unique");
        let types: Vec<Rc<dyn Type>> = self.elements.iter().map(|e| {
            match e {
                TupleElement::Mortar(t) => t.clone(),
                TupleElement::EvalRes(_) => panic!("to_dynamic on non-type tuple"),
            }
        }).collect();
        let mut errors = vec![];
        let mut js_elements = Vec::new();
        for (elem, elem_type) in tuple.elements.into_iter().zip(types.iter()) {
            let elem_res = match elem {
                TupleElement::EvalRes(er) => er,
                TupleElement::Mortar(_) => panic!("to_dynamic on mortar tuple element"),
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

impl AspectTuple for Tuple {
    fn element_types(&self) -> Vec<Rc<dyn Type>> {
        self.elements.iter().map(|e| {
            match e {
                TupleElement::Mortar(t) => t.clone(),
                TupleElement::EvalRes(_) => panic!("element_types on non-type tuple"),
            }
        }).collect()
    }

    fn take_elements(&self, value: Rc<dyn Value>) -> Vec<EvalRes> {
        let tuple: Tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(value).unwrap()).ok().expect("take_elements: Rc not unique");
        tuple.elements.into_iter().map(|e| {
            match e {
                TupleElement::EvalRes(er) => er,
                TupleElement::Mortar(_) => panic!("take_elements encountered Mortar element"),
            }
        }).collect()
    }
}

impl Type for Tuple {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(self.clone_as_type())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
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
        if *key == aspect_lift_dynamic_aspect_id() {
            let element_lifts: Vec<Box<dyn AspectLiftDynamic>> = self.elements.iter().map(|e| {
                let t = match e {
                    TupleElement::Mortar(t) => t.clone(),
                    TupleElement::EvalRes(_) => panic!("lift_js on non-type tuple"),
                };
                let a =
                    t
                        .aspect(_xx, &aspect_lift_dynamic_aspect_id())
                        .expect(&format!("Tuple element type ({}) missing AspectLiftDynamic", t.desc()));
                *a.downcast::<Box<dyn AspectLiftDynamic>>().unwrap()
            }).collect();
            return Some(Box::new(Box::new(TupleLiftJs { element_lifts }) as Box<dyn AspectLiftDynamic>));
        }
        return None;
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(_xx, &super::type_any::r#const::type_type_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tuple type"))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        let result_type = cv.value.type_(xx, loc);
        let tuple: Tuple =
            Rc::try_unwrap(downcast_value::<Tuple>(cv.value).expect("Tuple cast_to: expected Tuple value"))
                .ok()
                .expect("Tuple cast_to: Rc not unique");
        let needs_runtime = tuple.elements.iter().any(|e| match e {
            TupleElement::Mortar(_) => true,
            TupleElement::EvalRes(er) => er.v.as_ref().map_or(false, |cv| !matches!(cv.code.code, Code_::None)),
        });
        if needs_runtime {
            // Has mortar or JS-coded elements: build tuple with proper JS codes for all
            // elements, consume to JS, then wrap with __tuple_to_type().
            let mut converted_elements = Vec::new();
            let mut converted_type = self.clone_as_type();
            for (i, elem) in tuple.elements.into_iter().enumerate() {
                let converted = match elem {
                    TupleElement::Mortar(t) => TupleElement::Mortar(t),
                    TupleElement::EvalRes(er) => {
                        if let Some(entry_cv) = er.v {
                            if matches!(entry_cv.code.code, Code_::None) {
                                let elem_type =
                                    tuple_element_type(xx, loc, &TupleElement::EvalRes(EvalRes::ok(CodeValue {
                                        code: entry_cv.code.clone(),
                                        value: entry_cv.value,
                                    })));
                                let id = crate::langhelp::ScopeEntryId::Local(xx.vx.next_local_id.next());
                                xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(elem_type)));
                                converted_type.elements[i] =
                                    TupleElement::Mortar(
                                        Rc::new(super::type_any::r#const::TypeAnyConst) as Rc<dyn Type>,
                                    );
                                TupleElement::EvalRes(EvalRes::ok(CodeValue {
                                    code: Code {
                                        loc,
                                        code: Code_::Dyn(Rc::new(CodeDynamic {
                                            loc,
                                            type_: CodeDynamicType::BindingUse(
                                                crate::code_dynamic::CodeDynamicBindingUse { id },
                                            ),
                                        })),
                                    },
                                    value: Rc::new(super::type_any::r#const::TypeAnyConst),
                                }))
                            } else {
                                TupleElement::EvalRes(EvalRes::ok(entry_cv))
                            }
                        } else {
                            TupleElement::EvalRes(er)
                        }
                    },
                };
                converted_elements.push(converted);
            }
            let converted_tuple = Tuple {
                source: tuple.source,
                elements: converted_elements,
            };
            let converted_cv = CodeValue {
                code: cv.code,
                value: Rc::new(converted_tuple),
            };
            match converted_type.to_dynamic(xx, loc, converted_cv).into_res() {
                Ok(tuple_js) => {
                    let call_code = CodeDynamic {
                        loc,
                        type_: CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                            func: Box::new(CodeDynamic {
                                loc,
                                type_: CodeDynamicType::BindingUse(
                                    crate::code_dynamic::CodeDynamicBindingUse {
                                        id: crate::eval::TUPLE_TO_TYPE_FN_SCOPE_ID,
                                    },
                                ),
                            }),
                            args: vec![tuple_js],
                        }),
                    };
                    EvalRes::ok(CodeValue {
                        code: Code {
                            loc,
                            code: Code_::Dyn(Rc::new(call_code)),
                        },
                        value: result_type.as_value(),
                    })
                },
                Err(errors) => EvalRes {
                    v: None,
                    errors: Some(errors),
                },
            }
        } else {
            // No mortar data: all compile-time, store type as external.
            let id = crate::langhelp::ScopeEntryId::Local(xx.vx.next_local_id.next());
            xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(result_type.clone())));
            EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(crate::code_dynamic::CodeDynamicBindingUse { id }),
                    })),
                },
                value: result_type.as_value(),
            })
        }
    }

    fn desc(&self) -> String {
        format!("tuple({})", self.elements.iter().map(|e| {
            match e {
                TupleElement::Mortar(t) => t.desc(),
                TupleElement::EvalRes(_) => "?".to_string(),
            }
        }).collect::<Vec<_>>().join(", "))
    }
}

impl Value for Tuple {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        let elements = self.elements.iter().map(|e| {
            TupleElement::Mortar(tuple_element_type(xx, loc, e))
        }).collect();
        Rc::new(Tuple {
            source: None,
            elements,
        })
    }
}

fn tuple_element_type(_xx: &ExprContext, loc: QExprId, e: &TupleElement) -> Rc<dyn Type> {
    match e {
        TupleElement::Mortar(t) => t.clone(),
        TupleElement::EvalRes(er) => match &er.v {
            Some(cv) => cv.value.type_(_xx, loc),
            None => Rc::new(Tuple {
                source: None,
                elements: vec![],
            }) as Rc<dyn Type>,
        },
    }
}

enum TupleBindSubResult {
    Const(CodeValue),
    Target(TargetBindSub),
}

/// Represents a single element entry in a Tuple.
pub(crate) enum TupleElement {
    /// Element has been fully evaluated and is available immediately.
    EvalRes(EvalRes),
    /// Element available only via the mortar source. Type is known but value is
    /// deferred to runtime.
    Mortar(Rc<dyn Type>),
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
                    loc: (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)),
                    code: Code_::None,
                },
                value: lifted,
            })));
        }
        Ok(Rc::new(Tuple {
            source: None,
            elements,
        }))
    }
}

/// When a compound value (Tuple/Record) is the last expression in a block, the
/// block's code contains binding declarations but the compound value has
/// Code_::None. When consumed to JS, the generated JS references those bindings.
/// This helper wraps the consumed JS with the carry code so the bindings are in
/// scope.
pub(crate) fn wrap_with_carry(loc: QExprId, carry: Code, js: CodeDynamic) -> CodeDynamic {
    if let Some(Some(carry_js)) = carry.cast::<CodeDynamic>().v {
        let mut entries = match &carry_js.type_ {
            CodeDynamicType::Block(block) => block.entries.clone(),
            _ => vec![CodeDynamicBlockEntry::Code(carry_js.clone())],
        };
        entries.push(CodeDynamicBlockEntry::Code(js));
        return CodeDynamic {
            loc,
            type_: CodeDynamicType::Block(CodeDynamicBlock { entries }),
        };
    }
    js
}
