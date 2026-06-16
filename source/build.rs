pub mod buildsrc;

use {
    quote::quote,
    std::{
        collections::HashSet,
        env,
        fs,
        path::PathBuf,
    },
};

fn main() {
    println!("cargo:rerun-if-changed=build.rs");
    let mut builtin_unique_ids = HashSet::new();
    let mut scope_ids = HashSet::new();
    let mut out = quote!{
        use std::rc::Rc;
        use std::cell::{
            Cell,
            RefCell
        };
        use std::collections::HashMap;
        use dumpster::unsync::Gc;
        use crate::langhelp::{
            Code,
            CodeValue,
            ConstKey,
            DefSetId,
            ExprContext,
            QExprId,
            Scope,
            ScopeEntryId,
            Unique,
            Value,
        };
        use crate::eval::TargetDynamic;
        use crate::eval::V8EvalContextSlot;
        use crate::eval::V8LogStore;
        use crate::eval::V8UniqueCounter;
        use crate::langhelp::LogEntry;
        use crate::eval::RcScopeEntryWrap;
        use crate::eval::const_eval;
        use crate::eval::definition_set::DefinitionSet;
        use crate::eval::expr::Expr;
        use crate::eval::value::type_source_function::{
            SourceFunctionData,
            TypeSourceFunctionConst
        };
        use crate::eval::value::type_source_tagged_union::{
            SourceTaggedUnionData,
            SourceTaggedUnionRef,
            SourceTaggedUnionVariant,
        };
        use crate::eval::value::type_source_tagged_union_constructor::SourceTaggedUnionConstructorRef;
        use crate::eval::value::type_source_data_type::{
            SourceDataMethodData,
            SourceDataTypeData,
            SourceDataTypeInterfaceImplData,
            SourceDataTypeInterfaceMethodImplData,
            SourceDataTypeRef,
        };
        use crate::eval::value::type_source_data_type_constructor::TypeSourceDataTypeConstructorConst;
        use crate::eval::value::type_source_interface::{
            SourceInterfaceData,
            SourceInterfaceMethodData,
            SourceInterfaceRef,
            TypeSourceInterfaceConst,
        };
        use crate::eval::pull::{
            AnyType,
            StagedExpr
        };
        use crate::eval::dynamic_util::throw_error;
    };
    buildsrc::aspects::build(&mut out, &mut builtin_unique_ids, &mut scope_ids);
    let mut interface_specs = buildsrc::aspects::interface_specs();
    interface_specs.extend(buildsrc::traits::specs());
    buildsrc::interfaces::build(&mut out, &mut builtin_unique_ids, &mut scope_ids, interface_specs);
    buildsrc::identities::build(&mut out, &mut builtin_unique_ids, &mut scope_ids);
    buildsrc::artifacts::build(&mut out);
    buildsrc::dyn_wrap_structs::build(&mut out, &mut builtin_unique_ids, buildsrc::traits::specs());
    buildsrc::dyn_wrap_fns::build(&mut out, &mut scope_ids);
    fs::write(PathBuf::from(env::var_os("OUT_DIR").unwrap()).join("gen.rs"), out.to_string()).unwrap();
}
