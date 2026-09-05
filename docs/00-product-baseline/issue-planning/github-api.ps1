$ErrorActionPreference = 'Stop'
$env:GCM_INTERACTIVE = 'Never'
$env:GIT_TERMINAL_PROMPT = '0'
$taskGhPath = Join-Path $env:TEMP 'ikaros-v2-github-cli/bin/gh.exe'
$taskGhToken = if (Test-Path -LiteralPath $taskGhPath) { & $taskGhPath auth token --hostname github.com 2>$null } else { $null }
$savedCredential = if ($taskGhToken) { @('password=' + $taskGhToken) } else { "protocol=https`nhost=github.com`n`n" | git credential fill 2>$null }
$taskGhToken = $null
$credentialParts = @{}
foreach ($line in $savedCredential) {
    if ($line -match '^([^=]+)=(.*)$') { $credentialParts[$matches[1]] = $matches[2] }
}
if (-not $credentialParts['password']) { throw 'Existing GitHub credential unavailable.' }
$script:GitHubHeaders = @{
    Authorization = 'Bearer ' + $credentialParts['password']
    Accept = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
}
$savedCredential = $null
$credentialParts = $null

function Invoke-GitHub {
    param([string]$Path, [string]$Method = 'GET', [object]$Body)
    $request = @{ Uri = "https://api.github.com/$Path"; Headers = $script:GitHubHeaders; Method = $Method }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 60 -Compress))
    }
    Invoke-RestMethod @request
}

function Invoke-GitHubGraphQL {
    param([string]$Query, [object]$Variables = @{})
    $response = Invoke-GitHub -Path 'graphql' -Method 'POST' -Body @{query = $Query; variables = $Variables}
    if ($response.errors) { throw ($response.errors | ConvertTo-Json -Depth 20 -Compress) }
    $response.data
}
