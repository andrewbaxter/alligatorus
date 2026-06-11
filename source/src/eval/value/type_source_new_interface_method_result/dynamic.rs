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
            CodeDynamicFieldUse,
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
                throw_error,
            },
            dynamic_wrap::{
                AnyType,
                DynamicWrap,
                dynamic_wrap_method_name,
            },
            value::{
                type_any::r#const::TypeAnyConst,
                type_function::dynamic::TypeFunctionDynamic,
                type_source_interface::SourceInterfaceRef,
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
            ScopeEntry,
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

dynamic_wrap!(struct SourceNewInterfaceMethodResult(self_, scope) {
    data {
        interface_ref: Rc < SourceInterfaceRef >,
        method_index: usize
    }
    fn arg_(type_ref: AnyType) -> () {
        let methods = self_.interface_ref.data.methods.borrow();
        let method = &methods[self_.method_index];
        let mut arg_type = method.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            drop(methods);
            throw_error(scope, "Interface method already has an arg type");
            return;
        }
        *arg_type = Some(type_ref.0);
    }
    fn ret_(type_ref: AnyType) -> () {
        let methods = self_.interface_ref.data.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type = method.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            drop(methods);
            throw_error(scope, "Interface method already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});
