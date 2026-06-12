//! Dispatch for types that are *user-defined* via the builtin `type`
//! interface. When a source-defined data type implements the `type` interface,
//! its instances act as types; this module runs the user-provided
//! `can_cast_from`, `cast_from`, and `desc` impls to drive `Type` methods.
//!
//! The impls are stored as raw `Expr` ASTs (the inner of the user-passed
//! `stage(...)`). They reference `arg`, which we bind to a record
//! `{xx, source_type, cv}` before `const_eval`.

use {
    crate::{
        eval::{
            expr::Expr,
            value::{
                record::{
                    Record,
                    RecordField,
                },
                type_source_data_type::SourceDataTypeData,
                type_source_interface::BuiltinInterfaceKind,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EvalRes,
            ExprContext,
            QExprId,
            Scope,
            ScopeEntry,
            Type,
            Value,
        },
    },
    dumpster::unsync::Gc,
    std::{
        cell::RefCell,
        collections::HashMap,
        rc::Rc,
    },
};

/// Lookup the user's impl Expr for a specific `type`-interface method by name.
fn lookup_impl<'a>(
    data: &'a Gc<SourceDataTypeData>,
    method_name: &str,
) -> Option<Rc<dyn Value>> {
    let impls = data.interface_impls.borrow();
    for impl_data in impls.iter() {
        if impl_data.interface.builtin_kind != Some(BuiltinInterfaceKind::Type) {
            continue;
        }
        let methods = impl_data.interface.methods.borrow();
        for method_impl in &impl_data.method_impls {
            if methods[method_impl.interface_method_index].name == method_name {
                return method_impl.impl_expr.borrow().clone();
            }
        }
    }
    None
}

/// Build a child `ExprContext` with `arg` bound to a record
/// `{xx, source_type, cv}` and `const_eval` the impl expression. Returns the
/// result of the impl.
fn run_impl(
    impl_expr: Rc<dyn Value>,
    xx: &ExprContext,
    loc: QExprId,
    source_type: Rc<dyn Type>,
    cv: CodeValue,
) -> EvalRes {
    // `arg` is bound to a Record built fresh on each scope_use so that
    // baseless-record field access (which requires `Rc::try_unwrap` to move
    // fields out) sees a unique `Rc`.
    let arg_factory = ArgRecordFactory {
        loc,
        source_type,
        cv,
    };
    let child_scope_values: HashMap<ConstKey, Box<dyn ScopeEntry>> = HashMap::new();
    let child_scope = Rc::new(Scope {
        values: RefCell::new(child_scope_values),
        parent: Some(xx.scope.clone()),
    });
    child_scope.values.borrow_mut().insert(
        ConstKey::String("arg".to_string()),
        Box::new(arg_factory) as Box<dyn ScopeEntry>,
    );
    let child_xx = Rc::new(ExprContext {
        vx: xx.vx.clone(),
        import_state: xx.import_state.clone(),
        scope: child_scope,
        target: xx.target.clone(),
    });
    let expr_ast = must_downcast_ref::<Expr>(impl_expr.as_ref());
    crate::eval::const_eval(&child_xx, expr_ast)
}

/// Scope entry that yields a fresh `Record` on each `use_` so that baseless
/// record field access (which requires `Rc::try_unwrap`) always sees a unique
/// `Rc`. `cv` is cloned on each use (Code is `Clone` and value is `Rc<dyn
/// Value>`).
pub(crate) struct ArgRecordFactory {
    pub(crate) loc: QExprId,
    pub(crate) source_type: Rc<dyn Type>,
    pub(crate) cv: CodeValue,
}

fn clone_cv(cv: &CodeValue) -> CodeValue {
    CodeValue {
        code: cv.code.clone(),
        value: cv.value.clone(),
    }
}

impl ScopeEntry for ArgRecordFactory {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        let mut fields = std::collections::BTreeMap::new();
        // `source_type` is wrapped in `ValueTypeRef` so that `.aspect(...)`
        // field access on it is dispatched to the inner Rust `Type::aspect`.
        fields.insert(
            ConstKey::String("source_type".to_string()),
            RecordField::EvalRes(crate::langhelp::EvalRes::ok(CodeValue {
                code: Code { loc: self.loc, code: Code_::None },
                value: Rc::new(ValueTypeRef { type_: self.source_type.clone() }),
            })),
        );
        fields.insert(
            ConstKey::String("cv".to_string()),
            RecordField::EvalRes(crate::langhelp::EvalRes::ok(clone_cv(&self.cv))),
        );
        fields.insert(
            ConstKey::String("xx".to_string()),
            RecordField::EvalRes(crate::langhelp::EvalRes::ok(CodeValue {
                code: Code { loc: self.loc, code: Code_::None },
                value: Rc::new(crate::eval::value::value_null::r#const::ValueNullConst),
            })),
        );
        CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(Record { base: None, fields }),
        }
    }
}

pub(crate) fn user_defined_can_cast_from(
    data: &Gc<SourceDataTypeData>,
    xx: &ExprContext,
    loc: QExprId,
    source_type: &dyn Type,
) -> bool {
    let Some(impl_expr) = lookup_impl(data, "can_cast_from") else {
        return false;
    };
    let Some(source_type_rc) = source_type.clone_rc() else {
        return false;
    };
    let placeholder_cv = CodeValue {
        code: Code { loc, code: Code_::None },
        value: Rc::new(crate::eval::value::value_null::r#const::ValueNullConst),
    };
    let res = run_impl(impl_expr, xx, loc, source_type_rc, placeholder_cv);
    let Some(out_cv) = res.v else {
        return false;
    };
    let v_type = out_cv.value.type_(xx, loc);
    if let Some(b) = crate::builtin::aspect_bool_const_get_maybe(xx, v_type.as_ref()) {
        return b.get(out_cv.value.as_ref());
    }
    false
}

pub(crate) fn user_defined_cast_from(
    data: &Gc<SourceDataTypeData>,
    xx: &ExprContext,
    loc: QExprId,
    cv: CodeValue,
) -> EvalRes {
    let Some(impl_expr) = lookup_impl(data, "cast_from") else {
        return EvalRes::err(loc, "User-defined type missing cast_from impl".to_string());
    };
    let source_type = cv.value.type_(xx, loc);
    run_impl(impl_expr, xx, loc, source_type, cv)
}

/// Find the JS field name for the named builtin-`type` interface method on
/// the bundled instance.
fn type_method_field_name(data: &Gc<SourceDataTypeData>, method_name: &str) -> Option<String> {
    let impls = data.interface_impls.borrow();
    for impl_data in impls.iter() {
        if impl_data.interface.builtin_kind
            != Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
        {
            continue;
        }
        let methods = impl_data.interface.methods.borrow();
        for method_impl in &impl_data.method_impls {
            if methods[method_impl.interface_method_index].name == method_name {
                return Some(methods[method_impl.interface_method_index].field_name.clone());
            }
        }
    }
    None
}

fn cast_from_field_name(data: &Gc<SourceDataTypeData>) -> Option<String> {
    type_method_field_name(data, "cast_from")
}

pub(crate) fn user_defined_can_cast_from_v8(
    data: &Gc<SourceDataTypeData>,
    bundle: &Rc<crate::eval::isolate_bundle::IsolateBundle>,
    xx: &ExprContext,
    loc: QExprId,
    source_type: &dyn Type,
) -> bool {
    let Some(field_name) = type_method_field_name(data, "can_cast_from") else {
        return false;
    };
    let Some(source_type_rc) = source_type.clone_rc() else {
        return false;
    };
    let dispatcher_ptr = match xx.vx.v8_dispatcher.get() {
        Some(p) => p,
        None => return false,
    };
    let dispatcher: &mut dyn crate::eval::V8CastDispatcher = unsafe {
        let mut p = dispatcher_ptr;
        p.as_mut()
    };
    match dispatcher.dispatch_can_cast_from(bundle, &field_name, xx, loc, source_type_rc) {
        Ok(b) => b,
        Err(_) => false,
    }
}

/// v8 dispatch for `TypeUserDefined.cast_from`. Synchronously invokes the
/// compiled JS method on the bundled instance via the active v8 scope
/// (threaded through `EvalContext.v8_dispatcher` as a `NonNull<dyn ..>`
/// trait-object pointer set up by `eval()` for the duration of
/// `const_eval`). The method returns an External-wrapped `CodeValue` built
/// by the user impl (via `eval_const`); the dispatcher unwraps and returns
/// it as-is, so the produced code flows into the caller's evaluation
/// without any lift through a fixed type.
pub(crate) fn user_defined_cast_from_v8(
    data: &Gc<SourceDataTypeData>,
    bundle: &Rc<crate::eval::isolate_bundle::IsolateBundle>,
    xx: &ExprContext,
    loc: QExprId,
    cv: CodeValue,
) -> EvalRes {
    let field_name = match cast_from_field_name(data) {
        Some(n) => n,
        None => return EvalRes::err(loc, "cast_from field name not found".to_string()),
    };
    let dispatcher_ptr = match xx.vx.v8_dispatcher.get() {
        Some(p) => p,
        None => {
            return EvalRes::err(
                loc,
                "v8 dispatch attempted with no active dispatcher — cast_from must be called from \
                 within const_eval"
                    .to_string(),
            );
        },
    };
    // SAFETY: pointer set by eval() to a stack-resident dispatcher impl
    // alive for the duration of const_eval. NonNull preserves the fat
    // pointer (data + vtable) end-to-end, so trait-method dispatch goes
    // through the right vtable.
    let dispatcher: &mut dyn crate::eval::V8CastDispatcher = unsafe {
        let mut p = dispatcher_ptr;
        p.as_mut()
    };
    match dispatcher.dispatch_cast_from(bundle, &field_name, xx, loc, cv) {
        Ok(out_cv) => EvalRes::ok(out_cv),
        Err(e) => EvalRes::err(loc, format!("v8 cast_from dispatch: {}", e)),
    }
}

pub(crate) fn user_defined_desc(data: &Gc<SourceDataTypeData>) -> String {
    data.name.clone().unwrap_or_else(|| "user-defined type".to_string())
}

/// A `Value` wrapping a `Rc<dyn Type>`, typed as `TypeTypeRef`. Supports
/// `.aspect(unique) -> Option` field access, delegating to `Type::aspect` on
/// the inner type.
pub(crate) struct ValueTypeRef {
    pub type_: Rc<dyn Type>,
}

impl crate::langhelp::Value for ValueTypeRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeTypeRef)
    }
}

pub(crate) struct TypeTypeRef;

impl crate::langhelp::Aspect for TypeTypeRef { }

impl crate::langhelp::Value for TypeTypeRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl crate::builtin::AspectField for TypeTypeRef {
    fn field(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_: CodeValue,
        key: CodeValue,
    ) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match crate::builtin::aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        let key_name = key_string.get(key.value.as_ref());
        if key_name == "aspect" {
            return EvalRes::ok(CodeValue {
                code: Code { loc, code: Code_::None },
                value: Rc::new(TypeAspectLookupFn { receiver: self_.value }),
            });
        }
        EvalRes::err(loc, format!("Field '{}' not found on type reference", key_name))
    }

    fn field_type_opt(
        &self,
        _xx: &ExprContext,
        key: &ConstKey,
    ) -> Option<Rc<dyn Type>> {
        let ConstKey::String(k) = key else {
            return None;
        };
        if k == "aspect" {
            return Some(Rc::new(crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
                arg: Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic),
                ret: Rc::new(crate::eval::value::type_option_aspect::TypeOption),
            }));
        }
        None
    }
}

impl Type for TypeTypeRef {
    fn as_value(&self) -> Rc<dyn crate::langhelp::Value> {
        Rc::new(TypeTypeRef)
    }

    fn aspect(&self, _xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == crate::builtin::aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeRef) as Box<dyn crate::builtin::AspectField>));
        }
        None
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to type reference".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from type reference".to_string())
    }

    fn desc(&self) -> String {
        "type_ref".to_string()
    }
}

/// The closure-like value produced by `type_ref.aspect`. When called with a
/// `Unique` argument, returns a `ValueOption` (Some/None) reflecting whether
/// the inner type has that aspect.
struct TypeAspectLookupFn {
    receiver: Rc<dyn crate::langhelp::Value>,
}

impl crate::langhelp::Value for TypeAspectLookupFn {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAspectLookupFnType)
    }
}

struct TypeAspectLookupFnType;

impl crate::langhelp::Aspect for TypeAspectLookupFnType { }

impl crate::langhelp::Value for TypeAspectLookupFnType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl crate::builtin::AspectFunction for TypeAspectLookupFnType {
    fn call(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_: CodeValue,
        argument: CodeValue,
    ) -> EvalRes {
        let lookup = crate::invariant::must_downcast_ref::<TypeAspectLookupFn>(self_.value.as_ref());
        let type_ref = crate::invariant::must_downcast_ref::<ValueTypeRef>(lookup.receiver.as_ref());
        let unique = crate::invariant::must_downcast_ref::<crate::langhelp::Unique>(argument.value.as_ref());
        let is_some = type_ref.type_.aspect(xx, unique).is_some();
        EvalRes::ok(CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(crate::eval::value::type_option_aspect::ValueOption { is_some }),
        })
    }
}

impl Type for TypeAspectLookupFnType {
    fn as_value(&self) -> Rc<dyn crate::langhelp::Value> {
        Rc::new(TypeAspectLookupFnType)
    }

    fn aspect(&self, _xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == crate::builtin::aspect_function_aspect_id() {
            return Some(
                Box::new(Box::new(TypeAspectLookupFnType) as Box<dyn crate::builtin::AspectFunction>),
            );
        }
        None
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to aspect lookup fn".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from aspect lookup fn".to_string())
    }

    fn desc(&self) -> String {
        "type.aspect".to_string()
    }
}

