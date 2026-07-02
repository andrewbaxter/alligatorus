package compilerxyz.eval;

import compilerxyz.util.TList;

public record CompileRes(TList<LogEntry> logs, CompileResResult result) {}
