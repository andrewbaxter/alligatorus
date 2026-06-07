pub mod artifact;
pub mod r#const;
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

pub(crate) struct TypeSourceDataTypeConst {
    pub data: Gc<SourceDataTypeData>,
}

pub(crate) struct SourceDataTypeRef {
    pub data: Gc<SourceDataTypeData>,
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceDataTypeRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(dynamic::TypeSourceDataTypeDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let type_: &Rc<dyn crate::langhelp::Type> = crate::eval::from_v8_ref(&*value);
        let dt =
            (type_.as_ref() as &dyn std::any::Any)
                .downcast_ref::<TypeSourceDataTypeConst>()
                .expect("SourceDataTypeRef::from_v8: expected TypeSourceDataTypeConst");
        Rc::new(SourceDataTypeRef { data: dt.data.clone() })
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn crate::langhelp::Type> = Rc::new(TypeSourceDataTypeConst { data: self.data.clone() });
        crate::eval::to_v8(scope, type_).into()
    }
}
