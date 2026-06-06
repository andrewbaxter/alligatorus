pub(crate) fn throw_error(scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>, msg: &str) {
    let msg = v8::String::new(scope, msg).unwrap();
    let exception = v8::Exception::error(scope, msg);
    scope.throw_exception(exception);
}
