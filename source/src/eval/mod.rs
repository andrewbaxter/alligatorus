#[macro_use]
pub mod dynamic_wrap;
#[macro_use]
pub mod util;
pub mod aspect;
pub mod code_value_extern;
pub mod definition_set;
pub mod dynamic_util;
pub mod expr;
pub mod expr_artifact;
pub mod import_install;
pub mod isolate_bundle;
pub mod user_defined_scope_dispatch;
pub mod user_defined_type_dispatch;
pub mod value;

use {
    core::mem,
    crate::{
        builtin::{
            AspectLiftDynamic,
            aspect_artifact_get,
            aspect_bind_dynamic_get_maybe,
            aspect_bind_get,
            aspect_bool_const_get_maybe,
            aspect_consume_dynamic_get,
            aspect_field_get,
            aspect_function_get,
            aspect_initial_bind_get,
            aspect_iterable_get,
            aspect_lift_dynamic_get,
            aspect_operator_add_get,
            aspect_operator_eq_get,
            aspect_operator_lt_get,
            aspect_set_get,
            aspect_stack_value_dynamic_get,
            aspect_string_const_get_maybe,
            aspect_tagged_union_get,
            aspect_type_get,
        },
        code_dynamic::{
            BINDING_PREFIX,
            BUILTIN_PREFIX,
            CodeDynamic,
            CodeDynamicBind,
            CodeDynamicBindingUse,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicCall,
            CodeDynamicIf,
            CodeDynamicMatch,
            CodeDynamicMatchBranch,
            CodeDynamicNewArray,
            CodeDynamicNewObject,
            CodeDynamicType,
            DEF_PREFIX,
            EXPR_PREFIX,
            TAG_VAR,
        },
        eval::dynamic_util::wrap_with_carry,
        invariant::must_downcast_ref,
        langhelp::{
            Code,
            CodeValue,
            Code_,
            CompileId,
            CompileImportContext,
            CompileRes,
            CompileResResult,
            CompileResultEntry,
            ConstKey,
            Counter,
            DefSetId,
            EResult,
            EvalContext,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            External,
            GlobalContext,
            ImportStateId,
            LogEntry,
            QExprId,
            Scope,
            ScopeEntry,
            ScopeEntryId,
            SourceId,
            Type,
            Value,
            downcast_value,
            make_builtin_deserializers,
        },
        syntax::{
            ExprId,
            SourceSpecAbsolute,
        },
    },
    definition_set::DefinitionSet,
    dumpster::unsync::Gc,
    dynamic_wrap::DynamicWrap,
    expr::{
        Expr,
        ExprAs,
        ExprBind,
        ExprBlock,
        ExprCall,
        ExprField,
        ExprForeach,
        ExprLower,
        ExprMatch,
        ExprMatchBranch,
        ExprSet,
        ExprStage,
        ExprTuple,
        Expr_,
        Module,
    },
    flowcontrol::shed,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        collections::HashMap,
        rc::Rc,
        sync::{
            Arc,
            OnceLock,
        },
    },
    v8::tc_scope,
    value::{
        record::{
            Record,
            RecordField,
        },
        tuple::{
            Tuple,
            TupleElement,
        },
        type_function::dynamic::TypeFunctionDynamic,
        type_null::dynamic::TypeNullDynamic,
        type_number::dynamic::TypeNumberDynamic,
        type_string::dynamic::TypeStringDynamic,
        value_null::r#const::ValueNullConst,
    },
};

/// Identity for the builtin `value` interface (every value must implement
/// `type()`). Stable across compile runs.
pub(crate) fn builtin_value_iface_identity() -> crate::langhelp::Unique {
    crate::langhelp::Unique::Builtin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

/// Identity for the builtin `type` interface (when implemented, instances of
/// the implementing data type act as types).
pub(crate) fn builtin_type_iface_identity() -> crate::langhelp::Unique {
    crate::langhelp::Unique::Builtin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

/// Well-known scope entry ID for __record_to_type mortar function.
pub(crate) const RECORD_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for the __stage function (not a real scope entry).
const STAGE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for __tuple_to_type mortar function.
pub(crate) const TUPLE_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__cve_get_value` mortar function — narrow
/// extractor that takes a CodeValueExtern and returns the v8 representation
/// of the wrapped CodeValue's `value` slot. For a `ValueUserDefined`, that's
/// the underlying JS instance object (the same `bundle.instance` the value
/// holds); other `Value` shapes are not supported because no test exercises
/// them yet (we'll handle them when they appear). No `AspectField` dispatch
/// happens here — callers compose further access through normal source-level
/// typing.
pub(crate) const CVE_GET_VALUE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__value_get_type` — invoked at v8 time on
/// a value JS instance (the output of `__cve_get_value`). Calls the user's
/// compiled `value.type` impl (`_im_v0`) to obtain the source-language type
/// as a `v8::External` wrapping `Rc<dyn Type>`, then re-wraps it as a
/// `TypeRefV8` JS object so source-level `.aspect(unique)` lookup works on
/// the result (typed `TypeTypeRefV8Dynamic`).
pub(crate) const VALUE_GET_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__aspect_lookup` — the runtime hook that
/// powers the source-level `aspect(base, key)` builtin. Takes a `TypeRefV8`
/// JS object (the result of `value.type`) and a `Unique` external (the iface
/// identity), invokes `type_ref.aspect(&xx, &unique)` in Rust, and returns
/// either `{ "\"some\"": <bundle.instance> }` or `{ "\"none\"": null }` —
/// matching the JS tagged-union encoding the source-level `option(T)` type
/// expects for `some`/`none` variants.
pub(crate) const ASPECT_LOOKUP_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__cast_value` rust-in-v8 function — unwraps
/// an `Rc<dyn Value>` external to its concrete `ValueUserDefined` form,
/// asserting that the value's data type matches the supplied
/// `TypeSourceDataTypeConst` template. Panics on mismatch. Returns the
/// `ValueUserDefined`'s `bundle.instance` — i.e. the JS object representing
/// the user-defined data type instance — so source-level code can drive
/// field/method access on it normally.
pub(crate) const CAST_VALUE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for __cve_extract mortar function — extracts the
/// raw runtime JS value from a CodeValueExtern by consuming the wrapped
/// CodeValue's code (or const value) into a JS value.
pub(crate) const CVE_EXTRACT_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__data_type_ref_to_type` rust-in-v8 function
/// — `TypeSourceDataTypeRefDynamic::cast_to` (when target is `TypeAnyDynamic`
/// or another type-of-types) emits a call to this function. It reads the
/// `Rc<SourceDataTypeRef>` from the v8 value (the ref's own representation)
/// and returns a `Rc<dyn Type>` (`TypeSourceDataTypeConst`) — the
/// representation `TypeAnyDynamic` expects.
pub(crate) const DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__tagged_union_ref_to_type` rust-in-v8
/// function — parallel to `__data_type_ref_to_type`, for tagged union refs.
pub(crate) const TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__interface_ref_to_type` rust-in-v8 function.
pub(crate) const INTERFACE_REF_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__function_const_to_type` rust-in-v8
/// function — for `Rc<TypeSourceFunctionConst>`.
pub(crate) const FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__data_type_constructor_to_type` rust-in-v8
/// function — for `Rc<TypeSourceDataTypeConstructorConst>`.
pub(crate) const DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__tagged_union_constructor_to_type`
/// rust-in-v8 function — for `Rc<SourceTaggedUnionConstructorRef>`.
pub(crate) const TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__data_instance_to_type` rust-in-v8
/// function. Wraps a source-defined data type *instance* (the JS object)
/// together with its compile-time-known `SourceDataTypeRef` into an
/// `Rc<dyn Type>` external (a `TypeSourceDataTypeConst` whose
/// `isolate_bundle` is the v8 instance handle). Used by
/// `TypeSourceDataTypeDynamic::cast_to` when the destination is the
/// `type` interface (so the instance acts as a `Rc<dyn Type>` for
/// downstream `__record_to_type` / `__tuple_to_type` shallow extraction).
pub(crate) const DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__data_instance_to_target` — parallel to
/// `__data_instance_to_type` but produces a `v8::External` wrapping
/// `Rc<dyn Target>` (a `UserDefinedTarget`) instead of a Type. Used by
/// `TypeSourceDataTypeDynamic::cast_to` when the destination interface is
/// the builtin `target` interface.
pub(crate) const DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__data_instance_to_scope_entry` — parallel
/// to `__data_instance_to_type` but produces a `v8::External` wrapping
/// `Rc<dyn ScopeEntry>` (a `UserDefinedScopeEntry`) instead of a Type. Used
/// by `TypeSourceDataTypeDynamic::cast_to` when the destination interface is
/// the builtin `scope_entry` interface.
pub(crate) const DATA_INSTANCE_TO_SCOPE_ENTRY_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__data_instance_to_value` — parallel to
/// `__data_instance_to_type` but produces a `v8::External` wrapping
/// `Rc<dyn Value>` (a `ValueUserDefined`) instead of a Type. Used by
/// `TypeSourceDataTypeDynamic::cast_to` when the destination interface is
/// the builtin `value` interface — so the downstream Rust dispatch can call
/// the user's `value.type` impl at v8 to recover the value's source-language
/// type for further `aspect_field` / `aspect_function` routing.
pub(crate) const DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__data_instance_to_aspect` — wraps the v8
/// instance in an `AspectUserDefined` external (a `Value` impl that also
/// satisfies `AspectField` / `AspectFunction` by dispatching the trait
/// methods to the instance's interface methods). Used by
/// `TypeSourceDataTypeDynamic::cast_to` when the destination is
/// `TypeAspectDynamic` (the source-level `aspect` builtin type).
pub(crate) const DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__data_instance_to_code` — parallel to
/// `__data_instance_to_value`/_type/_aspect, but wraps the v8 instance into
/// a `Rc<dyn CodeDyn>=CodeUserDefined` external. Used by
/// `TypeSourceDataTypeDynamic::cast_to` when the destination is the builtin
/// `code_dyn` interface, so source data types impl'ing `code_dyn` can be
/// passed as the `code` slot of `new_code_value`.
pub(crate) const DATA_INSTANCE_TO_CODE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for `__unique_eq(a, b)` — Rust-in-v8 helper that
/// reads two v8::External(Unique) values and returns boolean value equality.
/// Used by `TypeUnique::eq` when emitting dynamic comparisons: JS `===` on
/// externals is reference equality, but Uniques are produced as fresh
/// externals at each conversion site, so we need value comparison.
pub(crate) const UNIQUE_EQ_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// `CodeDyn` wrapper around a v8 instance of a source data type that
/// implements the builtin `code_dyn` interface. Parallel to `ValueUserDefined`
/// (for the `value` interface) and `TypeUserDefined` (for the `type` interface).
/// Produced by the `__data_instance_to_code` rust-in-v8 callback.
pub(crate) struct CodeUserDefined {
    #[allow(dead_code)]
    pub(crate) bundle: Rc<crate::eval::isolate_bundle::IsolateBundle>,
}

impl crate::langhelp::CodeDyn for CodeUserDefined { }

pub(crate) trait Target {
    fn bind(&self, xx: &ExprContext, loc: QExprId, sub: TargetBindSub) -> (Code, ScopeEntryId);
    fn bind_sub_record(&self, loc: QExprId, carry: Code, fields: Vec<(ConstKey, TargetBindSub)>) -> TargetBindSub;
    fn bind_sub_tuple(&self, loc: QExprId, carry: Code, elements: Vec<TargetBindSub>) -> TargetBindSub;
    fn block(&self, loc: QExprId, entries: Vec<Code>) -> EResult<Code>;
    fn code_to_bind_sub(&self, loc: QExprId, code: Code, type_: Rc<dyn Type>) -> EResult<TargetBindSub>;
    fn if_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        conditions: Vec<(CodeValue, &Expr)>,
        else_: Option<&Expr>,
    ) -> EvalRes;
    fn match_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ConstKey,
        cases: Vec<(String, Rc<dyn Type>, &Expr)>,
        fallback: Option<&Expr>,
    ) -> EvalRes;
    fn maybe_bind_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>>;
    fn stage(&self, xx: &ExprContext, loc: QExprId, tree: Expr, lower_values: Vec<CodeValue>) -> EvalRes;
}

pub(crate) fn compile_importid(
    fx: &Arc<GlobalContext>,
    import_state: ImportStateId,
    source: Module,
    compile_path: Vec<(CompileId, ExprId)>,
) -> CompileRes {
    let mut root_expr = match source {
        Module::V1(e) => e,
    };
    let root_loc = root_expr.loc;
    let cix = Rc::new(CompileImportContext {
        gx: fx.clone(),
        compile_path: compile_path,
        builtin_deserializers: make_builtin_deserializers(),
        deserialized_artifacts: RefCell::new(HashMap::new()),
        pending_finalizers: RefCell::new(Vec::new()),
        isolate: Rc::new(RefCell::new(v8::Isolate::new(Default::default()))),
        import_state: import_state,
        definition_sets: RefCell::new(Vec::new()),
        def_set_counter: Counter::default(),
        v8_context: RefCell::new(None),
        builtins_registered: Cell::new(false),
        source_id: root_loc.0,
        builtin_scope_entries: RefCell::new(Vec::new()),
    });

    // Step 1: Count lower excess and wrap root in stage layers Root is treated as
    // implicit stage (stage_depth=1)
    fn max_lower_excess(expr: &Expr, stage_depth: usize, lower_depth: usize) -> usize {
        //! Walk the expression tree counting stage/lower nesting depths. Returns the max
        //! (lower_depth - stage_depth) where lower_depth > stage_depth. This tells us how
        //! many extra `stage` wrappers are needed at the root.
        fn recurse_children(expr: &Expr, sd: usize, ld: usize) -> usize {
            match &expr.r#type {
                Expr_::Lower(e) => {
                    let new_ld = ld + 1;
                    let excess = new_ld.saturating_sub(sd);
                    excess.max(max_lower_excess(&e.expr, sd, new_ld))
                },
                Expr_::Stage(e) => max_lower_excess(&e.expr, sd + 1, ld),
                Expr_::Graft(e) => max_lower_excess(&e.expr, sd, ld),
                Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => 0,
                Expr_::As(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.target, sd, ld))
                },
                Expr_::Call(c) => {
                    max_lower_excess(&c.func, sd, ld).max(max_lower_excess(&c.arg, sd, ld))
                },
                Expr_::Bind(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.value, sd, ld))
                },
                Expr_::Set(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.value, sd, ld))
                },
                Expr_::Field(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.key, sd, ld))
                },
                Expr_::Block(e) => {
                    e.exprs.iter().map(|sub| max_lower_excess(sub, sd, ld)).max().unwrap_or(0)
                },
                Expr_::Tuple(t) => {
                    t.elements.iter().map(|e| max_lower_excess(e, sd, ld)).max().unwrap_or(0)
                },
                Expr_::Record(r) => {
                    r.entries.iter().map(|(k, v)| {
                        max_lower_excess(k, sd, ld).max(max_lower_excess(v, sd, ld))
                    }).max().unwrap_or(0)
                },
                Expr_::If(e) => {
                    let mut m = 0;
                    for b in &e.conditions {
                        m = m.max(max_lower_excess(&b.condition, sd, ld));
                        m = m.max(max_lower_excess(&b.body, sd, ld));
                    }
                    if let Some(el) = &e.else_ {
                        m = m.max(max_lower_excess(el, sd, ld));
                    }
                    m
                },
                Expr_::OperatorLt(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.reference, sd, ld))
                },
                Expr_::OperatorEq(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.reference, sd, ld))
                },
                Expr_::OperatorAdd(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.reference, sd, ld))
                },
                Expr_::Foreach(e) => {
                    max_lower_excess(&e.base, sd, ld)
                        .max(max_lower_excess(&e.element_key, sd, ld))
                        .max(max_lower_excess(&e.body, sd, ld))
                },
                Expr_::Match(e) => {
                    let mut m = max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.element_key, sd, ld));
                    for c in &e.cases {
                        m = m.max(max_lower_excess(&c.condition, sd, ld));
                        m = m.max(max_lower_excess(&c.body, sd, ld));
                    }
                    if let Some(f) = &e.fallback {
                        m = m.max(max_lower_excess(f, sd, ld));
                    }
                    m
                },
            }
        }

        recurse_children(expr, stage_depth, lower_depth)
    }

    let excess = max_lower_excess(&root_expr, 0, 0);
    for _ in 0 .. excess {
        root_expr = Expr {
            loc: root_loc,
            r#type: Expr_::Stage(ExprStage { expr: Rc::new(root_expr) }),
        };
    }

    // Step 2: Repeatedly eval until root is no longer a Stage, then once more.
    //
    // The shared v8 isolate (created in `cix.isolate`) spans all stages —
    // slots and builtin globals are set per-stage but `v8::Global<...>`
    // handles captured during one stage remain valid in later stages. This
    // sidesteps v8's strict reverse-creation drop ordering for multiple
    // `OwnedIsolate` instances, which would otherwise be triggered by
    // per-stage isolates outliving their stage frames via persistent
    // bundles.
    let mut all_logs = Vec::new();
    loop {
        let is_stage = matches!(&root_expr.r#type, Expr_::Stage(_));
        let eval_result = match eval(&cix, import_state, &root_expr) {
            Ok(v) => v,
            Err(errors) => return CompileRes {
                logs: all_logs,
                generated_mortar_scripts: vec![],
                result: CompileResResult::Err(errors),
            },
        };
        let eval_value = eval_result.value;
        all_logs.extend(eval_result.logs);
        if !is_stage {
            // This was the final eval (root was not a stage) Serialize result
            let logs = all_logs;
            let ex = Rc::new(EvalContext {
                ix: cix.clone(),
                next_local_id: Default::default(),
                externals: RefCell::new(Vec::new()),
                v8_scope: std::cell::Cell::new(None),
                v8_dispatcher: std::cell::Cell::new(None),
            });
            let xx = Rc::new(ExprContext {
                vx: ex,
                import_state: Rc::new(Cell::new(import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                target: Rc::new(TargetDynamic),
            });
            let value_type = eval_value.type_(&xx, root_loc);
            let value_serial = match aspect_artifact_get(&xx, root_loc, value_type.as_ref()).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    generated_mortar_scripts: vec![],
                    result: CompileResResult::Err(e),
                },
            };
            let value = match value_serial.to_serial(&xx, root_loc, eval_value).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    generated_mortar_scripts: vec![],
                    result: CompileResResult::Err(e),
                },
            };
            let generated_mortar_scripts = mem::take(&mut *fx.generated_mortar_scripts.lock().unwrap());
            return CompileRes {
                logs,
                generated_mortar_scripts,
                result: CompileResResult::Ok(value),
            };
        }

        // Result is an Expr (from stage eval) - use it as next expression
        let next_expr = Rc::try_unwrap(downcast_value::<Expr>(eval_value)).ok().expect("Stage eval Expr: Rc not unique");
        root_expr = next_expr;
    }
}

pub(crate) fn const_eval(xx: &ExprContext, expr: &Expr) -> EvalRes {
    match &expr.r#type {
        Expr_::Literal(p) => {
            return EvalRes {
                v: Some(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None,
                    },
                    value: p.consume(),
                }),
                errors: None,
            };
        },
        Expr_::ScopeUse(p) => return er_seq_cv!(expr.loc, erb, {
            let key = erb.must_er(const_eval(xx, p.key.as_ref()))?;
            let constkey = erb.must_res(extract_const_key(xx, expr.loc, key.value.as_ref()))?;
            let scope = shed!{
                'found _;
                let mut at = &xx.scope;
                loop {
                    if at.values.borrow().contains_key(&constkey) {
                        break 'found at;
                    }
                    if let Some(parent) = &at.parent {
                        at = parent;
                    } else {
                        break;
                    }
                }
                return erb.err_new(format!("No bound value with id [{:?}]", constkey));
            };
            let scope = scope.values.borrow();
            let binding = scope.get(&constkey).unwrap();
            return Ok(binding.use_(xx, expr.loc, key.code));
        }),
        Expr_::Call(p) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                func = const_eval(xx, p.func.as_ref()),
                arg = const_eval(xx, p.arg.as_ref()),
            });
            let func_type = func.value.type_(xx, expr.loc);
            let aspect_func = erb.must_er(aspect_function_get(xx, expr.loc, func_type.as_ref()))?;
            let v = erb.must_er(aspect_func.call(xx, expr.loc, func, arg))?;
            return Ok(v);
        }),
        Expr_::Set(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                value = const_eval(xx, e.value.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let base_set = erb.must_er(aspect_set_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(base_set.set(xx, expr.loc, base, value))?;
            return Ok(v);
        }),
        Expr_::Field(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                field = const_eval(xx, e.key.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_field = erb.must_er(aspect_field_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_field.field(xx, expr.loc, base, field))?;
            return Ok(v);
        }),
        Expr_::Bind(_) => {
            return EvalRes::err(expr.loc, format!("Bind expression must be directly inside a block"));
        },
        Expr_::Block(e) => {
            let child_scope = Rc::new(Scope {
                values: RefCell::new(HashMap::new()),
                parent: Some(xx.scope.clone()),
            });
            let child_xx = ExprContext {
                vx: xx.vx.clone(),
                import_state: xx.import_state.clone(),
                scope: child_scope,
                target: xx.target.clone(),
            };
            return er_seq_cv!(expr.loc, erb, {
                let mut entries = vec![];
                let mut last_val = None;
                for sub in &e.exprs {
                    if let Expr_::Bind(bind) = &sub.r#type {
                        er_all!(erb, {
                            base = const_eval(&child_xx, bind.base.as_ref()),
                            value = const_eval(&child_xx, bind.value.as_ref()),
                        });
                        let base_constkey = erb.must_res(extract_const_key(&child_xx, sub.loc, base.value.as_ref()))?;
                        entries.push(base.code);
                        let value_type = value.value.type_(&child_xx, sub.loc);
                        let bind_aspect = erb.must_er(aspect_bind_get(&child_xx, sub.loc, value_type.as_ref()))?;
                        let binding = erb.must_er(bind_aspect.bind(&child_xx, sub.loc, value))?;
                        entries.push(binding.0);
                        child_xx.scope.values.borrow_mut().insert(base_constkey, binding.1);
                        last_val = Some(Rc::new(ValueNullConst) as Rc<dyn Value>);
                    } else {
                        let r = erb.must_er(const_eval(&child_xx, sub))?;
                        entries.push(r.code);
                        last_val = Some(r.value);
                    }
                }
                return Ok(CodeValue {
                    code: erb.must_er(xx.target.block(expr.loc, entries))?,
                    value: last_val.unwrap_or_else(|| Rc::new(ValueNullConst) as Rc<dyn Value>)
                });
            });
        },
        Expr_::Stage(e) => {
            fn find_matched_lowers_inner<'a>(expr: &'a Expr, sd: usize, ld: usize, out: &mut Vec<&'a Expr>) {
                match &expr.r#type {
                    Expr_::Lower(e) => {
                        let new_ld = ld + 1;
                        if new_ld - sd == 1 {
                            // Matched! Collect the inner expression. Don't descend further.
                            out.push(&e.expr);
                        } else {
                            find_matched_lowers_inner(&e.expr, sd, new_ld, out);
                        }
                    },
                    Expr_::Stage(e) => find_matched_lowers_inner(&e.expr, sd + 1, ld, out),
                    Expr_::Graft(e) => find_matched_lowers_inner(&e.expr, sd, ld, out),
                    Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => { },
                    Expr_::As(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.target, sd, ld, out);
                    },
                    Expr_::Call(c) => {
                        find_matched_lowers_inner(&c.func, sd, ld, out);
                        find_matched_lowers_inner(&c.arg, sd, ld, out);
                    },
                    Expr_::Bind(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.value, sd, ld, out);
                    },
                    Expr_::Set(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.value, sd, ld, out);
                    },
                    Expr_::Field(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.key, sd, ld, out);
                    },
                    Expr_::Block(e) => {
                        for sub in &e.exprs {
                            find_matched_lowers_inner(sub, sd, ld, out);
                        }
                    },
                    Expr_::Tuple(t) => {
                        for e in &t.elements {
                            find_matched_lowers_inner(e, sd, ld, out);
                        }
                    },
                    Expr_::Record(r) => {
                        for (k, v) in &r.entries {
                            find_matched_lowers_inner(k, sd, ld, out);
                            find_matched_lowers_inner(v, sd, ld, out);
                        }
                    },
                    Expr_::If(e) => {
                        for b in &e.conditions {
                            find_matched_lowers_inner(&b.condition, sd, ld, out);
                            find_matched_lowers_inner(&b.body, sd, ld, out);
                        }
                        if let Some(el) = &e.else_ {
                            find_matched_lowers_inner(el, sd, ld, out);
                        }
                    },
                    Expr_::OperatorLt(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.reference, sd, ld, out);
                    },
                    Expr_::OperatorEq(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.reference, sd, ld, out);
                    },
                    Expr_::OperatorAdd(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.reference, sd, ld, out);
                    },
                    Expr_::Foreach(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.element_key, sd, ld, out);
                        find_matched_lowers_inner(&e.body, sd, ld, out);
                    },
                    Expr_::Match(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.element_key, sd, ld, out);
                        for c in &e.cases {
                            find_matched_lowers_inner(&c.condition, sd, ld, out);
                            find_matched_lowers_inner(&c.body, sd, ld, out);
                        }
                        if let Some(f) = &e.fallback {
                            find_matched_lowers_inner(f, sd, ld, out);
                        }
                    },
                }
            }

            let mut matched_lower_exprs = Vec::new();
            find_matched_lowers_inner(&e.expr, 0, 0, &mut matched_lower_exprs);
            if matched_lower_exprs.is_empty() {
                // No matched lowers - return the inner tree as-is
                return EvalRes::ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None,
                    },
                    value: Rc::new(e.expr.as_ref().clone()),
                });
            }

            // half_eval each matched lower's inner expression
            return er_seq_cv!(expr.loc, erb, {
                let mut lower_values = Vec::new();
                for lower_inner in &matched_lower_exprs {
                    let cv = erb.must_er(const_eval(xx, lower_inner))?;
                    lower_values.push(cv);
                }
                let v = erb.must_er(xx.target.stage(xx, expr.loc, e.expr.as_ref().clone(), lower_values))?;
                return Ok(v);
            });
        },
        Expr_::Lower(e) => {
            // Transparently evaluate inner expression
            return const_eval(xx, &e.expr);
        },
        Expr_::Graft(e) => {
            // Outside of stage processing, Graft just transparently evaluates its inner
            // expression — like Lower. Inside a stage, the stage processor handles the
            // graft specially (the inner is evaluated in the current context and its
            // EvalRes is embedded in the staged tree as-is).
            return const_eval(xx, &e.expr);
        },
        Expr_::Tuple(t) => {
            let elements: Vec<TupleElement> =
                t.elements.iter().map(|e| TupleElement::EvalRes(const_eval(xx, e))).collect();
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None,
                },
                value: Rc::new(Tuple {
                    base: None,
                    elements,
                }),
            });
        },
        Expr_::Record(r) => {
            let mut entries = std::collections::BTreeMap::new();
            let mut key_codes = vec![];
            let mut errors = vec![];
            for (k_expr, v_expr) in &r.entries {
                let k_res = const_eval(xx, k_expr);
                if let Some(errs) = k_res.errors {
                    errors.extend(errs);
                }
                let Some(k_cv) = k_res.v else {
                    continue;
                };
                if let Some(js) = k_cv.code.cast::<CodeDynamic>().v.flatten() {
                    key_codes.push(js.clone());
                }
                match extract_const_key(xx, expr.loc, k_cv.value.as_ref()) {
                    Ok(key) => {
                        entries.insert(key, const_eval(xx, v_expr));
                    },
                    Err(e) => errors.push(ExprErr {
                        loc: expr.loc,
                        errors: vec![ExprSubErr::Opaque(e)],
                    }),
                }
            }
            if !errors.is_empty() {
                return EvalRes {
                    v: None,
                    errors: Some(errors),
                };
            }
            let code = if key_codes.is_empty() {
                Code {
                    loc: expr.loc,
                    code: Code_::None,
                }
            } else {
                let block_entries = key_codes.into_iter().map(CodeDynamicBlockEntry::Code).collect();
                Code {
                    loc: expr.loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc: expr.loc,
                        type_: CodeDynamicType::Block(CodeDynamicBlock { entries: block_entries }),
                    })),
                }
            };
            let fields = entries.into_iter().map(|(k, v)| (k, RecordField::EvalRes(v))).collect();
            return EvalRes::ok(CodeValue {
                code,
                value: Rc::new(Record {
                    base: None,
                    fields,
                }),
            });
        },
        Expr_::OperatorLt(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                reference = const_eval(xx, e.reference.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_lt = erb.must_er(aspect_operator_lt_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_lt.lt(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::OperatorEq(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                reference = const_eval(xx, e.reference.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_eq = erb.must_er(aspect_operator_eq_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_eq.eq(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::OperatorAdd(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                reference = const_eval(xx, e.reference.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_add = erb.must_er(aspect_operator_add_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_add.add(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::Foreach(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                element_key_cv = const_eval(xx, e.element_key.as_ref()),
            });
            let element_key = erb.must_res(extract_const_key(xx, expr.loc, element_key_cv.value.as_ref()))?;
            let base_type = base.value.type_(xx, expr.loc);
            let iterable = erb.must_er(aspect_iterable_get(xx, expr.loc, base_type.as_ref()))?;

            // Create child scope with element bound, then eval body
            let element_scope_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
            let child_scope = Rc::new(Scope {
                values: RefCell::new(HashMap::new()),
                parent: Some(xx.scope.clone()),
            });
            let element_type = iterable.element_type();
            let element_ib = erb.must_er(aspect_initial_bind_get(xx, expr.loc, element_type.as_ref()))?;
            child_scope.values.borrow_mut().insert(element_key, element_ib.initial_bind(element_scope_id));
            let child_xx = ExprContext {
                vx: xx.vx.clone(),
                import_state: xx.import_state.clone(),
                scope: child_scope,
                target: xx.target.clone(),
            };
            let body = erb.must_er(const_eval(&child_xx, e.body.as_ref()))?;
            let v = erb.must_er(iterable.foreach(xx, expr.loc, base, element_scope_id, body))?;
            return Ok(v);
        }),
        Expr_::Match(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                element_key_cv = const_eval(xx, e.element_key.as_ref()),
            });
            let element_key = erb.must_res(extract_const_key(xx, expr.loc, element_key_cv.value.as_ref()))?;
            let base_type = base.value.type_(xx, expr.loc);
            let tagged_union = erb.must_er(aspect_tagged_union_get(xx, expr.loc, base_type.as_ref()))?;
            let variants = tagged_union.variants();

            // Validate case tags against variant tags
            let mut case_tags: Vec<ConstKey> = Vec::new();
            for case in &e.cases {
                let cond_cv = erb.must_er(const_eval(xx, &case.condition))?;
                let tag = erb.must_res(extract_const_key(xx, expr.loc, cond_cv.value.as_ref()))?;
                if !variants.iter().any(|(t, _, _)| t == &tag) {
                    return erb.err_new(format!("Match case tag '{:?}' not found in tagged union", tag));
                }
                case_tags.push(tag);
            }

            // Check exhaustiveness
            let is_exhaustive = variants.iter().all(|(vt, _, _)| case_tags.contains(vt));
            if !is_exhaustive && e.fallback.is_none() {
                for (variant_tag, _, _) in &variants {
                    if !case_tags.contains(variant_tag) {
                        return erb.err_new(format!("Non-exhaustive match: missing case for tag '{:?}'", variant_tag));
                    }
                }
            }
            if is_exhaustive && e.fallback.is_some() {
                return erb.err_new("Exhaustive match must not have a fallback");
            }

            // Delegate code generation to target
            let mut cases = Vec::new();
            for (i, case) in e.cases.iter().enumerate() {
                let tag = &case_tags[i];
                let (_, dynamic_tag, variant_type) = variants.iter().find(|(t, _, _)| t == tag).unwrap();
                cases.push((dynamic_tag.clone(), variant_type.clone(), &case.body));
            }
            let v = erb.must_er(xx.target.match_(xx, expr.loc, base, element_key, cases, e.fallback.as_deref()))?;
            return Ok(v);
        }),
        Expr_::Import(e) => return er_seq_cv!(expr.loc, erb, {
            let spec_cv = erb.must_er(const_eval(xx, &e.spec))?;
            let spec_str = erb.must_res(extract_const_key(xx, expr.loc, spec_cv.value.as_ref()))?;
            let spec_name = match spec_str {
                ConstKey::String(s) => s,
                _ => return erb.err_new("Import spec must be a string"),
            };
            let source_spec = SourceSpecAbsolute::Virtual(spec_name);
            let (source_id, source_module) = erb.must_res(xx.vx.ix.gx.sources.get(source_spec.clone()))?;
            let compile_id = CompileId(xx.import_state.get(), source_id);

            // Check if already compiling/compiled; spawn thread if new.
            // (v8 requires each isolate be entered by at most one thread, so
            // the imported module's compile runs on its own thread.)
            let entry = {
                let mut results = xx.vx.ix.gx.compile_results.lock().unwrap();
                if let Some(entry) = results.get(&compile_id) {
                    entry.clone()
                } else {
                    let entry = Arc::new(CompileResultEntry { result: std::sync::RwLock::new(None), });
                    let started = Arc::new((std::sync::Mutex::new(false), std::sync::Condvar::new()));
                    let started_clone = started.clone();
                    let gx = xx.vx.ix.gx.clone();
                    let import_state = xx.import_state.get();
                    let entry_clone = entry.clone();
                    std::thread::spawn(move || {
                        let mut write_guard = entry_clone.result.write().unwrap();
                        {
                            let (lock, cvar) = &*started_clone;
                            *lock.lock().unwrap() = true;
                            cvar.notify_one();
                        }
                        let module = crate::source_module_to_module(source_id, &source_module);
                        let result = compile_importid(&gx, import_state, module, vec![]);
                        *write_guard = Some(Ok(result));
                    });
                    let (lock, cvar) = &*started;
                    let mut started_flag = lock.lock().unwrap();
                    while !*started_flag {
                        started_flag = cvar.wait(started_flag).unwrap();
                    }
                    results.insert(compile_id, entry.clone());
                    entry
                }
            };

            let state = Rc::new(value::value_pending_import::PendingImportState {
                entry,
                gx: xx.vx.ix.gx.clone(),
            });
            // Imports of data types that implement the builtin `type`
            // interface must resolve eagerly against the importer's
            // `ExprContext` — re-compiling each interface impl's `impl_expr`
            // in this isolate (so its captured externals land in this
            // compile's `ex.externals`) and running an eager v8 script
            // that defines the methods and constructs the instance object
            // requires access to this compile's v8 scope and the same
            // builtin scope entries the originating compile saw. Other
            // imports use the lazy proxy as before, preserving existing
            // test fixtures.
            if state.imported_needs_v8_bundle() {
                return Ok(state.resolve(xx));
            }
            return Ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None,
                },
                value: Rc::new(value::value_pending_import::ValuePendingImport {
                    state,
                    resolved: RefCell::new(None),
                }),
            });
        }),
        Expr_::If(e) => return er_seq_cv!(expr.loc, erb, {
            // Try constant folding first: check if all conditions resolve to constant bools
            let mut conditions_evaluated = Vec::new();
            let mut all_const = true;
            for branch in &e.conditions {
                let cond = erb.must_er(const_eval(xx, &branch.condition))?;
                let cond_type = cond.value.type_(xx, expr.loc);
                if let Some(aspect_bool) = aspect_bool_const_get_maybe(xx, cond_type.as_ref()) {
                    let val = aspect_bool.get(cond.value.as_ref());
                    conditions_evaluated.push((val, cond, &branch.body));
                } else {
                    all_const = false;
                    conditions_evaluated.push((false, cond, &branch.body));
                }
            }
            if all_const {
                // Constant folding: find the first true branch
                for (val, _cond, body) in &conditions_evaluated {
                    if *val {
                        let body_cv = erb.must_er(const_eval(xx, body))?;
                        return Ok(body_cv);
                    }
                }

                // No true branch; use else or null
                if let Some(else_expr) = &e.else_ {
                    let else_cv = erb.must_er(const_eval(xx, else_expr))?;
                    return Ok(else_cv);
                }
                return Ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None
                    },
                    value: Rc::new(ValueNullConst),
                });
            } else {
                // Delegate code generation to target
                let conditions: Vec<(CodeValue, &Expr)> =
                    conditions_evaluated.into_iter().map(|(_, cond, body)| (cond, body as &Expr)).collect();
                let v = erb.must_er(xx.target.if_(xx, expr.loc, conditions, e.else_.as_deref(),))?;
                return Ok(v);
            }
        }),
        Expr_::As(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                target = const_eval(xx, e.target.as_ref()),
            });
            let target_meta_type = target.value.type_(xx, expr.loc);
            let aspect_type = erb.must_er(aspect_type_get(xx, expr.loc, target_meta_type.as_ref()))?;
            let target_type = aspect_type.type_(target.value.as_ref());
            let v = erb.must_er(typecheck_convert(xx, expr.loc, target_type.as_ref(), base))?;
            return Ok(v);
        }),
    }
}

/// EvalResBuilder
pub(crate) struct Erb {
    errors: Vec<ExprErr>,
    loc: QExprId,
}

impl Erb {
    pub(crate) fn err_new(&mut self, message: impl ToString) -> Result<CodeValue, ()> {
        self.errors.push(ExprErr {
            loc: self.loc,
            errors: vec![ExprSubErr::Opaque(message.to_string())],
        });
        return Err(());
    }

    pub(crate) fn err_push(&mut self, message: impl ToString) {
        self.errors.push(ExprErr {
            loc: self.loc,
            errors: vec![ExprSubErr::Opaque(message.to_string())],
        });
    }

    pub(crate) fn must_er<T>(&mut self, r: EResult<T>) -> Result<T, ()> {
        if let Some(errs) = r.errors {
            self.errors.extend(errs);
        }
        let Some(cv) = r.v else {
            return Err(());
        };
        return Ok(cv);
    }

    pub(crate) fn must_res<T>(&mut self, r: Result<T, String>) -> Result<T, ()> {
        match r {
            Ok(r) => {
                return Ok(r);
            },
            Err(e) => {
                self.err_push(e);
                return Err(());
            },
        }
    }

    pub(crate) fn new(loc: QExprId) -> Self {
        return Self {
            loc: loc,
            errors: Default::default(),
        };
    }
}

fn eval(
    cix: &Rc<CompileImportContext>,
    import_state: ImportStateId,
    expr: &Expr,
) -> Result<EvalResult, Vec<ExprErr>> {
    let mut isolate_refmut = cix.isolate.borrow_mut();
    let dynamic_scope_isolate: &mut v8::OwnedIsolate = &mut *isolate_refmut;
    let ex = Rc::new(EvalContext {
        ix: cix.clone(),
        next_local_id: Default::default(),
        externals: RefCell::new(Vec::new()),
                v8_scope: std::cell::Cell::new(None),
                v8_dispatcher: std::cell::Cell::new(None),
    });
    dynamic_scope_isolate.set_slot(V8RcStore { weaks: RefCell::new(Vec::new()) });
    dynamic_scope_isolate.set_slot(V8LogStore { logs: RefCell::new(Vec::new()) });
    dynamic_scope_isolate.set_slot(V8UniqueCounter {
        compile_id: cix
            .compile_path
            .last()
            .map(|(c, _)| c.clone())
            .unwrap_or(CompileId(ImportStateId(0), SourceId(0))),
        counter: Counter::default(),
    });
    dynamic_scope_isolate.set_slot(V8EvalContextSlot { ex: ex.clone() });
    let dynamic_scope_handle = std::pin::pin!(v8::HandleScope::new(dynamic_scope_isolate));
    let mut dynamic_scope_handle2 = dynamic_scope_handle.init();
    let dynamic_scope_context = {
        let mut ctx_slot = cix.v8_context.borrow_mut();
        match ctx_slot.as_ref() {
            Some(global) => v8::Local::new(&mut dynamic_scope_handle2, global),
            None => {
                let ctx = v8::Context::new(&mut dynamic_scope_handle2, Default::default());
                *ctx_slot = Some(v8::Global::new(&mut dynamic_scope_handle2, ctx));
                ctx
            },
        }
    };
    let dynamic_scope_context2 = &mut v8::ContextScope::new(&mut dynamic_scope_handle2, dynamic_scope_context);
    tc_scope!(let dynamic_scope, dynamic_scope_context2);
    // Install the active v8 scope on EvalContext as a raw `NonNull`. Anyone
    // reaching v8 from `const_eval` (currently `StatelessV8CastDispatcher`)
    // reads this and re-borrows. A drop guard clears it on `eval()` exit
    // (early returns included) so a stale pointer can't outlive this
    // function via Rc-shared `EvalContext`.
    {
        let scope_ref: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> =
            &mut **dynamic_scope;
        let scope_ptr = std::ptr::NonNull::from(scope_ref);
        let scope_ptr_static: std::ptr::NonNull<
            v8::PinnedRef<'static, v8::HandleScope<'static, v8::Context>>,
        > = unsafe { std::mem::transmute(scope_ptr) };
        ex.v8_scope.set(Some(scope_ptr_static));
    }
    struct V8RefsGuard<'a>(&'a EvalContext);
    impl<'a> Drop for V8RefsGuard<'a> {
        fn drop(&mut self) {
            self.0.v8_scope.set(None);
            self.0.v8_dispatcher.set(None);
        }
    }
    let _v8_refs_guard = V8RefsGuard(&ex);
    let root_loc = expr.loc;
    let target: Rc<dyn Target> = Rc::new(TargetDynamic);
    let scope = Rc::new(Scope {
        values: RefCell::new(HashMap::new()),
        parent: None,
    });
    let xx = Rc::new(ExprContext {
        vx: ex.clone(),
        import_state: Rc::new(Cell::new(import_state)),
        scope: scope.clone(),
        target: target.clone(),
    });
    // First stage registers builtin v8 closures on the (now-persistent) context's
    // global; later stages reuse them. ScopeEntry/scope.values are rebuilt every
    // stage because each stage's top-level `Scope` is fresh.
    let register_v8 = !cix.builtins_registered.get();
    let mut builtin_scope_entries: Vec<(String, ScopeEntryId, Rc<dyn Type>)> = Vec::new();
    for (key, dynamic_key, type_, dynamic_value) in [
        //. .
        dynamic_wrap!(fn log(ex, dynamic_scope, scope)(message: String) ->() {
            let timestamp_ms =
                std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as u64;
            scope.get_slot::<V8LogStore>().unwrap().logs.borrow_mut().push(LogEntry {
                timestamp_ms,
                message
            });
        }),
        dynamic_wrap!(fn define(ex, dynamic_scope, scope)() -> Gc < DefinitionSet > {
            let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
            let def_set_id = DefSetId(ex_slot.ex.ix.def_set_counter.next());
            let import_id = ex_slot.ex.ix.import_state;
            let source_id = ex_slot.ex.ix.source_id;
            let ds = Gc::new(DefinitionSet::new(import_id, def_set_id, source_id));
            ex_slot.ex.ix.definition_sets.borrow_mut().push(ds.clone());
            ds
        }),
        dynamic_wrap!(fn str_to_json(ex, dynamic_scope, scope)(s: String) -> String {
            serde_json::to_string(&s).unwrap()
        }),
        dynamic_wrap!(fn constkey_to_str(ex, dynamic_scope, scope)(key: crate:: langhelp:: ConstKey) -> String {
            key.to_str()
        }),
        // `new_code_value(code, value)` — explicit constructor for the result
        // of a const-eval-style interface impl. The Rust counterpart of
        // `AspectFunction::call` (etc.) explicitly builds a
        // `CodeValue { code, value }` and returns it; this builtin gives source
        // impls the same primitive. The `code` arg's runtime form becomes the
        // result CV's `Code` (wrapped in `Code_::Dyn(ValueAsCodeDyn{...})`),
        // and the `value` arg becomes the CV's `value`. Wrapped as a
        // `CodeValueExtern` so it crosses the v8 boundary back to the Rust
        // dispatcher unchanged.
        dynamic_wrap!(fn new_code_value(ex, dynamic_scope, scope)(
            code: crate::langhelp::Code,
            value: Rc<dyn crate::langhelp::Value>,
        ) -> crate::eval::code_value_extern::CodeValueExtern {
            let cv = crate::langhelp::CodeValue { code, value };
            crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
        }),
        // `gen_binding_name(xx)` returns a fresh JS binding name (e.g. "a17")
        // unique within this compile. Used by target.bind impls to mint the
        // `let aN; { ... aN = code; }` binding key for a new dynamic binding.
        // The `xx` argument is ignored — uniqueness comes from `ex.ix`'s
        // shared counter; the parameter exists so the source-side signature
        // matches the Rust `bind`'s `xx: &ExprContext` carry.
        dynamic_wrap!(fn gen_binding_name(ex, dynamic_scope, scope)(
            _xx: crate::eval::code_value_extern::CodeValueExtern
        ) -> String {
            let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
            let id = ex_slot.ex.next_local_id.next();
            format!("a{}", id)
        }),
        // v8-callable `eval_const`: invoked synchronously from a user-defined
        // `cast_from`'s JS body. Takes the active `ExprContext` (carried as
        // External via `arg.xx`) and a staged inner expression. Runs Rust
        // `const_eval` on the staged Expr in the given context and returns
        // the result `CodeValue` wrapped as a v8 External so the cast_from
        // method's return travels back through v8 to the Rust dispatcher.
        dynamic_wrap!(
            fn eval_const(
                ex,
                dynamic_scope,
                scope
            )(
                xx: crate::eval::code_value_extern::ExprContextRef,
                staged: crate::eval::dynamic_wrap::StagedExpr,
            ) -> crate::eval::code_value_extern::CodeValueExtern {
                let expr_value = staged.0;
                let expr_ast: Expr =
                    Rc::try_unwrap(crate::langhelp::downcast_value::<Expr>(expr_value))
                        .ok()
                        .expect("eval_const: staged Expr Rc not unique");
                let res = const_eval(&xx.0, &expr_ast);
                let cv = res.v.expect("eval_const: const_eval produced no value");
                crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
            }
        ),
        // `const_eval`: the user-facing name (same impl as `eval_const`, kept
        // around for backwards compatibility with `t0120_impl_type`). Takes a
        // tuple of (ExprContext, expr), runs const-eval, returns the CV.
        dynamic_wrap!(
            fn const_eval(
                ex,
                dynamic_scope,
                scope
            )(
                xx: crate::eval::code_value_extern::ExprContextRef,
                staged: crate::eval::dynamic_wrap::StagedExpr,
            ) -> crate::eval::code_value_extern::CodeValueExtern {
                let expr_value = staged.0;
                let expr_ast: Expr =
                    Rc::try_unwrap(crate::langhelp::downcast_value::<Expr>(expr_value))
                        .ok()
                        .expect("const_eval: staged Expr Rc not unique");
                let res = const_eval(&xx.0, &expr_ast);
                let cv = res
                    .v
                    .unwrap_or_else(|| panic!("const_eval: const_eval produced no value; errors: {:?}", res.errors));
                crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
            }
        ),
        // `new_expr_context(target, scope)` — build a fresh ExprContext
        // layered on the current evaluation context. `target` is a value
        // impl'ing the builtin `target` interface (extracted as
        // `Rc<dyn Target>` from a v8::External via TypeAnyTargetDynamic);
        // `scope` is a record `{name: scope_entry_instance, …}` decoded into
        // a `HashMap<ConstKey, Rc<dyn ScopeEntry>>`.
        dynamic_wrap!(
            fn new_expr_context(
                ex,
                dynamic_scope,
                scope
            )(
                target_arg: Rc<dyn Target>,
                scope_arg: std::collections::HashMap<ConstKey, Rc<dyn crate::langhelp::ScopeEntry>>,
            ) -> crate::eval::code_value_extern::ExprContextRef {
                let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
                let parent_ex = ex_slot.ex.clone();
                // Convert `Rc<dyn ScopeEntry>` values into the `Box<dyn ScopeEntry>`
                // that `Scope.values` stores. Each Rc is unique by construction
                // (one external per record property), so try_unwrap succeeds;
                // fall back to a wrapper that defers to the original Rc otherwise.
                let mut scope_values: HashMap<ConstKey, Box<dyn crate::langhelp::ScopeEntry>> =
                    HashMap::new();
                for (k, v) in scope_arg {
                    scope_values.insert(k, Box::new(RcScopeEntryWrap(v)));
                }
                let new_scope = Rc::new(Scope {
                    values: RefCell::new(scope_values),
                    parent: None,
                });
                let xx = Rc::new(ExprContext {
                    vx: parent_ex.clone(),
                    import_state: Rc::new(Cell::new(parent_ex.ix.import_state)),
                    scope: new_scope,
                    target: target_arg,
                });
                crate::eval::code_value_extern::ExprContextRef(xx)
            }
        ),
    ] {
        if register_v8 {
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(dynamic_key)).unwrap().cast(),
                    dynamic_value,
                );
        }
        let ib = aspect_initial_bind_get(&xx, root_loc, type_.as_ref()).into_res()?;
        builtin_scope_entries.push((key.to_string(), dynamic_key, type_));
        scope.values.borrow_mut().insert(ConstKey::String(key.to_string()), ib.initial_bind(dynamic_key));
    }

    // Synthetic builtin interfaces: `value` and `type`. These are exposed as
    // `Rc<SourceInterfaceRef>` so `impl_interface(value | type, ...)` accepts
    // them through the same path as source-defined interfaces. They share a
    // single throwaway DefinitionSet that is never compiled or serialized.
    {
        use value::type_source_interface::SourceInterfaceRef;
        let builtin_ifaces = value::type_source_interface::build_builtin_interfaces(import_state);
        let iface_dyn_type: Rc<dyn Type> =
            Rc::new(value::type_source_interface::dynamic::TypeSourceInterfaceDynamic);
        for (name, data) in
            [
                ("value", builtin_ifaces.value),
                ("type", builtin_ifaces.type_),
                ("aspect_function", builtin_ifaces.aspect_function),
                ("aspect_field", builtin_ifaces.aspect_field),
                ("scope_entry", builtin_ifaces.scope_entry),
                ("target", builtin_ifaces.target),
                ("code_dyn", builtin_ifaces.code_dyn),
            ] {
            let iface_ref = Rc::new(SourceInterfaceRef { data });
            let dynamic_key = ScopeEntryId::Builtin(
                // Distinct ids per name to avoid collisions.
                line!() as usize + match name {
                    "value" => 0,
                    "type" => 1,
                    "aspect_function" => 2,
                    "aspect_field" => 3,
                    "scope_entry" => 4,
                    "target" => 5,
                    "code_dyn" => 6,
                    _ => unreachable!(),
                },
            );
            if register_v8 {
                let v8_val = to_v8(dynamic_scope, iface_ref.clone());
                dynamic_scope
                    .get_current_context()
                    .global(dynamic_scope)
                    .set(
                        dynamic_scope,
                        v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(dynamic_key))
                            .unwrap()
                            .cast(),
                        v8_val.into(),
                    );
            }
            // Bind each interface with the specific `Const` wrapper carrying
            // that interface's data. `scope_use("type")` etc. plays double
            // duty in source: it's a type-of-types value AND the iface ref
            // that `impl_interface(<iface>, methods)` dispatches on. The
            // Const wrapper carries the specific iface identity so the
            // `impl_interface` lookup matches; switching to `TypeAnyDynamic`
            // breaks `impl_interface(type, …)` and similar (see
            // `test_00100_impl_type`).
            let iface_const_type: Rc<dyn Type> = Rc::new(
                value::type_source_interface::TypeSourceInterfaceConst {
                    data: iface_ref.data.clone(),
                },
            );
            let ib = aspect_initial_bind_get(&xx, root_loc, iface_const_type.as_ref()).into_res()?;
            builtin_scope_entries.push((name.to_string(), dynamic_key, iface_const_type.clone()));
            scope
                .values
                .borrow_mut()
                .insert(ConstKey::String(name.to_string()), ib.initial_bind(dynamic_key));
        }
        let _ = iface_dyn_type; // unused now
    }

    // Type references (static scope entries, no JS representation)
    for (
        key,
        type_ref,
    ) in [
        ("null", Rc::new(TypeNullDynamic) as Rc<dyn Type>),
        ("string", Rc::new(TypeStringDynamic) as Rc<dyn Type>),
        ("number", Rc::new(TypeNumberDynamic) as Rc<dyn Type>),
        ("constkey", Rc::new(value::type_constkey::dynamic::TypeConstkeyDynamic) as Rc<dyn Type>),
        ("expr", Rc::new(value::type_expr::TypeExpr) as Rc<dyn Type>),
        ("any", Rc::new(value::type_any::dynamic::TypeAnyDynamic) as Rc<dyn Type>),
        ("aspect", Rc::new(value::type_aspect::dynamic::TypeAspectDynamic) as Rc<dyn Type>),
        ("codedyn", Rc::new(value::type_code_source::TypeCodeDynHandleDynamic) as Rc<dyn Type>),
    ] {
        // Type-of-types scope entry: value is `ValueAnyConst` carrying the
        // `Rc<dyn Type>`. Meta-type is `TypeAnyConst`, so cast to `any`,
        // consume-dynamic, etc. dispatch through `TypeAnyConst` which
        // extracts the inner type via `AspectType` (infallible downcast to
        // `ValueAnyConst`).
        let cv = CodeValue {
            code: Code {
                loc: root_loc,
                code: Code_::None,
            },
            value: Rc::new(value::type_any::r#const::ValueAnyConst { inner: type_ref.clone() }),
        };
        let any_const_type: Rc<dyn Type> = Rc::new(value::type_any::r#const::TypeAnyConst);
        let bind = aspect_bind_get(&xx, root_loc, any_const_type.as_ref()).into_res()?;
        let (_, entry) = bind.bind(&xx, root_loc, cv).into_res()?;
        scope.values.borrow_mut().insert(ConstKey::String(key.to_string()), entry);
    }

    // Aspect id scope entries: expose Rust-side aspect ids as `Unique` values
    // so they can be passed to `.aspect(...)` lookups. Dual-mode — `use_`
    // returns a CodeValue whose `code` references a v8 External (so the JS
    // path can pass the Unique to `eval_const_v8` / `.aspect` JS methods) and
    // whose `value` is the actual Unique (so const-eval consumers can downcast
    // and use it directly).
    let unique_type: Rc<dyn Type> = Rc::new(value::value_unique::TypeUnique);
    for (key, unique, line) in [
        ("aspect_string_dynamic", crate::builtin::aspect_string_dynamic_aspect_id(), line!() as usize),
        ("aspect_function_id", crate::builtin::aspect_function_aspect_id(), line!() as usize),
        ("aspect_field_id", crate::builtin::aspect_field_aspect_id(), line!() as usize),
    ] {
        let dynamic_key = ScopeEntryId::Builtin(line);
        if register_v8 {
            let unique_v8 = to_v8(dynamic_scope, unique.clone());
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(dynamic_key))
                        .unwrap()
                        .cast(),
                    unique_v8.into(),
                );
        }
        builtin_scope_entries.push((key.to_string(), dynamic_key, unique_type.clone()));
        scope.values.borrow_mut().insert(
            ConstKey::String(key.to_string()),
            Box::new(value::value_unique::ScopeEntryUniqueDualMode {
                dynamic_key,
                unique,
            }) as Box<dyn ScopeEntry>,
        );
    }

    // list/map constructors: jswrap fns for v8 with constructor types for half-eval
    for (
        key,
        constructor_type,
        (_, dynamic_key, _, dynamic_value),
    ) in [
        (
            "list",
            Rc::new(value::type_list_constructor::r#const::TypeListConstructorConst) as Rc<dyn Type>,
            dynamic_wrap!(
                fn list(
                    ex,
                    dynamic_scope,
                    scope
                )(element_type: dynamic_wrap:: AnyType) -> value:: type_list:: dynamic:: TypeListDynamic {
                    value::type_list::dynamic::TypeListDynamic { element_type: element_type.0 }
                }
            ),
        ),
        (
            "map",
            Rc::new(value::type_map_constructor::r#const::TypeMapConstructorConst) as Rc<dyn Type>,
            dynamic_wrap!(
                fn map(
                    ex,
                    dynamic_scope,
                    scope
                )(
                    key_type: dynamic_wrap:: AnyType,
                    value_type: dynamic_wrap:: AnyType
                ) -> value:: type_map:: dynamic:: TypeMapDynamic {
                    value::type_map::dynamic::TypeMapDynamic {
                        key_type: key_type.0,
                        value_type: value_type.0
                    }
                }
            ),
        ),
        (
            "option",
            Rc::new(value::type_option_constructor::r#const::TypeOptionConstructorConst) as Rc<dyn Type>,
            dynamic_wrap!(
                fn option(
                    ex,
                    dynamic_scope,
                    scope
                )(element_type: dynamic_wrap:: AnyType) -> value:: type_option:: dynamic:: TypeOptionDynamic {
                    value::type_option::dynamic::TypeOptionDynamic { element_type: element_type.0 }
                }
            ),
        ),
    ] {
        if register_v8 {
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(dynamic_key)).unwrap().cast(),
                    dynamic_value,
                );
        }
        let ib = aspect_initial_bind_get(&xx, root_loc, constructor_type.as_ref()).into_res()?;
        builtin_scope_entries.push((key.to_string(), dynamic_key, constructor_type));
        scope.values.borrow_mut().insert(ConstKey::String(key.to_string()), ib.initial_bind(dynamic_key));
    }
    // `aspect` — the source-level builtin function for aspect lookup.
    // Pure compile-time dispatch: `TypeFunctionAspectDynamic::call` emits the
    // JS code that does the runtime lookup, so there's no v8-side function
    // to register here (no `dynamic_wrap!` companion). The compile-time
    // scope entry is enough.
    {
        let aspect_type: Rc<dyn Type> =
            Rc::new(value::type_function_aspect::dynamic::TypeFunctionAspectDynamic);
        let aspect_dynamic_key = ScopeEntryId::Builtin(line!() as usize);
        let ib = aspect_initial_bind_get(&xx, root_loc, aspect_type.as_ref()).into_res()?;
        builtin_scope_entries.push(("aspect_lookup".to_string(), aspect_dynamic_key, aspect_type));
        scope
            .values
            .borrow_mut()
            .insert(ConstKey::String("aspect_lookup".to_string()), ib.initial_bind(aspect_dynamic_key));
    }
    // `cast_value` — source-level helper to unwrap an opaque `Rc<dyn Value>`
    // (e.g. `cve.value`) at a known user-defined data type. Same shape as
    // `aspect_lookup`: compile-time dispatch via `TypeFunctionCastValueDynamic`
    // emits the JS that calls `__cast_value`.
    {
        let cast_value_type: Rc<dyn Type> =
            Rc::new(value::type_function_cast_value::dynamic::TypeFunctionCastValueDynamic);
        let cast_value_key = ScopeEntryId::Builtin(line!() as usize);
        let ib = aspect_initial_bind_get(&xx, root_loc, cast_value_type.as_ref()).into_res()?;
        builtin_scope_entries.push(("cast_value".to_string(), cast_value_key, cast_value_type));
        scope
            .values
            .borrow_mut()
            .insert(ConstKey::String("cast_value".to_string()), ib.initial_bind(cast_value_key));
    }
    // The v8 dispatcher (`StatelessV8CastDispatcher`) is stateless — it
    // reads the active scope from `ex.v8_scope` (installed at the top of
    // `eval()`) on each call, so its lifetime isn't tied to a `&mut` on
    // `dynamic_scope`. Installed once here for the whole `const_eval`.
    let mut dispatcher_impl = StatelessV8CastDispatcher;
    let dispatcher_ref: &mut dyn V8CastDispatcher = &mut dispatcher_impl;
    let dispatcher_ptr: std::ptr::NonNull<dyn V8CastDispatcher> =
        std::ptr::NonNull::from(dispatcher_ref);
    // SAFETY: lifetime-erase the trait-object pointer so it can sit in
    // EvalContext. The dispatcher impl is on this stack frame for the rest
    // of `eval()`; the v8_dispatcher field is cleared just before return,
    // so the pointer is never readable after the impl drops.
    let dispatcher_ptr_static: std::ptr::NonNull<dyn V8CastDispatcher + 'static> =
        unsafe { std::mem::transmute(dispatcher_ptr) };
    ex.v8_dispatcher.set(Some(dispatcher_ptr_static));
    *cix.builtin_scope_entries.borrow_mut() = builtin_scope_entries.clone();
    let result = const_eval(&xx, expr);
    if let Some(errs) = result.errors {
        return Err(errs);
    }
    let cv = result.v.unwrap();
    let value_type = cv.value.type_(&xx, root_loc);
    let (stage1_code, fallback_value) = match &cv.code.code {
        Code_::None => (None, Some(cv.value)),
        _ => {
            let to_dynamic = aspect_consume_dynamic_get(&xx, root_loc, value_type.as_ref()).into_res()?;
            let js = to_dynamic.to_dynamic(&xx, root_loc, cv).into_res()?;
            (Some(js), None)
        },
    };

    // Compile definition set implementations Collect function data first to release
    // the borrow on definition_sets, since eval_half may trigger function calls that
    // re-borrow it.
    struct DefFuncInfo {
        arg_type: Rc<dyn Type>,
        impl_expr_ast: Rc<dyn Value>,
        ret_type: Rc<dyn Type>,
        scope_entry_id: ScopeEntryId,
    }

    let mut precompiled_def_funcs: Vec<String> = vec![];
    let def_func_infos: Vec<DefFuncInfo> = {
        let sets = ex.ix.definition_sets.borrow();
        let mut infos = vec![];
        for ds in sets.iter() {
            for func_data in ds.functions.borrow().iter() {
                if func_data.impl_expr.borrow().is_none() {
                    // Either compiled in an earlier stage of this compile (v8
                    // closure persists on the shared `v8_context` global) or
                    // imported from another compile (compiled_js needs
                    // emitting in this stage).
                    if let Some(js) = func_data.compiled_js.borrow().clone() {
                        precompiled_def_funcs.push(js);
                    }
                    continue;
                }
                let impl_expr_ast =
                    func_data.impl_expr.borrow_mut().take().expect("Definition set function missing implementation");
                let arg_type = func_data.arg_type.borrow().clone().expect("Definition set function missing arg type");
                let ret_type =
                    func_data.ret_type.borrow().clone().expect("Definition set function missing return type");
                let scope_id = func_data.scope_entry_id.get().expect("Definition set function not registered");
                infos.push(DefFuncInfo {
                    impl_expr_ast,
                    arg_type,
                    ret_type,
                    scope_entry_id: scope_id,
                });
            }
        }
        infos
    };

    // (factored out: see top-level `build_cast_arg_record_type` and
    // `build_builtin_scope` below — used here for compiling def-set bodies,
    // and from import-side `from_serial` for re-compiling imported
    // interface impls.)

    let mut def_funcs: Vec<(ScopeEntryId, Option<ScopeEntryId>, CodeDynamic, Vec<(ScopeEntryId, External)>)> = vec![];
    for info in &def_func_infos {
        let impl_expr_ast = must_downcast_ref::<Expr>(info.impl_expr_ast.as_ref());

        // Create a separate evaluation context for the function body
        let body_ex = Rc::new(EvalContext {
            ix: ex.ix.clone(),
            next_local_id: Counter::default(),
            externals: RefCell::new(Vec::new()),
                v8_scope: std::cell::Cell::new(None),
                v8_dispatcher: std::cell::Cell::new(None),
        });
        let arg_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        let func_xx = {
            let mut body_scope = build_builtin_scope(&xx, root_loc, &builtin_scope_entries)?;
            if let Some(arg_ib) = crate::builtin::aspect_initial_bind_get_maybe(&xx, info.arg_type.as_ref()) {
                body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
            }
            Rc::new(ExprContext {
                vx: body_ex,
                import_state: xx.import_state.clone(),
                scope: Rc::new(Scope {
                    values: RefCell::new(body_scope),
                    parent: None,
                }),
                target: target.clone(),
            })
        };
        let impl_result = const_eval(&func_xx, impl_expr_ast);
        if let Some(errs) = impl_result.errors {
            return Err(errs);
        }
        let impl_cv = impl_result.v.unwrap();

        // Type check return value against ret type
        let impl_cv = {
            let r = typecheck_convert(&func_xx, root_loc, info.ret_type.as_ref(), impl_cv);
            if let Some(errs) = r.errors {
                return Err(errs);
            }
            r.v.unwrap()
        };
        let impl_code =
            impl_cv
                .code
                .cast::<CodeDynamic>()
                .into_res()?
                .expect("Definition set function body must produce JS code")
                .clone();
        let captured = func_xx.vx.externals.borrow().clone();
        for ds in ex.ix.definition_sets.borrow().iter() {
            for func_data in ds.functions.borrow().iter() {
                if func_data.scope_entry_id.get() == Some(info.scope_entry_id) {
                    *func_data.captured_externals.borrow_mut() = captured.clone();
                }
            }
        }
        def_funcs.push((info.scope_entry_id, Some(arg_id), impl_code, captured));
    }

    // Compile data type method implementations
    enum CompiledDataMethodBody {
        Fresh {
            self_scope_entry_id: ScopeEntryId,
            data_field: String,
            impl_code: CodeDynamic,
            /// True when the method body's "self" binding actually represents
            /// a function argument (`arg`), as is the case for builtin-`type`
            /// interface impl methods. Renders as `function f(a0) { ... }`
            /// instead of `function f() { let a0 = this._data_field; ... }`.
            is_arg_method: bool,
            /// `outer_self` binding: the v8 instance itself (`this`). Always
            /// allocated for every method body so the source can address the
            /// instance via `mb.scope_use("outer_self")` (typed at compile
            /// time as `TypeSourceDataTypeDynamic`). Used by e.g.
            /// `type.aspect`: `option(aspect).some(outer_self)` captures the
            /// instance through `__data_instance_to_aspect(this)`.
            outer_self_id: ScopeEntryId,
            /// The function's `arg` parameter. Every method takes an argument
            /// (null / empty tuple counts), so this is always allocated. For
            /// non-Type interface methods the emit is
            /// `function(<arg_key>) { let <self_key> = this[..]; … }`. For
            /// Type interface methods (where the parameter conceptually IS
            /// `arg` per `is_arg_method`), it's `function(<self_key>)` and
            /// `arg_param_id` is unused.
            arg_param_id: ScopeEntryId,
        },
        Precompiled(String),
    }

    struct CompiledDataMethod {
        body: CompiledDataMethodBody,
        scope_entry_id: ScopeEntryId,
        /// Externals captured by the body's compile env (Local ids assigned
        /// fresh from 0 within the body). Rendered as `let a<id> = globalThis[
        /// "<method-namespaced key>"];` in a block wrapping the function
        /// definition so the function closes over them; installed as v8
        /// globals under the method-namespaced keys at call time.
        captured_externals: Vec<(ScopeEntryId, External)>,
    }

    let mut def_data_methods: Vec<CompiledDataMethod> = vec![];
    // Snapshot definition sets and their data types up front so we don't hold a
    // RefCell borrow on `definition_sets` across const_eval / typecheck calls —
    // those paths can invoke constructors that need `borrow_mut()` on the
    // same RefCell.
    let dss: Vec<_> = ex.ix.definition_sets.borrow().iter().cloned().collect();
    for ds in &dss {
        let dts: Vec<_> = ds.data_types.borrow().iter().cloned().collect();
        for dt_data in &dts {
            let underlying_type: Rc<dyn Type> = dt_data.underlying_type.get().clone();
            let data_field = dt_data.data_field.clone();
            for method in dt_data.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not assigned");

                // Check for pre-compiled JS first
                if let Some(compiled_js) = method.compiled_js.borrow().clone() {
                    def_data_methods.push(CompiledDataMethod {
                        scope_entry_id: method_id,
                        body: CompiledDataMethodBody::Precompiled(compiled_js),
                        captured_externals: method.captured_externals.borrow().clone(),
                    });
                    continue;
                }
                let impl_expr = method.impl_expr.borrow();
                let impl_expr_ast = must_downcast_ref::<expr::Expr>(impl_expr.as_ref().unwrap().as_ref());
                let ret_type: Rc<dyn Type> =
                    method.ret_type.borrow().clone().expect("Data type method missing return type");

                // Create a separate evaluation context for the method body
                let body_ex = Rc::new(EvalContext {
                    ix: ex.ix.clone(),
                    next_local_id: Counter::default(),
                    externals: RefCell::new(Vec::new()),
                v8_scope: std::cell::Cell::new(None),
                v8_dispatcher: std::cell::Cell::new(None),
                });
                let self_id = ScopeEntryId::Local(body_ex.next_local_id.next());
                method.self_scope_entry_id.set(Some(self_id));
                let arg_id = ScopeEntryId::Local(body_ex.next_local_id.next());
                // Regular data-type methods don't currently need the data
                // instance via `outer_self` — sentinel id (would shift Local
                // IDs and break snapshot tests if always allocated here).
                let outer_self_id = ScopeEntryId::Builtin(usize::MAX);
                let method_xx = {
                    let mut body_scope = build_builtin_scope(&xx, root_loc, &builtin_scope_entries)?;
                    if let Some(self_ib) = crate::builtin::aspect_initial_bind_get_maybe(&xx, underlying_type.as_ref()) {
                        body_scope.insert(ConstKey::String("self".to_string()), self_ib.initial_bind(self_id));
                    }
                    if let Some(arg_type) = method.arg_type.borrow().clone() {
                        if let Some(arg_ib) = crate::builtin::aspect_initial_bind_get_maybe(&xx, arg_type.as_ref()) {
                            body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
                        }
                    }
                    Rc::new(ExprContext {
                        vx: body_ex,
                        import_state: xx.import_state.clone(),
                        scope: Rc::new(Scope {
                            values: RefCell::new(body_scope),
                            parent: None,
                        }),
                        target: target.clone(),
                    })
                };
                let impl_result = const_eval(&method_xx, impl_expr_ast);
                if let Some(errs) = impl_result.errors {
                    return Err(errs);
                }
                let impl_cv = impl_result.v.unwrap();

                // Type check return value
                let impl_cv = {
                    let r = typecheck_convert(&method_xx, root_loc, ret_type.as_ref(), impl_cv);
                    if let Some(errs) = r.errors {
                        return Err(errs);
                    }
                    r.v.unwrap()
                };
                let impl_code =
                    impl_cv
                        .code
                        .cast::<CodeDynamic>()
                        .into_res()?
                        .expect("Method body must produce JS code")
                        .clone();
                // Snapshot the body's externals — its compiled_js references these
                // by their Local id (`a##`), and they must be installed as v8
                // globals whenever the body runs (including after cross-module
                // import when the original env's globals are gone).
                let captured = method_xx.vx.externals.borrow().clone();
                *method.captured_externals.borrow_mut() = captured.clone();
                def_data_methods.push(CompiledDataMethod {
                    scope_entry_id: method_id,
                    body: CompiledDataMethodBody::Fresh {
                        self_scope_entry_id: self_id,
                        data_field: data_field.clone(),
                        impl_code,
                        is_arg_method: false,
                        outer_self_id,
                        arg_param_id: arg_id,
                    },
                    captured_externals: captured,
                });
            }

            // Compile interface method implementations
            for impl_data in dt_data.interface_impls.borrow().iter() {
                let builtin_kind = impl_data.interface.builtin_kind;
                for method_impl in &impl_data.method_impls {
                    let method_id =
                        method_impl.scope_entry_id.get().expect("Interface method impl scope entry ID not assigned");
                    if let Some(compiled_js) = method_impl.compiled_js.borrow().clone() {
                        def_data_methods.push(CompiledDataMethod {
                            scope_entry_id: method_id,
                            body: CompiledDataMethodBody::Precompiled(compiled_js),
                            captured_externals: method_impl.captured_externals.borrow().clone(),
                        });
                        continue;
                    }
                    let impl_expr_value =
                        method_impl.impl_expr.borrow().clone().expect("Interface method impl missing impl_expr");
                    let impl_expr_ast = must_downcast_ref::<expr::Expr>(impl_expr_value.as_ref());
                    // Note: impl_expr_artifact serialization deferred to the
                    // post-render pass below — see the second-pass comment
                    // after the rendering loop. Serializing here can trigger
                    // `serialize_definition_set` for types referenced in the
                    // body (e.g. via `lower(nvalue.type)`), which requires
                    // every method/impl of those types to have `compiled_js`
                    // set. That doesn't happen until the rendering loop.

                    // Resolve ret type from interface method
                    let ret_type: Rc<dyn Type> = {
                        let iface_methods = impl_data.interface.methods.borrow();
                        let iface_method = &iface_methods[method_impl.interface_method_index];
                        iface_method.ret_type.borrow().clone().expect("Interface method missing return type")
                    };

                    // Fresh `EvalContext` per body — `next_local_id` starts at 0
                    // so `compiled_js` is stable across compiles, and the body's
                    // captured `externals` are stored on the method and replayed
                    // at v8 call time (rendered as a block-scoped alias from
                    // method-namespaced globals into the body's bare `a##`
                    // names).
                    let body_ex = Rc::new(EvalContext {
                        ix: ex.ix.clone(),
                        next_local_id: Counter::default(),
                        externals: RefCell::new(Vec::new()),
                        v8_scope: std::cell::Cell::new(None),
                        v8_dispatcher: std::cell::Cell::new(None),
                    });
                    // For builtin-`type` interface impls the body sees `arg`
                    // (a record `{xx, source_type, cv}`) instead of `self`.
                    // For source-defined and builtin-`value` impls keep the
                    // `self` setup.
                    let self_id = ScopeEntryId::Local(body_ex.next_local_id.next());
                    method_impl.self_scope_entry_id.set(Some(self_id));
                    let outer_self_id = ScopeEntryId::Local(body_ex.next_local_id.next());
                    let arg_id = ScopeEntryId::Local(body_ex.next_local_id.next());
                    let method_xx = {
                        let mut body_scope = build_builtin_scope(&xx, root_loc, &builtin_scope_entries)?;
                        // Universal `outer_self` = the data type instance (JS
                        // `this`). Bound for every method body so source code
                        // can address the instance regardless of how `self`
                        // is bound for that particular interface method.
                        let outer_self_type: Rc<dyn Type> = Rc::new(
                            value::type_source_data_type::dynamic::TypeSourceDataTypeDynamic {
                                data: dt_data.clone(),
                            },
                        );
                        if let Some(outer_self_ib) = crate::builtin::aspect_initial_bind_get_maybe(
                            &xx,
                            outer_self_type.as_ref(),
                        ) {
                            body_scope.insert(
                                ConstKey::String("outer_self".to_string()),
                                outer_self_ib.initial_bind(outer_self_id),
                            );
                        }
                        match builtin_kind {
                            Some(value::type_source_interface::BuiltinInterfaceKind::Type) => {
                                let method_name = {
                                    let iface_methods = impl_data.interface.methods.borrow();
                                    iface_methods[method_impl.interface_method_index].name.clone()
                                };
                                if method_name == "aspect" {
                                    // `aspect(arg)` takes `{key: Unique}` and
                                    // returns `option(aspect)`. The compiled
                                    // JS function takes one parameter (the
                                    // arg object), bound under `self_id`
                                    // (whose `scope_entry_id_dynamic_key` is
                                    // the JS parameter name). To make the
                                    // source's `arg` access compile to that
                                    // parameter, bind the `arg` scope entry
                                    // with `self_id`. Then the data type
                                    // itself is the `self` for the impl,
                                    // bound under a fresh local id (so
                                    // `option.some(self)` can route through
                                    // `TypeSourceDataTypeDynamic::cast_to(aspect)`).
                                    let mut fields = std::collections::BTreeMap::new();
                                    fields.insert(
                                        ConstKey::String("key".to_string()),
                                        value::record::RecordField::Mortar(
                                            Rc::new(value::value_unique::TypeUnique) as Rc<dyn Type>,
                                        ),
                                    );
                                    let arg_record_type: Rc<dyn Type> = Rc::new(
                                        value::record::Record {
                                            base: Some(Box::new(value::record::RecordBaseDynamic)),
                                            fields,
                                        },
                                    );
                                    let arg_ib = aspect_initial_bind_get(
                                        &xx,
                                        root_loc,
                                        arg_record_type.as_ref(),
                                    ).into_res()?;
                                    body_scope.insert(
                                        ConstKey::String("arg".to_string()),
                                        arg_ib.initial_bind(self_id),
                                    );
                                    // Source-level `self` is not used by the
                                    // `aspect` method body — source impls
                                    // address the data type instance via
                                    // `outer_self` (universally bound above).
                                } else {
                                    let arg_record_type = build_cast_arg_record_type();
                                    let arg_ib = aspect_initial_bind_get(&xx, root_loc, arg_record_type.as_ref()).into_res()?;
                                    body_scope.insert(
                                        ConstKey::String("arg".to_string()),
                                        arg_ib.initial_bind(self_id),
                                    );
                                }
                            },
                            Some(value::type_source_interface::BuiltinInterfaceKind::AspectFunction)
                            | Some(value::type_source_interface::BuiltinInterfaceKind::AspectField)
                            | Some(value::type_source_interface::BuiltinInterfaceKind::ScopeEntry)
                            | Some(value::type_source_interface::BuiltinInterfaceKind::Target) => {
                                // `self` = impl data type's underlying, same as
                                // a regular method.
                                if let Some(self_ib) =
                                    crate::builtin::aspect_initial_bind_get_maybe(&xx, underlying_type.as_ref()) {
                                    body_scope.insert(
                                        ConstKey::String("self".to_string()),
                                        self_ib.initial_bind(self_id),
                                    );
                                }
                                // `arg` = the call's inputs as a record of
                                // `CodeValueExtern`s, one per Rust trait param
                                // (e.g. `{self_, argument}` for
                                // aspect_function.call). Field-access on a
                                // CodeValueExtern goes through
                                // `TypeCodeValueExternDynamic::AspectField`,
                                // which emits a runtime call to the
                                // `__cve_field` mortar fn.
                                let method_name = {
                                    let iface_methods = impl_data.interface.methods.borrow();
                                    iface_methods[method_impl.interface_method_index].name.clone()
                                };
                                let arg_field_names: Vec<&str> = match (builtin_kind, method_name.as_str()) {
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::AspectFunction), "call")
                                        => vec!["self_", "argument"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::AspectField), "field")
                                        => vec!["self_", "key"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::AspectField), "field_type_opt")
                                        => vec!["key"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::ScopeEntry), "use_")
                                        => vec!["loc", "code"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "bind")
                                        => vec!["xx", "loc", "sub"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "bind_sub_record")
                                        => vec!["loc", "carry", "fields"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "bind_sub_tuple")
                                        => vec!["loc", "carry", "elements"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "block")
                                        => vec!["loc", "entries"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "code_to_bind_sub")
                                        => vec!["loc", "code", "type"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "if_")
                                        => vec!["xx", "loc", "conditions", "else_"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "match_")
                                        => vec!["xx", "loc", "base", "element_key", "cases", "fallback"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "maybe_bind_sub")
                                        => vec!["xx", "loc", "cv"],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "stage")
                                        => vec!["xx", "loc", "tree", "lower_values"],
                                    _ => vec![],
                                };
                                let arg_type: Rc<dyn Type> = build_aspect_arg_record_type(&arg_field_names);
                                if let Some(arg_ib) =
                                    crate::builtin::aspect_initial_bind_get_maybe(&xx, arg_type.as_ref()) {
                                    body_scope.insert(
                                        ConstKey::String("arg".to_string()),
                                        arg_ib.initial_bind(arg_id),
                                    );
                                }
                            },
                            _ => {
                                if let Some(self_ib) =
                                    crate::builtin::aspect_initial_bind_get_maybe(&xx, underlying_type.as_ref()) {
                                    body_scope.insert(
                                        ConstKey::String("self".to_string()),
                                        self_ib.initial_bind(self_id),
                                    );
                                }
                                // Bind `arg` to the method's declared
                                // `arg_type` — user-defined (non-builtin)
                                // interface methods like `to_code(arg: nvalue)`
                                // dispatch through normal JS call, so `arg` is
                                // the function parameter at v8.
                                let iface_method_arg_type = {
                                    let iface_methods = impl_data.interface.methods.borrow();
                                    iface_methods[method_impl.interface_method_index]
                                        .arg_type
                                        .borrow()
                                        .clone()
                                };
                                if let Some(arg_type) = iface_method_arg_type {
                                    if let Some(arg_ib) =
                                        crate::builtin::aspect_initial_bind_get_maybe(&xx, arg_type.as_ref())
                                    {
                                        body_scope.insert(
                                            ConstKey::String("arg".to_string()),
                                            arg_ib.initial_bind(arg_id),
                                        );
                                    }
                                }
                            },
                        }
                        Rc::new(ExprContext {
                            vx: body_ex,
                            import_state: xx.import_state.clone(),
                            scope: Rc::new(Scope {
                                values: RefCell::new(body_scope),
                                parent: None,
                            }),
                            target: target.clone(),
                        })
                    };
                    let impl_result = const_eval(&method_xx, impl_expr_ast);
                    if let Some(errs) = impl_result.errors {
                        return Err(errs);
                    }
                    let impl_cv = impl_result.v.unwrap();
                    let impl_cv = {
                        let r = typecheck_convert(&method_xx, root_loc, ret_type.as_ref(), impl_cv);
                        if let Some(errs) = r.errors {
                            return Err(errs);
                        }
                        r.v.unwrap()
                    };
                    let impl_code =
                        impl_cv
                            .code
                            .cast::<CodeDynamic>()
                            .into_res()?
                            .expect("Interface method body must produce JS code")
                            .clone();
                    // Snapshot the body's externals (see same call above).
                    let captured = method_xx.vx.externals.borrow().clone();
                    *method_impl.captured_externals.borrow_mut() = captured.clone();
                    def_data_methods.push(CompiledDataMethod {
                        scope_entry_id: method_id,
                        body: CompiledDataMethodBody::Fresh {
                            self_scope_entry_id: self_id,
                            data_field: data_field.clone(),
                            impl_code,
                            is_arg_method: matches!(
                                builtin_kind,
                                Some(value::type_source_interface::BuiltinInterfaceKind::Type)
                            ),
                            outer_self_id,
                            arg_param_id: arg_id,
                        },
                        captured_externals: captured,
                    });
                }
            }
        }
    }
    let has_js = stage1_code.is_some() || !def_funcs.is_empty() || !def_data_methods.is_empty();
    if !has_js {
        let logs = dynamic_scope.get_slot::<V8LogStore>().unwrap().logs.take();
        return Ok(EvalResult {
            value: fallback_value.expect("No JS and no fallback value"),
            logs,
        });
    }
    let mut code1 = JsSourceBuilder::default();

    // Emit precompiled (imported) functions first so their JS bindings exist
    // before any fresh function body that might reference them.
    for js in &precompiled_def_funcs {
        for line in js.lines() {
            code1.push(root_loc, line.to_string());
        }
    }

    // Render definition set function implementations as JS functions.
    // Wrap each in a block so captured externals live as block-scoped `let`s
    // (closure-captured by the function), and expose the function name via an
    // outer `var` hoist. See same pattern in the data-type method render below.
    for (scope_id, arg_scope_id, impl_code, captured_externals) in &def_funcs {
        let start_line = code1.lines.len();
        let func_name = scope_entry_id_dynamic_key(*scope_id);
        code1.push(root_loc, format!("var {};", func_name));
        code1.push(root_loc, "{".to_string());
        for (id, _) in captured_externals {
            let local_key = scope_entry_id_dynamic_key(*id);
            code1.push(
                root_loc,
                format!(
                    "let {} = globalThis[\"{}_{}\"];",
                    local_key, func_name, local_key,
                ),
            );
        }
        if let Some(arg_id) = arg_scope_id {
            code1.push(
                root_loc,
                format!("{} = function({}) {{", func_name, scope_entry_id_dynamic_key(*arg_id)),
            );
        } else {
            code1.push(root_loc, format!("{} = function() {{", func_name));
        }
        let body_var = render_code(&mut code1, impl_code);
        code1.push(root_loc, format!("return {};", body_var));
        code1.push(root_loc, "};".to_string());
        code1.push(root_loc, "}".to_string());
        let compiled = code1.lines[start_line..].join("\n");
        for ds in ex.ix.definition_sets.borrow().iter() {
            for func_data in ds.functions.borrow().iter() {
                if func_data.scope_entry_id.get() == Some(*scope_id) {
                    *func_data.compiled_js.borrow_mut() = Some(compiled.clone());
                }
            }
        }
    }

    // Render data type method implementations as JS functions. Normal
    // methods use `this` to access the wrapper object (self = this.<data_field>).
    // builtin-`type` interface methods take their `self`-bound entry as a
    // direct function parameter — that entry actually represents the cast
    // `arg`, not the data type's underlying value.
    for method in &def_data_methods {
        match &method.body {
            CompiledDataMethodBody::Fresh { self_scope_entry_id, data_field, impl_code, is_arg_method, outer_self_id, arg_param_id } => {
                let start_line = code1.lines.len();
                let func_name = scope_entry_id_dynamic_key(method.scope_entry_id);
                let self_key = scope_entry_id_dynamic_key(*self_scope_entry_id);
                // Wrap in a block so captured externals live as block-scoped
                // `let`s — the inner `function ...` closes over them — and the
                // function name escapes via the outer `var` hoist. This gives
                // each body its own `a##` namespace without colliding with
                // sibling bodies' externals.
                code1.push(root_loc, format!("var {};", func_name));
                code1.push(root_loc, "{".to_string());
                for (id, _) in &method.captured_externals {
                    let local_key = scope_entry_id_dynamic_key(*id);
                    code1.push(
                        root_loc,
                        format!(
                            "let {} = globalThis[\"{}_{}\"];",
                            local_key, func_name, local_key,
                        ),
                    );
                }
                if *is_arg_method {
                    // Type interface impl methods bind self_key directly to
                    // the function's argument parameter — `self` IS `arg`
                    // per that interface's convention.
                    code1.push(root_loc, format!("{} = function({}) {{", func_name, self_key));
                } else {
                    // Every other method body: function takes `arg` as
                    // parameter and `self` is read from `this[data_field]`.
                    let arg_key = scope_entry_id_dynamic_key(*arg_param_id);
                    code1.push(root_loc, format!("{} = function({}) {{", func_name, arg_key));
                    code1.push(
                        root_loc,
                        format!(
                            "let {} = this[{}];",
                            self_key,
                            serde_json::to_string(&ConstKey::String(data_field.clone()).to_str()).unwrap(),
                        ),
                    );
                }
                // `outer_self` binding (the v8 instance itself). Only emit
                // when the body actually references it — keeps the JS output
                // unchanged for bodies that don't use it.
                if code_uses_binding(impl_code, *outer_self_id) {
                    code1.push(root_loc, format!("let {} = this;", scope_entry_id_dynamic_key(*outer_self_id)));
                }
                let body_var = render_code(&mut code1, impl_code);
                code1.push(root_loc, format!("return {};", body_var));
                code1.push(root_loc, "};".to_string());
                code1.push(root_loc, "}".to_string());

                // Store compiled JS for later serialization
                let compiled = code1.lines[start_line..].join("\n");
                for ds in ex.ix.definition_sets.borrow().iter() {
                    for dt_data in ds.data_types.borrow().iter() {
                        for m in dt_data.methods.borrow().iter() {
                            if m.scope_entry_id.get() == Some(method.scope_entry_id) {
                                *m.compiled_js.borrow_mut() = Some(compiled.clone());
                            }
                        }
                        for impl_data in dt_data.interface_impls.borrow().iter() {
                            for m in &impl_data.method_impls {
                                if m.scope_entry_id.get() == Some(method.scope_entry_id) {
                                    *m.compiled_js.borrow_mut() = Some(compiled.clone());
                                }
                            }
                        }
                    }
                }
            },
            CompiledDataMethodBody::Precompiled(js) => {
                for line in js.lines() {
                    code1.push(root_loc, line.to_string());
                }
            },
        }
    }

    // Render main code. Wrap the main body in a block scope so its top-level
    // `let eN;` declarations don't collide across stages on the shared v8
    // context. The block's completion value is the last expression statement
    // inside, which v8 returns as the script result. The `/*main*/` marker
    // lets test helpers identify and strip this wrapping unambiguously.
    code1.push(root_loc, "{/*main*/".to_string());
    if let Some(code) = stage1_code {
        let root_var = render_code(&mut code1, &code);
        code1.push(root_loc, format!("{};", root_var));
    } else {
        code1.push(root_loc, "null;".to_string());
    }
    code1.push(root_loc, "}/*main*/".to_string());

    // Register __stage v8 function Signature: __stage(tree, lift1, val1, lift2, val2,
    // ...) If lift is null, val is already a StagedExprCell external. If lift is a
    // StageLiftCell external, val is a raw v8 value to lift.
    {
        fn stage_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let tree_cell: Rc<expr::StagedExprCell> =
                from_v8_ref::<Rc<expr::StagedExprCell>>(&*args.get(0)).clone();
            let tree_value = tree_cell.value.borrow_mut().take().expect("stage tree already consumed");
            let tree = must_downcast_ref::<Expr>(tree_value.as_ref()).clone();

            enum StageLowerValue {
                Expr(Expr),
                Value(Rc<dyn Value>),
            }

            let mut values: Vec<StageLowerValue> = Vec::new();
            let mut i = 1;
            while i < args.length() {
                let lift_arg = args.get(i);
                let val_arg = args.get(i + 1);
                i += 2;
                if lift_arg.is_null_or_undefined() {
                    // Already a StagedExprCell external
                    let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*val_arg).clone();
                    let value = cell.value.borrow_mut().take().expect("stage lower value already consumed");
                    if (value.as_ref() as &dyn Any).is::<Expr>() {
                        let expr =
                            Rc::try_unwrap(downcast_value::<Expr>(value))
                                .ok()
                                .expect("Stage lower Expr: Rc not unique");
                        values.push(StageLowerValue::Expr(expr));
                    } else {
                        values.push(StageLowerValue::Value(value));
                    }
                } else {
                    // Lift the raw v8 value
                    let lift_cell: Rc<StageLiftCell> = from_v8_ref::<Rc<StageLiftCell>>(&*lift_arg).clone();
                    let lift_xx = ExprContext {
                        vx: lift_cell.vx.clone(),
                        import_state: lift_cell.import_state.clone(),
                        scope: lift_cell.scope.clone(),
                        target: lift_cell.target.clone(),
                    };
                    let lift =
                        aspect_lift_dynamic_get(&lift_xx, lift_cell.loc, lift_cell.source_type.as_ref())
                            .into_res()
                            .expect("__stage: source type missing AspectLiftDynamic");
                    let value = lift.lift(&lift_xx, &*scope, val_arg).expect("__stage: lift failed");
                    values.push(StageLowerValue::Value(value));
                }
            }

            /// Clone an expression tree, replacing Lower nodes at net depth 1 (lower_depth -
            /// stage_depth == 1) with Expr values from the iterator.
            fn clone_with_lower_substitutions(
                expr: &Expr,
                sd: usize,
                ld: usize,
                values: &mut dyn Iterator<Item = StageLowerValue>,
            ) -> Expr {
                match &expr.r#type {
                    Expr_::Lower(e) => {
                        let new_ld = ld + 1;
                        if new_ld - sd == 1 {
                            match values.next().expect("Not enough lower values for substitution") {
                                StageLowerValue::Expr(expr_val) => expr_val,
                                StageLowerValue::Value(value) => Expr {
                                    loc: expr.loc,
                                    r#type: Expr_::Literal(crate::eval::expr::make_lit_value_once(value)),
                                },
                            }
                        } else {
                            Expr {
                                loc: expr.loc,
                                r#type: Expr_::Lower(
                                    ExprLower {
                                        expr: Rc::new(clone_with_lower_substitutions(&e.expr, sd, new_ld, values)),
                                    },
                                ),
                            }
                        }
                    },
                    Expr_::Stage(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Stage(
                            ExprStage { expr: Rc::new(clone_with_lower_substitutions(&e.expr, sd + 1, ld, values)) },
                        ),
                    },
                    Expr_::Graft(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Graft(
                            expr::ExprGraft {
                                expr: Rc::new(clone_with_lower_substitutions(&e.expr, sd, ld, values)),
                            },
                        ),
                    },
                    Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => expr.clone(),
                    Expr_::As(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::As(ExprAs {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            target: Rc::new(clone_with_lower_substitutions(&e.target, sd, ld, values)),
                        }),
                    },
                    Expr_::Call(c) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Call(ExprCall {
                            func: Rc::new(clone_with_lower_substitutions(&c.func, sd, ld, values)),
                            arg: Rc::new(clone_with_lower_substitutions(&c.arg, sd, ld, values)),
                        }),
                    },
                    Expr_::Bind(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Bind(ExprBind {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            value: Rc::new(clone_with_lower_substitutions(&e.value, sd, ld, values)),
                        }),
                    },
                    Expr_::Set(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Set(ExprSet {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            value: Rc::new(clone_with_lower_substitutions(&e.value, sd, ld, values)),
                        }),
                    },
                    Expr_::Field(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Field(ExprField {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            key: Rc::new(clone_with_lower_substitutions(&e.key, sd, ld, values)),
                        }),
                    },
                    Expr_::Block(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Block(
                            ExprBlock {
                                exprs: e
                                    .exprs
                                    .iter()
                                    .map(|sub| clone_with_lower_substitutions(sub, sd, ld, values))
                                    .collect(),
                            },
                        ),
                    },
                    Expr_::Tuple(t) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Tuple(
                            ExprTuple {
                                elements: t
                                    .elements
                                    .iter()
                                    .map(|e| clone_with_lower_substitutions(e, sd, ld, values))
                                    .collect(),
                            },
                        ),
                    },
                    Expr_::Record(r) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Record(expr::ExprRecord { entries: r.entries.iter().map(|(k, v)| {
                            (
                                clone_with_lower_substitutions(k, sd, ld, values),
                                clone_with_lower_substitutions(v, sd, ld, values),
                            )
                        }).collect() }),
                    },
                    Expr_::If(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::If(expr::ExprIf {
                            conditions: e.conditions.iter().map(|b| expr::ExprConditionBranch {
                                condition: clone_with_lower_substitutions(&b.condition, sd, ld, values),
                                body: clone_with_lower_substitutions(&b.body, sd, ld, values),
                            }).collect(),
                            else_: e
                                .else_
                                .as_ref()
                                .map(|el| Rc::new(clone_with_lower_substitutions(el, sd, ld, values))),
                        }),
                    },
                    Expr_::OperatorLt(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorLt(expr::ExprOperatorLt {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            reference: Rc::new(clone_with_lower_substitutions(&e.reference, sd, ld, values)),
                        }),
                    },
                    Expr_::OperatorEq(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorEq(expr::ExprOperatorEq {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            reference: Rc::new(clone_with_lower_substitutions(&e.reference, sd, ld, values)),
                        }),
                    },
                    Expr_::OperatorAdd(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorAdd(expr::ExprOperatorAdd {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            reference: Rc::new(clone_with_lower_substitutions(&e.reference, sd, ld, values)),
                        }),
                    },
                    Expr_::Foreach(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Foreach(ExprForeach {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            element_key: Rc::new(clone_with_lower_substitutions(&e.element_key, sd, ld, values)),
                            body: Rc::new(clone_with_lower_substitutions(&e.body, sd, ld, values)),
                        }),
                    },
                    Expr_::Match(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Match(ExprMatch {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            element_key: Rc::new(clone_with_lower_substitutions(&e.element_key, sd, ld, values)),
                            cases: e.cases.iter().map(|c| ExprMatchBranch {
                                condition: clone_with_lower_substitutions(&c.condition, sd, ld, values),
                                body: clone_with_lower_substitutions(&c.body, sd, ld, values),
                            }).collect(),
                            fallback: e
                                .fallback
                                .as_ref()
                                .map(|f| Rc::new(clone_with_lower_substitutions(f, sd, ld, values))),
                        }),
                    },
                }
            }

            let new_tree = clone_with_lower_substitutions(&tree, 0, 0, &mut values.into_iter());
            let result_cell =
                Rc::new(expr::StagedExprCell { value: RefCell::new(Some(Rc::new(new_tree) as Rc<dyn Value>)) });
            let ext = to_v8(scope, result_cell);
            rv.set(ext.into());
        }

        if register_v8 {
            let stage_fn = v8::Function::builder(stage_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(STAGE_FN_SCOPE_ID)).unwrap().cast(),
                    stage_fn.into(),
                );
        }
    }

    // Register __record_to_type and __tuple_to_type mortar functions. These convert
    // JS objects/arrays into Rc`<dyn Type>` externals at runtime.
    {
        /// Recursively convert a v8 value (external, object, or array) to Rc`<dyn Type>`.
        fn v8_value_to_type(scope: &mut v8::PinnedRef<v8::HandleScope>, value: v8::Local<v8::Value>) -> Rc<dyn Type> {
            if value.is_external() {
                let any = from_v8_any(&*value);
                if any.is::<Rc<dyn Type>>() {
                    return crate::invariant::must_downcast_ref::<Rc<dyn Type>>(any).clone();
                }
                // Source-defined ref wrappers — promote each to the matching `dyn Type`.
                if let Some(iface_ref) = any.downcast_ref::<Rc<crate::eval::value::type_source_interface::SourceInterfaceRef>>() {
                    return Rc::new(crate::eval::value::type_source_interface::TypeSourceInterfaceConst {
                        data: iface_ref.data.clone(),
                    });
                }
                if let Some(tu_ref) = any.downcast_ref::<Rc<crate::eval::value::type_source_tagged_union::SourceTaggedUnionRef>>() {
                    return Rc::new(crate::eval::value::type_source_tagged_union::r#const::TypeSourceTaggedUnionConst {
                        data: tu_ref.data.clone(),
                    });
                }
                if let Some(dt_ref) = any.downcast_ref::<Rc<crate::eval::value::type_source_data_type::SourceDataTypeRef>>() {
                    return Rc::new(crate::eval::value::type_source_data_type::dynamic::TypeSourceDataTypeDynamic {
                        data: dt_ref.data.clone(),
                    });
                }
                return from_v8_ref::<Rc<dyn Type>>(&*value).clone();
            }
            panic!("v8_value_to_type: expected `Rc<dyn Type>` external, got something else");
        }

        fn record_to_type_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            // Shallow: each property value must already be an `Rc<dyn Type>` external.
            let value = args.get(0);
            let obj = v8::Local::<v8::Object>::try_from(value).expect("__record_to_type: expected JS object");
            let keys = obj.get_own_property_names(scope, v8::GetPropertyNamesArgs::default()).unwrap();
            let mut fields = std::collections::BTreeMap::new();
            for i in 0 .. keys.length() {
                let key = keys.get_index(scope, i).unwrap();
                let key_str = key.to_string(scope).unwrap().to_rust_string_lossy(scope);
                let val = obj.get(scope, key).unwrap();
                let const_key =
                    ConstKey::from_str(
                        &key_str,
                    ).expect(&format!("Invalid ConstKey in v8 object property: {}", key_str));
                fields.insert(const_key, RecordField::Mortar(v8_value_to_type(scope, val)));
            }
            let type_: Rc<dyn Type> = Rc::new(Record {
                base: None,
                fields,
            });
            let ext = rc_type_to_v8(scope, type_);
            rv.set(ext.into());
        }

        fn tuple_to_type_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            // Shallow: each element must already be an `Rc<dyn Type>` external.
            let value = args.get(0);
            let arr = v8::Local::<v8::Array>::try_from(value).expect("__tuple_to_type: expected JS array");
            let len = arr.length();
            let mut elements = Vec::with_capacity(len as usize);
            for i in 0 .. len {
                let elem = arr.get_index(scope, i).unwrap();
                elements.push(TupleElement::Mortar(v8_value_to_type(scope, elem)));
            }
            let type_: Rc<dyn Type> = Rc::new(Tuple {
                base: None,
                elements,
            });
            let ext = rc_type_to_v8(scope, type_);
            rv.set(ext.into());
        }

        fn cve_extract_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            // arg(0) = the CodeValueExtern.
            // Return the raw JS value carried by the inner CV — for const
            // string values, the JS string; for dynamic values, the v8 value
            // produced by evaluating the CV's code.
            let cve_v8 = args.get(0);
            let cve_cell: Rc<RefCell<Option<CodeValue>>> =
                from_v8_ref::<Rc<RefCell<Option<CodeValue>>>>(&*cve_v8).clone();
            let inner_value: Rc<dyn crate::langhelp::Value> = {
                let guard = cve_cell.borrow();
                let cv = guard.as_ref().expect("__cve_extract: inner CodeValue already consumed");
                cv.value.clone()
            };
            let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
            let xx = Rc::new(ExprContext {
                vx: ex_slot.ex.clone(),
                import_state: Rc::new(Cell::new(ex_slot.ex.ix.import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                target: Rc::new(TargetDynamic) as Rc<dyn Target>,
            });
            let loc = (ex_slot.ex.ix.source_id, crate::syntax::ExprId(0));
            let value_type = inner_value.type_(&xx, loc);
            // Fast path for const strings — return a v8::String.
            if let Some(aspect_str) =
                crate::builtin::aspect_string_const_get_maybe(&xx, value_type.as_ref()) {
                let s = aspect_str.get(inner_value.as_ref());
                rv.set(v8::String::new(scope, &s).unwrap().into());
                return;
            }
            panic!("__cve_extract: unsupported inner CV value type ({})", value_type.desc());
        }

        fn cve_get_value_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            // arg(0) = the CodeValueExtern (v8 external holding
            // `Rc<RefCell<Option<CodeValue>>>`). Returns the wrapped CV's
            // `value` slot as a v8::External wrapping `Rc<dyn Value>` — no
            // unwrapping into per-Value-shape JS primitives here. Consumers
            // that want the concrete shape (e.g. a JS instance for a
            // user-defined data value) call `__cast_value` to unwrap with an
            // expected static type.
            let cve_v8 = args.get(0);
            let cve_cell: Rc<RefCell<Option<CodeValue>>> =
                from_v8_ref::<Rc<RefCell<Option<CodeValue>>>>(&*cve_v8).clone();
            let value: Rc<dyn crate::langhelp::Value> = {
                let borrow = cve_cell.borrow();
                borrow
                    .as_ref()
                    .expect("__cve_get_value: inner CodeValue already consumed")
                    .value
                    .clone()
            };
            let ext = rc_value_to_v8(scope, value);
            rv.set(ext.into());
        }

        fn value_get_type_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            // arg(0) = a v8::External wrapping `Rc<dyn Value>` (the shape
            // `__cve_get_value` returns). Every `Value` impl provides
            // `type_()`; let it dispatch — for `ValueUserDefined` that
            // routes through `_im_v0` on the bundle and gives the
            // source-language type, for const value shapes it returns the
            // matching static singleton, etc.
            let value: Rc<dyn crate::langhelp::Value> = rc_value_from_v8(&*args.get(0));
            let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
            let target: Rc<dyn Target> = Rc::new(TargetDynamic);
            let xx = Rc::new(ExprContext {
                vx: ex_slot.ex.clone(),
                import_state: Rc::new(Cell::new(ex_slot.ex.ix.import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                target,
            });
            let loc = (ex_slot.ex.ix.source_id, crate::syntax::ExprId(0));
            let type_ref: Rc<dyn Type> = value.type_(&xx, loc);
            let type_ref_v8 = crate::eval::dynamic_wrap::DynamicWrap::into_v8(
                crate::eval::value::type_ref_v8::TypeRefV8 { type_ref },
                scope,
            );
            rv.set(type_ref_v8);
        }

        fn aspect_lookup_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            // arg(0) = the JS object produced by `dynamic_wrap!(struct
            // TypeRefV8)` — a plain Object whose `_d` field carries the
            // backing `External(Rc<TypeRefV8>)`. The dynamic_wrap struct
            // macro doesn't impl `from_v8`, so unwrap the `_d` slot
            // ourselves.
            // arg(1) = a `Unique` external (the aspect identity)
            let arg0 = args.get(0);
            let arg1 = args.get(1);
            let arg0_obj = v8::Local::<v8::Object>::try_from(arg0)
                .expect("__aspect_lookup: expected JS Object for TypeRefV8");
            let d_key = v8::String::new(scope, "_d")
                .expect("__aspect_lookup: alloc _d key");
            let d_val = arg0_obj
                .get(scope, d_key.into())
                .expect("__aspect_lookup: TypeRefV8 missing _d field");
            let type_ref: Rc<dyn Type> =
                from_v8_ref::<Rc<crate::eval::value::type_ref_v8::TypeRefV8>>(&*d_val)
                    .type_ref
                    .clone();
            let unique: crate::langhelp::Unique =
                from_v8_ref::<crate::langhelp::Unique>(&*arg1).clone();
            let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
            let target: Rc<dyn Target> = Rc::new(TargetDynamic);
            let xx = Rc::new(ExprContext {
                vx: ex_slot.ex.clone(),
                import_state: Rc::new(Cell::new(ex_slot.ex.ix.import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                target,
            });
            let result = v8::Object::new(scope);
            match type_ref.aspect(&xx, &unique) {
                Some(aspect_box) => {
                    // Invariant (held by `TypeUserDefined::aspect` for
                    // is_aspect identities, plus all builtin aspect types):
                    // the inner type is `Box<dyn AspectField>`. We upcast
                    // to `dyn Aspect` to read `v8_instance()` — no RTTI.
                    let aspect_field: Box<dyn crate::builtin::AspectField> =
                        crate::invariant::must_downcast_box::<
                            Box<dyn crate::builtin::AspectField>,
                        >(aspect_box);
                    let aspect_dyn: &dyn crate::langhelp::Aspect = &*aspect_field;
                    let instance_v8: v8::Local<v8::Value> = match aspect_dyn.v8_instance() {
                        Some(g) => v8::Local::new(scope, &g).into(),
                        None => v8::null(scope).into(),
                    };
                    let some_key = v8::String::new(
                        scope,
                        &ConstKey::String("some".to_string()).to_str(),
                    )
                        .unwrap();
                    result.set(scope, some_key.into(), instance_v8);
                },
                None => {
                    let none_key = v8::String::new(
                        scope,
                        &ConstKey::String("none".to_string()).to_str(),
                    )
                        .unwrap();
                    let null_v8: v8::Local<v8::Value> = v8::null(scope).into();
                    result.set(scope, none_key.into(), null_v8);
                },
            }
            rv.set(result.into());
        }

        if register_v8 {
            let record_to_type_fn = v8::Function::builder(record_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(RECORD_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    record_to_type_fn.into(),
                );
            let tuple_to_type_fn = v8::Function::builder(tuple_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(TUPLE_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    tuple_to_type_fn.into(),
                );
            let cve_get_value_fn =
                v8::Function::builder(cve_get_value_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(CVE_GET_VALUE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    cve_get_value_fn.into(),
                );
            let cve_extract_fn = v8::Function::builder(cve_extract_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(CVE_EXTRACT_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    cve_extract_fn.into(),
                );
            let value_get_type_fn =
                v8::Function::builder(value_get_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(VALUE_GET_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    value_get_type_fn.into(),
                );
            let aspect_lookup_fn =
                v8::Function::builder(aspect_lookup_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(ASPECT_LOOKUP_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    aspect_lookup_fn.into(),
                );
            // `__cast_value(value, type)` — unwrap `Rc<dyn Value>` to a JS
            // instance, asserting the value's data type matches `type`'s
            // (`TypeSourceDataTypeConst`) data identity. Used by source-level
            // `cast_value(value, <user_data_type>.type)` to bridge from the
            // opaque `value`-iface form (what `__cve_get_value` now returns)
            // back to a concrete user-defined instance at a known type.
            fn cast_value_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let value: Rc<dyn crate::langhelp::Value> =
                    rc_value_from_v8(&*args.get(0));
                let type_: Rc<dyn Type> = rc_type_from_v8(&*args.get(1));
                let type_const = crate::invariant::must_downcast_ref::<
                    crate::eval::value::type_source_data_type::TypeSourceDataTypeConst,
                >(type_.as_ref());
                let vud = crate::invariant::must_downcast_ref::<
                    crate::eval::value::value_user_defined::dynamic::ValueUserDefined,
                >(value.as_ref());
                // Identity check: the value's data type must match the
                // template's. `ValueUserDefined` doesn't currently carry its
                // own data ref (the data is implicit in the bundle); for now
                // we trust the source-level static signature placed the right
                // value here. Once `ValueUserDefined` carries data, switch to
                // `assert!(vud.data.identity == type_const.data.identity)`.
                let _ = type_const;
                let instance = v8::Local::new(scope, &vud.bundle.instance);
                rv.set(instance.into());
            }
            let cast_value_fn =
                v8::Function::builder(cast_value_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(CAST_VALUE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    cast_value_fn.into(),
                );
            fn data_type_ref_to_type_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let dt_ref: Rc<crate::eval::value::type_source_data_type::SourceDataTypeRef> =
                    from_v8_ref::<Rc<crate::eval::value::type_source_data_type::SourceDataTypeRef>>(
                        &*args.get(0),
                    )
                        .clone();
                let type_: Rc<dyn Type> =
                    Rc::new(crate::eval::value::type_source_data_type::TypeSourceDataTypeConst {
                        data: dt_ref.data.clone(),
                    });
                let ext = rc_type_to_v8(scope, type_);
                rv.set(ext.into());
            }
            let data_type_ref_to_type_fn =
                v8::Function::builder(data_type_ref_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    data_type_ref_to_type_fn.into(),
                );
            fn tagged_union_ref_to_type_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let tu_ref: Rc<crate::eval::value::type_source_tagged_union::SourceTaggedUnionRef> =
                    from_v8_ref::<Rc<crate::eval::value::type_source_tagged_union::SourceTaggedUnionRef>>(
                        &*args.get(0),
                    )
                        .clone();
                let type_: Rc<dyn Type> =
                    Rc::new(
                        crate::eval::value::type_source_tagged_union::r#const::TypeSourceTaggedUnionConst {
                            data: tu_ref.data.clone(),
                        },
                    );
                let ext = rc_type_to_v8(scope, type_);
                rv.set(ext.into());
            }
            let tagged_union_ref_to_type_fn =
                v8::Function::builder(tagged_union_ref_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    tagged_union_ref_to_type_fn.into(),
                );
            fn interface_ref_to_type_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let iface_ref: Rc<crate::eval::value::type_source_interface::SourceInterfaceRef> =
                    from_v8_ref::<Rc<crate::eval::value::type_source_interface::SourceInterfaceRef>>(
                        &*args.get(0),
                    )
                        .clone();
                let type_: Rc<dyn Type> =
                    Rc::new(crate::eval::value::type_source_interface::TypeSourceInterfaceConst {
                        data: iface_ref.data.clone(),
                    });
                let ext = rc_type_to_v8(scope, type_);
                rv.set(ext.into());
            }
            let interface_ref_to_type_fn =
                v8::Function::builder(interface_ref_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(INTERFACE_REF_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    interface_ref_to_type_fn.into(),
                );
            fn function_const_to_type_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let func_ref: Rc<crate::eval::value::type_source_function::TypeSourceFunctionConst> =
                    from_v8_ref::<Rc<crate::eval::value::type_source_function::TypeSourceFunctionConst>>(
                        &*args.get(0),
                    )
                        .clone();
                let type_: Rc<dyn Type> =
                    Rc::new(crate::eval::value::type_source_function::TypeSourceFunctionConst {
                        data: func_ref.data.clone(),
                    });
                let ext = rc_type_to_v8(scope, type_);
                rv.set(ext.into());
            }
            let function_const_to_type_fn =
                v8::Function::builder(function_const_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    function_const_to_type_fn.into(),
                );
            fn data_type_constructor_to_type_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let ctor_ref: Rc<
                    crate::eval::value::type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
                > =
                    from_v8_ref::<
                        Rc<
                            crate::eval::value::type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
                        >,
                    >(&*args.get(0)).clone();
                let type_: Rc<dyn Type> = Rc::new(
                    crate::eval::value::type_source_data_type_constructor::TypeSourceDataTypeConstructorConst {
                        data: ctor_ref.data.clone(),
                    },
                );
                let ext = rc_type_to_v8(scope, type_);
                rv.set(ext.into());
            }
            let data_type_constructor_to_type_fn =
                v8::Function::builder(data_type_constructor_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    data_type_constructor_to_type_fn.into(),
                );
            fn tagged_union_constructor_to_type_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let ctor_ref: Rc<
                    crate::eval::value::type_source_tagged_union_constructor::SourceTaggedUnionConstructorRef,
                > =
                    from_v8_ref::<
                        Rc<
                            crate::eval::value::type_source_tagged_union_constructor::SourceTaggedUnionConstructorRef,
                        >,
                    >(&*args.get(0)).clone();
                let type_: Rc<dyn Type> = Rc::new(
                    crate::eval::value::type_source_tagged_union_constructor::r#const::TypeSourceTaggedUnionConstructorConst {
                        data: ctor_ref.data.clone(),
                        variant_index: ctor_ref.variant_index,
                    },
                );
                let ext = rc_type_to_v8(scope, type_);
                rv.set(ext.into());
            }
            let tagged_union_constructor_to_type_fn =
                v8::Function::builder(tagged_union_constructor_to_type_callback)
                    .build(dynamic_scope)
                    .unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    tagged_union_constructor_to_type_fn.into(),
                );
            // `__data_instance_to_type(type_template, instance)` —
            // wraps a source-defined data type *instance* into an
            // `Rc<dyn Type>` external. arg(0) is a compile-time-captured
            // `TypeSourceDataTypeConst` external carrying this data type's
            // `Gc<SourceDataTypeData>`; arg(1) is the runtime v8 instance
            // whose handle becomes the bundle of the produced
            // `TypeUserDefined`.
            fn data_instance_to_type_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let template_type: Rc<dyn Type> =
                    from_v8_ref::<Rc<dyn Type>>(&*args.get(0)).clone();
                let template = crate::invariant::must_downcast_ref::<
                    crate::eval::value::type_source_data_type::TypeSourceDataTypeConst,
                >(template_type.as_ref());
                let instance = v8::Local::<v8::Object>::try_from(args.get(1))
                    .expect("__data_instance_to_type: expected JS object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let type_: Rc<dyn Type> =
                    Rc::new(crate::eval::value::type_source_data_type::dynamic::TypeUserDefined {
                        data: template.data.clone(),
                        bundle: Some(Rc::new(
                            crate::eval::isolate_bundle::IsolateBundle { instance: instance_global },
                        )),
                    });
                let ext = rc_type_to_v8(scope, type_);
                rv.set(ext.into());
            }
            let data_instance_to_type_fn =
                v8::Function::builder(data_instance_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    data_instance_to_type_fn.into(),
                );
            // `__data_instance_to_target(type_template, instance)` — parallel to
            // `__data_instance_to_type`, but returns a `v8::External` wrapping
            // `Rc<dyn Target>` built from a `UserDefinedTarget` adapter.
            fn data_instance_to_target_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let _template_type: Rc<dyn Type> =
                    from_v8_ref::<Rc<dyn Type>>(&*args.get(0)).clone();
                let instance = v8::Local::<v8::Object>::try_from(args.get(1))
                    .expect("__data_instance_to_target: expected JS object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let target: Rc<dyn Target> = Rc::new(
                    crate::eval::user_defined_scope_dispatch::UserDefinedTarget {
                        instance: instance_global,
                    },
                );
                let ext = rc_target_to_v8(scope, target);
                rv.set(ext.into());
            }
            let data_instance_to_target_fn =
                v8::Function::builder(data_instance_to_target_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    data_instance_to_target_fn.into(),
                );
            // `__data_instance_to_scope_entry(type_template, instance)` —
            // parallel to `__data_instance_to_type`, but returns a
            // `v8::External` wrapping `Rc<dyn ScopeEntry>` built from a
            // `UserDefinedScopeEntry` adapter.
            fn data_instance_to_scope_entry_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let _template_type: Rc<dyn Type> =
                    from_v8_ref::<Rc<dyn Type>>(&*args.get(0)).clone();
                let instance = v8::Local::<v8::Object>::try_from(args.get(1))
                    .expect("__data_instance_to_scope_entry: expected JS object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let entry: Rc<dyn crate::langhelp::ScopeEntry> = Rc::new(
                    crate::eval::user_defined_scope_dispatch::UserDefinedScopeEntry {
                        instance: instance_global,
                    },
                );
                let ext = rc_scope_entry_to_v8(scope, entry);
                rv.set(ext.into());
            }
            let data_instance_to_scope_entry_fn =
                v8::Function::builder(data_instance_to_scope_entry_callback)
                    .build(dynamic_scope)
                    .unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(DATA_INSTANCE_TO_SCOPE_ENTRY_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    data_instance_to_scope_entry_fn.into(),
                );
            // `__data_instance_to_value(type_template, instance)` — parallel
            // to `__data_instance_to_type`, but returns a `v8::External`
            // wrapping `Rc<dyn Value>` built from a `ValueUserDefined`
            // holding the v8 instance handle. `ValueUserDefined::type_()`
            // later invokes `value.type` (`_im_v0`) on this instance to
            // recover its source-language type.
            fn data_instance_to_value_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let _template_type: Rc<dyn Type> =
                    from_v8_ref::<Rc<dyn Type>>(&*args.get(0)).clone();
                let instance = v8::Local::<v8::Object>::try_from(args.get(1))
                    .expect("__data_instance_to_value: expected JS object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let value: Rc<dyn crate::langhelp::Value> = Rc::new(
                    crate::eval::value::value_user_defined::dynamic::ValueUserDefined {
                        bundle: Rc::new(crate::eval::isolate_bundle::IsolateBundle {
                            instance: instance_global,
                        }),
                    },
                );
                let ext = rc_value_to_v8(scope, value);
                rv.set(ext.into());
            }
            let data_instance_to_value_fn =
                v8::Function::builder(data_instance_to_value_callback)
                    .build(dynamic_scope)
                    .unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    data_instance_to_value_fn.into(),
                );
            // `__data_instance_to_aspect(instance)` — wraps an instance into
            // an `AspectUserDefined` external. Used by source code that
            // produces an `aspect` (via `option(aspect).some(self)`) from a
            // data type implementing one of the aspect interfaces.
            fn data_instance_to_aspect_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let instance = v8::Local::<v8::Object>::try_from(args.get(0))
                    .expect("__data_instance_to_aspect: expected JS object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let aspect: Rc<crate::eval::value::value_user_defined::aspect::AspectUserDefined> = Rc::new(
                    crate::eval::value::value_user_defined::aspect::AspectUserDefined {
                        bundle: Rc::new(crate::eval::isolate_bundle::IsolateBundle {
                            instance: instance_global,
                        }),
                    },
                );
                let ext = to_v8(scope, aspect);
                rv.set(ext.into());
            }
            let data_instance_to_aspect_fn =
                v8::Function::builder(data_instance_to_aspect_callback)
                    .build(dynamic_scope)
                    .unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    data_instance_to_aspect_fn.into(),
                );
            // `__data_instance_to_code(instance)` — wraps an instance into a
            // `CodeUserDefined` external (an `Rc<dyn CodeDyn>`). Used by
            // source data types implementing the `code_dyn` builtin interface
            // so their instances can be passed as the code slot of
            // `new_code_value`.
            fn data_instance_to_code_callback<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let instance = v8::Local::<v8::Object>::try_from(args.get(0))
                    .expect("__data_instance_to_code: expected JS object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let code: Rc<dyn crate::langhelp::CodeDyn> = Rc::new(CodeUserDefined {
                    bundle: Rc::new(crate::eval::isolate_bundle::IsolateBundle {
                        instance: instance_global,
                    }),
                });
                let ext = to_v8(scope, code);
                rv.set(ext.into());
            }
            let data_instance_to_code_fn =
                v8::Function::builder(data_instance_to_code_callback)
                    .build(dynamic_scope)
                    .unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(DATA_INSTANCE_TO_CODE_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    data_instance_to_code_fn.into(),
                );
            // `__unique_eq(a, b)` — value-equality compare for Unique
            // externals. Each `Unique` reaches v8 as a fresh `v8::External`,
            // so JS `===` is unsafe; this helper reads both Rc<Unique> and
            // returns the boolean comparison.
            fn unique_eq_callback<'s>(
                _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let a = from_v8_ref::<crate::langhelp::Unique>(&*args.get(0)).clone();
                let b = from_v8_ref::<crate::langhelp::Unique>(&*args.get(1)).clone();
                rv.set_bool(a == b);
            }
            let unique_eq_fn =
                v8::Function::builder(unique_eq_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &scope_entry_id_dynamic_key(UNIQUE_EQ_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    unique_eq_fn.into(),
                );
            cix.builtins_registered.set(true);
        }
    }

    // Set externals as globals before running the script.
    //
    // Per-method-body captured externals (stored on the method's data) are
    // installed under method-namespaced keys (`<func_name>_<local_key>`). The
    // rendered body's wrapping block aliases each into a bare `a##` binding
    // the function closes over, so sibling methods can't collide and the
    // parent's externals stay independent.
    let mut all_externals: Vec<(String, External)> = ex
        .externals
        .borrow()
        .iter()
        .map(|(id, v)| (scope_entry_id_dynamic_key(*id), v.clone()))
        .collect();
    for method in &def_data_methods {
        let func_name = scope_entry_id_dynamic_key(method.scope_entry_id);
        for (id, value) in &method.captured_externals {
            let local_key = scope_entry_id_dynamic_key(*id);
            all_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
        }
    }
    for (scope_id, _, _, captured) in &def_funcs {
        let func_name = scope_entry_id_dynamic_key(*scope_id);
        for (id, value) in captured {
            let local_key = scope_entry_id_dynamic_key(*id);
            all_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
        }
    }
    for (key_str, value) in &all_externals {
        let key = v8::String::new(dynamic_scope, key_str).unwrap().cast();
        match value {
            External::StagedExprCell(cell) => {
                let ext = to_v8(dynamic_scope, cell.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::StageLiftCell(cell) => {
                let ext = to_v8(dynamic_scope, cell.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::Type(type_) => {
                let ext = rc_type_to_v8(dynamic_scope, type_.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::Unique(u) => {
                let ext = to_v8(dynamic_scope, u.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::IsolateBundle(b) => {
                let inst = v8::Local::new(dynamic_scope, &b.instance);
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, inst.into());
            },
        }
    }
    let dynamic_source = code1.lines.join("\n");
    eprintln!("source: [[{}]]", dynamic_source);
    cix.gx.generated_mortar_scripts.lock().unwrap().push(dynamic_source.clone());
    let script =
        v8::Script::compile(dynamic_scope, v8::String::new(dynamic_scope, &dynamic_source).unwrap(), None).unwrap();
    match script.run(dynamic_scope) {
        Some(stage2) => {
            // If the const_eval value was already concrete (`Code_::None`)
            // and JS only ran for side effects (def-set methods, etc.),
            // use the fallback value directly — its type may not have
            // AspectLiftDynamic and there's nothing to lift anyway.
            let value: Rc<dyn Value> = match fallback_value {
                Some(v) => v,
                None => {
                    let value_from_js =
                        aspect_lift_dynamic_get(&xx, root_loc, value_type.as_ref()).into_res()?;
                    value_from_js.lift(&xx, &*dynamic_scope, stage2).map_err(|e| vec![ExprErr {
                        loc: root_loc,
                        errors: vec![ExprSubErr::Opaque(e)],
                    }])?
                },
            };
            let logs = dynamic_scope.get_slot::<V8LogStore>().unwrap().logs.take();
            Ok(EvalResult {
                value,
                logs,
            })
        },
        None => {
            let text;
            match dynamic_scope.exception() {
                Some(e) => {
                    text = e.to_rust_string_lossy(&dynamic_scope);
                },
                None => {
                    text = format!("Script failed to return, but no exception captured (compiler bug)");
                },
            }
            match dynamic_scope.message().and_then(|e| e.get_line_number(&dynamic_scope)) {
                Some(dynamic_line) => {
                    Err(vec![ExprErr {
                        loc: *code1.mapping.get(&(dynamic_line as usize)).unwrap_or(&root_loc),
                        errors: vec![ExprSubErr::Opaque(text)],
                    }])
                },
                None => {
                    Err(vec![ExprErr {
                        loc: root_loc,
                        errors: vec![ExprSubErr::Opaque(text)],
                    }])
                },
            }
        },
    }
}

struct EvalResult {
    logs: Vec<LogEntry>,
    value: Rc<dyn Value>,
}

fn extract_const_key(xx: &ExprContext, loc: QExprId, v: &dyn Value) -> Result<ConstKey, String> {
    let vtype = v.type_(xx, loc);
    if let Some(a) = aspect_string_const_get_maybe(xx, vtype.as_ref()) {
        return Ok(ConstKey::String(a.get(v)));
    }
    return Err(format!("TODO value type [X] must expose const key aspect [LIST] but doesn't"));
}

fn from_v8_any(value: &v8::Value) -> &dyn std::any::Any {
    assert!(value.is_external(), "from_v8_any: expected v8::External");
    let ext = unsafe {
        &*(value as *const v8::Value as *const v8::External)
    };
    let ptr = ext.value() as *const Box<dyn std::any::Any>;
    unsafe {
        &**ptr
    }
}

/// Get a reference to the value inside a `v8::External` created by `to_v8`.
pub(crate) fn from_v8_ref<T: 'static>(value: &v8::Value) -> &T {
    must_downcast_ref::<T>(from_v8_any(value))
}

#[derive(Default)]
pub(crate) struct JsSourceBuilder {
    pub(crate) lines: Vec<String>,
    pub(crate) mapping: HashMap<usize, QExprId>,
    pub(crate) next_expr_id: usize,
}

impl JsSourceBuilder {
    pub(crate) fn next_expr_id(&mut self) -> usize {
        let id = self.next_expr_id;
        self.next_expr_id += 1;
        id
    }

    pub(crate) fn push(&mut self, id: QExprId, line: String) {
        let dynamic_num = self.lines.len() + 1;
        self.mapping.insert(dynamic_num, id);
        self.lines.push(line);
    }
}

/// Extract a clone of `Rc<dyn Type>` from a `v8::External`. Only operates on
/// `dyn Type` — for concrete `Rc<T>`, use `from_v8_ref` directly.
pub(crate) fn rc_type_from_v8(value: &v8::Value) -> Rc<dyn Type> {
    from_v8_ref::<Rc<dyn Type>>(value).clone()
}

/// Store an `Rc<dyn Type>` as a `v8::External`. Only operates on `dyn Type` — for
/// concrete `Rc<T>`, use `to_v8` directly.
pub(crate) fn rc_type_to_v8<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn Type>) -> v8::Local<'s, v8::External> {
    to_v8(scope, value)
}

/// Thin `Box<dyn ScopeEntry>` wrapper around an `Rc<dyn ScopeEntry>`. Used to
/// fit `Scope.values` (which stores `Box`es) when the underlying entry was
/// constructed as a shared `Rc` (e.g. extracted from a v8::External).
pub(crate) struct RcScopeEntryWrap(pub Rc<dyn crate::langhelp::ScopeEntry>);

impl crate::langhelp::ScopeEntry for RcScopeEntryWrap {
    fn use_(&self, xx: &crate::langhelp::ExprContext, loc: QExprId, code: Code) -> CodeValue {
        self.0.use_(xx, loc, code)
    }
}

/// Extract a clone of `Rc<dyn crate::langhelp::ScopeEntry>` from a `v8::External`.
pub(crate) fn rc_scope_entry_from_v8(value: &v8::Value) -> Rc<dyn crate::langhelp::ScopeEntry> {
    from_v8_ref::<Rc<dyn crate::langhelp::ScopeEntry>>(value).clone()
}

/// Store an `Rc<dyn ScopeEntry>` as a `v8::External`.
pub(crate) fn rc_scope_entry_to_v8<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn crate::langhelp::ScopeEntry>) -> v8::Local<'s, v8::External> {
    to_v8(scope, value)
}

/// Extract a clone of `Rc<dyn Value>` from a `v8::External`.
pub(crate) fn rc_value_from_v8(value: &v8::Value) -> Rc<dyn crate::langhelp::Value> {
    from_v8_ref::<Rc<dyn crate::langhelp::Value>>(value).clone()
}

/// Store an `Rc<dyn Value>` as a `v8::External`.
pub(crate) fn rc_value_to_v8<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn crate::langhelp::Value>) -> v8::Local<'s, v8::External> {
    to_v8(scope, value)
}

/// Extract a clone of `Rc<dyn Target>` from a `v8::External`.
pub(crate) fn rc_target_from_v8(value: &v8::Value) -> Rc<dyn Target> {
    from_v8_ref::<Rc<dyn Target>>(value).clone()
}

/// Store an `Rc<dyn Target>` as a `v8::External`.
pub(crate) fn rc_target_to_v8<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn Target>) -> v8::Local<'s, v8::External> {
    to_v8(scope, value)
}

/// Walk a `CodeDynamic` tree looking for a `BindingUse(target_id)`. Used so
/// `outer_self`-style scope bindings only emit their `let … = this;` shim
/// when the body actually references them.
fn code_uses_binding(code: &CodeDynamic, target_id: ScopeEntryId) -> bool {
    match &code.type_ {
        CodeDynamicType::BindingUse(b) => b.id == target_id,
        CodeDynamicType::BindingSet(b) => code_uses_binding(b.value.as_ref(), target_id),
        CodeDynamicType::Block(b) => b.entries.iter().any(|e| match e {
            crate::code_dynamic::CodeDynamicBlockEntry::Code(c) => code_uses_binding(c, target_id),
            crate::code_dynamic::CodeDynamicBlockEntry::Bind(b) =>
                code_uses_binding(&b.value, target_id),
        }),
        CodeDynamicType::Call(c) =>
            code_uses_binding(&c.func, target_id) || c.args.iter().any(|a| code_uses_binding(a, target_id)),
        CodeDynamicType::FieldSet(f) =>
            code_uses_binding(&f.base, target_id) || code_uses_binding(&f.value, target_id),
        CodeDynamicType::FieldUse(f) => code_uses_binding(&f.base, target_id),
        CodeDynamicType::Foreach(f) =>
            code_uses_binding(&f.base, target_id) || code_uses_binding(&f.body, target_id),
        CodeDynamicType::ForeachMap(f) =>
            code_uses_binding(&f.base, target_id) || code_uses_binding(&f.body, target_id),
        CodeDynamicType::If(i) => i.conditions.iter().any(|(c, b)| code_uses_binding(c, target_id) || code_uses_binding(b, target_id))
            || i.else_.as_ref().map(|e| code_uses_binding(e, target_id)).unwrap_or(false),
        CodeDynamicType::Literal(_) => false,
        CodeDynamicType::Match(m) => code_uses_binding(&m.base, target_id)
            || m.cases.iter().any(|c| code_uses_binding(&c.body, target_id))
            || m.fallback.as_ref().map(|f| code_uses_binding(f, target_id)).unwrap_or(false),
        CodeDynamicType::NewArray(n) => n.elements.iter().any(|e| code_uses_binding(e, target_id)),
        CodeDynamicType::NewMap(n) => n.fields.iter().any(|(k, v)| code_uses_binding(k, target_id) || code_uses_binding(v, target_id)),
        CodeDynamicType::NewObject(n) => n.fields.iter().any(|(_, v)| code_uses_binding(v, target_id)),
        CodeDynamicType::OperatorAdd(o) =>
            code_uses_binding(&o.base, target_id) || code_uses_binding(&o.reference, target_id),
        CodeDynamicType::OperatorEq(o) =>
            code_uses_binding(&o.base, target_id) || code_uses_binding(&o.reference, target_id),
        CodeDynamicType::OperatorLt(o) =>
            code_uses_binding(&o.base, target_id) || code_uses_binding(&o.reference, target_id),
        CodeDynamicType::SubscriptUse(s) =>
            code_uses_binding(&s.base, target_id) || code_uses_binding(&s.key, target_id),
    }
}

pub(crate) fn render_code(lines: &mut JsSourceBuilder, code: &CodeDynamic) -> String {
    let out = format!("{}{}", EXPR_PREFIX, lines.next_expr_id());
    lines.push(code.loc, format!("let {};", out));
    match &code.type_ {
        CodeDynamicType::Block(c) => {
            for entry in &c.entries {
                match entry {
                    CodeDynamicBlockEntry::Bind(b) => {
                        let bind_key = scope_entry_id_dynamic_key(b.base);
                        lines.push(code.loc, format!("let {};", bind_key));
                        lines.push(code.loc, "{".to_string());
                        let val_var = render_code(lines, &b.value);
                        lines.push(code.loc, format!("{} = {};", bind_key, val_var));
                        lines.push(code.loc, "}".to_string());
                    },
                    CodeDynamicBlockEntry::Code(sub) => {
                        lines.push(code.loc, "{".to_string());
                        let sub_var = render_code(lines, sub);
                        lines.push(code.loc, format!("{} = {};", out, sub_var));
                        lines.push(code.loc, "}".to_string());
                    },
                }
            }
        },
        CodeDynamicType::Literal(c) => {
            lines.push(code.loc, format!("{} = {};", out, serde_json::to_string(c).unwrap()));
        },
        CodeDynamicType::BindingUse(c) => {
            lines.push(code.loc, format!("{} = {};", out, scope_entry_id_dynamic_key(c.id)));
        },
        CodeDynamicType::Call(c) => {
            // Special case: method call preserves `this` binding
            if let CodeDynamicType::FieldUse(f) = &c.func.type_ {
                let base_var = render_code(lines, &f.base);
                let mut arg_vars = vec![];
                for arg in &c.args {
                    arg_vars.push(render_code(lines, arg));
                }
                lines.push(
                    code.loc,
                    format!(
                        "{} = {}[{}]({});",
                        out,
                        base_var,
                        serde_json::to_string(&f.field).unwrap(),
                        arg_vars.join(",")
                    ),
                );
            } else {
                let func_var = render_code(lines, &c.func);
                let mut arg_vars = vec![];
                for arg in &c.args {
                    arg_vars.push(render_code(lines, arg));
                }
                lines.push(code.loc, format!("{} = ({})({});", out, func_var, arg_vars.join(",")));
            }
        },
        CodeDynamicType::FieldUse(dynamic_code_field) => {
            let base = render_code(lines, &dynamic_code_field.base);
            lines.push(
                code.loc,
                format!("{} = {}[{}];", out, base, serde_json::to_string(&dynamic_code_field.field).unwrap()),
            );
        },
        CodeDynamicType::SubscriptUse(s) => {
            let base = render_code(lines, &s.base);
            let key = render_code(lines, &s.key);
            lines.push(code.loc, format!("{} = {}[{}];", out, base, key));
        },
        CodeDynamicType::BindingSet(c) => {
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("{} = {};", scope_entry_id_dynamic_key(c.base), val_var));
            lines.push(code.loc, format!("{} = null;", out));
        },
        CodeDynamicType::FieldSet(c) => {
            let base_var = render_code(lines, &c.base);
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("{}[{}] = {};", base_var, serde_json::to_string(&c.key).unwrap(), val_var));
            lines.push(code.loc, format!("{} = null;", out));
        },
        CodeDynamicType::NewObject(c) => {
            let mut fields_str = vec![];
            for (key, value) in &c.fields {
                let val_var = render_code(lines, value);
                fields_str.push(format!("{}: {}", serde_json::to_string(key).unwrap(), val_var));
            }
            lines.push(code.loc, format!("{} = {{ {} }};", out, fields_str.join(", ")));
        },
        CodeDynamicType::NewMap(c) => {
            let mut fields_str = vec![];
            for (key, value) in &c.fields {
                let key_var = render_code(lines, key);
                let val_var = render_code(lines, value);
                fields_str.push(format!("[{}]: {}", key_var, val_var));
            }
            lines.push(code.loc, format!("{} = {{ {} }};", out, fields_str.join(", ")));
        },
        CodeDynamicType::NewArray(c) => {
            let mut elem_vars = vec![];
            for elem in &c.elements {
                elem_vars.push(render_code(lines, elem));
            }
            lines.push(code.loc, format!("{} = [{}];", out, elem_vars.join(", ")));
        },
        CodeDynamicType::If(c) => {
            for (i, (cond, body)) in c.conditions.iter().enumerate() {
                let cond_var = render_code(lines, cond);
                if i == 0 {
                    lines.push(code.loc, format!("if ({}) {{", cond_var));
                } else {
                    lines.push(code.loc, format!("}} else if ({}) {{", cond_var));
                }
                let body_var = render_code(lines, body);
                lines.push(code.loc, format!("{} = {};", out, body_var));
            }
            if let Some(else_) = &c.else_ {
                lines.push(code.loc, "} else {".to_string());
                let else_var = render_code(lines, else_);
                lines.push(code.loc, format!("{} = {};", out, else_var));
            }
            lines.push(code.loc, "}".to_string());
        },
        CodeDynamicType::OperatorLt(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} < {});", out, base_var, ref_var));
        },
        CodeDynamicType::OperatorEq(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} === {});", out, base_var, ref_var));
        },
        CodeDynamicType::OperatorAdd(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} + {});", out, base_var, ref_var));
        },
        CodeDynamicType::Foreach(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_dynamic_key(c.element_key);
            lines.push(code.loc, format!("for (let {} of {}) {{", elem_key, base_var));
            let body_var = render_code(lines, &c.body);
            lines.push(code.loc, format!("{} = {};", out, body_var));
            lines.push(code.loc, "}".to_string());
        },
        CodeDynamicType::ForeachMap(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_dynamic_key(c.element_key);
            lines.push(code.loc, format!("for (let __entry of Object.entries({})) {{", base_var));
            lines.push(
                code.loc,
                format!(
                    "let {} = {{ {}: __entry[0], {}: __entry[1] }};",
                    elem_key,
                    serde_json::to_string(&ConstKey::String("key".to_string()).to_str()).unwrap(),
                    serde_json::to_string(&ConstKey::String("value".to_string()).to_str()).unwrap(),
                ),
            );
            let body_var = render_code(lines, &c.body);
            lines.push(code.loc, format!("{} = {};", out, body_var));
            lines.push(code.loc, "}".to_string());
        },
        CodeDynamicType::Match(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_dynamic_key(c.element_key);
            lines.push(code.loc, format!("{{ let [{}, {}] = Object.entries({})[0];", TAG_VAR, elem_key, base_var));
            lines.push(code.loc, format!("switch ({}) {{", TAG_VAR));
            for case in &c.cases {
                lines.push(code.loc, format!("case {}: {{", serde_json::to_string(&case.dynamic_tag).unwrap()));
                let body_var = render_code(lines, &case.body);
                lines.push(code.loc, format!("{} = {};", out, body_var));
                lines.push(code.loc, "break; }".to_string());
            }
            if let Some(fallback) = &c.fallback {
                lines.push(code.loc, "default: {".to_string());
                let fb_var = render_code(lines, fallback);
                lines.push(code.loc, format!("{} = {};", out, fb_var));
                lines.push(code.loc, "}".to_string());
            }
            lines.push(code.loc, "} }".to_string());
        },
    }
    return out;
}

pub(crate) fn scope_entry_id_dynamic_key(e: ScopeEntryId) -> String {
    match e {
        ScopeEntryId::Local(id) => format!("{}{}", BINDING_PREFIX, id),
        ScopeEntryId::Builtin(id) => format!("{}{}", BUILTIN_PREFIX, id),
        ScopeEntryId::Def(import_id, def_set_id, entry_id) => {
            format!("{}{}_{}_{}", DEF_PREFIX, import_id.0, def_set_id.0, entry_id)
        },
    }
}


pub fn setup_v8() {
    static V: OnceLock<()> = OnceLock::new();
    V.get_or_init(|| {
        let platform = v8::new_default_platform(0, false).make_shared();
        v8::V8::initialize_platform(platform);
        v8::V8::initialize();
        return ();
    });
}

/// Stored as a v8::External so __stage can lift raw values. The source type's
/// `AspectLiftDynamic` is looked up at lift time so the cell stays
/// artifact-serializable (the boxed aspect was not).
pub(crate) struct StageLiftCell {
    pub import_state: Rc<Cell<ImportStateId>>,
    pub loc: QExprId,
    pub source_type: Rc<dyn Type>,
    pub scope: Rc<Scope>,
    pub target: Rc<dyn Target>,
    pub vx: Rc<EvalContext>,
}

pub(crate) struct TargetBindSub {
    pub data: Code,
    pub type_: Rc<dyn Type>,
}
pub(crate) struct TargetDynamic;

impl Target for TargetDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, sub: TargetBindSub) -> (Code, ScopeEntryId) {
        let js =
            sub
                .data
                .cast::<CodeDynamic>()
                .into_res()
                .expect("TargetDynamic bind: expected CodeDynamic")
                .unwrap()
                .clone();
        let js_key = ScopeEntryId::Local(xx.vx.next_local_id.next());
        let code = Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::Block(
                    CodeDynamicBlock { entries: vec![CodeDynamicBlockEntry::Bind(CodeDynamicBind {
                        base: js_key,
                        value: Box::new(js),
                    })] },
                ),
            })),
        };
        (code, js_key)
    }

    fn bind_sub_record(&self, loc: QExprId, carry: Code, fields: Vec<(ConstKey, TargetBindSub)>) -> TargetBindSub {
        let mut js_fields = Vec::new();
        let mut mortar_fields = std::collections::BTreeMap::new();
        for (key, sub) in fields {
            let cd =
                sub
                    .data
                    .cast::<CodeDynamic>()
                    .into_res()
                    .expect("TargetDynamic bind_sub_record: expected CodeDynamic")
                    .unwrap()
                    .clone();
            js_fields.push((key.to_str(), cd));
            mortar_fields.insert(key, value::record::RecordField::Mortar(sub.type_));
        }
        let record_type = Rc::new(Record {
            base: None,
            fields: mortar_fields,
        }) as Rc<dyn Type>;
        let js = wrap_with_carry(loc, carry, CodeDynamic {
            loc: loc,
            type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: js_fields }),
        });
        TargetBindSub {
            data: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(js)),
            },
            type_: record_type,
        }
    }

    fn bind_sub_tuple(&self, loc: QExprId, carry: Code, elements: Vec<TargetBindSub>) -> TargetBindSub {
        let mut js_elements = Vec::new();
        let mut mortar_elements = Vec::new();
        for sub in elements {
            let cd =
                sub
                    .data
                    .cast::<CodeDynamic>()
                    .into_res()
                    .expect("TargetDynamic bind_sub_tuple: expected CodeDynamic")
                    .unwrap()
                    .clone();
            js_elements.push(cd);
            mortar_elements.push(TupleElement::Mortar(sub.type_));
        }
        let tuple_type = Rc::new(Tuple {
            base: None,
            elements: mortar_elements,
        }) as Rc<dyn Type>;
        let js = wrap_with_carry(loc, carry, CodeDynamic {
            loc: loc,
            type_: CodeDynamicType::NewArray(CodeDynamicNewArray { elements: js_elements }),
        });
        TargetBindSub {
            data: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(js)),
            },
            type_: tuple_type,
        }
    }

    fn block(&self, loc: QExprId, entries: Vec<Code>) -> EResult<Code> {
        let mut dynamic_entries = vec![];
        let mut errors = vec![];
        for entry in &entries {
            match entry.cast::<CodeDynamic>().into_res() {
                Ok(Some(js)) => {
                    match &js.type_ {
                        CodeDynamicType::Block(block) if
                            !block.entries.is_empty() &&
                                block.entries.iter().all(|e| matches!(e, CodeDynamicBlockEntry::Bind(_))) => {
                            dynamic_entries.extend(block.entries.clone());
                        },
                        _ => dynamic_entries.push(CodeDynamicBlockEntry::Code(js.clone())),
                    }
                },
                Ok(None) => { },
                Err(e) => errors.extend(e),
            }
        }
        if !errors.is_empty() {
            return EResult {
                v: None,
                errors: Some(errors),
            };
        }
        if dynamic_entries.is_empty() {
            return EResult::ok(Code {
                loc: loc,
                code: Code_::None,
            });
        }
        return EResult::ok(Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::Block(CodeDynamicBlock { entries: dynamic_entries }),
            })),
        });
    }

    fn code_to_bind_sub(&self, _loc: QExprId, code: Code, type_: Rc<dyn Type>) -> EResult<TargetBindSub> {
        EResult::ok(TargetBindSub {
            data: code,
            type_: type_,
        })
    }

    fn if_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        conditions: Vec<(CodeValue, &Expr)>,
        else_: Option<&Expr>,
    ) -> EvalRes {
        fn cv_to_dynamic(erb: &mut Erb, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> Result<CodeDynamic, ()> {
            let vtype = cv.value.type_(xx, loc);
            let consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, vtype.as_ref()))?;
            erb.must_er(consume.to_dynamic(xx, loc, cv))
        }

        return er_seq_cv!(loc, erb, {
            let mut dynamic_conditions = Vec::new();
            // The first branch's body type drives the if's result type; mirrors
            // `match_`. Subsequent branches & the else are typecheck-converted to
            // it so the if can stand in for any expression of that type.
            let mut result_type: Option<Rc<dyn Type>> = None;
            for (cond, body) in conditions {
                let cond_js = cv_to_dynamic(&mut erb, xx, loc, cond)?;
                let body_cv = erb.must_er(const_eval(xx, body))?;
                if result_type.is_none() {
                    result_type = Some(body_cv.value.type_(xx, loc));
                }
                let body_cv = if let Some(rt) = &result_type {
                    erb.must_er(typecheck_convert(xx, loc, rt.as_ref(), body_cv))?
                } else {
                    body_cv
                };
                let body_js = cv_to_dynamic(&mut erb, xx, loc, body_cv)?;
                dynamic_conditions.push((cond_js, body_js));
            }
            let dynamic_else = if let Some(else_expr) = else_ {
                let else_cv = erb.must_er(const_eval(xx, else_expr))?;
                let else_cv = if let Some(rt) = &result_type {
                    erb.must_er(typecheck_convert(xx, loc, rt.as_ref(), else_cv))?
                } else {
                    result_type = Some(else_cv.value.type_(xx, loc));
                    else_cv
                };
                let else_js = cv_to_dynamic(&mut erb, xx, loc, else_cv)?;
                Some(Box::new(else_js))
            } else {
                None
            };
            let if_js = CodeDynamic {
                loc,
                type_: CodeDynamicType::If(CodeDynamicIf {
                    conditions: dynamic_conditions,
                    else_: dynamic_else,
                }),
            };
            let final_type: Rc<dyn Type> =
                result_type.unwrap_or_else(|| Rc::new(TypeNullDynamic) as Rc<dyn Type>);
            let stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, final_type.as_ref()))?;
            return Ok(stack.to_value(loc, if_js));
        });
    }

    fn match_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ConstKey,
        cases: Vec<(String, Rc<dyn Type>, &Expr)>,
        fallback: Option<&Expr>,
    ) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_type = base.value.type_(xx, loc);
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, base_type.as_ref()))?;
            let base_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, base))?;
            let element_scope_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
            // The first branch's value type drives the match's result type; all
            // other branches (and the fallback) are typecheck-converted to it.
            let mut dynamic_cases = Vec::new();
            let mut result_type: Option<Rc<dyn Type>> = None;
            for (tag, variant_type, body_expr) in &cases {
                let child_scope = Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: Some(xx.scope.clone()),
                });
                child_scope.values.borrow_mut().insert(element_key.clone(), {
                    let variant_ib = erb.must_er(aspect_initial_bind_get(xx, loc, variant_type.as_ref()))?;
                    variant_ib.initial_bind(element_scope_id)
                });
                let child_xx = ExprContext {
                    vx: xx.vx.clone(),
                    import_state: xx.import_state.clone(),
                    scope: child_scope,
                    target: xx.target.clone(),
                };
                let body_cv = erb.must_er(const_eval(&child_xx, body_expr))?;
                let body_cv = match &result_type {
                    None => {
                        result_type = Some(body_cv.value.type_(&child_xx, loc));
                        body_cv
                    },
                    Some(rt) => erb.must_er(typecheck_convert(&child_xx, loc, rt.as_ref(), body_cv))?,
                };
                let body_type = body_cv.value.type_(&child_xx, loc);
                let body_consume = erb.must_er(aspect_consume_dynamic_get(&child_xx, loc, body_type.as_ref()))?;
                let body_js = erb.must_er(body_consume.to_dynamic(&child_xx, loc, body_cv))?;
                dynamic_cases.push(CodeDynamicMatchBranch {
                    dynamic_tag: ConstKey::String(tag.clone()).to_str(),
                    body: body_js,
                });
            }
            let dynamic_fallback = if let Some(fallback_expr) = fallback {
                let fb_cv = erb.must_er(const_eval(xx, fallback_expr))?;
                let fb_cv = match &result_type {
                    None => {
                        result_type = Some(fb_cv.value.type_(xx, loc));
                        fb_cv
                    },
                    Some(rt) => erb.must_er(typecheck_convert(xx, loc, rt.as_ref(), fb_cv))?,
                };
                let fb_type = fb_cv.value.type_(xx, loc);
                let fb_consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, fb_type.as_ref()))?;
                let fb_js = erb.must_er(fb_consume.to_dynamic(xx, loc, fb_cv))?;
                Some(Box::new(fb_js))
            } else {
                None
            };
            let match_js = CodeDynamic {
                loc,
                type_: CodeDynamicType::Match(CodeDynamicMatch {
                    base: Box::new(base_js),
                    element_key: element_scope_id,
                    cases: dynamic_cases,
                    fallback: dynamic_fallback,
                }),
            };
            // Fall back to null if there were no branches (shouldn't happen — match
            // requires at least one case or a fallback — but keeps the type
            // signature total).
            let stack_type: Rc<dyn Type> =
                result_type.unwrap_or_else(|| Rc::new(TypeNullDynamic) as Rc<dyn Type>);
            let stack = aspect_stack_value_dynamic_get(xx, loc, stack_type.as_ref());
            let stack = erb.must_er(stack)?;
            return Ok(stack.to_value(loc, match_js));
        });
    }

    fn maybe_bind_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        let vtype = cv.value.type_(xx, loc);
        if let Some(bind_dyn) = aspect_bind_dynamic_get_maybe(xx, vtype.as_ref()) {
            return bind_dyn.bind_dynamic(xx, loc, cv);
        }
        EResult::ok(None)
    }

    fn stage(&self, xx: &ExprContext, loc: QExprId, tree: Expr, lower_values: Vec<CodeValue>) -> EvalRes {
        // Store tree as external
        let tree_cell = Rc::new(expr::StagedExprCell { value: RefCell::new(Some(Rc::new(tree) as Rc<dyn Value>)) });
        let tree_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((tree_id, External::StagedExprCell(tree_cell)));
        let tree_js = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: tree_id }),
        };

        // Build args for __stage(tree, lift1, val1, lift2, val2, ...). For each lower
        // value, pass a (lift, value) pair:
        //
        // * Code_::None: lift=null, value=StagedExprCell external
        //
        // * Code_::Dyn:  lift=StageLiftCell external, value=raw v8 result __stage handles the
        //   lifting internally.
        let mut stage_args = vec![tree_js];
        return er_seq_cv!(loc, erb, {
            for cv in lower_values {
                let vtype = cv.value.type_(xx, loc);
                match cv.code.code {
                    Code_::None => {
                        let cell = Rc::new(expr::StagedExprCell { value: RefCell::new(Some(cv.value)), });
                        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                        xx.vx.externals.borrow_mut().push((id, External::StagedExprCell(cell)));
                        stage_args.push(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::Literal(serde_json::Value::Null),
                        });
                        stage_args.push(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
                        });
                    },
                    Code_::Dyn(_) => {
                        let lift_cell = Rc::new(StageLiftCell {
                            source_type: vtype.clone(),
                            loc,
                            vx: xx.vx.clone(),
                            import_state: xx.import_state.clone(),
                            scope: xx.scope.clone(),
                            target: xx.target.clone(),
                        });
                        let lift_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                        xx.vx.externals.borrow_mut().push((lift_id, External::StageLiftCell(lift_cell)));
                        let inner_consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, vtype.as_ref()))?;
                        let inner_js = erb.must_er(inner_consume.to_dynamic(xx, loc, cv))?;
                        stage_args.push(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: lift_id }),
                        });
                        stage_args.push(inner_js);
                    },
                }
            }
            let stage_js = CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: STAGE_FN_SCOPE_ID, }),
                    }),
                    args: stage_args,
                }),
            };
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(stage_js)),
                },
                value: Rc::new(value::value_dynamic::ValueDynamic {
                    source: value::value_dynamic::ValueDynamicSource::Expr,
                    type_: Rc::new(value::type_expr::TypeExprStagedPlaceholder),
                }),
            });
        });
    }
}

/// Store a value as a `v8::External`. The value is boxed as `dyn Any` so that
/// `from_v8_ref` can safely downcast to any type, including trait object wrappers
/// like `Rc<dyn Type>`. A guaranteed finalizer drops the value when the isolate is
/// disposed.
pub(crate) fn to_v8<'s, T: 'static>(scope: &mut v8::PinScope<'s, '_, ()>, value: T) -> v8::Local<'s, v8::External> {
    let any: Box<dyn std::any::Any> = Box::new(value);
    let ptr = Box::into_raw(Box::new(any)) as *mut std::ffi::c_void;
    let ext = v8::External::new(&*scope, ptr);
    let weak = v8::Weak::with_guaranteed_finalizer(&mut *scope, ext, Box::new(move || unsafe {
        drop(Box::from_raw(ptr as *mut Box<dyn std::any::Any>));
    }));
    scope.get_slot::<V8RcStore>().unwrap().weaks.borrow_mut().push(weak);
    ext
}

/// `arg = { xx, source_type, cv }` as seen inside builtin-`type` interface
/// method bodies. Used to typecheck the user's impl_expr.
pub(crate) fn build_cast_arg_record_type() -> Rc<dyn Type> {
    let mut fields = std::collections::BTreeMap::new();
    fields.insert(
        ConstKey::String("xx".to_string()),
        value::record::RecordField::Mortar(
            Rc::new(crate::eval::code_value_extern::TypeExprContextRefDynamic) as Rc<dyn Type>,
        ),
    );
    fields.insert(
        ConstKey::String("source_type".to_string()),
        value::record::RecordField::Mortar(Rc::new(value::type_ref_v8::TypeTypeRefV8Dynamic) as Rc<dyn Type>),
    );
    fields.insert(
        ConstKey::String("cv".to_string()),
        value::record::RecordField::Mortar(
            Rc::new(crate::eval::code_value_extern::TypeCodeValueExternDynamic) as Rc<dyn Type>,
        ),
    );
    Rc::new(value::record::Record { base: Some(Box::new(value::record::RecordBaseDynamic)), fields })
}

/// Build the `arg` record type for a builtin-interface impl method on the
/// new aspect_function / aspect_field / scope_entry / target kinds. Each field
/// is a `CodeValueExtern` wrapping the corresponding Rust trait param's
/// CodeValue (or other appropriate type).
pub(crate) fn build_aspect_arg_record_type(field_names: &[&str]) -> Rc<dyn Type> {
    let mut fields = std::collections::BTreeMap::new();
    let cve_type: Rc<dyn Type> = Rc::new(crate::eval::code_value_extern::TypeCodeValueExternDynamic);
    for name in field_names {
        fields.insert(
            ConstKey::String(name.to_string()),
            value::record::RecordField::Mortar(cve_type.clone()),
        );
    }
    Rc::new(value::record::Record { base: Some(Box::new(value::record::RecordBaseDynamic)), fields })
}

/// Build the scope a def-set method body sees: builtins + type-ref bindings
/// (`null`, `string`, `number`, `expr`). Same shape `eval()` constructs for
/// its impl-compilation pass; factored out so import-side `from_serial` can
/// re-compile an imported interface impl with the same scope.
pub(crate) fn build_builtin_scope(
    xx: &ExprContext,
    loc: QExprId,
    builtin_scope_entries: &[(String, ScopeEntryId, Rc<dyn Type>)],
) -> Result<HashMap<ConstKey, Box<dyn ScopeEntry>>, Vec<ExprErr>> {
    let mut scope_entries = HashMap::<ConstKey, Box<dyn ScopeEntry>>::new();
    for (name, dynamic_key, type_) in builtin_scope_entries {
        let ib = aspect_initial_bind_get(xx, loc, type_.as_ref()).into_res()?;
        scope_entries.insert(ConstKey::String(name.clone()), ib.initial_bind(*dynamic_key));
    }
    for (key, type_ref) in [
        ("null", Rc::new(TypeNullDynamic) as Rc<dyn Type>),
        ("string", Rc::new(TypeStringDynamic) as Rc<dyn Type>),
        ("number", Rc::new(TypeNumberDynamic) as Rc<dyn Type>),
        ("constkey", Rc::new(value::type_constkey::dynamic::TypeConstkeyDynamic) as Rc<dyn Type>),
        ("expr", Rc::new(value::type_expr::TypeExpr) as Rc<dyn Type>),
        ("aspect", Rc::new(value::type_aspect::dynamic::TypeAspectDynamic) as Rc<dyn Type>),
        ("codedyn", Rc::new(value::type_code_source::TypeCodeDynHandleDynamic) as Rc<dyn Type>),
    ] {
        let cv = CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(value::type_any::r#const::ValueAnyConst { inner: type_ref.clone() }),
        };
        let any_const_type: Rc<dyn Type> = Rc::new(value::type_any::r#const::TypeAnyConst);
        let bind = aspect_bind_get(xx, loc, any_const_type.as_ref()).into_res()?;
        let (_, entry) = bind.bind(xx, loc, cv).into_res()?;
        scope_entries.insert(ConstKey::String(key.to_string()), entry);
    }
    Ok(scope_entries)
}

pub(crate) fn typecheck_convert(xx: &ExprContext, loc: QExprId, dest_type: &dyn Type, source: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let source_type = source.value.type_(xx, loc);
        let dest;
        if source_type.can_cast_to(xx, loc, dest_type) {
            dest = erb.must_er(source_type.cast_to(xx, loc, source, dest_type))?;
        } else if dest_type.can_cast_from(xx, loc, source_type.as_ref()) {
            dest = erb.must_er(dest_type.cast_from(xx, loc, source))?;
        } else {
            return erb.err_new(
                format!("Incompatible types: source {} cannot convert to dest {}", source_type.desc(), dest_type.desc())
            );
        }
        return Ok(dest);
    });
}

/// v8 slot carrying the current stage's `EvalContext`. Read by v8 callbacks
/// like `eval_const_v8` so they can reconstruct an `ExprContext` for nested
/// Rust `const_eval` calls.
pub(crate) struct V8EvalContextSlot {
    pub(crate) ex: Rc<EvalContext>,
}

/// Dispatch interface for synchronously invoking compiled JS methods on a
/// bundled instance during `const_eval`. The implementer captures the
/// reads the active scope from `EvalContext.v8_scope` (raw pointer, set by
/// `eval()`) on each call. The trait object is stored in `EvalContext` as
/// `NonNull<dyn>` so the fat pointer (data + vtable) is preserved.
pub(crate) trait V8CastDispatcher {
    /// Dispatch `instance[method_field](arg_obj)` where `arg_obj` is a JS
    /// object exposing the user-defined `cast_from` body's `arg` parameter.
    /// `arg.xx` is set to an External of a child `Rc<ExprContext>` (with
    /// `arg` bound in its scope so `eval_const`'s inner `const_eval` can
    /// resolve graft/scope references). The method is expected to return an
    /// External-wrapped `CodeValue` — the dispatcher unwraps it and returns
    /// it directly, no lifting.
    fn dispatch_cast_from(
        &mut self,
        bundle: &crate::eval::isolate_bundle::IsolateBundle,
        method_field: &str,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> Result<CodeValue, String>;

    /// Dispatch `instance[method_field](arg_obj)` for `can_cast_from`. The
    /// method's JS body accesses `arg.source_type` (a `TypeRefV8`) and
    /// returns a boolean.
    fn dispatch_can_cast_from(
        &mut self,
        bundle: &crate::eval::isolate_bundle::IsolateBundle,
        method_field: &str,
        xx: &ExprContext,
        loc: QExprId,
        source_type: Rc<dyn Type>,
    ) -> Result<bool, String>;
}

/// Stateless dispatcher impl. Reads the active v8 scope from
/// `EvalContext.v8_scope` on each call rather than capturing it — so the
/// dispatcher's lifetime is independent of any `&mut` borrow on the scope,
/// and a single instance can live for the whole `eval()` call.
pub(crate) struct StatelessV8CastDispatcher;

impl V8CastDispatcher for StatelessV8CastDispatcher {
    fn dispatch_cast_from(
        &mut self,
        bundle: &crate::eval::isolate_bundle::IsolateBundle,
        method_field: &str,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> Result<CodeValue, String> {
        let scope_ptr = xx
            .vx
            .v8_scope
            .get()
            .ok_or_else(|| "v8 dispatch attempted with no active v8 scope".to_string())?;
        // SAFETY: pointer set by `eval()` for its full duration. While we
        // hold this `&mut`, no other code path is reading `v8_scope` (the
        // dispatcher is the only reader; method calls within it consume
        // the scope `&mut` until they return). Lifetimes are erased on
        // store/load but the underlying scope outlives every read.
        let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> =
            unsafe { &mut *(scope_ptr.as_ptr() as *mut _) };
        // Build a child ExprContext with `arg` bound in scope so the
        // user's staged inner expr (passed to `eval_const`) can resolve
        // `arg.cv` etc. through Rust `const_eval` at v8 invocation time.
        let arg_factory = crate::eval::user_defined_type_dispatch::ArgRecordFactory {
            loc,
            source_type: cv.value.type_(xx, loc),
            cv: CodeValue {
                code: cv.code.clone(),
                value: cv.value.clone(),
            },
        };
        let child_scope = Rc::new(Scope {
            values: RefCell::new(HashMap::new()),
            parent: Some(xx.scope.clone()),
        });
        child_scope.values.borrow_mut().insert(
            ConstKey::String("arg".to_string()),
            Box::new(arg_factory) as Box<dyn ScopeEntry>,
        );
        let child_xx = Rc::new(ExprContext {
            vx: xx.vx.clone(),
            import_state: xx.import_state.clone(),
            scope: child_scope,
            target: xx.target.clone(),
        });
        // Build the v8-side arg object: `xx` carries the child_xx as
        // External (for the user's body to pass to `eval_const`),
        // `source_type` is a `TypeRefV8` wrapping the source type (for
        // user can_cast_from impls), `cv` is null (the user impl reads
        // `arg.cv` at Rust const_eval time, not v8 time).
        let arg_obj = v8::Object::new(scope);
        let xx_external = to_v8(scope, child_xx.clone());
        let xx_key = v8::String::new(scope, &ConstKey::String("xx".to_string()).to_str())
            .ok_or_else(|| "xx key alloc".to_string())?;
        arg_obj.set(scope, xx_key.into(), xx_external.into());
        let cv_key = v8::String::new(scope, &ConstKey::String("cv".to_string()).to_str())
            .ok_or_else(|| "cv key alloc".to_string())?;
        let null_for_cv: v8::Local<v8::Value> = v8::null(scope).into();
        arg_obj.set(scope, cv_key.into(), null_for_cv);
        let st_key = v8::String::new(scope, &ConstKey::String("source_type".to_string()).to_str())
            .ok_or_else(|| "source_type key alloc".to_string())?;
        let source_type_v8 = crate::eval::dynamic_wrap::DynamicWrap::into_v8(
            crate::eval::value::type_ref_v8::TypeRefV8 {
                type_ref: cv.value.type_(xx, loc),
            },
            scope,
        );
        arg_obj.set(scope, st_key.into(), source_type_v8);
        let instance = v8::Local::new(scope, &bundle.instance);
        let method_key = v8::String::new(scope, &ConstKey::String(method_field.to_string()).to_str())
            .ok_or_else(|| "method_field alloc".to_string())?;
        let method_val = instance
            .get(scope, method_key.into())
            .ok_or_else(|| format!("instance missing field '{}'", method_field))?;
        let method_fn = v8::Local::<v8::Function>::try_from(method_val)
            .map_err(|_| format!("instance field '{}' is not a function", method_field))?;
        let recv: v8::Local<v8::Value> = instance.into();
        let arg_v: v8::Local<v8::Value> = arg_obj.into();
        let result = method_fn
            .call(scope, recv, &[arg_v])
            .ok_or_else(|| "cast_from v8 call returned no value".to_string())?;
        let cell: Rc<RefCell<Option<CodeValue>>> =
            from_v8_ref::<Rc<RefCell<Option<CodeValue>>>>(&*result).clone();
        let out_cv = cell
            .borrow_mut()
            .take()
            .ok_or_else(|| "cast_from v8 returned consumed CodeValueExtern".to_string())?;
        Ok(out_cv)
    }

    fn dispatch_can_cast_from(
        &mut self,
        bundle: &crate::eval::isolate_bundle::IsolateBundle,
        method_field: &str,
        xx: &ExprContext,
        loc: QExprId,
        source_type: Rc<dyn Type>,
    ) -> Result<bool, String> {
        let scope_ptr = xx
            .vx
            .v8_scope
            .get()
            .ok_or_else(|| "v8 dispatch attempted with no active v8 scope".to_string())?;
        let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> =
            unsafe { &mut *(scope_ptr.as_ptr() as *mut _) };
        let _ = loc;
        let arg_obj = v8::Object::new(scope);
        let xx_external = to_v8(scope, Rc::new(ExprContext {
            vx: xx.vx.clone(),
            import_state: xx.import_state.clone(),
            scope: xx.scope.clone(),
            target: xx.target.clone(),
        }));
        let xx_key = v8::String::new(scope, &ConstKey::String("xx".to_string()).to_str())
            .ok_or_else(|| "xx key alloc".to_string())?;
        arg_obj.set(scope, xx_key.into(), xx_external.into());
        let st_key = v8::String::new(scope, &ConstKey::String("source_type".to_string()).to_str())
            .ok_or_else(|| "source_type key alloc".to_string())?;
        let source_type_v8 = crate::eval::dynamic_wrap::DynamicWrap::into_v8(
            crate::eval::value::type_ref_v8::TypeRefV8 { type_ref: source_type },
            scope,
        );
        arg_obj.set(scope, st_key.into(), source_type_v8);
        let cv_key = v8::String::new(scope, &ConstKey::String("cv".to_string()).to_str())
            .ok_or_else(|| "cv key alloc".to_string())?;
        let null_for_cv: v8::Local<v8::Value> = v8::null(scope).into();
        arg_obj.set(scope, cv_key.into(), null_for_cv);
        let instance = v8::Local::new(scope, &bundle.instance);
        let method_key = v8::String::new(scope, &ConstKey::String(method_field.to_string()).to_str())
            .ok_or_else(|| "method_field alloc".to_string())?;
        let method_val = instance
            .get(scope, method_key.into())
            .ok_or_else(|| format!("instance missing field '{}'", method_field))?;
        let method_fn = v8::Local::<v8::Function>::try_from(method_val)
            .map_err(|_| format!("instance field '{}' is not a function", method_field))?;
        let recv: v8::Local<v8::Value> = instance.into();
        let arg_v: v8::Local<v8::Value> = arg_obj.into();
        let result = method_fn
            .call(scope, recv, &[arg_v])
            .ok_or_else(|| "can_cast_from v8 call returned no value".to_string())?;
        Ok(result.boolean_value(scope))
    }
}


pub(crate) struct V8LogStore {
    pub(crate) logs: RefCell<Vec<LogEntry>>,
}

// --- Rc`<T>` ↔ v8::External helpers ---
/// Weak handle storage, set as an isolate slot. During dispose_annex(), slots are
/// cleared first (Weak handles dropped — but isolate ptr is already null so
/// finalizers can't be cancelled), then finalizer_map is drained, running our
/// guaranteed finalizer callbacks.
pub(crate) struct V8RcStore {
    weaks: RefCell<Vec<v8::Weak<v8::External>>>,
}

pub(crate) struct V8UniqueCounter {
    pub(crate) compile_id: CompileId,
    pub(crate) counter: Counter,
}
