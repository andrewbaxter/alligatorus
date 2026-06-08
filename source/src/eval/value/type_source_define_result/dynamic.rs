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
            definition_set::DefinitionSet,
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
                tuple::Tuple,
                type_any::r#const::TypeAnyConst,
                type_function::dynamic::TypeFunctionDynamic,
                type_source_data_type::{
                    SourceDataTypeData,
                    SourceDataTypeRef,
                },
                type_source_function::{
                    SourceFunctionData,
                    TypeSourceFunctionConst,
                },
                type_source_interface::{
                    SourceInterfaceData,
                    SourceInterfaceRef,
                },
                type_source_new_data_result::dynamic::SourceNewDataResult,
                type_source_new_func_result::dynamic::SourceNewFuncResult,
                type_source_new_interface_result::dynamic::SourceNewInterfaceResult,
                type_source_tagged_union::SourceTaggedUnionData,
                type_source_tagged_union_builder::dynamic::SourceTaggedUnionBuilder,
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
    dumpster::unsync::Gc,
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

dynamic_wrap!(struct SourceDefineResult(self_, scope) {
    data {
        ds: Rc < Gc < DefinitionSet >>
    }
    fn new_func(name: String) -> SourceNewFuncResult {
        let ds = self_.ds.clone();
        let entry_id = ds.next_entry_id.next();
        let func_data = Gc::new(SourceFunctionData {
            set: (*ds).clone(),
            name: Some(name),
            description: RefCell::new(None),
            impl_expr: RefCell::new(None),
            arg_type: RefCell::new(None),
            ret_type: RefCell::new(None),
            scope_entry_id: Cell::new(
                Some(crate::langhelp::ScopeEntryId::Def(ds.import_id, ds.def_set_id, entry_id,))
            ),
        });
        ds.functions.borrow_mut().push(func_data.clone());
        let func_ref = Rc::new(TypeSourceFunctionConst { data: func_data, });
        SourceNewFuncResult { func_ref }
    }
    fn new_data(name: String) -> SourceNewDataResult {
        let ds = self_.ds.clone();
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
        let data_field = format!("_{}", ds.next_field_id.next());
        let dt_data = Gc::new(SourceDataTypeData {
            set: (*ds).clone(),
            name: Some(name),
            description: RefCell::new(None),
            underlying_type: RefCell::new(None),
            methods: RefCell::new(Vec::new()),
            interface_impls: RefCell::new(Vec::new()),
            identity,
            data_field,
            artifact_id: RefCell::new(None),
        });
        ds.data_types.borrow_mut().push(dt_data.clone());
        let data_type_ref = Rc::new(SourceDataTypeRef { data: dt_data, });
        SourceNewDataResult { data_type_ref }
    }
    fn build() -> Rc<crate::eval::definition_set::DefinitionSetExports> {
        Rc::new(crate::eval::definition_set::DefinitionSetExports { set: (*self_.ds).clone(), })
    }
    fn tagged_union() -> SourceTaggedUnionBuilder {
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
        let data = Rc::new(SourceTaggedUnionData {
            variants: RefCell::new(Vec::new()),
            identity,
            next_tag_id: crate::langhelp::Counter::default(),
        });
        SourceTaggedUnionBuilder { data }
    }
    fn new_interface() -> SourceNewInterfaceResult {
        let ds = self_.ds.clone();
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
        let iface_data = Gc::new(SourceInterfaceData {
            set: (*ds).clone(),
            description: RefCell::new(None),
            methods: RefCell::new(Vec::new()),
            identity,
        });
        ds.interfaces.borrow_mut().push(iface_data.clone());
        let interface_ref = Rc::new(SourceInterfaceRef { data: iface_data, });
        SourceNewInterfaceResult { interface_ref }
    }
});
