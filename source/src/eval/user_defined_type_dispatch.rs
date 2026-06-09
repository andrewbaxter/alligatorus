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
                return Some(method_impl.impl_expr.clone());
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
struct ArgRecordFactory {
    loc: QExprId,
    source_type: Rc<dyn Type>,
    cv: CodeValue,
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
        fields.insert(
            ConstKey::String("source_type".to_string()),
            RecordField::EvalRes(crate::langhelp::EvalRes::ok(CodeValue {
                code: Code { loc: self.loc, code: Code_::None },
                value: self.source_type.as_value(),
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
    _xx: &ExprContext,
    _loc: QExprId,
    _source_type: &dyn Type,
) -> bool {
    // TODO: invoke the user's `can_cast_from` impl. Currently the impl body
    // depends on `aspect_string_dynamic` + `Option.is_some` + `.aspect`
    // field on Type values, none of which are wired yet — so we
    // optimistically accept all source types and let `cast_from` produce the
    // result (which will error at run time if the underlying op isn't
    // available). When those builtins are wired, swap in real dispatch.
    lookup_impl(data, "can_cast_from").is_some()
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

pub(crate) fn user_defined_desc(data: &Gc<SourceDataTypeData>) -> String {
    data.name.clone().unwrap_or_else(|| "user-defined type".to_string())
}

