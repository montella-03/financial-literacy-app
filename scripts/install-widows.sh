#!/usr/bin/env bash
# Windows entry point for Git Bash. Keep the requested filename for compatibility.
set -euo pipefail
case "$(uname -s)" in
    MINGW*|MSYS*) ;;
    *) printf '%s\n' 'Run this installer on Windows using Git Bash, or run scripts/install-windows.ps1 in PowerShell.' >&2; exit 1 ;;
esac
command -v powershell.exe >/dev/null || { printf '%s\n' 'Windows PowerShell is required.' >&2; exit 1; }
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$script_dir/install-windows.ps1")"
