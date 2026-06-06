use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectLiftDynamic,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_lift_dynamic_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::{
            expr::{
                Expr,
                Expr_,
                StagedExprCell,
            },
            from_v8_ref,
            value::value_dynamic::bind_js_some,
        },
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            External,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        cell::RefCell,
        rc::Rc,
    },
};

impl Value for Expr {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }
}
pub(crate) struct TypeExpr;

fn expr_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for TypeExpr {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::value_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeExpr {
    fn desc(&self) -> String {
        return format!("expr");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeExpr)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == expr_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn AspectLiftDynamic>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        return EvalRes::err(loc, format!("Cannot convert expr to other type"));
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &expr_type_identity()).is_some() {
            let expr =
                (cv.value.as_ref() as &dyn Any)
                    .downcast_ref::<Expr>()
                    .expect("TypeExpr value must be an Expr")
                    .clone();
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(expr),
            });
        }

        // Wrap any non-Expr value as a literal Expr that reproduces the value
        let type_rc = cv.value.type_(xx, loc);
        let expr = Expr {
            loc,
            r#type: Expr_::Literal(Rc::new(move || {
                Rc::new(super::value_tuple::ValueStatic { type_: type_rc.clone() }) as Rc<dyn Value>
            })),
        };
        return EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(expr),
        });
    }
}

impl Aspect for TypeExpr { }

impl AspectConsumeDynamic for TypeExpr {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        // If there's already JS code (e.g. from a __stage call), use it directly
        if let Some(js) = cv.code.cast::<CodeDynamic>().v.flatten() {
            return EResult::ok(js.clone());
        }
        let cell = Rc::new(StagedExprCell { value: RefCell::new(Some(cv.value)) });
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, External::StagedExprCell(cell)));
        EResult::ok(CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
        })
    }
}

impl AspectBindDynamic for TypeExpr {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn super::value_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectLiftDynamic for TypeExpr {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let cell: Rc<StagedExprCell> = from_v8_ref::<Rc<StagedExprCell>>(&value).clone();
        Ok(cell.value.borrow_mut().take().expect("Expr value already consumed during lift"))
    }
}

/// Placeholder value for stage results. Its type exposes AspectLiftDynamic (to
/// extract the Expr from the v8::External returned by __stage) but NOT
/// AspectConsumeDynamic (so eval() preserves the Call CodeDynamic).
pub(crate) struct ValueStagedExprPlaceholder;
struct TypeStagedExprPlaceholder;

impl Value for ValueStagedExprPlaceholder {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeStagedExprPlaceholder)
    }
}

impl Value for TypeStagedExprPlaceholder {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::value_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeStagedExprPlaceholder {
    fn desc(&self) -> String {
        "stage result".to_string()
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeStagedExprPlaceholder)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeStagedExprPlaceholder) as Box<dyn AspectLiftDynamic>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert stage result".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to stage result".to_string())
    }
}

impl Aspect for TypeStagedExprPlaceholder { }

impl AspectLiftDynamic for TypeStagedExprPlaceholder {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let cell: Rc<StagedExprCell> = from_v8_ref::<Rc<StagedExprCell>>(&value).clone();
        Ok(cell.value.borrow_mut().take().expect("Stage result already consumed during lift"))
    }
}
