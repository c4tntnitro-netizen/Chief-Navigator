"""Exercise the real hooks in isolated repositories with synthetic identities."""

import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
NAME = "c4tntnitro-netizen"
EMAIL = "c4tntnitro-netizen@users.noreply.github.com"
BAD_NAME = "Anonymous Test"
BAD_EMAIL = "bad@example.invalid"


class GitIdentityRegression(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="chief-navigator-identity-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.repo = self.root / "repo"
        self.repo.mkdir()
        self.environment = dict(os.environ)
        for key in list(self.environment):
            if key.startswith(("GIT_AUTHOR_", "GIT_COMMITTER_", "GIT_CONFIG_")):
                del self.environment[key]
        self.environment.update(
            GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL=os.devnull,
            GIT_TERMINAL_PROMPT="0", HOME=str(self.root),
            XDG_CONFIG_HOME=str(self.root / "config"),
        )
        self.git("init", "--initial-branch=main", "-q")
        self.git("config", "user.name", NAME)
        self.git("config", "user.email", EMAIL)
        shutil.copytree(ROOT / ".githooks", self.repo / ".githooks")
        for hook in ("pre-commit", "commit-msg", "pre-push"):
            (self.repo / ".githooks" / hook).chmod(0o755)
        self.git("config", "core.hooksPath", ".githooks")
        self.remote = self.root / "remote.git"
        self.git("init", "--bare", "-q", str(self.remote))
        self.git("remote", "add", "origin", str(self.remote))

    def run_command(self, arguments, expected=0, extra_env=None, input_bytes=None):
        environment = dict(self.environment)
        environment.update(extra_env or {})
        result = subprocess.run(
            arguments, cwd=self.repo, env=environment, input=input_bytes,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False,
        )
        output = result.stdout.decode("utf-8", errors="replace")
        if expected == 0:
            self.assertEqual(result.returncode, 0, output)
        else:
            self.assertNotEqual(result.returncode, 0, output)
            self.assertIn("identity", output.lower(), output)
            self.assertNotIn(BAD_NAME, output)
            self.assertNotIn(BAD_EMAIL, output)
        return output.strip()

    def git(self, *arguments, **options):
        return self.run_command(["git", *arguments], **options)

    def commit(self, message="Test commit", *arguments, **options):
        return self.git("commit", "--allow-empty", "-q", "-m", message, *arguments, **options)

    def guard(self, *arguments, **options):
        return self.run_command(
            [sys.executable, str(self.repo / ".githooks" / "check_identity.py"), *arguments],
            **options,
        )

    def bad_commit(self, message="Historical synthetic commit"):
        self.commit(message, "--no-verify", "--author", f"{BAD_NAME} <{BAD_EMAIL}>")
        return self.git("rev-parse", "HEAD")

    def test_project_identity_and_github_committer(self):
        self.commit()
        self.commit(extra_env={"GIT_COMMITTER_NAME": "GitHub", "GIT_COMMITTER_EMAIL": "noreply@github.com"})
        self.git("push", "-q", "origin", "HEAD:main")
        self.guard("history", "HEAD")

    def test_explicit_author_and_environment_overrides(self):
        self.commit("Bad explicit author", "--author", f"{BAD_NAME} <{BAD_EMAIL}>", expected=1)
        self.commit(expected=1, extra_env={"GIT_AUTHOR_NAME": BAD_NAME, "GIT_AUTHOR_EMAIL": BAD_EMAIL})
        self.commit(expected=1, extra_env={"GIT_COMMITTER_NAME": BAD_NAME, "GIT_COMMITTER_EMAIL": BAD_EMAIL})
        self.git("config", "user.email", BAD_EMAIL)
        self.commit(expected=1)

    def test_final_coauthor_trailers(self):
        self.commit(f"Bad coauthor\n\nCo-authored-by: {BAD_NAME} <{BAD_EMAIL}>", expected=1)
        self.commit(f"Allowed coauthor\n\nCo-authored-by: {NAME} <{EMAIL}>")
        self.commit("Service is not a coauthor\n\nCo-authored-by: GitHub <noreply@github.com>", expected=1)
        self.git("push", "-q", "origin", "HEAD:main")

    def test_amend_checks_inherited_author(self):
        self.bad_commit()
        self.git("commit", "--allow-empty", "--amend", "--no-edit", expected=1)
        self.git("commit", "--allow-empty", "--amend", "--no-edit", "--reset-author", "-q")
        self.guard("history", "HEAD")

    def test_custom_trailer_configuration_cannot_hide_coauthor(self):
        self.git("config", "trailer.separators", "=")
        message = f"Synthetic trailer\n\nCo-authored-by: {BAD_NAME} <{BAD_EMAIL}>"
        self.commit(message, expected=1)
        self.commit(message, "--no-verify")
        self.guard("history", "HEAD", expected=1)
        self.git("push", "origin", "HEAD:main", expected=1)

    def test_push_checks_already_remote_ancestry(self):
        self.bad_commit()
        self.git("push", "--no-verify", "-q", "origin", "HEAD:main")
        self.commit("Allowed child")
        self.git("push", "origin", "HEAD:main", expected=1)
        self.git("push", "origin", "HEAD:new-branch", expected=1)
        self.git("push", "--force", "origin", "HEAD:main", expected=1)
        self.guard("history", "HEAD", expected=1)

    def test_push_checks_committer_and_coauthor_from_bypassed_commits(self):
        self.commit("Bad committer", "--no-verify", extra_env={
            "GIT_COMMITTER_NAME": BAD_NAME, "GIT_COMMITTER_EMAIL": BAD_EMAIL,
        })
        self.git("push", "origin", "HEAD:main", expected=1)
        self.git("switch", "--orphan", "coauthor")
        self.commit(f"Bad trailer\n\nCo-authored-by: {BAD_NAME} <{BAD_EMAIL}>", "--no-verify")
        self.git("push", "origin", "HEAD:coauthor", expected=1)

    def test_mailmap_and_replace_cannot_hide_raw_identity(self):
        bad_oid = self.bad_commit()
        (self.repo / "identity.mailmap").write_text(
            f"{NAME} <{EMAIL}> {BAD_NAME} <{BAD_EMAIL}>\n", encoding="utf-8",
        )
        self.git("config", "mailmap.file", "identity.mailmap")
        self.assertEqual(self.git("log", "-1", "--format=%aN <%aE>"), f"{NAME} <{EMAIL}>")
        self.guard("history", "HEAD", expected=1)
        tree_oid = self.git("rev-parse", "HEAD^{tree}")
        replacement_oid = self.git("commit-tree", tree_oid, input_bytes=b"Approved replacement\n")
        self.git("replace", bad_oid, replacement_oid)
        self.assertEqual(self.git("log", "-1", "--format=%an <%ae>"), f"{NAME} <{EMAIL}>")
        self.guard("history", "HEAD", expected=1)
        self.git("push", "origin", "HEAD:main", expected=1)

    def test_annotated_lightweight_nested_and_noncommit_tags(self):
        self.commit()
        self.git("tag", "lightweight")
        self.git("tag", "-a", "approved", "-m", "Approved tag")
        self.git("tag", "-a", "unapproved", "-m", "Synthetic tag", extra_env={
            "GIT_COMMITTER_NAME": BAD_NAME, "GIT_COMMITTER_EMAIL": BAD_EMAIL,
        })
        self.git("tag", "-a", "nested", "unapproved", "-m", "Nested tag")
        self.git("tag", "-a", "tree-tag", "HEAD^{tree}", "-m", "Tree tag")
        self.git("push", "-q", "origin", "refs/tags/lightweight", "refs/tags/approved", "refs/tags/tree-tag")
        self.git("push", "origin", "refs/tags/unapproved", expected=1)
        self.git("push", "origin", "refs/tags/nested", expected=1)

    def test_deletion_is_allowed_and_multiple_refs_are_all_checked(self):
        self.commit()
        self.git("push", "-q", "origin", "HEAD:main")
        self.git("branch", "approved")
        self.bad_commit()
        self.git("push", "origin", "approved:approved", "HEAD:bad", expected=1)
        self.git("push", "-q", "origin", ":main")

    def test_setup_is_idempotent_and_preserves_existing_hooks(self):
        powershell = shutil.which("pwsh") or shutil.which("powershell")
        if not powershell:
            self.skipTest("PowerShell is unavailable")
        (self.repo / "tools").mkdir()
        setup = self.repo / "tools" / "setup_git_identity.ps1"
        shutil.copyfile(ROOT / "tools" / setup.name, setup)
        command = [powershell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(setup)]
        self.run_command(command)
        self.run_command(command)
        self.assertEqual(self.git("config", "--local", "user.useConfigOnly"), "true")
        managed = self.repo / ".git" / "chief-navigator-hooks"
        self.assertEqual(self.git("config", "--local", "core.hooksPath"), managed.as_posix())
        self.commit()
        older = self.root / "older-worktree"
        self.git("worktree", "add", "-q", "--detach", str(older), "HEAD")
        self.assertFalse((older / ".githooks").exists())
        result = subprocess.run(
            ["git", "-C", str(older), "commit", "--allow-empty", "-m", "Older checkout", "--author", f"{BAD_NAME} <{BAD_EMAIL}>"],
            env=self.environment, capture_output=True,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(b"identity guard", result.stderr)
        self.git("config", "--unset", "core.hooksPath")
        existing = self.repo / ".git" / "hooks" / "pre-commit"
        existing.write_text("#!/bin/sh\nexit 0\n", encoding="utf-8")
        original = existing.read_bytes()
        result = subprocess.run(command, cwd=self.repo, env=self.environment, capture_output=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(existing.read_bytes(), original)
        self.assertNotIn("core.hookspath", self.git("config", "--local", "--list").lower())
        existing.unlink()
        (managed / "unknown-helper").write_text("Unrecognized content\n", encoding="utf-8")
        result = subprocess.run(command, cwd=self.repo, env=self.environment, capture_output=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn("core.hookspath", self.git("config", "--local", "--list").lower())

    def test_setup_rejects_other_linked_worktree_overrides(self):
        powershell = shutil.which("pwsh") or shutil.which("powershell")
        if not powershell:
            self.skipTest("PowerShell is unavailable")
        self.commit()
        older = self.root / "older-worktree"
        self.git("worktree", "add", "-q", "--detach", str(older), "HEAD")
        self.git("config", "extensions.worktreeConfig", "true")
        self.git("-C", str(older), "config", "--worktree", "core.hooksPath", "ignored-hooks")
        (self.repo / "tools").mkdir()
        setup = self.repo / "tools" / "setup_git_identity.ps1"
        shutil.copyfile(ROOT / "tools" / setup.name, setup)
        command = [powershell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(setup)]
        result = subprocess.run(command, cwd=self.repo, env=self.environment, capture_output=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(b"linked worktree", result.stderr.lower())
        self.git("-C", str(older), "config", "--worktree", "--unset", "core.hooksPath")
        self.run_command(command)


if __name__ == "__main__":
    unittest.main(verbosity=2)
