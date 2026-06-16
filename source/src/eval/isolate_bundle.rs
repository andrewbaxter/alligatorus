//! `IsolateBundle` carries a `v8::Global<v8::Object>` handle to a JS instance
//! built in an earlier stage. Bundled with a `ValueSourceDataTypeConst`, it lets
//! `TypePull.cast_from` reach the original JS instance — with its compiled
//! `_im0`…`_imN` method fields intact — at as-time.
//!
//! The handle is valid because all stages of one compile share a single v8 isolate
//! (created in `compile_importid` and threaded through `eval()` calls as
//! `Rc<RefCell<v8::OwnedIsolate>>`). The shared isolate keeps itself alive for the
//! whole compile, so persistent handles into it remain usable across stages. No
//! drop-order constraint applies: only one `OwnedIsolate` exists, and it drops
//! once when the compile finishes.
pub(crate) struct IsolateBundle {
    /// Persistent handle to the JS instance object (carries the `_im0`…`_imN` compiled
    /// method fields).
    pub instance: v8::Global<v8::Object>,
}
