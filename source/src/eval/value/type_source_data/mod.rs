pub mod artifact;
pub mod dynamic;

use {
    crate::langhelp::{
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

pub(crate) struct SourceDataMethodData {
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

pub(crate) struct SourceDataTypeData {
    pub set: Gc<crate::eval::definition_set::DefinitionSet>,
    pub name: Option<String>,
    pub description: RefCell<Option<String>>,
    pub underlying_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub methods: RefCell<Vec<SourceDataMethodData>>,
    pub interface_impls: RefCell<Vec<SourceDataTypeInterfaceImplData>>,
    pub identity: Unique,
    pub data_field: String,
    pub artifact_id: RefCell<Option<crate::langhelp::ArtifactId>>,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceDataTypeData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        let impls = self.interface_impls.try_borrow().map_err(|_| ())?;
        for impl_data in impls.iter() {
            visitor.visit_unsync(&impl_data.interface);
        }
        Ok(())
    }
}

pub(crate) struct SourceDataTypeInterfaceImplData {
    pub interface: Gc<super::type_source_interface::SourceInterfaceData>,
    pub method_impls: Vec<SourceDataTypeInterfaceMethodImplData>,
}

pub(crate) struct SourceDataTypeInterfaceMethodImplData {
    pub interface_method_index: usize,
    pub impl_expr: Rc<dyn Value>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub self_scope_entry_id: Cell<Option<ScopeEntryId>>,
}

pub(crate) struct SourceDataType {
    pub data: Gc<SourceDataTypeData>,
}
