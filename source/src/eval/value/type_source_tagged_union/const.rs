use {
    crate::{
        code_dynamic::CodeDynamic,
        eval::{
            TargetBindSub,
            artifact::Artifact,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
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

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Artifact {
        super::dynamic::TypeSourceTaggedUnionRefDynamic::build_type_artifact()
    }
}

impl Value for TypeSourceTaggedUnionConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
