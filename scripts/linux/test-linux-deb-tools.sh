#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
patch_script="$script_dir/patch-linux-deb.sh"
verify_script="$script_dir/verify-linux-deb.sh"
# shellcheck source=linux-deb-dependencies.sh
source "$script_dir/linux-deb-dependencies.sh"

for command in dpkg-deb truncate; do
    if ! command -v "$command" >/dev/null 2>&1; then
        echo "$command is required to test Linux DEB tooling." >&2
        exit 1
    fi
done

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nuvio-deb-test.XXXXXX")"
cleanup() {
    rm -rf "$work_dir"
}
trap cleanup EXIT

package_root="$work_dir/package root"
mkdir -p "$package_root/DEBIAN" "$package_root/usr/share/nuvio-test" "$package_root/opt/telumia/bin" "$package_root/opt/telumia/lib"
cat > "$package_root/DEBIAN/control" <<'CONTROL'
Package: nuvio-test
Version: 1.0-1
Architecture: all
Maintainer: Telumia contributors <140022384+Pepeu2010@users.noreply.github.com>
Depends: libmpv2 (>= 0.35),
 libwebkit2gtk-4.1-0 | nuvio-webkit-fallback,
 libxcomposite1,
 libxext6
Description: Linux DEB tooling fixture
CONTROL
printf 'fixture\n' > "$package_root/usr/share/nuvio-test/data file.txt"
printf '#!/bin/sh\nexit 0\n' > "$package_root/opt/telumia/bin/Telumia"
chmod +x "$package_root/opt/telumia/bin/Telumia"
printf 'fixture icon\n' > "$package_root/opt/telumia/lib/Telumia.png"

fixture="$work_dir/Nuvio fixture package.deb"
dpkg-deb --root-owner-group -b "$package_root" "$fixture" >/dev/null

"$patch_script" "$fixture"
"$patch_script" "$fixture"
"$verify_script" "$fixture"

desktop_entry="$(dpkg-deb --fsys-tarfile "$fixture" | tar -xOf - ./usr/share/applications/telumia.desktop)"
for expected_line in 'Name=Telumia' 'Exec=/opt/telumia/bin/Telumia %u' 'Icon=/opt/telumia/lib/Telumia.png'; do
    if ! grep -Fxq "$expected_line" <<< "$desktop_entry"; then
        echo "Launcher metadata missing: $expected_line" >&2
        exit 1
    fi
done

depends="$(dpkg-deb -f "$fixture" Depends)"
for dependency in libmpv2 libwebkit2gtk-4.1-0 libxcomposite1 libxext6; do
    count="$(deb_relationship_package_count "$depends" "$dependency")"
    if [[ "$count" -ne 1 ]]; then
        echo "Expected one $dependency relationship after two patch passes; found $count." >&2
        echo "Depends: $depends" >&2
        exit 1
    fi
done

non_root_fixture="$work_dir/non-root.deb"
dpkg-deb -b "$package_root" "$non_root_fixture" >/dev/null 2>&1
if "$verify_script" "$non_root_fixture" >/dev/null 2>&1; then
    echo "Verifier accepted a DEB containing non-root-owned entries." >&2
    exit 1
fi

corrupt_fixture="$work_dir/corrupt-data.deb"
cp "$fixture" "$corrupt_fixture"
truncate -s -128 "$corrupt_fixture"
if ! dpkg-deb -f "$corrupt_fixture" Maintainer >/dev/null 2>&1; then
    echo "Corrupt fixture no longer has readable control metadata." >&2
    exit 1
fi
if "$verify_script" "$corrupt_fixture" >/dev/null 2>&1; then
    echo "Verifier accepted a DEB with a truncated data archive." >&2
    exit 1
fi

printf 'Linux DEB tooling fixture tests passed.\n'
