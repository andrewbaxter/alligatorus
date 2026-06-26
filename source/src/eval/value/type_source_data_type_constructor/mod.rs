pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        eval::{
            from_dynamic_ref,
            push::Push,
            to_dynamic_ext,
        },
        langhelp::Type,
    },
    std::rc::Rc,
};

#[derive(Clone)]
pub(crate) struct TypeSourceDataTypeConstructorConst {
    pub data: dumpster::unsync::Gc<super::type_source_data_type::SourceDataTypeData>,
}

impl Push for Rc<TypeSourceDataTypeConstructorConst> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(dynamic::TypeSourceDataTypeConstructorDynamic)
    }

    fn from_dynamic<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_dynamic_ref::<Rc<TypeSourceDataTypeConstructorConst>>(&*value).clone()
    }

    fn into_dynamic<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_dynamic_ext(scope, self).into()
    }
}
