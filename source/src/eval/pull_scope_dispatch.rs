use {
    crate::{
        eval::{
            TargetBindSub,
            TargetDynamic,
            expr::Expr,
            push::Push,
        },
        langhelp::{
            BindingId,
            Code,
            CodeValue,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
        },
        r#gen::{
            Binding,
            IM_BINDING_USE_FIELD,
            Target,
        },
    },
    std::rc::Rc,
};

pub(crate) struct PullBinding {
    pub instance: v8::Global<v8::Object>,
}

impl Binding for PullBinding {
    fn use_(&self, xx: &ExprContext, _loc: QExprId, _code: Code) -> CodeValue {
        let scope_ptr = xx.vx.dynamic_scope.get().expect("PullBinding::use_: no active dynamic scope");
        let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
            &mut *(scope_ptr.as_ptr() as *mut _)
        };
        let instance = v8::Local::new(scope, &self.instance);
        let method_key =
            v8::String::new(
                scope,
                &ConstKey::String(IM_BINDING_USE_FIELD.to_string()).to_str(),
            ).expect("PullBinding::use_: alloc _im_se0 key");
        let method_val =
            instance.get(scope, method_key.into()).expect("PullBinding::use_: instance missing _im_se0");
        let method_fn =
            v8::Local::<v8::Function>::try_from(method_val).expect("PullBinding::use_: _im_se0 not a function");
        let recv: v8::Local<v8::Value> = instance.into();
        let null_arg: v8::Local<v8::Value> = v8::null(scope).into();
        let result =
            method_fn.call(scope, recv, &[null_arg]).expect("PullBinding::use_: _im_se0 returned no value");
        <CodeValue as Push>::from_dynamic(scope, result)
    }
}

pub(crate) struct PullTarget {
    pub instance: v8::Global<v8::Object>,
}

impl Target for PullTarget {
    fn bind(&self, xx: &ExprContext, loc: QExprId, sub: TargetBindSub) -> (Code, BindingId) {
        let _ = &self.instance;
        TargetDynamic.bind(xx, loc, sub)
    }

    fn bind_sub_record(&self, loc: QExprId, carry: Code, fields: Vec<(ConstKey, TargetBindSub)>) -> TargetBindSub {
        TargetDynamic.bind_sub_record(loc, carry, fields)
    }

    fn bind_sub_tuple(&self, loc: QExprId, carry: Code, elements: Vec<TargetBindSub>) -> TargetBindSub {
        TargetDynamic.bind_sub_tuple(loc, carry, elements)
    }

    fn code_to_bind_sub(&self, loc: QExprId, code: Code, type_: Rc<dyn Type>) -> EResult<TargetBindSub> {
        TargetDynamic.code_to_bind_sub(loc, code, type_)
    }

    fn if_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        conditions: Vec<(CodeValue, &Expr)>,
        else_: Option<&Expr>,
    ) -> EvalRes {
        TargetDynamic.if_(xx, loc, conditions, else_)
    }

    fn match_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ConstKey,
        cases: Vec<(String, Rc<dyn Type>, &Expr)>,
        fallback: Option<&Expr>,
    ) -> EvalRes {
        TargetDynamic.match_(xx, loc, base, element_key, cases, fallback)
    }

    fn maybe_bind_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        TargetDynamic.maybe_bind_sub(xx, loc, cv)
    }

    fn seq(&self, loc: QExprId, entries: Vec<Code>) -> EResult<Code> {
        TargetDynamic.seq(loc, entries)
    }

    fn stage(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        tree: Expr,
        lower_values: Vec<CodeValue>,
        graft_values: Vec<CodeValue>,
    ) -> EvalRes {
        TargetDynamic.stage(xx, loc, tree, lower_values, graft_values)
    }

    fn cast_exception(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        call_code: Code,
        catch_binding: BindingId,
        cast_code: Code,
    ) -> EResult<Code> {
        TargetDynamic.cast_exception(xx, loc, call_code, catch_binding, cast_code)
    }
}
