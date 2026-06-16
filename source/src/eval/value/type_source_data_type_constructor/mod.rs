pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        eval::{
            from_v8_ref,
            push::Push,
            to_v8,
        },
        langhelp::Type,
    },
    std::rc::Rc,
};

pub(crate) struct TypeSourceDataTypeConstructorConst {
    pub data: dumpster::unsync::Gc<super::type_source_data_type::SourceDataTypeData>,
}

impl Push for Rc<TypeSourceDataTypeConstructorConst> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(dynamic::TypeSourceDataTypeConstructorDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_v8_ref::<Rc<TypeSourceDataTypeConstructorConst>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self).into()
    }
}
