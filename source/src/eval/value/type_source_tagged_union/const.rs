use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamicSub,
            AspectConsumeDynamic,
            AspectStackValueDynamic,
            AspectTaggedUnion,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_sub_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_tagged_union_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            aspect::ScopeEntryDynamicSub,
            value::type_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic,
                bind_dynamic_sub_default,
            },
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
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_dynamic(xx, loc, cv)
    }
}

impl AspectBindDynamicSub for TypeSourceTaggedUnionConst {
    fn bind_dynamic_sub(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(CodeDynamic, ScopeEntryDynamicSub)> {
        bind_dynamic_sub_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceTaggedUnionConst {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
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

impl Type for TypeSourceTaggedUnionConst {
    fn desc(&self) -> String {
        format!("tagged_union")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() })
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
        if *key == aspect_bind_dynamic_sub_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as
                        Box<dyn AspectBindDynamicSub>,
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
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union"))
    }
}

impl Value for TypeSourceTaggedUnionConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
