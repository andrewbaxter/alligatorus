# Builds the alligatorus compiler as a nix package.
#
#   nix-build -E 'with import <nixpkgs> {}; callPackage ./package.nix {}'
#   result/bin/alligatorus build <path.at>
#   result/bin/at run <path.at> [args...]
#
# Only the files the maven build reads are staged, so unrelated changes in the
# repository don't invalidate the build. The lint module is left out of the
# reactor (its pom is staged only so maven can read the reactor). The wrapper puts clang on PATH (the in-language llvm `build` runs it
# through the `exec` builtin) and libpipewire on LIBRARY_PATH so apps that
# declare the pipewire library link and run.
{
  lib,
  maven,
  jdk21,
  clang,
  pipewire,
  runtimeShell,
}:
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
    ];
  };

  mvnJdk = jdk21;
  nativeBuildInputs = [ jdk21 ];
  mvnParameters = lib.escapeShellArgs [
    "-pl"
    "main"
    "-am"
    "-Dspotless.check.skip=true"
    "dependency:copy-dependencies"
    "-DincludeScope=runtime"
  ];
  mvnHash = "sha256-E0nY3+OW8PeMUPR1oJP5SOUVGgxflGgGMkyawIx6mlI=";
  doCheck = false;

  installPhase = ''
    runHook preInstall
    mkdir -p $out/share/alligatorus $out/bin
    cp main/target/allig3-main-*.jar main/target/dependency/*.jar $out/share/alligatorus/
    cat > $out/bin/alligatorus <<WRAP
    #!${runtimeShell}
    export PATH="${lib.makeBinPath [ clang ]}:\$PATH"
    export LIBRARY_PATH="${lib.makeLibraryPath [ pipewire ]}\''${LIBRARY_PATH:+:\$LIBRARY_PATH}"
    exec ${jdk21}/bin/java -cp "$out/share/alligatorus/*" compilerxyz.main.langhelp.Main "\$@"
    WRAP
    chmod +x $out/bin/alligatorus
    ln -s alligatorus $out/bin/at
    runHook postInstall
  '';

  meta = {
    description = "The alligatorus compiler";
    mainProgram = "alligatorus";
    platforms = lib.platforms.linux;
  };
}
