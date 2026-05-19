mod jswrap_structs;
mod data_type_ref;
mod data_type_constructor;
mod data_type;
mod function;

pub(crate) use {
    jswrap_structs::DefineResult,
    data_type_ref::MortarTypeDefinitionSetDataTypeRef,
    data_type_constructor::MortarTypeDefinitionSetDataTypeConstructor,
    function::MortarTypeDefinitionSetFunction,
};

use {
    std::{
        cell::{
            Cell,
            RefCell,
        },
        sync::atomic::{AtomicUsize, Ordering},
    },
    dumpster::unsync::Gc,
    crate::langhelp::{
        Counter,
        ScopeEntryId,
        Value,
    },
};

static NEXT_DATA_TYPE_IDENTITY: AtomicUsize = AtomicUsize::new(0);

pub(crate) fn next_data_type_identity() -> usize {
    NEXT_DATA_TYPE_IDENTITY.fetch_add(1, Ordering::Relaxed)
}

// --- DefinitionSet data ---

pub(crate) struct DefinitionSetFunctionData {
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
    pub underlying_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub methods: RefCell<Vec<DefinitionSetDataMethodData>>,
    pub identity: usize,
    pub data_field: String,
    pub unique_field: String,
}

pub(crate) struct DefinitionSet {
    pub functions: RefCell<Vec<DefinitionSetFunctionData>>,
    pub data_types: RefCell<Vec<DefinitionSetDataTypeData>>,
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
            if dt_data.underlying_type_expr.borrow().is_none() {
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

pub(crate) struct DefinitionSetDataType {
    pub set: Gc<DefinitionSet>,
    pub data_type_index: usize,
}
