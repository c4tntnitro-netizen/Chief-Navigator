<#
Install this repository's identity defaults and hooks without changing global Git settings.
Run after cloning: powershell -ExecutionPolicy Bypass -File tools/setup_git_identity.ps1
Requires Git and Python 3 on PATH. Hook copies and configuration are shared by linked
worktrees, including older worktrees that do not contain .githooks yet.

Hook files must retain LF endings. On Unix/macOS, set their executable bits with:
  chmod +x .githooks/pre-commit .githooks/commit-msg .githooks/pre-push
When first adding the hooks to Git, record those bits explicitly (Windows included):
  git update-index --chmod=+x -- .githooks/pre-commit .githooks/commit-msg .githooks/pre-push
This setup script does not stage files or replace unrelated existing hooks.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$identityRepoRoot = Split-Path -Parent $PSScriptRoot
$identityName = 'c4tntnitro-netizen'
$identityEmail = 'c4tntnitro-netizen@users.noreply.github.com'

function Invoke-IdentityGit {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $GitArguments)
    $identityGitOutput = & git -C $identityRepoRoot @GitArguments
    if ($LASTEXITCODE -ne 0) { throw 'Git identity setup failed.' }
    return $identityGitOutput
}

$identityActualRoot = Invoke-IdentityGit rev-parse --show-toplevel
if ([IO.Path]::GetFullPath($identityActualRoot) -ne [IO.Path]::GetFullPath($identityRepoRoot)) {
    throw 'Run this setup from the Chief Navigator repository.'
}
$identitySourceHooks = Join-Path $identityRepoRoot '.githooks'
$identityCommonDirectory = Invoke-IdentityGit rev-parse --git-common-dir
if (-not [IO.Path]::IsPathRooted($identityCommonDirectory)) {
    $identityCommonDirectory = Join-Path $identityRepoRoot $identityCommonDirectory
}
$identityDesiredHooks = [IO.Path]::GetFullPath((Join-Path $identityCommonDirectory 'chief-navigator-hooks'))
$identityManagedFiles = @('pre-commit', 'commit-msg', 'pre-push', 'check_identity.py')
foreach ($identityHookName in $identityManagedFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $identitySourceHooks $identityHookName) -PathType Leaf)) {
        throw 'The project identity hook files are missing; nothing was changed.'
    }
}

function Test-ApprovedIdentityHook {
    param([string] $HookPath, [string] $HookName)
    if ($identityManagedFiles -notcontains $HookName) { return $false }
    $identitySourceHash = (Get-FileHash -LiteralPath (Join-Path $identitySourceHooks $HookName) -Algorithm SHA256).Hash
    $identityExistingHash = (Get-FileHash -LiteralPath $HookPath -Algorithm SHA256).Hash
    return $identitySourceHash -eq $identityExistingHash
}

$identityCurrentHooks = Invoke-IdentityGit rev-parse --git-path hooks
if (-not [IO.Path]::IsPathRooted($identityCurrentHooks)) {
    $identityCurrentHooks = Join-Path $identityRepoRoot $identityCurrentHooks
}
$identityKnownHooks = @(
    'applypatch-msg', 'pre-applypatch', 'post-applypatch', 'pre-commit', 'pre-merge-commit',
    'prepare-commit-msg', 'commit-msg', 'post-commit', 'pre-rebase', 'post-checkout',
    'post-merge', 'pre-push', 'pre-receive', 'update', 'post-receive', 'post-update',
    'push-to-checkout', 'pre-auto-gc', 'post-rewrite', 'sendemail-validate',
    'fsmonitor-watchman', 'reference-transaction', 'proc-receive', 'post-index-change'
)
if ([IO.Path]::GetFullPath($identityCurrentHooks) -ne [IO.Path]::GetFullPath($identityDesiredHooks)) {
    foreach ($identityHookName in $identityKnownHooks) {
        $identityExistingHook = Join-Path $identityCurrentHooks $identityHookName
        if ((Test-Path -LiteralPath $identityExistingHook) -and
            -not (Test-ApprovedIdentityHook $identityExistingHook $identityHookName)) {
            throw 'Existing Git hooks need review before changing core.hooksPath; nothing was changed.'
        }
    }
}
if (Test-Path -LiteralPath $identityDesiredHooks) {
    foreach ($identityExistingItem in Get-ChildItem -LiteralPath $identityDesiredHooks -Force) {
        if ($identityExistingItem.PSIsContainer -or
            -not (Test-ApprovedIdentityHook $identityExistingItem.FullName $identityExistingItem.Name)) {
            throw 'The managed hooks directory contains unrecognized files; nothing was changed.'
        }
    }
}

New-Item -ItemType Directory -Path $identityDesiredHooks -Force | Out-Null
foreach ($identityHookName in $identityManagedFiles) {
    Copy-Item -LiteralPath (Join-Path $identitySourceHooks $identityHookName) -Destination $identityDesiredHooks -Force
}
if ($env:OS -ne 'Windows_NT') {
    & chmod +x (Join-Path $identityDesiredHooks 'pre-commit') (Join-Path $identityDesiredHooks 'commit-msg') (Join-Path $identityDesiredHooks 'pre-push')
    if ($LASTEXITCODE -ne 0) { throw 'Unable to mark the managed hooks executable.' }
}
$identityConfiguredHooks = $identityDesiredHooks.Replace('\', '/')
Invoke-IdentityGit config --local user.name $identityName
Invoke-IdentityGit config --local user.email $identityEmail
Invoke-IdentityGit config --local user.useConfigOnly true
Invoke-IdentityGit config --local core.hooksPath $identityConfiguredHooks

$identityExpectedSettings = @{
    'user.name' = $identityName
    'user.email' = $identityEmail
    'user.useConfigOnly' = 'true'
    'core.hooksPath' = $identityConfiguredHooks
}
foreach ($identityWorktreeRecord in (Invoke-IdentityGit worktree list --porcelain)) {
    if (-not $identityWorktreeRecord.StartsWith('worktree ')) { continue }
    $identityWorktreePath = $identityWorktreeRecord.Substring('worktree '.Length)
    if (-not (Test-Path -LiteralPath $identityWorktreePath -PathType Container)) { continue }
    foreach ($identitySetting in $identityExpectedSettings.GetEnumerator()) {
        $identityEffectiveValue = & git -C $identityWorktreePath config --get $identitySetting.Key
        if ($LASTEXITCODE -ne 0 -or $identityEffectiveValue -ne $identitySetting.Value) {
            throw 'A linked worktree or environment configuration overrides the shared identity defaults. Review it before committing.'
        }
    }
}
Write-Output 'Chief Navigator project identity and commit/push hooks are configured locally.'
Write-Output 'The absolute managed hook directory protects every linked worktree.'
