use std::any::Any;

pub(crate) fn must_downcast_box<T: 'static>(value: Box<dyn Any>) -> T {
    *value.downcast::<T>().unwrap()
}

pub(crate) fn must_downcast_ref<T: 'static>(value: &dyn Any) -> &T {
    value.downcast_ref::<T>().unwrap()
}
