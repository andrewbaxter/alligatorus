use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_consume_to_dynamic,
            },
            expr::{
                Expr,
                Expr_,
                StagedExprCell,
                make_lit_value,
            },
            from_v8_ref,
            value::{
                type_any::r#const::TypeAnyConst,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_empty::r#const::ValueEmptyConst,
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
            External,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value_,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectType,
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

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactExpr;
#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactExprStagedPlaceholder;
pub(crate) struct TypeExpr;

impl Aspect for TypeExpr { }

impl AspectBind for TypeExpr {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeExpr {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeExpr {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        if let Some(js) = cv.code.cast::<CodeDynamic>().v.flatten() {
            return EResult::ok(js.clone());
        }
        let cell = Rc::new(StagedExprCell { value: RefCell::new(Some(cv.value.into_direct())) });
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
    ) -> Result<Rc<dyn Value_>, String> {
        let cell: Rc<StagedExprCell> = from_v8_ref::<Rc<StagedExprCell>>(&value).clone();
        Ok(cell.value.borrow_mut().take().expect("Expr value already consumed during lift"))
    }
}

impl AspectType for TypeExpr {
    fn type_(&self, _value: &dyn Value_) -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }
}

impl ExporterAny for TypeExpr {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeExpr));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        return EResult::ok(Rc::new(TypeExpr));
    }
}

impl ExporterRootDelegate for TypeExpr {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        EResult::ok(Rc::new(TypeExpr) as Rc<dyn ExporterAny>)
    }

    fn self_to_serial(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        EResult::ok(Artifact::Body(make_body(serde_json::Value::Null)))
    }
}

impl Type for TypeExpr {
    fn as_value(&self) -> Rc<dyn Value_> {
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
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeExpr) as Box<dyn AspectType>));
        }
        return None;
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &expr_type_identity()).is_some() {
            if let Some(expr) = (cv.value.as_ref() as &dyn std::any::Any).downcast_ref::<Expr>() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(expr.clone()).into(),
                });
            }
            return EvalRes::ok(cv);
        }
        let type_rc = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let expr = Expr {
            loc,
            r#type: Expr_::Literal(
                make_lit_value(Rc::new(ValueEmptyConst { type_: type_rc }) as Rc<dyn Value_>),
            ),
        };
        return EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(expr).into(),
        });
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        return EvalRes::err(loc, format!("Cannot convert expr to other type"));
    }

    fn desc(&self) -> String {
        return format!("expr");
    }
}

impl Value_ for TypeExpr {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeExpr);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::Expr))
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
    ) -> Result<Rc<dyn Value_>, String> {
        let cell: Rc<StagedExprCell> = from_v8_ref::<Rc<StagedExprCell>>(&value).clone();
        Ok(cell.value.borrow_mut().take().expect("Stage result already consumed during lift"))
    }
}

impl ExporterAny for TypeExprStagedPlaceholder {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeExprStagedPlaceholder));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        return EResult::ok(Rc::new(TypeExprStagedPlaceholder));
    }
}

impl Type for TypeExprStagedPlaceholder {
    fn as_value(&self) -> Rc<dyn Value_> {
        Rc::new(TypeExprStagedPlaceholder)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprStagedPlaceholder) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprStagedPlaceholder) as Box<dyn AspectLiftDynamic>));
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

impl Value_ for TypeExprStagedPlaceholder {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeExprStagedPlaceholder);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::ExprStagedPlaceholder))
    }
}

impl ExporterAny for Expr {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl Value_ for Expr {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeExpr))
    }
}
