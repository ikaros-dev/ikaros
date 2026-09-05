param([int]$Limit = 0, [switch]$FinalizeOnly)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/github-api.ps1"
$manifest = Get-Content -LiteralPath "$PSScriptRoot/manifest.json" -Raw | ConvertFrom-Json -AsHashtable
$config = Get-Content -LiteralPath "$PSScriptRoot/github-config.json" -Raw | ConvertFrom-Json -AsHashtable
$fields = @(Get-Content -LiteralPath "$PSScriptRoot/issue-fields.json" -Raw | ConvertFrom-Json -AsHashtable)
$checkpointPath = "$PSScriptRoot/published.json"
$checkpoint = if (Test-Path -LiteralPath $checkpointPath) { Get-Content -LiteralPath $checkpointPath -Raw | ConvertFrom-Json -AsHashtable } else { @{repository='ikaros-dev/ikaros';projectUrl=$config.project.url;issues=@{};labels=@{}} }
$byKey = @{}
foreach ($issue in $manifest.issues) { $byKey[$issue.key] = $issue }
$priorityField = $fields | Where-Object name -eq 'Priority'
$effortField = $fields | Where-Object name -eq 'Effort'
$statusField = $config.project.fields.nodes | Where-Object name -eq 'Status'
function Save-Checkpoint {
    $checkpoint.updatedAt = [DateTime]::UtcNow.ToString('o')
    $checkpoint | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath $checkpointPath -Encoding utf8
}
function Get-Body($spec) {
    $dependencyText = if ($spec.dependencies.Count) {
        ($spec.dependencies | ForEach-Object {
            if ($checkpoint.issues.ContainsKey($_)) { '- ' + $checkpoint.issues[$_].url }
            else { '- ' + $_ + '（同批创建后回填真实 Issue 链接和阻塞关系）' }
        }) -join "`n"
    } else { '无前置阻塞，可以开始。' }
    $parentText = if ($spec.parent) { $checkpoint.issues[$spec.parent].url } else { '' }
    $spec.body.Replace('{{DEPENDENCIES}}', $dependencyText).Replace('{{PARENT}}', $parentText)
}
function Set-RelationsAndStatus($spec, $saved, [bool]$finalize = $false) {
    if (-not $saved.projectItemId) {
        $found = Invoke-GitHubGraphQL 'query($id:ID!){node(id:$id){... on Issue{projectItems(first:20){nodes{id project{id}}}}}}' @{id=$saved.id}
        $saved.projectItemId = ($found.node.projectItems.nodes | Where-Object { $_.project.id -eq $config.project.id }).id
        if (-not $saved.projectItemId) {
            $added = Invoke-GitHubGraphQL 'mutation($input:AddProjectV2ItemByIdInput!){addProjectV2ItemById(input:$input){item{id}}}' @{input=@{projectId=$config.project.id;contentId=$saved.id}}
            $saved.projectItemId=$added.addProjectV2ItemById.item.id
        }
        Save-Checkpoint
    }
    $status = $statusField.options | Where-Object { $_.name -like "*$($spec.status)" }
    if (-not $status) { throw "Missing status $($spec.status)" }
    $variables = @{}
    $declarations = [System.Collections.Generic.List[string]]::new()
    $operations = [System.Collections.Generic.List[string]]::new()
    if (-not $saved.statusSet) {
        $declarations.Add('$status: UpdateProjectV2ItemFieldValueInput!')
        $variables.status = @{projectId=$config.project.id;itemId=$saved.projectItemId;fieldId=$statusField.id;value=@{singleSelectOptionId=$status.id}}
        $operations.Add('status: updateProjectV2ItemFieldValue(input:$status) { clientMutationId }')
    }
    $newDeps = @($spec.dependencies | Where-Object { $checkpoint.issues.ContainsKey($_) -and $_ -notin $saved.dependenciesSet })
    for ($i=0;$i -lt $newDeps.Count;$i++) {
        $name = "dep$i"
        $declarations.Add("`$$name`: AddBlockedByInput!")
        $variables[$name] = @{issueId=$saved.id;blockingIssueId=$checkpoint.issues[$newDeps[$i]].id}
        $operations.Add("$name`: addBlockedBy(input:`$$name) { clientMutationId }")
    }
    if ($finalize -and -not $saved.bodyFinalized) {
        $declarations.Add('$body: UpdateIssueInput!')
        $variables.body = @{id=$saved.id;body=(Get-Body $spec)}
        $operations.Add('body: updateIssue(input:$body) { clientMutationId }')
    }
    if ($operations.Count) {
        $q = 'mutation(' + ($declarations -join ',') + ') {' + ($operations -join "`n") + '}'
        Invoke-GitHubGraphQL $q $variables | Out-Null
        $saved.statusSet = $true
        $saved.dependenciesSet = @($saved.dependenciesSet) + $newDeps
        if ($finalize) { $saved.bodyFinalized = $true }
        Save-Checkpoint
    }
}

function Set-BatchMetadata([string[]]$Keys) {
    if (-not $Keys.Count) { return }
    $ids = @($Keys | ForEach-Object { $checkpoint.issues[$_].id })
    $snapshot = Invoke-GitHubGraphQL 'query($ids:[ID!]!){nodes(ids:$ids){... on Issue{id projectItems(first:20){nodes{id project{id}}} blockedBy(first:100){nodes{id}}}}}' @{ids=$ids}
    $actualById=@{}
    foreach ($actual in $snapshot.nodes) { $actualById[$actual.id]=$actual }
    $declarations=[System.Collections.Generic.List[string]]::new()
    $operations=[System.Collections.Generic.List[string]]::new()
    $variables=@{}
    $updates=@{}
    foreach ($key in $Keys) {
        $spec=$byKey[$key]; $saved=$checkpoint.issues[$key]; $actual=$actualById[$saved.id]
        $saved.projectItemId=($actual.projectItems.nodes | Where-Object {$_.project.id -eq $config.project.id}).id
        if (-not $saved.projectItemId) {
            $added=Invoke-GitHubGraphQL 'mutation($input:AddProjectV2ItemByIdInput!){addProjectV2ItemById(input:$input){item{id}}}' @{input=@{projectId=$config.project.id;contentId=$saved.id}}
            $saved.projectItemId=$added.addProjectV2ItemById.item.id
        }
        $prefix=$key.Replace('-','_')
        if (-not $saved.statusSet) {
            $status=$statusField.options | Where-Object { $_.name -like "*$($spec.status)" }
            if (-not $status) { throw "Missing status $($spec.status)" }
            $name="s_$prefix"
            $declarations.Add("`$$name`: UpdateProjectV2ItemFieldValueInput!")
            $variables[$name]=@{projectId=$config.project.id;itemId=$saved.projectItemId;fieldId=$statusField.id;value=@{singleSelectOptionId=$status.id}}
            $operations.Add("$name`: updateProjectV2ItemFieldValue(input:`$$name){clientMutationId}")
        }
        $knownDeps=@($spec.dependencies | Where-Object {$checkpoint.issues.ContainsKey($_)})
        $newDeps=@($knownDeps | Where-Object {$checkpoint.issues[$_].id -notin $actual.blockedBy.nodes.id})
        for($i=0;$i -lt $newDeps.Count;$i++) {
            $name="d_$($prefix)_$i"
            $declarations.Add("`$$name`: AddBlockedByInput!")
            $variables[$name]=@{issueId=$saved.id;blockingIssueId=$checkpoint.issues[$newDeps[$i]].id}
            $operations.Add("$name`: addBlockedBy(input:`$$name){clientMutationId}")
        }
        $finalizeBody=(-not $saved.bodyFinalized -and $knownDeps.Count -eq $spec.dependencies.Count)
        if($finalizeBody) {
            $name="b_$prefix"
            $declarations.Add("`$$name`: UpdateIssueInput!")
            $variables[$name]=@{id=$saved.id;body=(Get-Body $spec)}
            $operations.Add("$name`: updateIssue(input:`$$name){clientMutationId}")
        }
        $updates[$key]=@{dependencies=$knownDeps;finalizeBody=$finalizeBody}
    }
    if($operations.Count) {
        $query='mutation('+($declarations -join ',')+'){'+($operations -join "`n")+'}'
        Invoke-GitHubGraphQL $query $variables | Out-Null
    }
    foreach($key in $Keys) {
        $saved=$checkpoint.issues[$key];$saved.statusSet=$true;$saved.dependenciesSet=$updates[$key].dependencies
        if($updates[$key].finalizeBody){$saved.bodyFinalized=$true}
    }
    Save-Checkpoint
    Write-Output "Metadata verified/configured: $($Keys -join ', ')"
}

if (-not $FinalizeOnly) {
    # Reconcile completed writes before a retry. Never blindly repeat createIssue.
    $cursor = $null
    do {
        $q = 'query($cursor:String){ repository(owner:"ikaros-dev",name:"ikaros"){ issues(first:100,after:$cursor,orderBy:{field:CREATED_AT,direction:DESC}){pageInfo{hasNextPage endCursor} nodes{id number title url body projectItems(first:20){nodes{id project{id}}}}}}}'
        $page = (Invoke-GitHubGraphQL $q @{cursor=$cursor}).repository.issues
        foreach ($existing in $page.nodes) {
            if ($existing.body -match '<!-- ikaros-v2-plan:([A-Z][0-9]{2}(?:-[0-9]{2})?) -->') {
                $key = $matches[1]
                if (-not $byKey.ContainsKey($key)) { continue }
                if (-not $checkpoint.issues.ContainsKey($key)) {
                    $projectItem = $existing.projectItems.nodes | Where-Object { $_.project.id -eq $config.project.id }
                    $checkpoint.issues[$key] = @{id=$existing.id;number=$existing.number;url=$existing.url;projectItemId=$projectItem.id;statusSet=$false;dependenciesSet=@();bodyFinalized=$false}
                } elseif ($checkpoint.issues[$key].id -ne $existing.id) { throw "Duplicate plan marker $key" }
            }
        }
        $cursor=$page.pageInfo.endCursor
    } while ($page.pageInfo.hasNextPage)
    Save-Checkpoint
    $existingLabels = Invoke-GitHub -Path 'repos/ikaros-dev/ikaros/labels?per_page=100'
    foreach ($label in $existingLabels) { $checkpoint.labels[[string]$label.name]=$label.node_id }
    $requiredLabels = @($manifest.issues | ForEach-Object { $_.labels | ForEach-Object { [string]$_ } } | Sort-Object -Unique)
    foreach ($name in $requiredLabels) {
        if ($checkpoint.labels.ContainsKey($name)) { continue }
        $color = if ($name -like 'subsystem/*') {'1D76DB'} elseif ($name -like 'phase/*') {'5319E7'} elseif ($name -like 'workflow/*') {'FBCA04'} else {'0E8A16'}
        $description = switch -Wildcard ($name) {
            'version/v2' { 'Ikaros V2 approved development plan' }
            'phase/*' { 'Product delivery phase; independent from Issue Priority' }
            'subsystem/*' { 'Functional ownership: ' + $name.Substring(10) }
            'kind/feature-group' { 'Concrete feature parent; progress tracked through native sub-issues' }
            'kind/feature-slice' { 'Independently verifiable and closable functional slice' }
            'workflow/hitl' { 'Requires a maintainer decision or acceptance review before dependent work' }
            'workflow/afk' { 'Can be implemented independently when contracts and blockers are satisfied' }
            'area/client' { 'Desktop or mobile client work associated with this repository issue' }
            default { 'Ikaros V2 development planning' }
        }
        $created = Invoke-GitHub -Path 'repos/ikaros-dev/ikaros/labels' -Method POST -Body @{name=$name;color=$color;description=$description}
        $checkpoint.labels[$name]=$created.node_id
        Save-Checkpoint
        Write-Output "Label configured: $name"
        Start-Sleep -Milliseconds 900
    }
    $createdCount=0
    $pendingKeys=[System.Collections.Generic.List[string]]::new()
    foreach ($key in $manifest.order) {
        $spec=$byKey[$key]
        if (-not $checkpoint.issues.ContainsKey($key)) {
            if ($Limit -gt 0 -and $createdCount -ge $Limit) { break }
            $type=$config.issueTypes | Where-Object name -eq $spec.type
            $priority=$priorityField.options | Where-Object name -eq $spec.priority
            $effort=$effortField.options | Where-Object name -eq $spec.effort
            if (-not $type -or -not $priority -or -not $effort) { throw "Unresolved metadata for $key" }
            $input=@{
                repositoryId=$config.repository.id;title=$spec.title;body=(Get-Body $spec)
                labelIds=@($spec.labels | ForEach-Object { $checkpoint.labels[$_] })
                issueTypeId=$type.id;projectV2Ids=@($config.project.id)
                issueFields=@(@{fieldId=$priorityField.id;singleSelectOptionId=$priority.id},@{fieldId=$effortField.id;singleSelectOptionId=$effort.id})
            }
            if ($spec.parent) { $input.parentIssueId=$checkpoint.issues[$spec.parent].id }
            $q='mutation($input:CreateIssueInput!){createIssue(input:$input){issue{id number url title issueType{name} parent{id} projectItems(first:20){nodes{id project{id}}}}}}'
            $created=(Invoke-GitHubGraphQL $q @{input=$input}).createIssue.issue
            if (-not $created.id) { throw "Creation outcome missing for $key; reconcile before retry" }
            $projectItem=$created.projectItems.nodes | Where-Object { $_.project.id -eq $config.project.id }
            $checkpoint.issues[$key]=@{id=$created.id;number=$created.number;url=$created.url;projectItemId=$projectItem.id;statusSet=$false;dependenciesSet=@();bodyFinalized=[bool]$spec.parent}
            Save-Checkpoint
            # Project membership is applied asynchronously by createIssue; resolve before setting status.
            if ($created.issueType.name -ne $spec.type) { throw "Type mismatch for $key" }
            if ($spec.parent -and $created.parent.id -ne $checkpoint.issues[$spec.parent].id) { throw "Parent mismatch for $key" }
            $createdCount++
            Write-Output "Created $key #$($created.number) ($($checkpoint.issues.Count)/$($manifest.issues.Count))"
        }
        $saved=$checkpoint.issues[$key]
        if(-not $saved.statusSet -or -not $saved.bodyFinalized -or $saved.dependenciesSet.Count -ne $spec.dependencies.Count){$pendingKeys.Add($key)}
        if($pendingKeys.Count -ge 8){Set-BatchMetadata $pendingKeys.ToArray();$pendingKeys.Clear()}
        if($createdCount -gt 0){Start-Sleep -Milliseconds 1200}
    }
    Set-BatchMetadata $pendingKeys.ToArray()
}
if ($checkpoint.issues.Count -eq $manifest.issues.Count) {
    $finalKeys=[System.Collections.Generic.List[string]]::new()
    foreach ($key in $manifest.order) {
        $spec=$byKey[$key]
        $saved=$checkpoint.issues[$key]
        if (-not $saved.bodyFinalized -or $saved.dependenciesSet.Count -ne $spec.dependencies.Count -or -not $saved.statusSet) {
            $finalKeys.Add($key)
            if($finalKeys.Count -ge 8){Set-BatchMetadata $finalKeys.ToArray();$finalKeys.Clear();Start-Sleep -Milliseconds 1200}
        }
    }
    Set-BatchMetadata $finalKeys.ToArray()
}
Write-Output "Checkpoint: $($checkpoint.issues.Count)/$($manifest.issues.Count) issues recorded."
