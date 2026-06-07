use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectStackValueDynamic,
            AspectTaggedUnion,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_tagged_union_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::value::type_dynamic::{
            ValueDynamic,
            ValueDynamicSource,
            bind_dynamic_some,
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

impl Value for TypeSourceTaggedUnionConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
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
                Box::new(Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectTaggedUnion>),
            );
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectConsumeDynamic>),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectBindDynamic>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConst { data: self.data.clone() }) as Box<dyn AspectStackValueDynamic>),
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

impl Aspect for TypeSourceTaggedUnionConst { }

impl AspectTaggedUnion for TypeSourceTaggedUnionConst {
    fn variants(&self) -> Vec<(crate::langhelp::ConstKey, String, Rc<dyn Type>)> {
        self.data.variants.borrow().iter().map(|v| (v.tag.clone(), v.js_tag.clone(), v.type_.clone())).collect()
    }
}

impl AspectConsumeDynamic for TypeSourceTaggedUnionConst {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamic for TypeSourceTaggedUnionConst {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| bind_dynamic_some(loc, c, vtype))
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
