//! Dispatch for types that are _user-defined_ via the builtin `type` interface.
//! When a source-defined data type implements the `type` interface, its instances
//! act as types; this module runs the user-provided `can_cast_from`, `cast_from`,
//! and `desc` impls to drive `Type` methods.
//!
//! The impls are stored as raw `Expr` ASTs (the inner of the user-passed
//! `stage(...)`). They reference `arg`, which we bind to a record
//! `{xx, source_type, cv}` before `const_eval`.
use crate::r#gen::ScopeEntry;
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

/// Scope entry that yields a fresh `Record` on each `use_` so that baseless record
/// field access (which requires `Rc::try_unwrap`) always sees a unique `Rc`. `cv`
/// is cloned on each use (Code is `Clone` and value is `Rc<dyn Value>`).
pub(crate) struct ArgRecordFactory {
    pub(crate) cv: CodeValue,
    pub(crate) loc: QExprId,
    pub(crate) source_type: Rc<dyn Type>,
}

impl ScopeEntry for ArgRecordFactory {
    fn use_(&self, _xx: &crate::langhelp::ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        let mut fields = std::collections::BTreeMap::new();

        // `source_type` is wrapped in `ValueTypeRef` so that `.aspect(...)` field access
        // on it is dispatched to the inner Rust `Type::aspect`.
        fields.insert(
            ConstKey::String("source_type".to_string()),
            RecordField::EvalRes(crate::langhelp::EvalRes::ok(CodeValue {
                code: Code {
                    loc: self.loc,
                    code: Code_::None,
                },
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
                code: Code {
                    loc: self.loc,
                    code: Code_::None,
                },
                value: Rc::new(crate::eval::value::value_null::r#const::ValueNullConst),
            })),
        );
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(Record {
                base: None,
                fields,
            }),
        }
    }
}

fn cast_from_field_name(data: &Gc<SourceDataTypeData>) -> Option<String> {
    type_method_field_name(data, "cast_from")
}

fn clone_cv(cv: &CodeValue) -> CodeValue {
    CodeValue {
        code: cv.code.clone(),
        value: cv.value.clone(),
    }
}

/// Lookup the user's impl Expr for a specific `type`-interface method by name.
fn lookup_impl<'a>(data: &'a Gc<SourceDataTypeData>, method_name: &str) -> Option<Rc<dyn Value>> {
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
    // baseless-record field access (which requires `Rc::try_unwrap` to move fields
    // out) sees a unique `Rc`.
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
    child_scope
        .values
        .borrow_mut()
        .insert(ConstKey::String("arg".to_string()), Box::new(arg_factory) as Box<dyn ScopeEntry>);
    let child_xx = Rc::new(ExprContext {
        vx: xx.vx.clone(),
        import_state: xx.import_state.clone(),
        scope: child_scope,
        target: xx.target.clone(),
    });
    let expr_ast = must_downcast_ref::<Expr>(impl_expr.as_ref());
    crate::eval::const_eval(&child_xx, expr_ast)
}

/// Find the JS field name for the named builtin-`type` interface method on the
/// bundled instance.
fn type_method_field_name(data: &Gc<SourceDataTypeData>, method_name: &str) -> Option<String> {
    let impls = data.interface_impls.borrow();
    for impl_data in impls.iter() {
        if impl_data.interface.builtin_kind !=
            Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type) {
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

/// The closure-like value produced by `type_ref.aspect`. When called with a
/// `Unique` argument, returns a `ValueOption` (Some/None) reflecting whether the
/// inner type has that aspect.
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

impl crate::r#gen::AspectFunction for TypeAspectLookupFnType {
    fn call(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes {
        let lookup = crate::invariant::must_downcast_ref::<TypeAspectLookupFn>(self_.value.as_ref());
        let type_ref = crate::invariant::must_downcast_ref::<ValueTypeRef>(lookup.receiver.as_ref());
        let unique = crate::invariant::must_downcast_ref::<crate::langhelp::Unique>(argument.value.as_ref());
        let is_some = type_ref.type_.aspect(xx, unique).is_some();
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(crate::eval::value::type_option_aspect::ValueOption { is_some }),
        })
    }
}

impl Type for TypeAspectLookupFnType {
    fn as_value(&self) -> Rc<dyn crate::langhelp::Value> {
        Rc::new(TypeAspectLookupFnType)
    }

    fn aspect(&self, _xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == crate::r#gen::aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeAspectLookupFnType) as Box<dyn crate::r#gen::AspectFunction>));
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
pub(crate) struct TypeTypeRef;

impl crate::langhelp::Aspect for TypeTypeRef { }

impl crate::langhelp::Value for TypeTypeRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl crate::r#gen::AspectField for TypeTypeRef {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        if key_name == "aspect" {
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Rc::new(TypeAspectLookupFn { receiver: self_.value }),
            });
        }
        EvalRes::err(loc, format!("Field '{}' not found on type reference", key_name))
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
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
        if *key == crate::r#gen::aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeRef) as Box<dyn crate::r#gen::AspectField>));
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

/// Invoke the user-defined `type.aspect` impl (compiled as `_im_t4`) on the
/// bundled v8 instance with `arg = {key}`. Decode the resulting `option(aspect)`
/// JS object — on `some`, extract the `Rc<dyn Aspect>` from the inner
/// External (the canonical v8 representation of `TypeAspectDynamic`-typed values,
/// produced by `__data_instance_to_aspect`); on `none`, return `None`. Returns
/// `None` if the user data type has no `aspect` impl (no `_im_t4` field).
pub(crate) fn pull_aspect_v8(
    data: &Gc<SourceDataTypeData>,
    bundle: &v8::Global<v8::Object>,
    xx: &ExprContext,
    _loc: QExprId,
    key: &crate::langhelp::Unique,
) -> Option<std::rc::Rc<dyn crate::langhelp::Aspect>> {
    let field_name = type_method_field_name(data, "aspect")?;
    let scope_ptr = xx.vx.v8_scope.get()?;

    // SAFETY: pointer set by eval() for its full duration. Only the v8 dispatch
    // paths and the value/type/scope_entry lift paths read this slot.
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    let arg_obj = v8::Object::new(scope);
    let key_push = crate::eval::to_v8(scope, key.clone());
    let key_str = v8::String::new(scope, &ConstKey::String("key".to_string()).to_str())?;
    arg_obj.set(scope, key_str.into(), key_push.into());
    let instance = v8::Local::new(scope, bundle);
    let method_key = v8::String::new(scope, &ConstKey::String(field_name).to_str())?;
    let method_val = instance.get(scope, method_key.into())?;
    let method_fn = v8::Local::<v8::Function>::try_from(method_val).ok()?;
    let recv: v8::Local<v8::Value> = instance.into();
    let arg_v: v8::Local<v8::Value> = arg_obj.into();
    let result = method_fn.call(scope, recv, &[arg_v])?;

    // The result is an `option(aspect)` JS object — `{some: ext}` or `{none: null}`
    // (see `TypeOptionVariantConstructorDynamic::call`).
    let result_obj = v8::Local::<v8::Object>::try_from(result).ok()?;
    let some_key = v8::String::new(scope, &ConstKey::String("some".to_string()).to_str())?;
    let some_val = result_obj.get(scope, some_key.into())?;
    if some_val.is_undefined() {
        return None;
    }

    // `some_val` is the v8::External produced by `__data_instance_to_aspect`,
    // which stores the canonical `Rc<dyn Aspect>` representation.
    let aspect: std::rc::Rc<dyn crate::langhelp::Aspect> =
        crate::eval::from_v8_ref::<std::rc::Rc<dyn crate::langhelp::Aspect>>(
            &*some_val,
        ).clone();
    Some(aspect)
}

pub(crate) fn pull_can_cast_from(
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
        code: Code {
            loc,
            code: Code_::None,
        },
        value: Rc::new(crate::eval::value::value_null::r#const::ValueNullConst),
    };
    let res = run_impl(impl_expr, xx, loc, source_type_rc, placeholder_cv);
    let Some(out_cv) = res.v else {
        return false;
    };
    let v_type = out_cv.value.type_(xx, loc);
    if let Some(b) = crate::r#gen::aspect_bool_const_get_maybe(xx, v_type.as_ref()) {
        return b.get(out_cv.value.as_ref());
    }
    false
}

pub(crate) fn pull_can_cast_from_v8(
    data: &Gc<SourceDataTypeData>,
    bundle: &v8::Global<v8::Object>,
    xx: &ExprContext,
    _loc: QExprId,
    source_type: &dyn Type,
) -> bool {
    let Some(field_name) = type_method_field_name(data, "can_cast_from") else {
        return false;
    };
    let Some(source_type_rc) = source_type.clone_rc() else {
        return false;
    };
    let Some(scope_ptr) = xx.vx.v8_scope.get() else {
        return false;
    };

    // SAFETY: pointer set by `eval()` for its full duration. Same pattern as
    // `pull_aspect_v8`; only the v8 dispatch paths read this slot, and method calls
    // consume the scope `&mut` until they return.
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    let arg_obj = v8::Object::new(scope);
    let xx_push = crate::eval::to_v8(scope, Rc::new(ExprContext {
        vx: xx.vx.clone(),
        import_state: xx.import_state.clone(),
        scope: xx.scope.clone(),
        target: xx.target.clone(),
    }));
    let Some(xx_key) = v8::String::new(scope, &ConstKey::String("xx".to_string()).to_str()) else {
        return false;
    };
    arg_obj.set(scope, xx_key.into(), xx_push.into());
    let Some(st_key) = v8::String::new(scope, &ConstKey::String("source_type".to_string()).to_str()) else {
        return false;
    };
    let source_type_v8 =
        crate::eval::push::Push::into_v8(crate::r#gen::TypeRefPush { type_ref: source_type_rc }, scope);
    arg_obj.set(scope, st_key.into(), source_type_v8);
    let Some(cv_key) = v8::String::new(scope, &ConstKey::String("cv".to_string()).to_str()) else {
        return false;
    };
    let null_for_cv: v8::Local<v8::Value> = v8::null(scope).into();
    arg_obj.set(scope, cv_key.into(), null_for_cv);
    let instance = v8::Local::new(scope, bundle);
    let Some(method_key) = v8::String::new(scope, &ConstKey::String(field_name).to_str()) else {
        return false;
    };
    let Some(method_val) = instance.get(scope, method_key.into()) else {
        return false;
    };
    let Ok(method_fn) = v8::Local::<v8::Function>::try_from(method_val) else {
        return false;
    };
    let recv: v8::Local<v8::Value> = instance.into();
    let arg_v: v8::Local<v8::Value> = arg_obj.into();
    let Some(result) = method_fn.call(scope, recv, &[arg_v]) else {
        return false;
    };
    result.boolean_value(scope)
}

pub(crate) fn pull_cast_from(
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

/// v8 dispatch for `TypePull.cast_from`. Synchronously invokes the compiled JS
/// method on the bundled instance via the active v8 scope (`EvalContext.v8_scope`,
/// installed by `eval()` for the duration of `const_eval`). The method returns an
/// External-wrapped `CodeValue` built by the user impl (via `eval_const`); we
/// unwrap and return it as-is, so the produced code flows into the caller's
/// evaluation without any lift through a fixed type.
pub(crate) fn pull_cast_from_v8(
    data: &Gc<SourceDataTypeData>,
    bundle: &v8::Global<v8::Object>,
    xx: &ExprContext,
    loc: QExprId,
    cv: CodeValue,
) -> EvalRes {
    let field_name = match cast_from_field_name(data) {
        Some(n) => n,
        None => return EvalRes::err(loc, "cast_from field name not found".to_string()),
    };
    let scope_ptr = match xx.vx.v8_scope.get() {
        Some(p) => p,
        None => {
            return EvalRes::err(loc, "v8 dispatch attempted with no active v8 scope — cast_from must be called from \
                 within const_eval".to_string());
        },
    };

    // SAFETY: pointer set by `eval()` for its full duration. While we hold this
    // `&mut`, no other code path is reading `v8_scope` (method calls consume the
    // scope `&mut` until they return). Lifetimes are erased on store/load but the
    // underlying scope outlives every read.
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };

    // Build the v8-side arg object: `xx` carries the caller's ExprContext as
    // External (for the user's body to pass to `eval_const`), `source_type` is a
    // `TypeRefPush` wrapping the source type, `cv` is a `CodeValueExtern` the impl
    // grafts into its staged expr at v8 time.
    let arg_obj = v8::Object::new(scope);
    let xx_push = crate::eval::to_v8(scope, Rc::new(ExprContext {
        vx: xx.vx.clone(),
        import_state: xx.import_state.clone(),
        scope: xx.scope.clone(),
        target: xx.target.clone(),
    }));
    let xx_key =
        match v8::String::new(scope, &ConstKey::String("xx".to_string()).to_str()) {
            Some(k) => k,
            None => return EvalRes::err(loc, "xx key alloc".to_string()),
        };
    arg_obj.set(scope, xx_key.into(), xx_push.into());
    let cv_key =
        match v8::String::new(scope, &ConstKey::String("cv".to_string()).to_str()) {
            Some(k) => k,
            None => return EvalRes::err(loc, "cv key alloc".to_string()),
        };
    let source_type = cv.value.type_(xx, loc);
    let cve_cell: Rc<RefCell<Option<CodeValue>>> = Rc::new(RefCell::new(Some(cv)));
    let cve_ext = crate::eval::to_v8(scope, cve_cell);
    arg_obj.set(scope, cv_key.into(), cve_ext.into());
    let st_key =
        match v8::String::new(scope, &ConstKey::String("source_type".to_string()).to_str()) {
            Some(k) => k,
            None => return EvalRes::err(loc, "source_type key alloc".to_string()),
        };
    let source_type_v8 =
        crate::eval::push::Push::into_v8(crate::r#gen::TypeRefPush { type_ref: source_type }, scope);
    arg_obj.set(scope, st_key.into(), source_type_v8);
    let instance = v8::Local::new(scope, bundle);
    let method_key =
        match v8::String::new(scope, &ConstKey::String(field_name).to_str()) {
            Some(k) => k,
            None => return EvalRes::err(loc, "method_field alloc".to_string()),
        };
    let method_val = match instance.get(scope, method_key.into()) {
        Some(v) => v,
        None => return EvalRes::err(loc, "instance missing cast_from field".to_string()),
    };
    let method_fn = match v8::Local::<v8::Function>::try_from(method_val) {
        Ok(f) => f,
        Err(_) => return EvalRes::err(loc, "instance cast_from field is not a function".to_string()),
    };
    let recv: v8::Local<v8::Value> = instance.into();
    let arg_v: v8::Local<v8::Value> = arg_obj.into();
    let result = {
        let tc = std::pin::pin!(v8::TryCatch::new(scope));
        let mut tc = tc.init();
        match method_fn.call(&mut tc, recv, &[arg_v]) {
            Some(r) => r,
            None => {
                let msg =
                    tc
                        .exception()
                        .map(|e| e.to_rust_string_lossy(&mut tc))
                        .unwrap_or_else(|| "<no exception>".to_string());
                return EvalRes::err(loc, format!("cast_from v8 call threw: {}", msg));
            },
        }
    };
    let cell: Rc<RefCell<Option<CodeValue>>> =
        crate::eval::from_v8_ref::<Rc<RefCell<Option<CodeValue>>>>(&*result).clone();
    let out_cv = match cell.borrow_mut().take() {
        Some(c) => c,
        None => return EvalRes::err(loc, "cast_from v8 returned consumed CodeValueExtern".to_string()),
    };
    EvalRes::ok(out_cv)
}

pub(crate) fn pull_desc(data: &Gc<SourceDataTypeData>) -> String {
    data.name.clone().unwrap_or_else(|| "user-defined type".to_string())
}

/// A `Value` wrapping a `Rc<dyn Type>`, typed as `TypeTypeRef`. Supports
/// `.aspect(unique) -> Option` field access, delegating to `Type::aspect` on the
/// inner type.
pub(crate) struct ValueTypeRef {
    pub type_: Rc<dyn Type>,
}

impl crate::langhelp::Value for ValueTypeRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeTypeRef)
    }
}
