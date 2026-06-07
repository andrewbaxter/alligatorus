use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamicSub,
            AspectBoolDynamic,
            AspectConsumeDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_sub_aspect_id,
            aspect_bool_const_get_maybe,
            aspect_bool_dynamic_aspect_id,
            aspect_bool_dynamic_get_maybe,
            aspect_consume_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicType,
        },
        eval::{
            Erb,
            aspect::ScopeEntryDynamicSub,
            value::type_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic,
                bind_dynamic_sub_default,
                mortar_consume_to_dynamic,
                mortar_dynamic_stack_to_value,
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
            ScopeEntry,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

pub(crate) struct TypeBoolDynamic;

impl Aspect for TypeBoolDynamic { }

impl AspectBind for TypeBoolDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_dynamic(xx, loc, cv)
    }
}

impl AspectBindDynamicSub for TypeBoolDynamic {
    fn bind_dynamic_sub(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(CodeDynamic, ScopeEntryDynamicSub)> {
        bind_dynamic_sub_default(xx, loc, cv)
    }
}

impl AspectBoolDynamic for TypeBoolDynamic { }

impl AspectConsumeDynamic for TypeBoolDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectStackValueDynamic for TypeBoolDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeBoolDynamic))
    }
}

impl Type for TypeBoolDynamic {
    fn desc(&self) -> String {
        return format!("mortar bool");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeBoolDynamic)
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bool_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectBoolDynamic>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_sub_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectBindDynamicSub>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_bool_dynamic_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeBoolDynamic),
            }),
        })
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_bool_const_get_maybe(xx, source_type).is_some() ||
            aspect_bool_dynamic_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            if let Some(aspect_bool) = aspect_bool_const_get_maybe(xx, type_.as_ref()) {
                let CodeValue { code, value } = cv;
                let mut seq = vec![];
                if let Some(c) = erb.must_er(code.cast::<CodeDynamic>())? {
                    seq.push(c.clone());
                };
                let bool_val = aspect_bool.get(value.as_ref());
                seq.push(CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::Literal(serde_json::Value::Bool(bool_val))
                });
                return Ok(CodeValue {
                    code: Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(CodeDynamic {
                            loc: loc,
                            type_: CodeDynamicType::Block(
                                CodeDynamicBlock { entries: seq.into_iter().map(CodeDynamicBlockEntry::Code).collect() }
                            )
                        }))
                    },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeBoolDynamic),
                    }),
                });
            } else {
                return Ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeBoolDynamic),
                    })
                });
            }
        });
    }
}

impl Value for TypeBoolDynamic {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
