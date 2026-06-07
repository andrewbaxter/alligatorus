use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectStackValueDynamic,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicType,
        },
        eval::{
            Erb,
            dynamic_wrap::{
                AnyType,
                DynamicWrap,
                dynamic_wrap_method_name,
            },
            value::{
                type_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_source_tagged_union::{
                    SourceTaggedUnionData,
                    SourceTaggedUnionRef,
                    SourceTaggedUnionVariant,
                },
                type_source_tagged_union_constructor::SourceTaggedUnionConstructorRef,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        cell::RefCell,
        mem,
        rc::Rc,
    },
};

dynamic_wrap!(struct SourceTaggedUnionBuilder(self_, scope) {
    data {
        data: Rc < SourceTaggedUnionData >
    }
    field type_: Rc < SourceTaggedUnionRef > {
        Rc::new(SourceTaggedUnionRef { data: self_.data.clone() })
    }
    fn variant(tag: String, type_ref: AnyType) -> Rc<SourceTaggedUnionConstructorRef> {
        let variant_index = {
            let mut variants = self_.data.variants.borrow_mut();
            let idx = variants.len();
            let js_tag = format!("{}{}", crate::code_dynamic::TAG_PREFIX, self_.data.next_tag_id.next());
            variants.push(SourceTaggedUnionVariant {
                tag: crate::langhelp::ConstKey::String(tag),
                js_tag,
                type_: type_ref.0,
            });
            idx
        };
        Rc::new(SourceTaggedUnionConstructorRef {
            data: self_.data.clone(),
            variant_index,
        })
    }
});
