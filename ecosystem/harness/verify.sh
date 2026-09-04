#!/usr/bin/env sh
# Compiles the synth module to LLVM IR, links it against a mock libpipewire, and checks its runtime behaviour.
# Usage: ecosystem/harness/verify.sh <classpath-for-compilerxyz.main.langhelp.Main> [workdir]
set -eu
CP="$1"; W="${2:-$(mktemp -d)}"; H="$(dirname "$0")"
java -cp "$CP" compilerxyz.main.langhelp.Main ecosystem/synth-midi-sine.at > "$W/synth.ll"
llvm-as "$W/synth.ll" -o "$W/synth.bc"
llc -O2 -filetype=obj "$W/synth.bc" -o "$W/synth.o"
llvm-objcopy --redefine-sym main=synth_main "$W/synth.o" "$W/synth_r.o"
clang -O1 "$H/mock_pipewire_harness.c" "$W/synth_r.o" -lm -o "$W/harness"
"$W/harness"
