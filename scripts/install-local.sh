#!/usr/bin/env bash
# Install the already-built production JAR without putting credentials in Git.
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
jar_path="$project_dir/target/financial-literacy-app-0.0.1-SNAPSHOT.jar"
if [[ ! -f "$jar_path" ]]; then
    printf '%s\n' 'Build first: mvn -Pproduction package' >&2
    exit 1
fi
for dependency in java python3 systemctl loginctl curl xdg-open; do
    command -v "$dependency" >/dev/null || { printf 'Missing dependency: %s\n' "$dependency" >&2; exit 1; }
done
# Linger starts the user service manager at boot, even before desktop login.
loginctl enable-linger "$USER"
install -d -m 700 "$HOME/.config/steady" "$HOME/.local/share/steady"
install -d "$HOME/.config/systemd/user" "$HOME/.local/bin" "$HOME/.local/share/applications"
python3 - "$project_dir/src/main/resources/application.yml" "$HOME/.config/steady/steady.env" <<'PY'
import os,re,sys
from pathlib import Path
config=Path(sys.argv[1]).read_text()
destination=Path(sys.argv[2])
if not destination.exists():
    values={}
    for key in ['POSTGRES_HOST','POSTGRES_PORT','POSTGRES_DB_NAME','POSTGRES_USER','POSTGRES_PASSWORD','POSTGRES_SSLMODE']:
        match=re.search(r'\$\{'+key+r':([^}]*)}',config)
        if key not in os.environ and match is None:
            raise SystemExit('Missing database setting: '+key)
        values[key]=os.environ.get(key,match.group(1) if match else '')
    values.update(SERVER_ADDRESS='127.0.0.1',SERVER_PORT='8808',VAADIN_LAUNCH_BROWSER='false')
    # systemd EnvironmentFile quoting; credentials never appear in command output.
    def quote(value):
        if '\n' in value or '\r' in value: raise SystemExit('Multiline environment values are not supported')
        return '"'+value.replace('\\','\\\\').replace('"','\\"')+'"'
    fd=os.open(destination,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(fd,'w') as out:
        for key,value in values.items():out.write(key+'='+quote(value)+'\n')
else:
    destination.chmod(0o600)
PY
# Stop before replacing a running JAR, since Java reads resources lazily.
if systemctl --user is-active --quiet steady.service; then
    systemctl --user stop steady.service
fi
install -m 600 "$jar_path" "$HOME/.local/share/steady/steady.jar"
install -m 644 "$project_dir/deploy/steady.service" "$HOME/.config/systemd/user/steady.service"
install -m 755 "$project_dir/deploy/steady" "$HOME/.local/bin/steady"
# Absolute paths also work when GNOME's session PATH omits ~/.local/bin.
python3 - "$project_dir/deploy/steady.desktop" "$HOME/.local/share/applications/steady.desktop" "$HOME/.local/bin/steady" <<'PY'
import sys
from pathlib import Path
text=Path(sys.argv[1]).read_text()
launcher=sys.argv[3].replace('\\','\\\\').replace('"','\\"').replace('`','\\`').replace('$','\\$').replace('%','%%')
text='\n'.join('Exec="'+launcher+'"' if line=='Exec=steady' else 'TryExec='+sys.argv[3] if line=='TryExec=steady' else line for line in text.splitlines())+'\n' 
Path(sys.argv[2]).write_text(text)
PY
if command -v update-desktop-database >/dev/null; then
    update-desktop-database "$HOME/.local/share/applications"
fi
if command -v xdg-desktop-menu >/dev/null; then
    xdg-desktop-menu forceupdate --mode user
fi
systemctl --user daemon-reload
systemctl --user enable steady.service
systemctl --user restart --no-block steady.service
printf '%s\n' 'Installed. Steady starts at boot after a 10-second delay.' 'Open http://localhost:8808 or launch Steady from Applications.' 'Logs: journalctl --user -u steady.service -f'
