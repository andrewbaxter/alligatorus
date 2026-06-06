pub mod dynamic;

use std::rc::Rc;

pub(crate) struct SourceDataTypeRef {
    pub data: dumpster::unsync::Gc<super::type_source_data::SourceDataTypeData>,
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceDataTypeRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(dynamic::TypeSourceDataTypeRefDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let type_: &Rc<dyn crate::langhelp::Type> = crate::eval::from_v8_ref(&*value);
        let dt =
            (type_.as_ref() as &dyn std::any::Any)
                .downcast_ref::<super::type_source_data::SourceDataType>()
                .expect("SourceDataTypeRef::from_v8: expected SourceDataType");
        Rc::new(SourceDataTypeRef { data: dt.data.clone() })
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn crate::langhelp::Type> =
            Rc::new(super::type_source_data::SourceDataType { data: self.data.clone() });
        crate::eval::to_v8(scope, type_).into()
    }
}
