use {
    crate::{
        jscode::JsCode,
        langhelp::{
            Artifacts,
            Aspect,
            Code,
            CodeValue,
            EvalRes,
            ExprContext,
            QExprId,
            SerialValue,
            SerialValueSource,
            Unique,
            Value,
            Type,
        },
    },
    paste::paste,
    std::any::Any,
};

macro_rules! unique{
    () => {
        Unique(SerialValueSource::Builtin, line!() as usize)
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
            pub fn[< $name: snake _get >](xx: & ExprContext, v: &dyn Type) -> Result <Box<dyn $name>,
            String > {
                let Some(out) = v.aspect(xx, &[< $name: snake _aspect_id >]()) else {
                    return Err(format!("TODO Type is missing aspect"));
                };
                return Ok(*out.downcast::<Box<dyn $name>>().unwrap());
            }
            pub fn[< $name: snake _get_maybe >](xx: & ExprContext, v: &dyn Type) -> Option<Box<dyn $name>> {
                let Some(out) = v.aspect(xx, &[< $name: snake _aspect_id >]()) else {
                    return None;
                };
                return Some(*out.downcast::<Box<dyn $name>>().unwrap());
            }
        }
    };
}

aspect!(AspectDrop {
    fn drop(&self, v: Box < dyn Value >) -> Code;
});

aspect!(AspectFunction {
    fn call(&self, xx: & ExprContext, loc: QExprId, function: CodeValue, argument: CodeValue) -> EvalRes;
});

aspect!(AspectSerial {
    fn to_serial(&self, artifacts: & mut Artifacts, value: Box < dyn Value >) -> SerialValue;
});

aspect!(AspectLiftJs {
    /// Value is guaranteed to be of the type described by the aspect's type.
    ///
    /// Used after evaluating a target block, for producing a value to serialize.
    fn lift(&self, value: & v8:: Value) -> Box < dyn Value >;
});

aspect!(AspectJsStackValue {
    /// Value is guaranteed to be of the type described by the aspect's type.
    fn to_value(&self, value: JsCode) -> CodeValue;
});

aspect!(AspectConsumeJs {
    /// Value is guaranteed to be of the type described by the aspect's type.
    ///
    /// Used before interacting with JS: namely, function call arguments.
    fn consume(&self, cv: CodeValue) -> JsCode;
});

aspect!(AspectJsString {});

aspect!(AspectNull {});

aspect!(AspectString {
    fn get(&self, value: & dyn Value) -> String;
});
