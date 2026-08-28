package compilerxyz.main.eval;

import compilerxyz.prelude.util.TList;

public record CompileRes(TList<LogEntry> logs, CompileResResult result) {}
