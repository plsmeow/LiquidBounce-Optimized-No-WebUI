{
  description = "LiquidBounce development environment";

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05"; };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      libs = with pkgs; [
        temurin-bin-25
        pciutils
        libpulseaudio
        libGL
        glfw
        openal
        # stdenv.cc.cc.lib
        git
        libX11
        libXcursor
        flite

        libgbm
        glib
        libxcb
        libxkbcommon
        libX11
        libXcomposite
        libXdamage
        libXext
        libXfixes
        libXrandr
        libgbm

        wayland
      ];

    in {
      devShells.${system}.default = pkgs.mkShell {
        packages = libs;
        buildInputs = libs;

        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
      };
    };
  nixConfig.bash-prompt-suffix = "[liquidbounce] ";
}
