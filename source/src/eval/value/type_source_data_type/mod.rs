pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        eval::{
            definition_set::DefinitionSet,
            expr_artifact::ArtifactExpr,
            from_v8_ref,
            push::Push,
            to_v8,
        },
        invariant::DeserialLazy,
        langhelp::{
            ArtifactId,
            External,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
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
    pub arg_type: RefCell<Option<Rc<dyn Type>>>,
    pub captured_externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub compiled_js: RefCell<Option<String>>,
    pub field_name: String,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub inner_scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub name: String,
    pub ret_type: RefCell<Option<Rc<dyn Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
}

impl SourceDataMethodData {
    pub fn finish(&self, dt_name: &str) -> Result<(), String> {
        if self.impl_expr.borrow().is_none() {
            return Err(format!("Data type '{}' method '{}' is missing implementation", dt_name, self.name));
        }
        if self.ret_type.borrow().is_none() {
            return Err(format!("Data type '{}' method '{}' is missing a return type", dt_name, self.name));
        }
        Ok(())
    }
}

pub(crate) struct SourceDataTypeData {
    pub artifact_id: RefCell<Option<ArtifactId>>,
    pub data_field: String,
    pub description: RefCell<Option<String>>,
    pub identity: Unique,
    pub interface_impls: RefCell<Vec<SourceDataTypeInterfaceImplData>>,
    pub methods: RefCell<Vec<Rc<SourceDataMethodData>>>,
    pub name: Option<String>,
    pub set: Gc<DefinitionSet>,
    pub underlying_type: DeserialLazy<Rc<dyn Type>>,
}

impl SourceDataTypeData {
    pub fn finish(&self) -> Result<(), String> {
        let name = self.name.as_deref().unwrap_or("(unnamed)");
        for method in self.methods.borrow().iter() {
            method.finish(name)?;
        }
        Ok(())
    }
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
    pub captured_externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub compiled_js: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub impl_expr_artifact: RefCell<Option<ArtifactExpr>>,
    pub inner_scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub interface_method_index: usize,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
}

pub(crate) struct SourceDataTypeRef {
    pub data: Gc<SourceDataTypeData>,
}

impl Push for Rc<SourceDataTypeRef> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(dynamic::TypeSourceDataTypeRefDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_v8_ref::<Rc<SourceDataTypeRef>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self).into()
    }
}
