param (
    [switch]$dev
)

$actual_version_with_prefix = git describe --tags --abbrev=0
$actual_version = $actual_version_with_prefix -replace '^v', ''
$major, $minor, $patch = $actual_version -split '\.'
$major = [int]$major
$minor = [int]$minor
$patch = [int]$patch

$range = "$actual_version_with_prefix..@"
$commits = git log $range --reverse --format=%s

foreach ($commit in $commits)
{
    if ($commit -match "BREAKING|^\w+!")
    {
        if ($dev)
        {
            $minor += 1
            $patch = 0
        }
        else
        {
            $major += 1
            $minor = 0
            $patch = 0
        }
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