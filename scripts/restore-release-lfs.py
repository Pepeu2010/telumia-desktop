"""Hydrate only HEAD's exact LFS objects from a hash-pinned release source archive."""
import argparse
import hashlib
import json
import pathlib
import shutil
import subprocess
import tempfile
import zipfile

ARCHIVE_SHA256 = "bc1267cee3520f55e5554f705414da9636433268f7a2a32f57042504f82e7a7c"


def digest(stream):
    value = hashlib.sha256()
    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
        value.update(chunk)
    return value.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=pathlib.Path)
    parser.add_argument("--verify-only", action="store_true")
    args = parser.parse_args()
    root = pathlib.Path(subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip()).resolve()
    with args.archive.open("rb") as stream:
        if digest(stream) != ARCHIVE_SHA256:
            raise SystemExit("Release source archive SHA-256 mismatch")
    objects = json.loads(subprocess.check_output(["git", "lfs", "ls-files", "--json"], text=True))["files"]
    if not objects:
        raise SystemExit("Expected LFS objects in the checkout")
    with zipfile.ZipFile(args.archive) as archive:
        # Check every expected object before replacing any checkout file.
        for obj in objects:
            name = pathlib.PurePosixPath(obj["name"])
            target = root.joinpath(*name.parts)
            if name.is_absolute() or ".." in name.parts or not target.resolve().is_relative_to(root) or target.is_symlink():
                raise SystemExit("Unsafe LFS target")
            pointer = subprocess.check_output(["git", "show", "HEAD:" + obj["name"]], text=True)
            if "oid sha256:" + obj["oid"] not in pointer or "size " + str(obj["size"]) not in pointer:
                raise SystemExit("LFS inventory differs from HEAD")
            info = archive.getinfo(obj["name"])
            if info.file_size != obj["size"] or obj["oid_type"] != "sha256":
                raise SystemExit("Release object size or hash type mismatch: " + obj["name"])
            with archive.open(info) as stream:
                if digest(stream) != obj["oid"]:
                    raise SystemExit("Release object differs from HEAD: " + obj["name"])
        if not args.verify_only:
            for obj in objects:
                target = root / obj["name"]
                target.parent.mkdir(parents=True, exist_ok=True)
                temporary = None
                try:
                    with tempfile.NamedTemporaryFile(dir=target.parent, delete=False) as output:
                        temporary = pathlib.Path(output.name)
                        with archive.open(obj["name"]) as stream:
                            shutil.copyfileobj(stream, output)
                    temporary.chmod(target.stat().st_mode)
                    temporary.replace(target)
                finally:
                    if temporary is not None:
                        temporary.unlink(missing_ok=True)
    print(json.dumps(dict(archiveSha256=ARCHIVE_SHA256, objectsVerified=len(objects),
                         hydrated=not args.verify_only, source="v1.0.0 release source archive; exact HEAD LFS hashes")))


if __name__ == "__main__":
    main()
