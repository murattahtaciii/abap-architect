#!/usr/bin/env bash
#
# Kurulu bir Eclipse kurulumundan (bundles.info) yerel bir target-platform dizini
# uretir ve Tycho icin local target dosyasini yazar. Boylece internetten p2
# hedefi indirmeden (offline) build alinabilir.
#
# Kullanim:
#   releng/sync-local-target.sh [eclipse-kurulum-dizini]
#
# Ornek:
#   releng/sync-local-target.sh ~/eclipse/java-2026-03/Eclipse.app/Contents/Eclipse
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ECLIPSE_HOME="${1:-${ECLIPSE_HOME:-}}"

if [ -z "$ECLIPSE_HOME" ]; then
  for candidate in "$HOME"/eclipse/*/Eclipse.app/Contents/Eclipse /Applications/Eclipse.app/Contents/Eclipse; do
    if [ -f "$candidate/.eclipseproduct" ]; then
      ECLIPSE_HOME="$candidate"
      break
    fi
  done
fi

if [ -z "$ECLIPSE_HOME" ] || [ ! -f "$ECLIPSE_HOME/.eclipseproduct" ]; then
  echo "HATA: Eclipse kurulum dizini bulunamadi. Arguman olarak verin:" >&2
  echo "  releng/sync-local-target.sh /path/to/Eclipse.app/Contents/Eclipse" >&2
  exit 1
fi

INFO="$ECLIPSE_HOME/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info"
if [ ! -f "$INFO" ]; then
  echo "HATA: bundles.info bulunamadi: $INFO" >&2
  exit 1
fi

DEST="$ROOT/releng/target-platform"
rm -rf "$DEST"
mkdir -p "$DEST/plugins" "$DEST/features"

count=0
missing=0
while IFS=, read -r id version location rest; do
  case "$id" in
    \#*|"") continue ;;
  esac
  path="${location#file:}"
  if [ -f "$path" ]; then
    cp -l "$path" "$DEST/plugins/" 2>/dev/null || cp "$path" "$DEST/plugins/"
    count=$((count + 1))
  elif [ -d "$path" ]; then
    case "$path" in
      *justj.openjdk*)
        echo "Atlandi (buyuk JRE bundle): $id"
        ;;
      *)
        base="$(basename "$path")"
        if [ -f "$path/META-INF/MANIFEST.MF" ]; then
          jar cfm "$DEST/plugins/$base.jar" "$path/META-INF/MANIFEST.MF" -C "$path" . >/dev/null 2>&1 \
            && count=$((count + 1)) \
            || missing=$((missing + 1))
        else
          missing=$((missing + 1))
        fi
        ;;
    esac
  else
    missing=$((missing + 1))
  fi
done < "$INFO"

TARGET_FILE="$ROOT/releng/target/com.murattahtaci.abaparchitect.target.local.target"
cat > "$TARGET_FILE" <<EOF
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<?pde version="3.8"?>
<target name="ABAP Architect Local Target" sequenceNumber="1">
  <locations>
    <location includeAllPlatforms="false" includeConfigurePhase="false" includeMode="planner" includeSource="false" type="Directory" path="$DEST"/>
  </locations>
</target>
EOF

echo "Kaynak    : $ECLIPSE_HOME"
echo "Bundles   : $count kopyalandi, $missing atlandi -> $DEST"
echo "Target    : $TARGET_FILE"
echo
echo "Offline build icin:  ./mvnw -Plocal-target clean verify"
