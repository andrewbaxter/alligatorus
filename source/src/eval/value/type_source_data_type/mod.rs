pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        invariant::DeserialLazy,
        langhelp::{
            ScopeEntryId,
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
    pub arg_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    /// Externals (`a##` bindings) captured by the body's compile environment.
    /// `compiled_js` references these by their Local id; they must be installed as v8
    /// globals before the body runs.
    pub captured_externals: RefCell<Vec<(ScopeEntryId, crate::langhelp::External)>>,
    /// Pre-compiled JS function body, set after method compilation in eval(). Format:
    /// complete function definition string (e.g. "function d0_0_0() { ... }")
    pub compiled_js: RefCell<Option<String>>,
    pub field_name: String,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub name: String,
    pub ret_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub inner_scope_entry_id: Cell<Option<ScopeEntryId>>,
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
    pub artifact_id: RefCell<Option<crate::langhelp::ArtifactId>>,
    pub data_field: String,
    pub description: RefCell<Option<String>>,
    pub identity: Unique,
    pub interface_impls: RefCell<Vec<SourceDataTypeInterfaceImplData>>,
    pub methods: RefCell<Vec<Rc<SourceDataMethodData>>>,
    pub name: Option<String>,
    pub set: Gc<crate::eval::definition_set::DefinitionSet>,
    pub underlying_type: DeserialLazy<Rc<dyn crate::langhelp::Type>>,
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
    /// Externals (`a##` bindings) captured by the body's compile environment. See
    /// `SourceDataMethodData::captured_externals`.
    pub captured_externals: RefCell<Vec<(ScopeEntryId, crate::langhelp::External)>>,
    pub compiled_js: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub impl_expr_artifact: RefCell<Option<crate::eval::expr_artifact::ArtifactExpr>>,
    pub interface_method_index: usize,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub inner_scope_entry_id: Cell<Option<ScopeEntryId>>,
}

pub(crate) struct SourceDataTypeRef {
    pub data: Gc<SourceDataTypeData>,
}

pub(crate) struct TypeSourceDataTypeConst {
    pub data: Gc<SourceDataTypeData>,
}

impl crate::eval::pull::Pull for Rc<SourceDataTypeRef> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<SourceDataTypeRef>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }

    fn dynamic_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(dynamic::TypeSourceDataTypeRefDynamic)
    }
}
