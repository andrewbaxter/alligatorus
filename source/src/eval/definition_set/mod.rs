mod jswrap_structs;
mod data_type_ref;
mod data_type_constructor;
mod data_type;
mod exports;
mod function;
mod interface_ref;
mod interface;

pub(crate) use {
    jswrap_structs::DefineResult,
    data_type_ref::MortarTypeDefinitionSetDataTypeRef,
    data_type_constructor::MortarTypeDefinitionSetDataTypeConstructor,
    exports::MortarTypeDefinitionSetExports,
    function::MortarTypeDefinitionSetFunction,
    interface_ref::MortarTypeDefinitionSetInterfaceRef,
};

use {
    std::{
        cell::{
            Cell,
            RefCell,
        },
        rc::Rc,
        sync::atomic::{AtomicUsize, Ordering},
    },
    dumpster::unsync::Gc,
    crate::langhelp::{
        Counter,
        ScopeEntryId,
        Unique,
        Value,
    },
};

static NEXT_INTERFACE_METHOD_ID: AtomicUsize = AtomicUsize::new(0);

pub(crate) fn next_interface_method_id() -> usize {
    NEXT_INTERFACE_METHOD_ID.fetch_add(1, Ordering::Relaxed)
}

// --- DefinitionSet data ---

pub(crate) struct DefinitionSetFunctionData {
    pub name: Option<String>,
    pub description: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Box<dyn Value>>>,
    pub arg_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub ret_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub arg_scope_entry_id: Cell<Option<ScopeEntryId>>,
}

pub(crate) struct DefinitionSetDataMethodData {
    pub name: String,
    pub impl_expr: RefCell<Option<Box<dyn Value>>>,
    pub ret_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub self_scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub field_name: String,
}

pub(crate) struct DefinitionSetDataTypeData {
    pub name: Option<String>,
    pub description: RefCell<Option<String>>,
    pub underlying_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub methods: RefCell<Vec<DefinitionSetDataMethodData>>,
    pub interface_impls: RefCell<Vec<DefinitionSetDataTypeInterfaceImplData>>,
    pub identity: Unique,
    pub data_field: String,
}

pub(crate) struct DefinitionSetDataTypeInterfaceImplData {
    pub interface_index: usize,
    pub method_impls: Vec<DefinitionSetDataTypeInterfaceMethodImplData>,
}

pub(crate) struct DefinitionSetDataTypeInterfaceMethodImplData {
    pub interface_method_index: usize,
    pub impl_expr: Box<dyn Value>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub self_scope_entry_id: Cell<Option<ScopeEntryId>>,
}

pub(crate) struct DefinitionSetInterfaceMethodData {
    pub name: String,
    pub arg_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub ret_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub field_name: String,
}

pub(crate) struct DefinitionSetInterfaceData {
    pub description: RefCell<Option<String>>,
    pub methods: RefCell<Vec<DefinitionSetInterfaceMethodData>>,
    pub identity: Unique,
}

pub(crate) struct DefinitionSet {
    pub functions: RefCell<Vec<DefinitionSetFunctionData>>,
    pub data_types: RefCell<Vec<DefinitionSetDataTypeData>>,
    pub interfaces: RefCell<Vec<DefinitionSetInterfaceData>>,
    pub finished: Cell<bool>,
    pub next_field_id: Counter,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for DefinitionSet {
    fn accept(&self, _visitor: &mut V) -> Result<(), ()> {
        Ok(())
    }
}

impl DefinitionSet {
    pub fn new() -> Self {
        DefinitionSet {
            functions: RefCell::new(Vec::new()),
            data_types: RefCell::new(Vec::new()),
            interfaces: RefCell::new(Vec::new()),
            finished: Cell::new(false),
            next_field_id: Counter::default(),
        }
    }

    pub fn finish(&self) -> Result<(), String> {
        if self.finished.get() {
            return Ok(());
        }
        let functions = self.functions.borrow();
        for (i, func_data) in functions.iter().enumerate() {
            if func_data.impl_expr.borrow().is_none() {
                return Err(format!("Definition set function {} is missing an implementation", i));
            }
        }
        let data_types = self.data_types.borrow();
        for (i, dt_data) in data_types.iter().enumerate() {
            if dt_data.underlying_type.borrow().is_none() {
                return Err(format!("Data type {} is missing underlying type", i));
            }
            for (j, method) in dt_data.methods.borrow().iter().enumerate() {
                if method.impl_expr.borrow().is_none() {
                    return Err(format!("Data type {} method {} is missing implementation", i, j));
                }
            }
        }
        self.finished.set(true);
        Ok(())
    }
}

pub(crate) struct DefinitionSetFunction {
    pub set: Gc<DefinitionSet>,
    pub func_index: usize,
}

pub(crate) struct DefinitionSetDataTypeRef {
    pub set: Gc<DefinitionSet>,
    pub data_type_index: usize,
}

pub(crate) struct DefinitionSetDataTypeConstructor {
    pub set: Gc<DefinitionSet>,
    pub data_type_index: usize,
}

pub(crate) struct DefinitionSetInterfaceRef {
    pub set: Gc<DefinitionSet>,
    pub interface_index: usize,
}

pub(crate) struct DefinitionSetInterface {
    pub set: Gc<DefinitionSet>,
    pub interface_index: usize,
}

pub(crate) struct DefinitionSetExports {
    pub set: Gc<DefinitionSet>,
}

pub(crate) struct DefinitionSetDataType {
    pub set: Gc<DefinitionSet>,
    pub data_type_index: usize,
}
