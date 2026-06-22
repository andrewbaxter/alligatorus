use std::{
    any::Any,
    cell::RefCell,
};

pub(crate) struct DeserialLazy<T>(RefCell<Option<T>>);

impl<T> DeserialLazy<T> {
    pub fn get(&self) -> std::cell::Ref<'_, T> {
        std::cell::Ref::map(self.0.borrow(), |opt| opt.as_ref().expect("DeserialLazy: value not yet set"))
    }

    pub fn new() -> Self {
        DeserialLazy(RefCell::new(None))
    }

    pub fn set(&self, value: T) {
        let mut inner = self.0.borrow_mut();
        assert!(inner.is_none(), "DeserialLazy: value already set");
        *inner = Some(value);
    }
}

pub(crate) fn must_downcast_box<T: 'static>(value: Box<dyn Any>) -> T {
    *value.downcast::<T>().unwrap()
}

pub(crate) fn must_downcast_ref<T: 'static>(value: &dyn Any) -> &T {
    value.downcast_ref::<T>().unwrap()
}
