//! `ScopeEntry` / `Target` adapters whose behavior is delegated to a
//! source-defined data type instance implementing the builtin `scope_entry` or
//! `target` interface. Used by `new_expr_context` to install the user's ntarget
//! instance and per-name scope entries into a child `ExprContext`.
//!
//! Each adapter holds a `v8::Global<v8::Object>` to the user's instance. The
//! current implementation is a minimal placeholder: `ScopeEntry::use_` produces an
//! opaque `any`-typed CodeValue, and `Target` delegates every method to
//! `TargetDynamic`. Wiring each method through the corresponding `_im_*` field on
//! the v8 instance is the next step.
use crate::r#gen::ScopeEntry;
use {
    crate::{
        eval::pull::Pull,
        langhelp::{
            Code,
            CodeValue,
            QExprId,
        },
    },
    std::rc::Rc,
};

/// `ScopeEntry` whose `use_` invokes the v8 instance's `_im_se0` method (the
/// compiled `scope_entry.use_` impl on the user's `nscope_entry`-like data type).
/// At this point the lookup landed and we know what to dispatch to; the remaining
/// work is teaching the resulting CodeValue's type how to route field/call
/// operations back through the user's interface impls so `console.log(...)`
/// accumulates the user's code AST. For now the produced value is opaque
/// `TypeAnyDynamic` — sufficient to confirm the scope walk finds the entry but not
/// yet to render the staged expression.
pub(crate) struct UserDefinedScopeEntry {
    pub instance: v8::Global<v8::Object>,
}

impl ScopeEntry for UserDefinedScopeEntry {
    fn use_(&self, xx: &crate::langhelp::ExprContext, _loc: QExprId, _code: Code) -> CodeValue {
        let scope_ptr = xx.vx.v8_scope.get().expect("UserDefinedScopeEntry::use_: no active v8 scope");
        let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
            &mut *(scope_ptr.as_ptr() as *mut _)
        };
        let instance = v8::Local::new(scope, &self.instance);
        let method_key =
            v8::String::new(
                scope,
                &crate::langhelp::ConstKey::String("_im_se0".to_string()).to_str(),
            ).expect("UserDefinedScopeEntry::use_: alloc _im_se0 key");
        let method_val =
            instance.get(scope, method_key.into()).expect("UserDefinedScopeEntry::use_: instance missing _im_se0");
        let method_fn =
            v8::Local::<v8::Function>::try_from(
                method_val,
            ).expect("UserDefinedScopeEntry::use_: _im_se0 not a function");
        let recv: v8::Local<v8::Value> = instance.into();
        let null_arg: v8::Local<v8::Value> = v8::null(scope).into();
        let result =
            method_fn
                .call(scope, recv, &[null_arg])
                .expect("UserDefinedScopeEntry::use_: _im_se0 returned no value");
        <CodeValue as Pull>::from_v8(scope, result)
    }
}

/// `Target` whose methods all dispatch to the v8 instance's `_im_tg{N}` method
/// fields. The current placeholder delegates each method to `TargetDynamic`; the
/// v8-side dispatch into the user impls will land here when wired up.
pub(crate) struct UserDefinedTarget {
    pub instance: v8::Global<v8::Object>,
}

impl crate::r#gen::Target for UserDefinedTarget {
    fn bind(
        &self,
        xx: &crate::langhelp::ExprContext,
        loc: QExprId,
        sub: crate::eval::TargetBindSub,
    ) -> (Code, crate::langhelp::ScopeEntryId) {
        let _ = &self.instance;
        crate::eval::TargetDynamic.bind(xx, loc, sub)
    }

    fn bind_sub_record(
        &self,
        loc: QExprId,
        carry: Code,
        fields: Vec<(crate::langhelp::ConstKey, crate::eval::TargetBindSub)>,
    ) -> crate::eval::TargetBindSub {
        crate::eval::TargetDynamic.bind_sub_record(loc, carry, fields)
    }

    fn bind_sub_tuple(
        &self,
        loc: QExprId,
        carry: Code,
        elements: Vec<crate::eval::TargetBindSub>,
    ) -> crate::eval::TargetBindSub {
        crate::eval::TargetDynamic.bind_sub_tuple(loc, carry, elements)
    }

    fn seq(&self, loc: QExprId, entries: Vec<Code>) -> crate::langhelp::EResult<Code> {
        crate::eval::TargetDynamic.seq(loc, entries)
    }

    fn code_to_bind_sub(
        &self,
        loc: QExprId,
        code: Code,
        type_: Rc<dyn crate::langhelp::Type>,
    ) -> crate::langhelp::EResult<crate::eval::TargetBindSub> {
        crate::eval::TargetDynamic.code_to_bind_sub(loc, code, type_)
    }

    fn if_(
        &self,
        xx: &crate::langhelp::ExprContext,
        loc: QExprId,
        conditions: Vec<(crate::langhelp::CodeValue, &crate::eval::expr::Expr)>,
        else_: Option<&crate::eval::expr::Expr>,
    ) -> crate::langhelp::EvalRes {
        crate::eval::TargetDynamic.if_(xx, loc, conditions, else_)
    }

    fn match_(
        &self,
        xx: &crate::langhelp::ExprContext,
        loc: QExprId,
        base: crate::langhelp::CodeValue,
        element_key: crate::langhelp::ConstKey,
        cases: Vec<(String, Rc<dyn crate::langhelp::Type>, &crate::eval::expr::Expr)>,
        fallback: Option<&crate::eval::expr::Expr>,
    ) -> crate::langhelp::EvalRes {
        crate::eval::TargetDynamic.match_(xx, loc, base, element_key, cases, fallback)
    }

    fn maybe_bind_sub(
        &self,
        xx: &crate::langhelp::ExprContext,
        loc: QExprId,
        cv: crate::langhelp::CodeValue,
    ) -> crate::langhelp::EResult<Option<crate::eval::TargetBindSub>> {
        crate::eval::TargetDynamic.maybe_bind_sub(xx, loc, cv)
    }

    fn stage(
        &self,
        xx: &crate::langhelp::ExprContext,
        loc: QExprId,
        tree: crate::eval::expr::Expr,
        lower_values: Vec<crate::langhelp::CodeValue>,
    ) -> crate::langhelp::EvalRes {
        crate::eval::TargetDynamic.stage(xx, loc, tree, lower_values)
    }
}
