#!/usr/bin/env bash
# Buduje Liquid.exe (self-contained, win-x64) i instalator dist/Liquid_Setup_1.0.0.exe
# Wymaga: .NET 8 SDK od Microsoftu (z Microsoft.NET.Sdk.WindowsDesktop) oraz NSIS (makensis).
set -euo pipefail
cd "$(dirname "$0")"
if [ ! -f installer/PawnIO_setup.exe ]; then
  echo "Pobieram sterownik PawnIO..."
  curl -fsSL -o installer/PawnIO_setup.exe https://github.com/namazso/PawnIO.Setup/releases/latest/download/PawnIO_setup.exe
fi
rm -rf dist && mkdir -p dist/app
dotnet publish src/Liquid/Liquid.csproj -c Release -o dist/app
(cd installer && makensis -V2 liquid.nsi)
ls -la dist
