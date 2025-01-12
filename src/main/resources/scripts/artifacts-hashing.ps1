$targetDirectory = "../../../../target/artifacts"
$hashFile = "../../../../target/artifacts/artifacts-hashes.txt"

# Clear or create the hash file
if (Test-Path $hashFile)
{
    Remove-Item $hashFile
}
New-Item -Path $hashFile -ItemType File -Force | Out-Null

# Loop through each .exe file, calculate its hash, and append to the hash file
Get-ChildItem -Path $targetDirectory -Recurse -Include "*.exe", "*.jar" | ForEach-Object {
    $fileName = $_.Name
    $hash = Get-FileHash -Path $_.FullName -Algorithm SHA256
    Add-Content -Path $hashFile -Value "- $fileName"
    Add-Content -Path $hashFile -Value "    - $( $hash.Hash )"
}