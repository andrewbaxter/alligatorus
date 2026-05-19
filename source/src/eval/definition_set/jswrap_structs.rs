use {
    super::{
        DefinitionSet,
        DefinitionSetDataMethodData,
        DefinitionSetDataTypeConstructor,
        DefinitionSetDataTypeData,
        DefinitionSetDataTypeRef,
        DefinitionSetFunction,
        DefinitionSetFunctionData,
        next_data_type_identity,
    },
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectField,
            AspectJsStackValue,
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_string_get,
        },
        jscode::{
            JsCode,
            JsCodeType,
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
            Type,
            Unique,
            Value,
        },
    },
    crate::eval::{
        Erb,
        jswrap::{
            JsWrap,
            StagedExpr,
            jswrap_method_name,
        },
        mortar_function::MortarTypeFunction,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
        },
        rc_to_v8,
        rc_from_v8,
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        mem,
        rc::Rc,
    },
};

jswrap!(struct DefineResult(self_, scope) {
    data { ds: Rc<Gc<DefinitionSet>> }
    fn new_func() -> NewFuncResult {
        let ds = self_.ds.clone();
        let idx = {
            let mut functions = ds.functions.borrow_mut();
            let idx = functions.len();
            functions.push(DefinitionSetFunctionData {
                impl_expr: RefCell::new(None),
                arg_type_expr: RefCell::new(None),
                ret_type_expr: RefCell::new(None),
                scope_entry_id: Cell::new(None),
                arg_scope_entry_id: Cell::new(None),
            });
            idx
        };
        let func_ref = Rc::new(DefinitionSetFunction {
            set: (*ds).clone(),
            func_index: idx,
        });
        NewFuncResult { func_ref }
    }
    fn new_data() -> NewDataResult {
        let ds = self_.ds.clone();
        let identity = next_data_type_identity();
        let unique_field = format!("_{}", ds.next_field_id.next());
        let data_field = format!("_{}", ds.next_field_id.next());
        let idx = {
            let mut data_types = ds.data_types.borrow_mut();
            let idx = data_types.len();
            data_types.push(DefinitionSetDataTypeData {
                underlying_type_expr: RefCell::new(None),
                methods: RefCell::new(Vec::new()),
                identity,
                data_field,
                unique_field,
            });
            idx
        };
        let data_type_ref = Rc::new(DefinitionSetDataTypeRef {
            set: (*ds).clone(),
            data_type_index: idx,
        });
        NewDataResult { data_type_ref }
    }
});

jswrap!(struct NewFuncResult(self_, scope) {
    data { func_ref: Rc<DefinitionSetFunction> }
    field func: Rc<DefinitionSetFunction> { self_.func_ref.clone() }
    fn impl_(staged: StagedExpr) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut impl_expr = func_data.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            drop(functions);
            let msg = v8::String::new(scope, "Function already has an implementation").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *impl_expr = Some(staged.0);
    }
    fn arg_(staged: StagedExpr) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut arg_type_expr = func_data.arg_type_expr.borrow_mut();
        if arg_type_expr.is_some() {
            drop(arg_type_expr);
            drop(functions);
            let msg = v8::String::new(scope, "Function already has an arg type").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *arg_type_expr = Some(staged.0);
    }
    fn ret_(staged: StagedExpr) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut ret_type_expr = func_data.ret_type_expr.borrow_mut();
        if ret_type_expr.is_some() {
            drop(ret_type_expr);
            drop(functions);
            let msg = v8::String::new(scope, "Function already has a ret type").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *ret_type_expr = Some(staged.0);
    }
});

// --- Data type JsWrap structs ---

jswrap!(struct NewDataResult(self_, scope) {
    data { data_type_ref: Rc<DefinitionSetDataTypeRef> }
    field type_: Rc<DefinitionSetDataTypeRef> { self_.data_type_ref.clone() }
    field new_: Rc<DefinitionSetDataTypeConstructor> {
        Rc::new(DefinitionSetDataTypeConstructor {
            set: self_.data_type_ref.set.clone(),
            data_type_index: self_.data_type_ref.data_type_index,
        })
    }
    fn set_underlying(staged: StagedExpr) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        let mut underlying = dt.underlying_type_expr.borrow_mut();
        if underlying.is_some() {
            drop(underlying);
            drop(data_types);
            let msg = v8::String::new(scope, "Data type already has an underlying type").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *underlying = Some(staged.0);
    }
    fn new_func(name: String) -> NewDataMethodResult {
        let ds_ref = &self_.data_type_ref;
        let field_name = format!("_{}", ds_ref.set.next_field_id.next());
        let method_index = {
            let data_types = ds_ref.set.data_types.borrow();
            let dt = &data_types[ds_ref.data_type_index];
            let mut methods = dt.methods.borrow_mut();
            let idx = methods.len();
            methods.push(DefinitionSetDataMethodData {
                name,
                impl_expr: RefCell::new(None),
                ret_type_expr: RefCell::new(None),
                scope_entry_id: Cell::new(None),
                self_scope_entry_id: Cell::new(None),
                field_name,
            });
            idx
        };
        NewDataMethodResult {
            data_type_ref: self_.data_type_ref.clone(),
            method_index,
        }
    }
});

jswrap!(struct NewDataMethodResult(self_, scope) {
    data { data_type_ref: Rc<DefinitionSetDataTypeRef>, method_index: usize }
    fn impl_(staged: StagedExpr) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        let methods = dt.methods.borrow();
        let method = &methods[self_.method_index];
        let mut impl_expr = method.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            drop(methods);
            drop(data_types);
            let msg = v8::String::new(scope, "Method already has an implementation").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *impl_expr = Some(staged.0);
    }
    fn ret_(staged: StagedExpr) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        let methods = dt.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type_expr = method.ret_type_expr.borrow_mut();
        if ret_type_expr.is_some() {
            drop(ret_type_expr);
            drop(methods);
            drop(data_types);
            let msg = v8::String::new(scope, "Method already has a ret type").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *ret_type_expr = Some(staged.0);
    }
});

// --- JsWrap for Rc<DefinitionSetDataTypeRef> ---

impl JsWrap for Rc<DefinitionSetDataTypeRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetDataTypeRef)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc<DefinitionSetDataTypeConstructor> ---

impl JsWrap for Rc<DefinitionSetDataTypeConstructor> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetDataTypeConstructor)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc<DefinitionSetFunction> ---

impl JsWrap for Rc<DefinitionSetFunction> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetFunction)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}
