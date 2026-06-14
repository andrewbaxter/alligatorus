use crate::r#gen::ScopeEntry;
use {
    crate::{
        r#gen::{AspectBind, AspectBindDynamic, AspectConsumeDynamic, AspectInitialBind, AspectStackValueDynamic, AspectTaggedUnion, aspect_bind_aspect_id, aspect_bind_dynamic_aspect_id, aspect_consume_dynamic_aspect_id, aspect_initial_bind_aspect_id, aspect_stack_value_dynamic_aspect_id, aspect_tagged_union_aspect_id},
        code_dynamic::CodeDynamic,
        eval::{
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
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
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::r#gen::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceTaggedUnionConst {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
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
    fn variants(&self) -> Vec<(crate::langhelp::ConstKey, String, Rc<dyn Type>)> {
        self.data.variants.borrow().iter().map(|v| (v.tag.clone(), v.js_tag.clone(), v.type_.clone())).collect()
    }
}

impl crate::r#gen::AspectType for TypeSourceTaggedUnionConst {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() })
    }
}

impl Type for TypeSourceTaggedUnionConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() }),
        })
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
        if *key == crate::r#gen::aspect_type_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as
                        Box<dyn crate::r#gen::AspectType>,
                ),
            );
        }
        return None;
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        // Cast from another value to this tagged union type — accept any source
        // whose runtime structure already matches (the JS object representation
        // is the same; we just rewrap the value with the more specific type).
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &self.data.identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(crate::eval::dynamic_util::ValueDynamic {
                    source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() }),
                }),
            });
        }
        // From AnyType (or other loose typing): trust the caller — the runtime
        // value is expected to be a JS object that matches this tagged union's
        // structure.
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(crate::eval::dynamic_util::ValueDynamic {
                source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() }),
            }),
        })
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some() {
            // The tagged-union ref's v8 form is the runtime ref carried by
            // `TypeSourceTaggedUnionRefDynamic`. Re-type the cv and delegate
            // — that type's `cast_to(any)` emits `__tagged_union_ref_to_type`
            // to wrap as `Rc<dyn Type>` at v8.
            let dyn_cv = CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(super::dynamic::TypeSourceTaggedUnionRefDynamic),
                }),
            };
            return super::dynamic::TypeSourceTaggedUnionRefDynamic.cast_to(xx, loc, dyn_cv, type_);
        }
        EvalRes::err(loc, format!("Cannot convert tagged_union"))
    }

    fn desc(&self) -> String {
        format!("tagged_union")
    }

    fn type_to_serial(
        &self,
        _xx: &ExprContext,
        _loc: QExprId,
    ) -> Option<crate::langhelp::Artifact> {
        // For now we serialize via the ref-form deserializer (loses the specific
        // tagged-union's variants/identity at round-trip; proper per-tagged-union
        // artifact would mirror the per-data-type artifact pattern).
        Some(super::dynamic::TypeSourceTaggedUnionRefDynamic::build_type_artifact())
    }
}

impl Value for TypeSourceTaggedUnionConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
