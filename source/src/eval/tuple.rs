use {
    crate::{
        builtin::{
            AspectTuple,
            aspect_tuple_aspect_id,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntry,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct ValueTuple {
    pub elements: Vec<EvalRes>,
}

impl Value for ValueTuple {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        let types: Vec<Rc<dyn Type>> = self.elements.iter().map(|e| {
            match &e.v {
                Some(cv) => cv.value.type_(xx, loc),
                None => Rc::new(TypeTuple { types: vec![] }) as Rc<dyn Type>,
            }
        }).collect();
        Rc::new(TypeTuple { types })
    }
}

pub(crate) struct TypeTuple {
    pub types: Vec<Rc<dyn Type>>,
}

impl Value for TypeTuple {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeTuple { types: vec![] })
    }
}

impl Type for TypeTuple {
    fn desc(&self) -> String {
        format!("tuple({})", self.types.iter().map(|t| t.desc()).collect::<Vec<_>>().join(", "))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_tuple_aspect_id() {
            return Some(Box::new(Box::new(TypeTuple { types: self.types.clone() }) as Box<dyn AspectTuple>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tuple to other type"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tuple type"))
    }
}

impl Aspect for TypeTuple { }

impl AspectTuple for TypeTuple {
    fn element_types(&self) -> Vec<Rc<dyn Type>> {
        self.types.clone()
    }

    fn take_elements(&self, value: Box<dyn Value>) -> Vec<EvalRes> {
        let tuple: Box<ValueTuple> = (value as Box<dyn Any>).downcast().unwrap();
        tuple.elements
    }
}

pub(crate) struct ValueRecord {
    pub entries: Vec<(String, EvalRes)>,
}

impl Value for ValueRecord {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeTuple { types: vec![] })
    }
}

/// A value whose data is entirely carried by its type.
pub(crate) struct StaticValue {
    pub type_: Rc<dyn Type>,
}

impl Value for StaticValue {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        self.type_.clone()
    }
}

/// A scope entry for non-JS values. Returns Code_::None and reconstructs the value from its type.
pub(crate) struct StaticScopeEntry {
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for StaticScopeEntry {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::None },
            value: Box::new(StaticValue { type_: self.type_.clone() }),
        }
    }
}
