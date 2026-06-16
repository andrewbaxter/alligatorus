use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::{
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_consume_to_dynamic,
            },
            expr::{
                Expr,
                Expr_,
                StagedExprCell,
            },
            from_v8_ref,
            value::{
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_empty::r#const::ValueEmptyConst,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
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
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_type_aspect_id,
            expr_type_identity,
        },
    },
    std::{
        any::Any,
        cell::RefCell,
        rc::Rc,
    },
};

pub(crate) struct DeserializerTypeExpr;

impl crate::builtin::ArtifactBuiltinDeserializer for DeserializerTypeExpr {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: crate::langhelp::QExprId,
        _data: &serde_json::Value,
    ) -> crate::langhelp::EResult<(Rc<dyn Type>, crate::langhelp::Code)> {
        crate::langhelp::EResult::ok((Rc::new(TypeExpr), crate::langhelp::Code {
            loc,
            code: crate::langhelp::Code_::None,
        }))
    }

    fn value_from_serial(
        &self,
        _xx: &ExprContext,
        loc: crate::langhelp::QExprId,
        _data: &serde_json::Value,
    ) -> crate::langhelp::EvalRes {
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::None,
            },
            value: Rc::new(TypeExpr) as Rc<dyn crate::langhelp::Value>,
        })
    }
}

pub(crate) fn register_expr_artifact(
    m:
        &mut std
        ::collections
        ::HashMap<crate::langhelp::ArtifactIdBuiltin, Rc<dyn crate::builtin::ArtifactBuiltinDeserializer>>,
) {
    m.insert(TypeExpr::artifact_id(), Rc::new(DeserializerTypeExpr));
}
pub(crate) struct TypeExpr;

impl Aspect for TypeExpr { }

impl AspectBind for TypeExpr {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeExpr {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeExpr {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
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

impl AspectInitialBind for TypeExpr {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeExpr),
        })
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

impl crate::r#gen::AspectLowerDynamic for TypeExpr {
    fn lower(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let cell: Rc<StagedExprCell> = from_v8_ref::<Rc<StagedExprCell>>(&value).clone();
        Ok(cell.value.borrow_mut().take().expect("Expr value already consumed during lower"))
    }
}

impl crate::r#gen::AspectType for TypeExpr {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }
}

impl Type for TypeExpr {
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
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn AspectBind>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn AspectLiftDynamic>));
        }
        if *key == crate::r#gen::aspect_lower_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn crate::r#gen::AspectLowerDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn crate::r#gen::AspectType>));
        }
        return None;
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        // Expressions are code, not type-of-types — no `cast_to(any)`.
        false
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &expr_type_identity()).is_some() {
            // If the value is itself an `Expr` (e.g. from a `stage(...)` literal), clone it
            // directly; otherwise the static type is already `expr` (e.g. via a dynamic
            // binding) and we pass through the CV unchanged.
            if let Some(expr) = (cv.value.as_ref() as &dyn std::any::Any).downcast_ref::<Expr>() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(expr.clone()),
                });
            }
            return EvalRes::ok(cv);
        }

        // Wrap any non-Expr value as a literal Expr that reproduces the value
        let type_rc = cv.value.type_(xx, loc);
        let expr = Expr {
            loc,
            r#type: Expr_::Literal(
                crate::eval::expr::make_lit_value(Rc::new(ValueEmptyConst { type_: type_rc }) as Rc<dyn Value>),
            ),
        };
        return EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(expr),
        });
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        return EvalRes::err(loc, format!("Cannot convert expr to other type"));
    }

    fn desc(&self) -> String {
        return format!("expr");
    }

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> crate::langhelp::Artifact {
        Self::build_type_artifact()
    }
}

impl Value for TypeExpr {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_any::r#const::TypeAnyConst)
    }
}
pub(crate) struct TypeExprStagedPlaceholder;

impl Aspect for TypeExprStagedPlaceholder { }

impl AspectConsumeDynamic for TypeExprStagedPlaceholder {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectLiftDynamic for TypeExprStagedPlaceholder {
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

impl crate::r#gen::AspectLowerDynamic for TypeExprStagedPlaceholder {
    fn lower(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let cell: Rc<StagedExprCell> = from_v8_ref::<Rc<StagedExprCell>>(&value).clone();
        Ok(cell.value.borrow_mut().take().expect("Stage result already consumed during lower"))
    }
}

impl Type for TypeExprStagedPlaceholder {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeExprStagedPlaceholder)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprStagedPlaceholder) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprStagedPlaceholder) as Box<dyn AspectLiftDynamic>));
        }
        if *key == crate::r#gen::aspect_lower_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeExprStagedPlaceholder) as Box<dyn crate::r#gen::AspectLowerDynamic>),
            );
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to stage result".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert stage result".to_string())
    }

    fn desc(&self) -> String {
        "stage result".to_string()
    }
}

impl Value for TypeExprStagedPlaceholder {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_any::r#const::TypeAnyConst)
    }
}

impl Value for Expr {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }
}
