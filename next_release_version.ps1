# Defina a versão atual
$versao_atual = git describe --tags --abbrev=0

# Remova o caractere "v" do início da string
$versao_atual = $versao_atual -replace '^v', ''

# Defina as variáveis para contagem de commits
$major, $minor, $patch = $versao_atual -split '\.'
$major = [int]$major
$minor = [int]$minor
$patch = [int]$patch

# Obtenha os commits desde a versão atual
$commits = git log --since="$versao_atual" --reverse --format=%s

# Itere sobre os commits
foreach ($commit in $commits)
{
    # Verifique se o commit é um breaking change
    if ($commit -match "^BREAKING")
    {
        $major += 1
        $minor = 0
        $patch = 0
    }
    # Verifique se o commit é um feat
    elseif ($commit -match "^feat")
    {
        $minor += 1
        $patch = 0
    }
    # Verifique se o commit é um fix
    elseif ($commit -match "^fix")
    {
        $patch += 1
    }
}

# Calcule a próxima versão
$proxima_versao = "$major.$minor.$patch"

# Imprima a próxima versão
Write-Host "Próxima versão: $proxima_versao"