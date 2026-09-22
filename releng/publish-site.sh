#!/usr/bin/env bash
#
# p2 update site'i build eder ve GitHub Pages (gh-pages dalı) üzerine yayınlar.
# Kullanim:
#   releng/publish-site.sh
#
# Ön koşul: origin remote'u tanımlı ve push yetkiniz olmalı.
# Yayın sonrası Eclipse kurulum adresi:
#   https://<kullanici>.github.io/<repo>/
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SITE="$ROOT/releng/io.github.murattahtaciii.abaparchitect.site/target/repository"

cd "$ROOT"

if ! git remote get-url origin >/dev/null 2>&1; then
  echo "HATA: 'origin' remote'u tanimli degil. Once GitHub deposunu baglayin." >&2
  exit 1
fi

echo "==> Update site build ediliyor..."
./mvnw -Plocal-target clean verify

if [ ! -f "$SITE/content.jar" ]; then
  echo "HATA: Update site bulunamadi: $SITE" >&2
  exit 1
fi

WORK="$(mktemp -d)"
cleanup() {
  git worktree remove "$WORK" --force >/dev/null 2>&1 || rm -rf "$WORK"
}
trap cleanup EXIT

echo "==> gh-pages worktree hazirlaniyor..."
if git ls-remote --exit-code --heads origin gh-pages >/dev/null 2>&1; then
  git worktree add "$WORK" gh-pages
  git -C "$WORK" pull --ff-only origin gh-pages
else
  git worktree add "$WORK" --detach
  git -C "$WORK" checkout --orphan gh-pages
  git -C "$WORK" rm -rf . >/dev/null 2>&1 || true
fi

echo "==> Site kopyalaniyor..."
find "$WORK" -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +
cp -R "$SITE"/. "$WORK"/

git -C "$WORK" add -A
if git -C "$WORK" diff --cached --quiet; then
  echo "Degisiklik yok, push atlandi."
else
  git -C "$WORK" commit -m "Update site $(date '+%Y-%m-%d %H:%M')"
  git -C "$WORK" push origin gh-pages
  echo "==> Yayinlandi."
fi

URL="$(git remote get-url origin | sed -E 's#(git@|https://)github.com[:/]#https://github.com/#; s#\.git$##')"
OWNER_REPO="${URL#https://github.com/}"
OWNER="${OWNER_REPO%%/*}"
REPO="${OWNER_REPO##*/}"
echo
echo "Eclipse kurulum adresi (GitHub Pages acikken):"
echo "  https://${OWNER}.github.io/${REPO}/"
echo
echo "GitHub Pages'i ilk kez acmak icin:"
echo "  gh api repos/${OWNER_REPO}/pages -f 'source[branch]=gh-pages' -f 'source[path]=/'"
