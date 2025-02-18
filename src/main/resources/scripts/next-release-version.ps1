param (
    [switch]$dev
)

$tags = git tag --list

if ($null -eq $tags -or $tags.Count -eq 0)
{
    $major = 0
    $minor = 0
    $patch = 0
    $range = ""
}
else
{
    $actual_version_with_prefix = git describe --tags --abbrev=0
    $actual_version = $actual_version_with_prefix -replace '^v', ''
    $major, $minor, $patch = $actual_version -split '\.'
    $major = [int]$major
    $minor = [int]$minor
    $patch = [int]$patch
    $range = "$actual_version_with_prefix..@"
}

$commits = git log $range --reverse --format=%s%b

foreach ($commit in $commits)
{
    if ($commit -cmatch "BREAKING CHANGE|^\w+!")
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
        if ($dev)
        {
            $patch += 1
        }
        else
        {
            $minor += 1
            $patch = 0
        }

    }
    elseif ($commit -match "^fix")
    {
        $patch += 1
    }
}

$next_version = "$major.$minor.$patch"

Write-Output $next_version
Exit 0