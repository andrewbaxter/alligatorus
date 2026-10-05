# Entry point for nix-build: builds package.nix against the ambient nixpkgs.
#
#   nix-build build.nix
#   result/bin/alligatorus build <path.at>
#
# Pass a different nixpkgs with `nix-build build.nix --arg pkgs 'import <other> {}'`.
{
  pkgs ? import <nixpkgs> { },
}:
pkgs.callPackage ./package.nix { }
