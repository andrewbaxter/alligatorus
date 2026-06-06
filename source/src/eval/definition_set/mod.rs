mod dynamic_wrap_structs;
mod data_type_ref;
pub(crate) mod data_type_constructor;
pub(crate) mod data_type;
mod exports;
mod function;
mod interface_ref;
mod interface;
pub(crate) mod tagged_union;

pub(crate) use {
    data_type_constructor::TypeSourceDataTypeConstructorDynamic,
    data_type_ref::TypeSourceDataTypeRefDynamic,
    dynamic_wrap_structs::DefineResult,
    exports::TypeSourceExportsDynamic,
    function::TypeSourceFunctionDynamic,
    interface_ref::TypeSourceInterfaceRefDynamic,
};
use {
    crate::langhelp::{
        Counter,
        DefSetId,
        ImportStateId,
        ScopeEntryId,
        Unique,
        Value,
    },
    dumpster::unsync::Gc,
    std::{
        cell::{
            Cell,
            RefCell,
        },
        rc::Rc,
    },
};

pub(crate) fn next_interface_method_id(set: &DefinitionSet) -> usize {
    set.next_interface_method_id.next()
}

// --- DefinitionSet data ---
pub(crate) struct DefinitionSetFunctionData {
    pub set: Gc<DefinitionSet>,
    pub name: Option<String>,
    pub description: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub arg_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub ret_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for DefinitionSetFunctionData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        Ok(())
    }
}

pub(crate) struct DefinitionSetDataMethodData {
    pub name: String,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub ret_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub self_scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub field_name: String,
    /// Pre-compiled JS function body, set after method compilation in eval(). Format:
    /// complete function definition string (e.g. "function d0_0_0() { ... }")
    pub compiled_js: RefCell<Option<String>>,
}

pub(crate) struct DefinitionSetDataTypeData {
    pub set: Gc<DefinitionSet>,
    pub name: Option<String>,
    pub description: RefCell<Option<String>>,
    pub underlying_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub methods: RefCell<Vec<DefinitionSetDataMethodData>>,
    pub interface_impls: RefCell<Vec<DefinitionSetDataTypeInterfaceImplData>>,
    pub identity: Unique,
    pub data_field: String,
    pub artifact_id: RefCell<Option<crate::langhelp::ArtifactId>>,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for DefinitionSetDataTypeData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        let impls = self.interface_impls.try_borrow().map_err(|_| ())?;
        for impl_data in impls.iter() {
            visitor.visit_unsync(&impl_data.interface);
        }
        Ok(())
    }
}

pub(crate) struct DefinitionSetDataTypeInterfaceImplData {
    pub interface: Gc<DefinitionSetInterfaceData>,
    pub method_impls: Vec<DefinitionSetDataTypeInterfaceMethodImplData>,
}

pub(crate) struct DefinitionSetDataTypeInterfaceMethodImplData {
    pub interface_method_index: usize,
    pub impl_expr: Rc<dyn Value>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub self_scope_entry_id: Cell<Option<ScopeEntryId>>,
}

pub(crate) struct DefinitionSetInterfaceMethodData {
    pub name: String,
    pub arg_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub ret_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub field_name: String,
}

pub(crate) struct DefinitionSetInterfaceData {
    pub set: Gc<DefinitionSet>,
    pub description: RefCell<Option<String>>,
    pub methods: RefCell<Vec<DefinitionSetInterfaceMethodData>>,
    pub identity: Unique,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for DefinitionSetInterfaceData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        Ok(())
    }
}

pub(crate) struct DefinitionSet {
    pub import_id: ImportStateId,
    pub def_set_id: DefSetId,
    pub functions: RefCell<Vec<Gc<DefinitionSetFunctionData>>>,
    pub data_types: RefCell<Vec<Gc<DefinitionSetDataTypeData>>>,
    pub interfaces: RefCell<Vec<Gc<DefinitionSetInterfaceData>>>,
    pub finished: Cell<bool>,
    pub next_field_id: Counter,
    pub next_entry_id: Counter,
    pub next_interface_method_id: Counter,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for DefinitionSet {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        let functions = self.functions.try_borrow().map_err(|_| ())?;
        for f in functions.iter() {
            visitor.visit_unsync(f);
        }
        drop(functions);
        let data_types = self.data_types.try_borrow().map_err(|_| ())?;
        for dt in data_types.iter() {
            visitor.visit_unsync(dt);
        }
        drop(data_types);
        let interfaces = self.interfaces.try_borrow().map_err(|_| ())?;
        for i in interfaces.iter() {
            visitor.visit_unsync(i);
        }
        Ok(())
    }
}

impl DefinitionSet {
    pub fn new(import_id: ImportStateId, def_set_id: DefSetId) -> Self {
        DefinitionSet {
            import_id,
            def_set_id,
            functions: RefCell::new(Vec::new()),
            data_types: RefCell::new(Vec::new()),
            interfaces: RefCell::new(Vec::new()),
            finished: Cell::new(false),
            next_field_id: Counter::default(),
            next_entry_id: Counter::default(),
            next_interface_method_id: Counter::default(),
        }
    }

    pub fn finish(&self) -> Result<(), String> {
        if self.finished.get() {
            return Ok(());
        }
        let functions = self.functions.borrow();
        for func_data in functions.iter() {
            let name = func_data.name.as_deref().unwrap_or("(unnamed)");
            if func_data.impl_expr.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing an implementation", name));
            }
            if func_data.arg_type.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing an arg type", name));
            }
            if func_data.ret_type.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing a return type", name));
            }
        }
        let data_types = self.data_types.borrow();
        for dt_data in data_types.iter() {
            let name = dt_data.name.as_deref().unwrap_or("(unnamed)");
            if dt_data.underlying_type.borrow().is_none() {
                return Err(format!("Data type '{}' is missing underlying type", name));
            }
            for method in dt_data.methods.borrow().iter() {
                if method.impl_expr.borrow().is_none() {
                    return Err(format!("Data type '{}' method '{}' is missing implementation", name, method.name));
                }
                if method.ret_type.borrow().is_none() {
                    return Err(format!("Data type '{}' method '{}' is missing a return type", name, method.name));
                }
                // Note: data type methods don't have arg_type - they receive `self` implicitly
            }
        }
        let interfaces = self.interfaces.borrow();
        for (i, iface) in interfaces.iter().enumerate() {
            let iface_name = iface.description.borrow().clone().unwrap_or_else(|| format!("(interface {})", i));
            for method in iface.methods.borrow().iter() {
                if method.arg_type.borrow().is_none() {
                    return Err(
                        format!("Interface '{}' method '{}' is missing an arg type", iface_name, method.name),
                    );
                }
                if method.ret_type.borrow().is_none() {
                    return Err(
                        format!("Interface '{}' method '{}' is missing a return type", iface_name, method.name),
                    );
                }
            }
        }
        self.finished.set(true);
        Ok(())
    }
}

pub(crate) struct DefinitionSetFunction {
    pub data: Gc<DefinitionSetFunctionData>,
}

pub(crate) struct DefinitionSetDataTypeRef {
    pub data: Gc<DefinitionSetDataTypeData>,
}

pub(crate) struct DefinitionSetDataTypeConstructor {
    pub data: Gc<DefinitionSetDataTypeData>,
}

pub(crate) struct DefinitionSetInterfaceRef {
    pub data: Gc<DefinitionSetInterfaceData>,
}

pub(crate) struct DefinitionSetInterface {
    pub data: Gc<DefinitionSetInterfaceData>,
}

pub(crate) struct DefinitionSetExports {
    pub set: Gc<DefinitionSet>,
}

pub(crate) struct DefinitionSetDataType {
    pub data: Gc<DefinitionSetDataTypeData>,
}

// --- Tagged union data ---
pub(crate) struct TaggedUnionVariant {
    pub tag: crate::langhelp::ConstKey,
    pub js_tag: String,
    pub type_: Rc<dyn crate::langhelp::Type>,
}

pub(crate) struct TaggedUnionData {
    pub variants: RefCell<Vec<TaggedUnionVariant>>,
    pub identity: Unique,
    pub next_tag_id: Counter,
}

pub(crate) struct TaggedUnionRef {
    pub data: Rc<TaggedUnionData>,
}

pub(crate) struct TaggedUnionConstructorRef {
    pub data: Rc<TaggedUnionData>,
    pub variant_index: usize,
}
