//! v8-passable wrappers for `Rc<ExprContext>` and `CodeValue`. Used by the
//! v8-side `eval_const`: the active ExprContext is exposed to user-defined
//! `cast_from` impls as `arg.xx`, and `eval_const` returns the const-eval
//! result `CodeValue` wrapped in an External so it can travel back through
//! the v8 method's return value and be unwrapped by the v8 dispatcher.

use crate::r#gen::ScopeEntry;
use {
    crate::{
        r#gen::{AspectBind, AspectBindDynamic, AspectConsumeDynamic, AspectField, AspectStackValueDynamic, aspect_bind_aspect_id, aspect_bind_dynamic_aspect_id, aspect_consume_dynamic_aspect_id, aspect_field_aspect_id, aspect_stack_value_dynamic_aspect_id},
        code_dynamic::{
            CodeDynamic,
            CodeDynamicCall,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::{
            Erb,
            TargetBindSub,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
                mortar_consume_to_dynamic,
                mortar_dynamic_stack_to_value,
            },
            dynamic_wrap::DynamicWrap,
            from_v8_ref,
            to_v8,
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
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        cell::RefCell,
        mem,
        rc::Rc,
    },
};

// ---------- ExprContextRef ----------

use crate::r#gen::expr_context_ref_type_identity;

pub(crate) struct ExprContextRef(pub Rc<ExprContext>);

pub(crate) struct TypeExprContextRefDynamic;

impl DynamicWrap for ExprContextRef {
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        ExprContextRef(from_v8_ref::<Rc<ExprContext>>(&*value).clone())
    }

    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self.0).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeExprContextRefDynamic)
    }
}

impl Aspect for TypeExprContextRefDynamic { }

impl Value for TypeExprContextRefDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl AspectBind for TypeExprContextRefDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeExprContextRefDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeExprContextRefDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectStackValueDynamic for TypeExprContextRefDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeExprContextRefDynamic))
    }
}

impl Type for TypeExprContextRefDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeExprContextRefDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == expr_context_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprContextRefDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprContextRefDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeExprContextRefDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprContextRefDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &expr_context_ref_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &expr_context_ref_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeExprContextRefDynamic),
                }),
            });
        }
        EvalRes::err(loc, "Cannot convert to ExprContextRef".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from ExprContextRef".to_string())
    }

    fn desc(&self) -> String {
        "ExprContextRef".to_string()
    }
}

// ---------- CodeValueExtern ----------

use crate::r#gen::code_value_extern_type_identity;

pub(crate) struct CodeValueExtern(pub Rc<RefCell<Option<CodeValue>>>);

impl CodeValueExtern {
    /// Wrap a freshly-produced `CodeValue` in the opaque externalised form.
    pub fn wrap(cv: CodeValue) -> Self {
        CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
    }

    /// Take the contained `CodeValue` out, panicking if it's been
    /// extracted already.
    pub fn into_inner(self) -> CodeValue {
        self.0.borrow_mut().take().expect("CodeValueExtern: already taken")
    }
}

/// `DynamicWrap` for `CodeValue` itself — the v8 form of a `CodeValue`
/// return is the externalised `CodeValueExtern`. `from_v8` decodes the
/// CVE and takes its inner `CodeValue`; `into_v8` wraps a fresh `CodeValue`
/// as a CVE and emits it. This lets methods like `ScopeEntry::use_` keep
/// their original `-> CodeValue` signature while their source-side
/// `ret_type` is the CVE's mortar Type (`TypeCodeValueExternDynamic`),
/// derived automatically via `<CodeValue as DynamicWrap>::mortar_type()`.
impl DynamicWrap for CodeValue {
    fn from_v8<'s>(
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        let cve = <CodeValueExtern as DynamicWrap>::from_v8(scope, value);
        cve.into_inner()
    }

    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        CodeValueExtern::wrap(self).into_v8(scope)
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeCodeValueExternDynamic)
    }
}

pub(crate) struct TypeCodeValueExternDynamic;

impl DynamicWrap for CodeValueExtern {
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        let cell: Rc<RefCell<Option<CodeValue>>> =
            from_v8_ref::<Rc<RefCell<Option<CodeValue>>>>(&*value).clone();
        CodeValueExtern(cell)
    }

    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self.0).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeCodeValueExternDynamic)
    }
}

impl Aspect for TypeCodeValueExternDynamic { }

impl Value for TypeCodeValueExternDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl AspectBind for TypeCodeValueExternDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeValueExternDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeValueExternDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectStackValueDynamic for TypeCodeValueExternDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeCodeValueExternDynamic))
    }
}

impl AspectField for TypeCodeValueExternDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
            let key_type = key.value.type_(xx, loc);
            let aspect_string = erb.must_er(crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_str = aspect_string.get(key.value.as_ref());
            // A `CodeValueExtern` wraps a whole `CodeValue` (= `{code, value}`),
            // so the only two well-defined field accesses are `.code` and
            // `.value`. Anything else is a source-level mistake. Defaulting to
            // a generic v8 trampoline used to drive infinite recursion when the
            // wrapped value's source-language type re-entered the same user
            // `aspect_field` impl that emitted the access.
            if key_str == "code" {
                // Materialize the inner CodeValue's `code.code` (Code_ enum)
                // as a tagged-union JS object via `__cve_get_code_enum`. The
                // resulting source-level value is typed as
                // `TypeCodeStructDynamic` (whose only `.code` field is then
                // identity — see that file). Skips the abstract intermediate
                // `Code` struct representation: source-level
                // `cve.code` already produces the matchable Code_ shape.
                let call_code = CodeDynamic {
                    loc,
                    type_: CodeDynamicType::Call(CodeDynamicCall {
                        func: Box::new(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse {
                                id: crate::eval::CVE_GET_CODE_ENUM_FN_SCOPE_ID,
                            }),
                        }),
                        args: vec![self_code],
                    }),
                };
                return Ok(CodeValue {
                    code: Code { loc, code: Code_::Dyn(Rc::new(call_code)) },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(crate::eval::value::type_code_source::TypeCodeStructDynamic),
                    }),
                });
            }
            if key_str == "value" {
                // Emit `__cve_get_value(self_)`. Source typing is
                // `TypeValueDynamic` — an opaque value handle with no
                // `AspectField`. Any further `.<key>` on it is a compile-time
                // error; this is intentional. To do anything useful with the
                // returned value, callers go through an aspect interface
                // (e.g. a source-level `aspect_consume_nodejs`) — mirroring
                // how Rust uses `AspectConsumeDynamic` on a `dyn Value`.
                let call_code = CodeDynamic {
                    loc,
                    type_: CodeDynamicType::Call(CodeDynamicCall {
                        func: Box::new(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse {
                                id: crate::eval::CVE_GET_VALUE_FN_SCOPE_ID,
                            }),
                        }),
                        args: vec![self_code],
                    }),
                };
                return Ok(CodeValue {
                    code: Code { loc, code: Code_::Dyn(Rc::new(call_code)) },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(crate::eval::value::type_value::dynamic::TypeValueDynamic),
                    }),
                });
            }
            return erb.err_new(format!(
                "CodeValueExtern has no field '{}' (only 'code' and 'value')",
                key_str
            ));
        });
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(s) = key else {
            return None;
        };
        match s.as_str() {
            "code" => Some(Rc::new(crate::eval::value::type_code_source::TypeCodeStructDynamic)),
            "value" => Some(Rc::new(crate::eval::value::type_value::dynamic::TypeValueDynamic)),
            _ => None,
        }
    }
}


pub(crate) struct DeserializerCodeValueExtern;

impl crate::builtin::ArtifactBuiltinDeserializer for DeserializerCodeValueExtern {
    fn value_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EvalRes {
        EvalRes::err(loc, "CodeValueExtern values are not artifact-serializable".to_string())
    }

    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(TypeCodeValueExternDynamic), Code { loc, code: Code_::None }))
    }
}

pub(crate) fn register_artifact_deserializer(
    m: &mut std::collections::HashMap<
        crate::langhelp::ArtifactIdBuiltin,
        Rc<dyn crate::builtin::ArtifactBuiltinDeserializer>,
    >,
) {
    m.insert(TypeCodeValueExternDynamic::artifact_id(), Rc::new(DeserializerCodeValueExtern));
}

impl Type for TypeCodeValueExternDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeCodeValueExternDynamic)
    }

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Option<crate::langhelp::Artifact> {
        Some(Self::build_type_artifact())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == code_value_extern_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectField>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &code_value_extern_type_identity()).is_some()
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        // A `CodeValueExtern` can be unwrapped to any dest type whose owned
        // `Rc<dyn Type>` we can construct. `as_ T` emits a `__cve_extract`
        // call that returns the inner CV's raw JS value, and we re-type the
        // result as the requested `T`.
        type_.clone_rc().is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        let dest_type_rc = match type_.clone_rc() {
            Some(rc) => rc,
            None => return EvalRes::err(loc, format!("CodeValueExtern.cast_to: dest type ({}) doesn't support clone_rc", type_.desc())),
        };
        let self_code = match cv.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(loc, "CodeValueExtern.cast_to: receiver has no dynamic code".to_string()),
        };
        let extract_call = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse {
                        id: crate::eval::CVE_EXTRACT_FN_SCOPE_ID,
                    }),
                }),
                args: vec![self_code],
            }),
        };
        EvalRes::ok(CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(extract_call)) },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_type_rc,
            }),
        })
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &code_value_extern_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeCodeValueExternDynamic),
                }),
            });
        }
        EvalRes::err(loc, "Cannot convert to CodeValueExtern".to_string())
    }


    fn desc(&self) -> String {
        "CodeValueExtern".to_string()
    }
}
