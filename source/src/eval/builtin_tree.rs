use {
    crate::langhelp::{
        BindingId,
        Type,
        Unique,
    },
    std::rc::Rc,
};

pub(crate) enum BuiltinNode<'s> {
    Branch(Vec<(&'static str, BuiltinNode<'s>)>),
    Leaf(BuiltinLeaf<'s>),
}

pub(crate) enum BuiltinLeaf<'s> {
    Function {
        dynamic_key: BindingId,
        type_: Rc<dyn Type>,
        dynamic_value: v8::Local<'s, v8::Value>,
    },
    Interface {
        dynamic_key: BindingId,
        type_: Rc<dyn Type>,
        dynamic_value: v8::Local<'s, v8::Value>,
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
    AnyConst {
        type_: Rc<dyn Type>,
    },
    InitialBind {
        dynamic_key: BindingId,
        type_: Rc<dyn Type>,
    },
}
