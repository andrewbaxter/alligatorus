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
                tuple::Tuple,
                type_any::r#const::TypeAnyConst,
                type_function::dynamic::TypeFunctionDynamic,
                type_source_data_type::{
                    SourceDataMethodData,
                    SourceDataTypeInterfaceImplData,
                    SourceDataTypeInterfaceMethodImplData,
                    SourceDataTypeRef,
                },
                type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
                type_source_interface::SourceInterfaceRef,
                type_source_new_data_method_result::dynamic::SourceNewDataMethodResult,
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
        cell::{
            Cell,
            RefCell,
        },
        mem,
        rc::Rc,
    },
};

dynamic_wrap!(struct SourceNewDataResult(self_, scope) {
    data {
        data_type_ref: Rc < SourceDataTypeRef >
    }
    field type_: Rc < SourceDataTypeRef > {
        self_.data_type_ref.clone()
    }
    field new_: Rc < TypeSourceDataTypeConstructorConst > {
        Rc::new(TypeSourceDataTypeConstructorConst { data: self_.data_type_ref.data.clone(), })
    }
    fn set_description(description: String) -> () {
        *self_.data_type_ref.data.description.borrow_mut() = Some(description);
    }
    fn set_underlying(type_ref: AnyType) -> () {
        self_.data_type_ref.data.underlying_type.set(type_ref.0);
    }
    fn new_func(name: String) -> SourceNewDataMethodResult {
        let dt_data = &self_.data_type_ref.data;
        let field_name = format!("_{}", dt_data.set.next_field_id.next());
        let entry_id = dt_data.set.next_entry_id.next();
        let method_index = {
            let mut methods = dt_data.methods.borrow_mut();
            let idx = methods.len();
            methods.push(SourceDataMethodData {
                name,
                arg_type: RefCell::new(None),
                impl_expr: RefCell::new(None),
                ret_type: RefCell::new(None),
                scope_entry_id: Cell::new(
                    Some(crate::langhelp::ScopeEntryId::Def(dt_data.set.import_id, dt_data.set.def_set_id, entry_id,))
                ),
                self_scope_entry_id: Cell::new(None),
                field_name,
                compiled_js: RefCell::new(None),
            });
            idx
        };
        SourceNewDataMethodResult {
            data_type_ref: self_.data_type_ref.clone(),
            method_index,
        }
    }
    fn impl_interface(
        iface_ref: Rc<SourceInterfaceRef>,
        record: std::collections::BTreeMap<crate::langhelp::ConstKey, crate::eval::expr::Expr>
    ) -> () {
        let record_entries = record;
        let iface_data = &iface_ref.data;
        let iface_method_names: Vec<(usize, String)> = {
            let methods = iface_data.methods.borrow();
            methods.iter().enumerate().map(|(i, m)| (i, m.name.clone())).collect()
        };
        let mut method_impls = Vec::new();
        let dt_data = &self_.data_type_ref.data;
        for (imethod_idx, imethod_name) in &iface_method_names {
            let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
            match record_entries.get(&const_key) {
                Some(expr) => {
                    let entry_id = dt_data.set.next_entry_id.next();
                    method_impls.push(SourceDataTypeInterfaceMethodImplData {
                        interface_method_index: *imethod_idx,
                        impl_expr: RefCell::new(Some(Rc::new(expr.clone()))),
                        compiled_js: RefCell::new(None),
                        scope_entry_id: Cell::new(
                            Some(
                                crate::langhelp::ScopeEntryId::Def(
                                    dt_data.set.import_id,
                                    dt_data.set.def_set_id,
                                    entry_id,
                                )
                            )
                        ),
                        self_scope_entry_id: Cell::new(None),
                    });
                },
                None => {
                    throw_error(scope, &format!("Missing implementation for interface method '{}'", imethod_name));
                    return;
                },
            }
        }
        dt_data.interface_impls.borrow_mut().push(SourceDataTypeInterfaceImplData {
            interface: iface_data.clone(),
            method_impls,
        });
    }
});
