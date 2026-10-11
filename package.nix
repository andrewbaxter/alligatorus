# Builds the alligatorus compiler as a nix package.
#
#   nix-build build.nix
#   result/bin/alligatorus build <path.at>
#   result/bin/at run <path.at> [args...]
#
# Only the files the maven build reads are staged, so unrelated changes in the
# repository don't invalidate the build. The lint module is left out of the
# reactor (its pom is staged only so maven can read the reactor). The wrapper puts clang on PATH (the in-language llvm `build` runs it
# through the `exec` builtin), wasmtime on PATH (the executor of apps built by
# the wat target), git on PATH (git remote sources are fetched with it) and
# libpipewire on LIBRARY_PATH so apps that declare the pipewire library link and
# run.
{
  lib,
  maven,
  jdk21,
  clang,
  wasmtime,
  wabt,
  git,
  pipewire,
  makeWrapper,
  rustPlatform,
}:
let
  # The language server protocol's Java bindings are generated at build time from
  # the schema merman owns (merman/langserver/schema.json, the merman submodule).
  schemask = rustPlatform.buildRustPackage {
    pname = "schemask";
    version = "0.1.5";
    src = ./schemask/source;
    cargoLock.lockFile = ./schemask/source/Cargo.lock;
    doCheck = false;
  };
in
maven.buildMavenPackage rec {
  pname = "alligatorus";
  version = "0.0.0";

  src = lib.fileset.toSource {
    root = ./.;
    fileset = lib.fileset.unions [
      ./pom.xml
      ./.mvn
      ./prelude/pom.xml
      ./prelude/src
      ./codegen/pom.xml
      ./codegen/src
      ./main/pom.xml
      ./main/src
      ./lint/pom.xml
      ./merman/langserver/schema.json
    ];
  };

  mvnJdk = jdk21;
  nativeBuildInputs = [
    jdk21
    makeWrapper
    schemask
  ];
  mvnParameters = lib.escapeShellArgs [
    "-pl"
    "main"
    "-am"
    "-Dspotless.check.skip=true"
    "dependency:copy-dependencies"
    "-DincludeScope=runtime"
  ];
  mvnHash = "sha256-b4QOR/BIZbBRTe5VKX2Wkw2ROIQCqgBRJA8E6uZihDk=";
  doCheck = false;

  installPhase = ''
    runHook preInstall
    mkdir -p $out/share/alligatorus $out/bin
    cp main/target/allig3-main-*.jar main/target/dependency/*.jar $out/share/alligatorus/
    makeWrapper ${jdk21}/bin/java $out/bin/alligatorus \
      --prefix PATH : ${lib.makeBinPath [ clang wasmtime wabt git ]} \
      --prefix LIBRARY_PATH : ${lib.makeLibraryPath [ pipewire ]} \
      --add-flag -cp \
      --add-flag "$out/share/alligatorus/*" \
      --add-flag compilerxyz.main.langhelp.Main
    ln -s alligatorus $out/bin/at
    runHook postInstall
  '';

  meta = {
    description = "The alligatorus compiler";
    mainProgram = "alligatorus";
    platforms = lib.platforms.linux;
  };
}
