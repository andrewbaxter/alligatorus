pub mod artifact;
pub mod dynamic;

use std::rc::Rc;

pub(crate) struct TypeSourceDataTypeConstructorConst {
    pub data: dumpster::unsync::Gc<super::type_source_data::SourceDataTypeData>,
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<TypeSourceDataTypeConstructorConst> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(dynamic::TypeSourceDataTypeConstructorDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<TypeSourceDataTypeConstructorConst>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}
