use {
    crate::{
        jscode::JsCode,
        langhelp::{
            Artifacts,
            Aspect,
            Code,
            CodeValue,
            ConstKey,
            EvalRes,
            ScopeEntry,
            ScopeEntryId,
            EResult,
            ExprContext,
            QExprId,
            SerialValue,
            Type,
            Unique,
            Value,
        },
    },
    paste::paste,
    std::{
        any::Any,
        rc::Rc,
    },
};

macro_rules! unique{
    () => {
        Unique::Builtin {
            file: file!(),
            line: line!() as usize
        }
    };
}

macro_rules! aspect{
    ($name: ident {
        $($i: item) *
    }) => {
        paste!{
            pub trait $name: Aspect {
                $($i) *
            }
            pub fn[< $name: snake _aspect_id >]() -> Unique {
                return unique!();
            }
            pub fn[
                < $name: snake _get >
            ](xx: & ExprContext, loc: QExprId, v: & dyn Type) -> EResult < Box < dyn $name >> {
                let Some(out) = v.aspect(xx, &[< $name: snake _aspect_id >]()) else {
                    return EResult::err(loc, format!("Type ({}) is missing aspect {}", v.desc(), stringify!{
                        $name
                    }));
                };
                return EResult:: ok(* out.downcast::< Box < dyn $name >>().unwrap());
            }
            pub fn[< $name: snake _get_maybe >](xx: & ExprContext, v: & dyn Type) -> Option < Box < dyn $name >> {
                let Some(out) = v.aspect(xx, &[< $name: snake _aspect_id >]()) else {
                    return None;
                };
                return Some(* out.downcast::< Box < dyn $name >>().unwrap());
            }
        }
    };
}

aspect!(AspectDrop {
    fn drop(&self, v: Box < dyn Value >) -> Code;
});

aspect!(AspectFunction {
    fn call(&self, xx: & ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes;
});

aspect!(AspectSerial {
    fn to_serial(&self, artifacts: & mut Artifacts, self_: Box < dyn Value >) -> EResult < SerialValue >;
});

aspect!(AspectLiftJs {
    /// Value is guaranteed to be of the type described by the aspect's type.
    ///
    /// Used after evaluating a target block, for producing a value to serialize.
    fn lift(&self, scope: & v8:: PinScope, self_: v8:: Local < v8:: Value >) -> Result < Box < dyn Value >,
    String >;
});

aspect!(AspectJsStackValue {
    /// Value is guaranteed to be of the type described by the aspect's type.
    fn to_value(&self, loc: QExprId, self_: JsCode) -> CodeValue;
});

aspect!(AspectConsumeJs {
    /// Value is guaranteed to be of the type described by the aspect's type.
    ///
    /// Used before interacting with JS: namely, function call arguments.
    fn to_js(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue) -> EResult < JsCode >;
});

aspect!(AspectBind {
    fn bind(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue) -> EResult < Box < dyn ScopeEntry > >;
});

aspect!(AspectBindJs {
    fn bind_js(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue) -> EResult <(Option < JsCode >, CodeValue) >;
});

aspect!(AspectJsString {});

aspect!(AspectNull {});

aspect!(AspectString {
    fn get(&self, value: & dyn Value) -> String;
});

aspect!(AspectSet {
    fn set(&self, xx: & ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes;
});

aspect!(AspectField {
    fn field(&self, xx: & ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes;
    fn field_opt(&self, xx: & ExprContext, key: & str) -> Option < Rc < dyn Type >>;
});

aspect!(AspectTuple {
    fn element_types(&self) -> Vec < Rc < dyn Type >>;
    fn take_elements(&self, value: Box < dyn Value >) -> Vec < EvalRes >;
});

aspect!(AspectNumber {
    fn get(&self, value: & dyn Value) -> f64;
});

aspect!(AspectBool {
    fn get(&self, value: & dyn Value) -> bool;
});

aspect!(AspectJsNumber {});

aspect!(AspectJsBool {});

aspect!(AspectOperatorLt {
    fn lt(&self, xx: & ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes;
});

aspect!(AspectOperatorAdd {
    fn add(&self, xx: & ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes;
});

aspect!(AspectIterable {
    fn element_type(&self) -> Rc < dyn Type >;
    fn foreach(
        &self,
        xx: & ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ScopeEntryId,
        body: CodeValue,
    ) -> EvalRes;
});

aspect!(AspectTaggedUnion {
    /// Returns (const_key, js_tag, type) for each variant
    fn variants(&self) -> Vec <(ConstKey, String, Rc < dyn Type >) >;
});
