use {
    crate::langhelp::{
        BindingId,
        Type,
        Unique,
    },
    std::rc::Rc,
};

pub(crate) enum BuiltinLeaf<'s> {
    AnyConst {
        type_: Rc<dyn Type>,
    },
    AspectId {
        dynamic_key: BindingId,
        unique: Unique,
        dynamic_value: v8::Local<'s, v8::Value>,
    },
    Constructor {
        dynamic_key: BindingId,
        type_: Rc<dyn Type>,
        dynamic_value: v8::Local<'s, v8::Value>,
    },
    Function {
        dynamic_key: BindingId,
        type_: Rc<dyn Type>,
        dynamic_value: v8::Local<'s, v8::Value>,
    },
    InitialBind {
        dynamic_key: BindingId,
        type_: Rc<dyn Type>,
    },
    Interface {
        dynamic_key: BindingId,
        type_: Rc<dyn Type>,
        dynamic_value: v8::Local<'s, v8::Value>,
    },
}

pub(crate) enum BuiltinNode<'s> {
    Branch(Vec<(&'static str, BuiltinNode<'s>)>),
    Leaf(BuiltinLeaf<'s>),
}
