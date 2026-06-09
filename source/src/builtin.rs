pub(crate) use crate::eval::aspect::*;
#[allow(unused_imports)]
use {
    crate::langhelp::Unique,
    paste::paste,
};

macro_rules! aspect{
    ($name: ident {
        $($i: item) *
    }) => {
        paste!{
            #[allow(dead_code)] pub(crate) trait $name: Aspect {
                $($i) *
            }
            #[allow(dead_code)] pub(crate) fn[< $name: snake _aspect_id >]() -> Unique {
                return unique!();
            }
            #[
                allow(dead_code)
            ] pub(
                crate
            ) fn[
                < $name: snake _get >
            ](xx: & ExprContext, loc: QExprId, v: & dyn Type) -> EResult < Box < dyn $name >> {
                let Some(out) = v.aspect(xx, &[< $name: snake _aspect_id >]()) else {
                    return EResult::err(loc, format!("Type ({}) is missing aspect {}", v.desc(), stringify!{
                        $name
                    }));
                };
                return EResult:: ok(crate:: invariant:: must_downcast_box::< Box < dyn $name >>(out));
            }
            #[
                allow(dead_code)
            ] pub(
                crate
            ) fn[< $name: snake _get_maybe >](xx: & ExprContext, v: & dyn Type) -> Option < Box < dyn $name >> {
                let Some(out) = v.aspect(xx, &[< $name: snake _aspect_id >]()) else {
                    return None;
                };
                return Some(crate:: invariant:: must_downcast_box::< Box < dyn $name >>(out));
            }
        }
    };
}

macro_rules! builtin_artifact_id{
    () => {
        crate::langhelp::ArtifactIdBuiltin {
            file: file!().to_string(),
            line: line!() as usize,
        }
    };
}

/// Generates methods on a type for artifact serialization and registration.
///
/// Expands to
/// `impl $type { artifact_id(), build_artifact(data), build_type_artifact(), register(m) }`.
/// `$data` constrains the data shape so you can't accidentally serialize the wrong
/// data for a type.
macro_rules! generate_builtin_artifact{
    ($type: ty, $data: ty) => {
        impl $type {
            pub(crate) fn artifact_id() -> crate::langhelp::ArtifactIdBuiltin {
                crate::langhelp::ArtifactIdBuiltin {
                    file: file!().to_string(),
                    line: line!() as usize,
                }
            }
            pub(crate) fn build_artifact(data: $data) -> crate:: langhelp:: Artifact {
                crate::langhelp::Artifact {
                    type_: crate::langhelp::ArtifactId::Builtin(Self::artifact_id()),
                    data: serde_json::to_value(data).unwrap(),
                }
            }
            pub(crate) fn build_type_artifact() -> crate::langhelp::Artifact {
                crate::langhelp::Artifact {
                    type_: crate::langhelp::ArtifactId::Builtin(Self::artifact_id()),
                    data: serde_json::Value::Null,
                }
            }
            pub(crate) fn register(
                m:
                    &mut std
                    ::collections
                    ::HashMap<crate::langhelp::ArtifactIdBuiltin, std::rc::Rc<dyn crate::langhelp::Type>,>,
            ) {
                m.insert(Self::artifact_id(), std::rc::Rc::new(Self));
            }
        }
    };
}

macro_rules! unique{
    () => {
        Unique::Builtin {
            file: file!().to_string(),
            line: line!() as usize
        }
    };
}
