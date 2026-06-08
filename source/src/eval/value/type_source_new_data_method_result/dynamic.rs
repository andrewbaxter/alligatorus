use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicType,
        },
        eval::{
            Erb,
            dynamic_wrap::{
                AnyType,
                DynamicWrap,
                StagedExpr,
                dynamic_wrap_method_name,
            },
            value::{
                dynamic_util::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_source_data_type::SourceDataTypeRef,
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

dynamic_wrap!(struct SourceNewDataMethodResult(self_, scope) {
    data {
        data_type_ref: Rc < SourceDataTypeRef >,
        method_index: usize
    }
    fn impl_(staged: StagedExpr) -> () {
        let methods = self_.data_type_ref.data.methods.borrow();
        let method = &methods[self_.method_index];
        let mut impl_expr = method.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            drop(methods);
            crate::eval::dynamic_util::throw_error(scope, "Method already has an implementation");
            return;
        }
        *impl_expr = Some(staged.0);
    }
    fn ret_(type_ref: AnyType) -> () {
        let methods = self_.data_type_ref.data.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type = method.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            drop(methods);
            crate::eval::dynamic_util::throw_error(scope, "Method already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});
