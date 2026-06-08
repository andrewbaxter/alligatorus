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
            definition_set::next_interface_method_id,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            dynamic_wrap::{
                DynamicWrap,
                dynamic_wrap_method_name,
            },
            value::{
                type_any::r#const::TypeAnyConst,
                type_function::dynamic::TypeFunctionDynamic,
                type_source_interface::{
                    SourceInterfaceMethodData,
                    SourceInterfaceRef,
                },
                type_source_new_interface_method_result::dynamic::SourceNewInterfaceMethodResult,
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
        cell::RefCell,
        mem,
        rc::Rc,
    },
};

dynamic_wrap!(struct SourceNewInterfaceResult(self_, scope) {
    data {
        interface_ref: Rc < SourceInterfaceRef >
    }
    field type_: Rc < SourceInterfaceRef > {
        self_.interface_ref.clone()
    }
    fn set_description(description: String) -> () {
        *self_.interface_ref.data.description.borrow_mut() = Some(description);
    }
    fn new_func(name: String) -> SourceNewInterfaceMethodResult {
        let iface_data = &self_.interface_ref.data;
        let field_name = format!("_im{}", next_interface_method_id(&iface_data.set));
        let method_index = {
            let mut methods = iface_data.methods.borrow_mut();
            let idx = methods.len();
            methods.push(SourceInterfaceMethodData {
                name,
                arg_type: RefCell::new(None),
                ret_type: RefCell::new(None),
                field_name,
            });
            idx
        };
        SourceNewInterfaceMethodResult {
            interface_ref: self_.interface_ref.clone(),
            method_index,
        }
    }
});
