use {
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, scope_ids: &mut HashSet<usize>) {
    let specs: Vec<InvisibleBuiltin> = vec![InvisibleBuiltin {
        name: "stage",
        fn_scope_id: "STAGE_FN_SCOPE_ID",
        scope_id: 7017,
        body: quote!{
            let tree_cell: Rc<crate::eval::expr::StagedExprCell> =
                crate::eval::from_dynamic_ref::<Rc<crate::eval::expr::StagedExprCell>>(&*args.get(0)).clone();
            let tree_value = tree_cell.value.borrow_mut().take().expect("stage tree already consumed");
            let tree = crate::invariant::must_downcast_ref::<crate::eval::expr::Expr>(tree_value.as_ref()).clone();

            enum StageLowerValue {
                Expr(crate::eval::expr::Expr),
                Value(Rc<dyn crate::langhelp::Value>),
            }

            let lower_count = args.get(1).integer_value(scope).expect("__stage: lower_count not an integer") as usize;
            let mut values: Vec<StageLowerValue> = Vec::new();
            let mut i = 2;
            for _ in 0 .. lower_count {
                let lift_arg = args.get(i);
                let val_arg = args.get(i + 1);
                i += 2;
                if lift_arg.is_null_or_undefined() {
                    let cell: Rc<crate::eval::expr::StagedExprCell> =
                        crate::eval::from_dynamic_ref::<Rc<crate::eval::expr::StagedExprCell>>(&*val_arg).clone();
                    let value = cell.value.borrow_mut().take().expect("stage lower value already consumed");
                    if (value.as_ref() as &dyn std::any::Any).is::<crate::eval::expr::Expr>() {
                        let expr =
                            Rc::try_unwrap(crate::langhelp::downcast_value::<crate::eval::expr::Expr>(value))
                                .ok()
                                .expect("Stage lower Expr: Rc not unique");
                        values.push(StageLowerValue::Expr(expr));
                    } else {
                        values.push(StageLowerValue::Value(value));
                    }
                } else {
                    let lift_cell: Rc<crate::eval::StageLiftCell> =
                        crate::eval::from_dynamic_ref::<Rc<crate::eval::StageLiftCell>>(&*lift_arg).clone();
                    let lift_xx = crate::langhelp::ExprContext {
                        vx: lift_cell.vx.clone(),
                        import_state: lift_cell.import_state.clone(),
                        scope: lift_cell.scope.clone(),
                        target: lift_cell.target.clone(),
                        current_exception_type: lift_cell.current_exception_type.clone(),
                    };
                    let lift =
                        crate::r#gen::aspect_lift_get(&lift_xx, lift_cell.loc, lift_cell.source_type.as_ref())
                            .into_res()
                            .expect("__stage: source type missing AspectLift");
                    let value = lift.lift(&lift_xx, &*scope, val_arg).expect("__stage: lift failed");
                    values.push(StageLowerValue::Value(value));
                }
            }
            let mut graft_values: Vec<crate::langhelp::CodeValue> = Vec::new();
            while i < args.length() {
                let graft_arg = args.get(i);
                i += 1;
                let cell: Rc<RefCell<Option<crate::langhelp::CodeValue>>> =
                    crate::eval::from_dynamic_ref::<Rc<RefCell<Option<crate::langhelp::CodeValue>>>>(
                        &*graft_arg,
                    ).clone();
                let cv = cell.borrow_mut().take().expect("__stage: graft CodeValueExtern already consumed");
                graft_values.push(cv);
            }

            fn clone_with_lower_substitutions(
                expr: &crate::eval::expr::Expr,
                sd: usize,
                ld: usize,
                values: &mut dyn Iterator<Item = StageLowerValue>,
                grafts: &mut dyn Iterator<Item = crate::langhelp::CodeValue>,
            ) -> crate::eval::expr::Expr {
                use crate::eval::expr::{
                    self,
                    Expr,
                    ExprAs,
                    ExprBind,
                    ExprCall,
                    ExprField,
                    ExprForeach,
                    ExprLower,
                    ExprMatch,
                    ExprMatchBranch,
                    ExprSeq,
                    ExprSet,
                    ExprStage,
                    ExprTuple,
                    Expr_,
                };

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
                                        expr: Rc::new(
                                            clone_with_lower_substitutions(&e.expr, sd, new_ld, values, grafts),
                                        ),
                                    },
                                ),
                            }
                        }
                    },
                    Expr_::Stage(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Stage(
                            ExprStage {
                                expr: Rc::new(clone_with_lower_substitutions(&e.expr, sd + 1, ld, values, grafts),),
                            },
                        ),
                    },
                    Expr_::Graft(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Graft(match e {
                            expr::ExprGraft::Pending(_) if sd == 0 => expr::ExprGraft::Evaluated(
                                grafts.next().expect("Not enough graft values for substitution"),
                            ),
                            expr::ExprGraft::Pending(inner) => expr::ExprGraft::Pending(
                                Rc::new(clone_with_lower_substitutions(inner, sd, ld, values, grafts)),
                            ),
                            expr::ExprGraft::Evaluated(cv) => expr::ExprGraft::Evaluated(cv.clone()),
                        }),
                    },
                    Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => expr.clone(),
                    Expr_::As(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::As(ExprAs {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            target: Rc::new(clone_with_lower_substitutions(&e.target, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Call(c) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Call(ExprCall {
                            func: Rc::new(clone_with_lower_substitutions(&c.func, sd, ld, values, grafts)),
                            arg: Rc::new(clone_with_lower_substitutions(&c.arg, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Bind(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Bind(ExprBind {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            value: Rc::new(clone_with_lower_substitutions(&e.value, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Set(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Set(ExprSet {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            value: Rc::new(clone_with_lower_substitutions(&e.value, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Field(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Field(ExprField {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            key: Rc::new(clone_with_lower_substitutions(&e.key, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Seq(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Seq(
                            ExprSeq {
                                exprs: e
                                    .exprs
                                    .iter()
                                    .map(|sub| clone_with_lower_substitutions(sub, sd, ld, values, grafts))
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
                                    .map(|e| clone_with_lower_substitutions(e, sd, ld, values, grafts))
                                    .collect(),
                            },
                        ),
                    },
                    Expr_::Record(r) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Record(expr::ExprRecord { entries: r.entries.iter().map(|(k, v)| {
                            (
                                clone_with_lower_substitutions(k, sd, ld, values, grafts),
                                clone_with_lower_substitutions(v, sd, ld, values, grafts),
                            )
                        }).collect() }),
                    },
                    Expr_::If(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::If(expr::ExprIf {
                            conditions: e.conditions.iter().map(|b| expr::ExprConditionBranch {
                                condition: clone_with_lower_substitutions(&b.condition, sd, ld, values, grafts),
                                body: clone_with_lower_substitutions(&b.body, sd, ld, values, grafts),
                            }).collect(),
                            else_: e
                                .else_
                                .as_ref()
                                .map(|el| Rc::new(clone_with_lower_substitutions(el, sd, ld, values, grafts))),
                        }),
                    },
                    Expr_::OperatorLt(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorLt(expr::ExprOperatorLt {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            reference: Rc::new(clone_with_lower_substitutions(&e.reference, sd, ld, values, grafts),),
                        }),
                    },
                    Expr_::OperatorEq(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorEq(expr::ExprOperatorEq {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            reference: Rc::new(clone_with_lower_substitutions(&e.reference, sd, ld, values, grafts),),
                        }),
                    },
                    Expr_::OperatorAdd(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorAdd(expr::ExprOperatorAdd {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            reference: Rc::new(clone_with_lower_substitutions(&e.reference, sd, ld, values, grafts),),
                        }),
                    },
                    Expr_::Foreach(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Foreach(ExprForeach {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            element_key: Rc::new(
                                clone_with_lower_substitutions(&e.element_key, sd, ld, values, grafts),
                            ),
                            body: Rc::new(clone_with_lower_substitutions(&e.body, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Match(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Match(ExprMatch {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            element_key: Rc::new(
                                clone_with_lower_substitutions(&e.element_key, sd, ld, values, grafts),
                            ),
                            cases: e.cases.iter().map(|c| ExprMatchBranch {
                                condition: clone_with_lower_substitutions(&c.condition, sd, ld, values, grafts),
                                body: clone_with_lower_substitutions(&c.body, sd, ld, values, grafts),
                            }).collect(),
                            fallback: e
                                .fallback
                                .as_ref()
                                .map(|f| Rc::new(clone_with_lower_substitutions(f, sd, ld, values, grafts))),
                        }),
                    },
                    Expr_::Try(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Try(expr::ExprTry {
                            body: Rc::new(clone_with_lower_substitutions(&e.body, sd, ld, values, grafts)),
                            catch: e.catch.as_ref().map(|c| expr::ExprTryCatch {
                                binding_name: Rc::new(
                                    clone_with_lower_substitutions(&c.binding_name, sd, ld, values, grafts),
                                ),
                                exception_type: Rc::new(
                                    clone_with_lower_substitutions(&c.exception_type, sd, ld, values, grafts),
                                ),
                                body: Rc::new(clone_with_lower_substitutions(&c.body, sd, ld, values, grafts)),
                            }),
                            finally: e
                                .finally
                                .as_ref()
                                .map(|f| Rc::new(clone_with_lower_substitutions(f, sd, ld, values, grafts))),
                        }),
                    },
                }
            }

            let new_tree =
                clone_with_lower_substitutions(&tree, 0, 0, &mut values.into_iter(), &mut graft_values.into_iter(),);
            let result_cell =
                Rc::new(
                    crate::eval::expr::StagedExprCell {
                        value: RefCell::new(Some(Rc::new(new_tree) as Rc<dyn crate::langhelp::Value>)),
                    }
                );
            let ext = crate::eval::to_dynamic_ext(scope, result_cell);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "record_to_type",
        fn_scope_id: "RECORD_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7016,
        body: quote!{
            let value = args.get(0);
            let obj = v8::Local::<v8::Object>::try_from(value).expect("__record_to_type: expected dynamic object");
            let keys = obj.get_own_property_names(scope, v8::GetPropertyNamesArgs::default()).unwrap();
            let mut fields = std::collections::BTreeMap::new();
            for i in 0 .. keys.length() {
                let key = keys.get_index(scope, i).unwrap();
                let key_str = key.to_string(scope).unwrap().to_rust_string_lossy(scope);
                let val = obj.get(scope, key).unwrap();
                let const_key =
                    crate::langhelp::ConstKey::from_str(
                        &key_str,
                    ).expect(&format!("Invalid ConstKey in v8 object property: {}", key_str));
                fields.insert(
                    const_key,
                    crate::eval::value::type_record::RecordField::Dynamic(
                        crate::eval::dynamic_util::dynamic_value_to_type(scope, val),
                    ),
                );
            }
            let type_: Rc<dyn crate::langhelp::Type> = Rc::new(crate::eval::value::type_record::Record {
                base: None,
                fields,
            });
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "tuple_to_type",
        fn_scope_id: "TUPLE_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7020,
        body: quote!{
            let value = args.get(0);
            let arr = v8::Local::<v8::Array>::try_from(value).expect("__tuple_to_type: expected dynamic array");
            let len = arr.length();
            let mut elements = Vec::with_capacity(len as usize);
            for i in 0 .. len {
                let elem = arr.get_index(scope, i).unwrap();
                elements.push(
                    crate::eval::value::type_tuple::TupleElement::Dynamic(
                        crate::eval::dynamic_util::dynamic_value_to_type(scope, elem),
                    ),
                );
            }
            let type_: Rc<dyn crate::langhelp::Type> =
                Rc::new(crate::eval::value::type_tuple::Tuple { elements: elements });
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "cve_extract",
        fn_scope_id: "CVE_EXTRACT_FN_SCOPE_ID",
        scope_id: 7003,
        body: quote!{
            let cve_v8 = args.get(0);
            let cve_cell: Rc<RefCell<Option<crate::langhelp::CodeValue>>> =
                crate::eval::from_dynamic_ref::<Rc<RefCell<Option<crate::langhelp::CodeValue>>>>(&*cve_v8).clone();
            let inner_value: Rc<dyn crate::langhelp::Value> = {
                let guard = cve_cell.borrow();
                let cv = guard.as_ref().expect("__cve_extract: inner CodeValue already consumed");
                cv.value.clone().expect("__cve_extract: inner CodeValue has no value")
            };
            let xx = scope.get_slot::<crate::eval::DynamicExprContextSlot>().unwrap().xx.clone();
            let loc = (xx.vx.ix.source_id, crate::syntax::ExprId(0));
            let value_type =
                inner_value.type_(&xx, loc).into_res().expect("__cve_extract: inner CV type_() returned errors");
            if let Some(aspect_str) = crate::r#gen::aspect_string_const_get_maybe(&xx, value_type.as_ref()) {
                let s = aspect_str.get(inner_value.as_ref());
                rv.set(v8::String::new(scope, &s).unwrap().into());
                return;
            }
            panic!("__cve_extract: unsupported inner CV value type ({})", value_type.desc());
        },
    }, InvisibleBuiltin {
        name: "cve_get_code_enum",
        fn_scope_id: "CVE_GET_CODE_ENUM_FN_SCOPE_ID",
        scope_id: 7004,
        body: quote!{
            let cve_v8 = args.get(0);
            let cve_cell: Rc<RefCell<Option<crate::langhelp::CodeValue>>> =
                crate::eval::from_dynamic_ref::<Rc<RefCell<Option<crate::langhelp::CodeValue>>>>(&*cve_v8).clone();
            let inner_code: crate::langhelp::Code = {
                let guard = cve_cell.borrow();
                let cv = guard.as_ref().expect("__cve_get_code_enum: inner CodeValue already consumed");
                cv.code.clone()
            };
            let obj = v8::Object::new(scope);
            match inner_code.code {
                crate::langhelp::Code_::None => {
                    let key =
                        v8::String::new(
                            scope,
                            &crate::langhelp::ConstKey::String("none".to_string()).to_str(),
                        ).unwrap();
                    let null: v8::Local<v8::Value> = v8::null(scope).into();
                    obj.set(scope, key.into(), null);
                },
                crate::langhelp::Code_::Dyn(c) => {
                    let key =
                        v8::String::new(
                            scope,
                            &crate::langhelp::ConstKey::String("dyn".to_string()).to_str(),
                        ).unwrap();
                    let dynamic_val: v8::Local<v8::Value> =
                        if let Some(pull) =
                            (c.as_ref() as &dyn std::any::Any).downcast_ref::<crate::eval::CodePull>() {
                            v8::Local::new(scope, &pull.instance).into()
                        } else {
                            crate::eval::to_dynamic_ext(scope, c).into()
                        };
                    obj.set(scope, key.into(), dynamic_val);
                },
            }
            rv.set(obj.into());
        },
    }, InvisibleBuiltin {
        name: "cve_get_value",
        fn_scope_id: "CVE_GET_VALUE_FN_SCOPE_ID",
        scope_id: 7005,
        body: quote!{
            let cve_v8 = args.get(0);
            let cve_cell: Rc<RefCell<Option<crate::langhelp::CodeValue>>> =
                crate::eval::from_dynamic_ref::<Rc<RefCell<Option<crate::langhelp::CodeValue>>>>(&*cve_v8).clone();
            let value: Rc<dyn crate::langhelp::Value> = {
                let borrow = cve_cell.borrow();
                borrow
                    .as_ref()
                    .expect("__cve_get_value: inner CodeValue already consumed")
                    .value
                    .clone()
                    .expect("__cve_get_value: no value")
            };
            let ext = crate::eval::rc_value_to_dynamic_ext(scope, value);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "value_get_type",
        fn_scope_id: "VALUE_GET_TYPE_FN_SCOPE_ID",
        scope_id: 7022,
        body: quote!{
            let value: Rc<dyn crate::langhelp::Value> = crate::eval::rc_value_from_v8(&*args.get(0));
            let xx = scope.get_slot::<crate::eval::DynamicExprContextSlot>().unwrap().xx.clone();
            let loc = (xx.vx.ix.source_id, crate::syntax::ExprId(0));
            let type_ref: Rc<dyn crate::langhelp::Type> =
                value.type_(&xx, loc).into_res().expect("value_get_type: type_() returned errors");
            let type_ref_push =
                <crate::r#gen::TypeRefPush as crate::eval::push::Push>::into_dynamic(
                    crate::r#gen::TypeRefPush { type_ref },
                    scope,
                );
            rv.set(type_ref_push);
        },
    }, InvisibleBuiltin {
        name: "aspect_lookup",
        fn_scope_id: "ASPECT_LOOKUP_FN_SCOPE_ID",
        scope_id: 7001,
        body: quote!{
            let arg0 = args.get(0);
            let arg1 = args.get(1);
            let arg0_obj =
                v8::Local::<v8::Object>::try_from(
                    arg0,
                ).expect("__aspect_lookup: expected dynamic object for TypeRefPush");
            let d_key = v8::String::new(scope, "_d").expect("__aspect_lookup: alloc _d key");
            let d_val = arg0_obj.get(scope, d_key.into()).expect("__aspect_lookup: TypeRefPush missing _d field");
            let type_ref: Rc<dyn crate::langhelp::Type> =
                crate::eval::from_dynamic_ref::<Rc<crate::r#gen::TypeRefPush>>(&*d_val).type_ref.clone();
            let unique: crate::langhelp::Unique = *crate::eval::from_dynamic_ref::<crate::langhelp::Unique>(&*arg1);
            let xx = scope.get_slot::<crate::eval::DynamicExprContextSlot>().unwrap().xx.clone();
            let result = v8::Object::new(scope);
            match type_ref.aspect(&xx, &unique) {
                Some(aspect_box) => {
                    let aspect_field: Box<dyn crate::r#gen::AspectField> =
                        crate::invariant::must_downcast_box::<Box<dyn crate::r#gen::AspectField>>(aspect_box);
                    let aspect_dyn: &dyn crate::langhelp::Aspect = &*aspect_field;
                    let instance_dynamic: v8::Local<v8::Value> = match aspect_dyn.dynamic_instance() {
                        Some(g) => v8::Local::new(scope, &g).into(),
                        None => v8::null(scope).into(),
                    };
                    let some_key =
                        v8::String::new(
                            scope,
                            &crate::langhelp::ConstKey::String("some".to_string()).to_str(),
                        ).unwrap();
                    result.set(scope, some_key.into(), instance_dynamic);
                },
                None => {
                    let none_key =
                        v8::String::new(
                            scope,
                            &crate::langhelp::ConstKey::String("none".to_string()).to_str(),
                        ).unwrap();
                    let null_v8: v8::Local<v8::Value> = v8::null(scope).into();
                    result.set(scope, none_key.into(), null_v8);
                },
            }
            rv.set(result.into());
        },
    }, InvisibleBuiltin {
        name: "cast_value",
        fn_scope_id: "CAST_VALUE_FN_SCOPE_ID",
        scope_id: 7002,
        body: quote!{
            let value: Rc<dyn crate::langhelp::Value> = crate::eval::rc_value_from_v8(&*args.get(0));
            let type_: Rc<dyn crate::langhelp::Type> = crate::eval::rc_type_from_v8(&*args.get(1));
            let type_const =
                crate
                ::invariant
                ::must_downcast_ref::<crate::eval::value::type_source_object::r#const::TypeSourceObjectConst,>(
                    type_.as_ref()
                );
            let vud =
                crate::invariant::must_downcast_ref::<crate::eval::value::value_pull::dynamic::ValuePull>(
                    value.as_ref(),
                );
            let _ = type_const;
            let instance = v8::Local::new(scope, &vud.bundle);
            rv.set(instance.into());
        },
    }, InvisibleBuiltin {
        name: "object_ref_to_type",
        fn_scope_id: "DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7013,
        body: quote!{
            let dt_ref: Rc<crate::eval::value::type_source_object::SourceObjectRef> =
                crate::eval::from_dynamic_ref::<Rc<crate::eval::value::type_source_object::SourceObjectRef>>(
                    &*args.get(0),
                ).clone();
            let type_: Rc<dyn crate::langhelp::Type> =
                Rc::new(
                    crate::eval::value::type_source_object::r#const::TypeSourceObjectConst {
                        data: dt_ref.data.clone(),
                    }
                );
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "union_ref_to_type",
        fn_scope_id: "TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7019,
        body: quote!{
            let tu_ref: Rc<crate::eval::value::type_source_union::SourceUnionRef> =
                crate::eval::from_dynamic_ref::<Rc<crate::eval::value::type_source_union::SourceUnionRef>>(
                    &*args.get(0),
                ).clone();
            let type_: Rc<dyn crate::langhelp::Type> =
                Rc::new(
                    crate::eval::value::type_source_union::r#const::TypeSourceUnionConst { data: tu_ref.data.clone(), }
                );
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "interface_ref_to_type",
        fn_scope_id: "INTERFACE_REF_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7015,
        body: quote!{
            let iface_ref: Rc<crate::eval::value::type_source_interface::SourceInterfaceRef> =
                crate::eval::from_dynamic_ref::<Rc<crate::eval::value::type_source_interface::SourceInterfaceRef>,>(
                    &*args.get(0)
                ).clone();
            let type_: Rc<dyn crate::langhelp::Type> =
                Rc::new(
                    crate::eval::value::type_source_interface::r#const::TypeSourceInterfaceConst {
                        data: iface_ref.data.clone(),
                    }
                );
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "function_const_to_type",
        fn_scope_id: "FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7014,
        body: quote!{
            let func_ref: Rc<crate::eval::value::type_source_function::r#const::TypeSourceFunctionConst> =
                crate
                ::eval
                ::from_dynamic_ref::<Rc<crate::eval::value::type_source_function::r#const::TypeSourceFunctionConst>,>(
                    &*args.get(0)
                ).clone();
            let type_: Rc<dyn crate::langhelp::Type> =
                Rc::new(
                    crate::eval::value::type_source_function::r#const::TypeSourceFunctionConst {
                        data: func_ref.data.clone(),
                    }
                );
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "object_new_to_type",
        fn_scope_id: "DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7012,
        body: quote!{
            let ctor_ref: Rc<crate::eval::value::type_source_object_new::TypeSourceObjectNewConst> =
                crate
                ::eval
                ::from_dynamic_ref::<Rc<crate::eval::value::type_source_object_new::TypeSourceObjectNewConst>,>(
                    &*args.get(0)
                ).clone();
            let type_: Rc<dyn crate::langhelp::Type> =
                Rc::new(
                    crate::eval::value::type_source_object_new::TypeSourceObjectNewConst {
                        data: ctor_ref.data.clone(),
                    }
                );
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "union_new_to_type",
        fn_scope_id: "TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7018,
        body: quote!{
            let ctor_ref: Rc<crate::eval::value::type_source_union_new::SourceUnionNewRef> =
                crate::eval::from_dynamic_ref::<Rc<crate::eval::value::type_source_union_new::SourceUnionNewRef>,>(
                    &*args.get(0)
                ).clone();
            let type_: Rc<dyn crate::langhelp::Type> =
                Rc::new(crate::eval::value::type_source_union_new::r#const::TypeSourceUnionNewConst {
                    data: ctor_ref.data.clone(),
                    variant_index: ctor_ref.variant_index,
                });
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "data_instance_to_type",
        fn_scope_id: "DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID",
        scope_id: 7010,
        body: quote!{
            let template_type: Rc<dyn crate::langhelp::Type> =
                crate::eval::from_dynamic_ref::<Rc<dyn crate::langhelp::Type>>(&*args.get(0)).clone();
            let template =
                crate
                ::invariant
                ::must_downcast_ref::<crate::eval::value::type_source_object::r#const::TypeSourceObjectConst,>(
                    template_type.as_ref()
                );
            let instance =
                v8::Local::<v8::Object>::try_from(
                    args.get(1),
                ).expect("__data_instance_to_type: expected dynamic object for instance");
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
            let type_: Rc<dyn crate::langhelp::Type> = Rc::new(crate::eval::value::type_pull::r#const::TypePull {
                data: template.data.clone(),
                bundle: RefCell::new(Some(instance_global)),
                artifact_id: RefCell::new(None),
            });
            let ext = crate::eval::rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "data_instance_to_target",
        fn_scope_id: "DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID",
        scope_id: 7009,
        body: quote!{
            let instance =
                v8::Local::<v8::Object>::try_from(
                    args.get(1),
                ).expect("__data_instance_to_target: expected dynamic object for instance");
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
            let target: Rc<dyn crate::r#gen::Target> =
                Rc::new(crate::eval::pull_scope_dispatch::PullTarget { instance: instance_global });
            let ext = crate::eval::rc_target_to_dynamic_ext(scope, target);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "data_instance_to_binding",
        fn_scope_id: "DATA_INSTANCE_TO_BINDING_FN_SCOPE_ID",
        scope_id: 7008,
        body: quote!{
            let instance =
                v8::Local::<v8::Object>::try_from(
                    args.get(1),
                ).expect("__data_instance_to_binding: expected dynamic object for instance");
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
            let entry: Rc<dyn crate::r#gen::Binding> =
                Rc::new(crate::eval::pull_scope_dispatch::PullBinding { instance: instance_global });
            let ext = crate::eval::rc_binding_to_dynamic_ext(scope, entry);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "data_instance_to_value",
        fn_scope_id: "DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID",
        scope_id: 7011,
        body: quote!{
            let instance =
                v8::Local::<v8::Object>::try_from(
                    args.get(1),
                ).expect("__data_instance_to_value: expected dynamic object for instance");
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
            let value: Rc<dyn crate::langhelp::Value> =
                Rc::new(crate::eval::value::value_pull::dynamic::ValuePull { bundle: instance_global });
            let ext = crate::eval::rc_value_to_dynamic_ext(scope, value);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "data_instance_to_aspect",
        fn_scope_id: "DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID",
        scope_id: 7006,
        body: quote!{
            let instance =
                v8::Local::<v8::Object>::try_from(
                    args.get(0),
                ).expect("__data_instance_to_aspect: expected dynamic object for instance");
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
            let aspect: Rc<dyn crate::langhelp::Aspect> =
                Rc::new(crate::eval::aspect::AspectPull { bundle: instance_global });
            let ext = crate::eval::to_dynamic_ext(scope, aspect);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "data_instance_to_code",
        fn_scope_id: "DATA_INSTANCE_TO_CODE_FN_SCOPE_ID",
        scope_id: 7007,
        body: quote!{
            let instance =
                v8::Local::<v8::Object>::try_from(
                    args.get(1),
                ).expect("__data_instance_to_code: expected dynamic object for instance");
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
            let code: Rc<dyn crate::langhelp::CodeDyn> = Rc::new(crate::eval::CodePull { instance: instance_global });
            let ext = crate::eval::to_dynamic_ext(scope, code);
            rv.set(ext.into());
        },
    }, InvisibleBuiltin {
        name: "unique_eq",
        fn_scope_id: "UNIQUE_EQ_FN_SCOPE_ID",
        scope_id: 7021,
        body: quote!{
            let _ = scope;
            let a = *crate::eval::from_dynamic_ref::<crate::langhelp::Unique>(&*args.get(0));
            let b = *crate::eval::from_dynamic_ref::<crate::langhelp::Unique>(&*args.get(1));
            rv.set_bool(a == b);
        },
    }];
    let mut consts = TokenStream::new();
    let mut callbacks = TokenStream::new();
    let mut registrations = TokenStream::new();
    for spec in &specs {
        assert!(scope_ids.insert(spec.scope_id), "duplicate scope_id {} for {}", spec.scope_id, spec.name);
        let cb_ident = format_ident!("{}_callback", spec.name);
        let fn_scope_id_ident = format_ident!("{}", spec.fn_scope_id);
        let scope_id = spec.scope_id;
        let key_str = format!("{}{}", prefixes::BUILTIN_PREFIX, spec.scope_id);
        let body = &spec.body;
        consts.extend(quote!{
            pub(
                crate
            ) const #fn_scope_id_ident: crate:: langhelp:: BindingId = crate:: langhelp:: BindingId:: Builtin(
                #scope_id
            );
        });
        callbacks.extend(quote!{
            #[
                allow(unused_variables)
            ] fn #cb_ident <'s >(
                scope: & mut v8:: PinnedRef <'s,
                v8:: HandleScope <'_ >>,
                args: v8:: FunctionCallbackArguments <'s >,
                mut rv: v8:: ReturnValue <'s >,
            ) {
                #body
            }
        });
        registrations.extend(quote!{
            {
                let f = v8:: Function:: builder(#cb_ident).build(scope).unwrap();
                let key = v8:: String:: new(scope, #key_str).unwrap().cast();
                scope.get_current_context().global(scope).set(scope, key, f.into());
            }
        });
    }
    out.extend(quote!{
        #consts #callbacks pub(crate) fn install_invisible_builtins < 's,
        >(scope: & mut v8:: PinnedRef <'s, v8:: HandleScope <'_, v8:: Context >>) {
            #registrations
        }
    });
}

struct InvisibleBuiltin {
    body: TokenStream,
    fn_scope_id: &'static str,
    name: &'static str,
    scope_id: usize,
}
