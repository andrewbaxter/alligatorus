use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectStackValueDynamic,
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
                type_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_source_function::SourceFunction,
                type_source_interface::SourceInterface,
                type_source_interface_ref::SourceInterfaceRef,
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

dynamic_wrap!(struct SourceNewFuncResult(self_, scope) {
    data {
        func_ref: Rc < SourceFunction >
    }
    field func: Rc < SourceFunction > {
        self_.func_ref.clone()
    }
    fn set_description(description: String) -> () {
        *self_.func_ref.data.description.borrow_mut() = Some(description);
    }
    fn impl_(staged: StagedExpr) -> () {
        let mut impl_expr = self_.func_ref.data.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            crate::eval::dynamic_util::throw_error(scope, "Function already has an implementation");
            return;
        }
        *impl_expr = Some(staged.0);
    }
    fn arg_(type_ref: AnyType) -> () {
        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            crate::eval::dynamic_util::throw_error(scope, "Function already has an arg type");
            return;
        }
        *arg_type = Some(type_ref.0);
    }
    fn arg_type(iface_ref: Rc<SourceInterfaceRef>) -> () {
        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            crate::eval::dynamic_util::throw_error(scope, "Function already has an arg type");
            return;
        }
        *arg_type = Some(Rc::new(SourceInterface { data: iface_ref.data.clone(), }) as Rc<dyn crate::langhelp::Type>);
    }
    fn ret_(type_ref: AnyType) -> () {
        let mut ret_type = self_.func_ref.data.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            crate::eval::dynamic_util::throw_error(scope, "Function already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});
