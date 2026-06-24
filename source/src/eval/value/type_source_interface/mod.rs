pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        eval::{
            definition_set::DefinitionSet,
            from_v8_ref,
            push::Push,
            to_v8,
        },
        langhelp::{
            Type,
            Unique,
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

#[derive(Clone, Copy, PartialEq, Eq, Debug, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case")]
pub(crate) enum BuiltinInterfaceKind {
    AspectField,
    AspectFunction,
    CodeDyn,
    ScopeEntry,
    Target,
    Type,
    Value,
}

pub(crate) struct SourceInterfaceData {
    pub artifact_id: RefCell<Option<crate::artifact::ArtifactIdSource>>,
    pub builtin_kind: Option<BuiltinInterfaceKind>,
    pub description: RefCell<Option<String>>,
    pub identity: Unique,
    pub is_aspect: Cell<bool>,
    pub methods: RefCell<Vec<Rc<SourceInterfaceMethodData>>>,
    pub set: Gc<DefinitionSet>,
}

impl SourceInterfaceData {
    pub fn finish(&self) -> Result<(), String> {
        let name = self.description.borrow().clone().unwrap_or_else(|| "(unnamed)".to_string());
        for method in self.methods.borrow().iter() {
            method.finish(&name)?;
        }
        Ok(())
    }
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceInterfaceData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        Ok(())
    }
}

pub(crate) struct SourceInterfaceMethodData {
    pub arg_type: RefCell<Option<Rc<dyn Type>>>,
    pub field_name: String,
    pub name: String,
    pub ret_type: RefCell<Option<Rc<dyn Type>>>,
}

impl SourceInterfaceMethodData {
    pub fn finish(&self, iface_name: &str) -> Result<(), String> {
        if self.arg_type.borrow().is_none() {
            return Err(format!("Interface '{}' method '{}' is missing an arg type", iface_name, self.name));
        }
        if self.ret_type.borrow().is_none() {
            return Err(format!("Interface '{}' method '{}' is missing a return type", iface_name, self.name));
        }
        Ok(())
    }
}

pub(crate) struct SourceInterfaceRef {
    pub data: Gc<SourceInterfaceData>,
}

impl Push for Rc<SourceInterfaceRef> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(dynamic::TypeSourceInterfaceDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_v8_ref::<Rc<SourceInterfaceRef>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self).into()
    }
}
