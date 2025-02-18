param (
    [switch]$dev
)

# 1. Check the next release version with the script
$devParam = if ($dev)
{
    "-dev"
}
else
{
    ""
}
$nextVersion = Invoke-Expression "&'./src/main/resources/scripts/next-release-version.ps1' $devParam"

if (git tag -l | Where-Object { $_ -eq "v$nextVersion" })
{
    Write-Host "The tag v$nextVersion already exists. Canceling program execution."
    Exit 1
}


# 2. Update the version in the pom.xml file
$newVersionParam = "-DnewVersion=$nextVersion"
./mvnw versions:set $newVersionParam


# 3. Create a tag for the current version
git tag -a v$nextVersion -m "version $nextVersion"

# 4. Run Maven verify to update the changelog
./mvnw verify

# 5. Create a new commit to update the pom.xml and the CHANGELOG
git add .
git commit -m "docs: update to version $nextVersion"

# 6. Remove the tag of the previous commit and create the same tag on this new one
$previousTag = git tag -l | Select-Object -Last 1
if ($null -ne $previousTag -and $previousTag -eq "v$nextVersion")
{
    git tag -d $previousTag
}
git tag -a v$nextVersion -m "version $nextVersion"
Exit 0