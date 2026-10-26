#!/usr/bin/env python3
"""Reject accidental personal attribution in this project's commits and pushes."""

import os
import re
import subprocess
import sys
from pathlib import Path


PROJECT_NAME = "c4tntnitro-netizen"
PROJECT_EMAIL = "c4tntnitro-netizen@users.noreply.github.com"
PROJECT_IDENTITY = (PROJECT_NAME, PROJECT_EMAIL)
GITHUB_COMMITTER = ("GitHub", "noreply@github.com")
IDENTITY = re.compile(r"^(.*?) <([^<>]+)>$", re.DOTALL)
RAW_IDENTITY = re.compile(r"^(.*?) <([^<>]+)> -?[0-9]+ [+-][0-9]{4}$", re.DOTALL)
COAUTHOR = re.compile(r"^[ \t]*Co-authored-by[ \t]*:[ \t]*(.*)$", re.IGNORECASE)


class IdentityError(Exception):
    pass


def git(*args, input_bytes=None):
    environment = dict(os.environ, GIT_NO_REPLACE_OBJECTS="1")
    result = subprocess.run(
        ["git", *args], input=input_bytes, stdout=subprocess.PIPE,
        stderr=subprocess.PIPE, check=False, env=environment,
    )
    if result.returncode:
        # Git errors can echo author names, email addresses, or remote URLs.
        raise IdentityError("Git could not inspect identity metadata; the operation was blocked.")
    return result.stdout


def decode(data):
    return data.decode("utf-8", errors="replace")


def check_identity(value, role, location, raw=False):
    match = (RAW_IDENTITY if raw else IDENTITY).fullmatch(value)
    allowed = {PROJECT_IDENTITY}
    if role == "committer":
        allowed.add(GITHUB_COMMITTER)
    if match is None or match.groups() not in allowed:
        raise IdentityError(f"The {role} identity {location} is not approved for this project.")


def check_coauthors(message, location):
    # GitHub recognizes colon-form coauthor lines independently of the user's
    # trailer.separators configuration. Scan raw lines rather than Git's parser.
    for line in decode(message).splitlines():
        match = COAUTHOR.fullmatch(line)
        if match:
            check_identity(match.group(1).strip(), "coauthor", location)


def object_parts(oid):
    data = git("cat-file", "-p", oid)
    header, separator, message = data.partition(b"\n\n")
    if not separator:
        message = b""
    fields = {}
    for line in header.splitlines():
        if line.startswith(b" "):
            continue  # Continuation of a signed-object header.
        key, separator, value = line.partition(b" ")
        if separator:
            fields[decode(key)] = decode(value)
    return fields, message


def check_tag_chain(oid):
    """Check each annotated tag, including tags pointing to another tag."""
    while decode(git("cat-file", "-t", oid)).strip() == "tag":
        fields, _ = object_parts(oid)
        check_identity(fields.get("tagger", ""), "tagger", f"in tag {oid[:12]}", raw=True)
        oid = fields.get("object", "")
        if not re.fullmatch(r"[0-9a-fA-F]{40}|[0-9a-fA-F]{64}", oid):
            raise IdentityError("A tag target could not be inspected; the push was blocked.")
    return oid


def check_history(oid, checked_commits=None):
    if checked_commits is None:
        checked_commits = set()
    if decode(git("rev-parse", "--is-shallow-repository")).strip() == "true":
        raise IdentityError("Fetch the complete history before checking or pushing this repository.")
    target = check_tag_chain(oid)
    if decode(git("cat-file", "-t", target)).strip() != "commit":
        return  # A tree/blob tag has no commit ancestry.
    # Deliberately inspect the full ancestry, including already-remote commits.
    # Raw objects avoid mailmap rewriting, and replacement objects are disabled.
    for commit_oid in decode(git("rev-list", target)).splitlines():
        if commit_oid in checked_commits:
            continue
        checked_commits.add(commit_oid)
        commit, message = object_parts(commit_oid)
        location = f"in commit {commit_oid[:12]}"
        check_identity(commit.get("author", ""), "author", location, raw=True)
        check_identity(commit.get("committer", ""), "committer", location, raw=True)
        check_coauthors(message, location)


def check_push():
    checked_commits = set()
    checked_tips = set()
    for update in sys.stdin:
        fields = update.split()
        if len(fields) != 4:
            raise IdentityError("Push metadata could not be inspected; the push was blocked.")
        _, local_oid, _, _ = fields
        if set(local_oid) == {"0"}:
            continue  # Ref deletion introduces no identity metadata.
        if not re.fullmatch(r"[0-9a-fA-F]{40}|[0-9a-fA-F]{64}", local_oid):
            raise IdentityError("A pushed object could not be inspected; the push was blocked.")
        if local_oid in checked_tips:
            continue
        checked_tips.add(local_oid)
        check_history(local_oid, checked_commits)


def main():
    if len(sys.argv) < 2:
        raise IdentityError("An identity check mode is required.")
    mode = sys.argv[1]
    if mode == "pre-commit":
        for role in ("author", "committer"):
            value = decode(git("var", f"GIT_{role.upper()}_IDENT")).strip()
            check_identity(value, role, "for this commit", raw=True)
    elif mode == "commit-msg" and len(sys.argv) == 3:
        check_coauthors(Path(sys.argv[2]).read_bytes(), "in this commit message")
    elif mode == "pre-push":
        check_push()
    elif mode == "history" and len(sys.argv) == 3:
        oid = decode(git("rev-parse", "--verify", "--end-of-options", sys.argv[2])).strip()
        check_history(oid)
    else:
        raise IdentityError("The identity check mode is invalid.")


if __name__ == "__main__":
    try:
        main()
    except (IdentityError, OSError) as error:
        if isinstance(error, IdentityError):
            detail = str(error)
        else:
            detail = "The identity guard could not inspect its inputs; the operation was blocked."
        print(f"Chief Navigator identity guard: {detail}", file=sys.stderr)
        print(
            "Use the project identity and run tools/setup_git_identity.ps1 if needed. "
            "Names and addresses that failed this check are intentionally hidden.",
            file=sys.stderr,
        )
        sys.exit(1)
