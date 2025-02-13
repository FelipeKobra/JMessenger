$versao_atual = git describe --tags --abbrev=0

$versao_atual = $versao_atual -replace '^v', ''

$major, $minor, $patch = $versao_atual -split '\.'
$major = [int]$major
$minor = [int]$minor
$patch = [int]$patch

$commits = git log --since="$versao_atual" --reverse --format=%s

foreach ($commit in $commits)
{
    if ($commit -match "^BREAKING")
    {
        $major += 1
        $minor = 0
        $patch = 0
    }
    elseif ($commit -match "^feat")
    {
        $minor += 1
        $patch = 0
    }
    elseif ($commit -match "^fix")
    {
        $patch += 1
    }
}

$next_version = "$major.$minor.$patch"

Write-Host "Next release version: $next_version"