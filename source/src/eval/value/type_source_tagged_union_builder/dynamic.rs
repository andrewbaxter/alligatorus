use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicType,
        },
        eval::{
            Erb,
            TargetBindSub,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            dynamic_wrap::{
                AnyType,
                DynamicWrap,
                dynamic_wrap_method_name,
            },
            value::{
                tuple::Tuple,
                type_any::r#const::TypeAnyConst,
                type_function::dynamic::TypeFunctionDynamic,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
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
            ScopeEntry,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
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
                js_tag,
                tag: crate::langhelp::ConstKey::String(tag),
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
