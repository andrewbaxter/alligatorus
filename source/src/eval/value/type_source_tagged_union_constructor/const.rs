use {
    crate::artifact::ExporterAny,
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            EResult,
            dynamic_util::bind_via_target,
            typecheck_convert,
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_source_tagged_union::{
                    SourceTaggedUnionData,
                    r#const::TypeSourceTaggedUnionConst,
                },
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            ConstKey,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectFunction,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_stack_value_dynamic_get,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeSourceTaggedUnionConstructorConst {
    pub data: Rc<SourceTaggedUnionData>,
    pub variant_index: usize,
}

impl Aspect for TypeSourceTaggedUnionConstructorConst { }

impl AspectBind for TypeSourceTaggedUnionConstructorConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectFunction for TypeSourceTaggedUnionConstructorConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let variants = self.data.variants.borrow();
        let variant = &variants[self.variant_index];
        let js_tag = variant.js_tag.clone();
        let underlying_type = variant.type_.clone();
        drop(variants);
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
            let result_type = Rc::new(TypeSourceTaggedUnionConst { data: self.data.clone() });
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(
                    CodeDynamicNewObject { fields: vec![(ConstKey::String(js_tag).to_str(), underlying_js)], }
                ),
            }));
        });
    }
}

impl Type for TypeSourceTaggedUnionConstructorConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceTaggedUnionConstructorConst {
            data: self.data.clone(),
            variant_index: self.variant_index,
        }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionConstructorConst {
                data: self.data.clone(),
                variant_index: self.variant_index,
            }) as Box<dyn AspectBind>));
        }
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionConstructorConst {
                data: self.data.clone(),
                variant_index: self.variant_index,
            }) as Box<dyn AspectFunction>));
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union_constructor"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union_constructor"))
    }

    fn desc(&self) -> String {
        let variants = self.data.variants.borrow();
        let tag = &variants[self.variant_index].tag;
        format!("tagged_union_constructor({:?})", tag)
    }
}

impl ExporterAny for TypeSourceTaggedUnionConstructorConst {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for TypeSourceTaggedUnionConstructorConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeAnyConst))
    }
}
