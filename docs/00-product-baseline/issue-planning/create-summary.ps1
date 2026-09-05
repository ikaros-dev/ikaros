$ErrorActionPreference='Stop'
. "$PSScriptRoot/github-api.ps1"
$manifest=Get-Content "$PSScriptRoot/manifest.json" -Raw | ConvertFrom-Json -AsHashtable
$config=Get-Content "$PSScriptRoot/github-config.json" -Raw | ConvertFrom-Json -AsHashtable
$fields=Get-Content "$PSScriptRoot/issue-fields.json" -Raw | ConvertFrom-Json -AsHashtable
$published=Get-Content "$PSScriptRoot/published.json" -Raw | ConvertFrom-Json -AsHashtable
$summaryMarker='<!-- ikaros-v2-summary -->'
$topLevel=@($manifest.issues | Where-Object { -not $_.parent })
if($topLevel.Count -ne 75){throw "Expected 75 top-level issues, found $($topLevel.Count)"}
$summaryPath="$PSScriptRoot/summary.json"
$summary=if(Test-Path $summaryPath){Get-Content $summaryPath -Raw | ConvertFrom-Json -AsHashtable}else{@{}}
$priorityField=$fields | Where-Object name -eq 'Priority'
$effortField=$fields | Where-Object name -eq 'Effort'
$statusField=$config.project.fields.nodes | Where-Object name -eq 'Status'
$taskType=$config.issueTypes | Where-Object name -eq 'Task'
$backlog=$statusField.options | Where-Object name -like '*Backlog'
if(-not $summary.issueId){
    $labels=@('version/v2','area/docs','kind/feature-group')
    $existing=(Invoke-GitHubGraphQL 'query{repository(owner:"ikaros-dev",name:"ikaros"){labels(first:100){nodes{name id}}}}').repository.labels.nodes
    $labelIds=@()
    foreach($labelName in $labels){
        $label=$existing | Where-Object name -eq $labelName
        if(-not $label){throw "Required label missing: $labelName"}
        $labelIds += $label.id
    }
    $rows=$topLevel | ForEach-Object {
        $saved=$published.issues[$_.key]
        "- [$($_.key) $($_.title)]($($saved.url)) — $($_.phase) / $($_.domain)"
    }
    $body=@"
$summaryMarker
## V2 功能汇总

这是 Ikaros V2 全量开发计划的汇总 Issue。以下 75 个最上级大功能 Issue 作为本 Issue 的真实 Sub-issues，GitHub 的 Sub-issues progress 用于显示整体进度。

计划范围：P0、P1、P2；共 75 个大功能、404 个子功能、479 个原计划 Issue。此汇总 Issue 不替代各功能 Issue 的验收条件；具体开发、依赖和验收仍以子 Issue 为准。

## 最上级功能

$($rows -join "`n")

## 汇总验收

- [ ] 所有最上级功能 Issue 的子任务完成并通过各自整体验收。
- [ ] P0、P1、P2 的跨功能依赖均已解决。
- [ ] 发布验收、迁移、备份恢复、安全和性能证据齐备。

## 规划依据

- [Ikaros V2 产品需求文档](https://github.com/ikaros-dev/ikaros/blob/main/docs/00-product-baseline/Product-Requirements-Document.md)
- [V2 Implementation Roadmap](https://github.com/ikaros-dev/ikaros/blob/main/docs/00-product-baseline/Implementation-Roadmap-and-Dependency-Graph.md)
"@
    $input=@{
        repositoryId=$config.repository.id
        title='[V2] Ikaros V2 全量开发汇总'
        body=$body
        labelIds=$labelIds
        issueTypeId=$taskType.id
        projectV2Ids=@($config.project.id)
        issueFields=@(
            @{fieldId=$priorityField.id;singleSelectOptionId=($priorityField.options | Where-Object name -eq 'High').id},
            @{fieldId=$effortField.id;singleSelectOptionId=($effortField.options | Where-Object name -eq 'High').id}
        )
    }
    $q='mutation($input:CreateIssueInput!){createIssue(input:$input){issue{id number url title issueType{name} projectItems(first:20){nodes{id project{id}}}}}}'
    $created=(Invoke-GitHubGraphQL $q @{input=$input}).createIssue.issue
    if(-not $created.id){throw 'Summary Issue creation returned no ID'}
    $summary=@{issueId=$created.id;number=$created.number;url=$created.url;projectItemId=(($created.projectItems.nodes | Where-Object {$_.project.id -eq $config.project.id}).id);linked=@()}
    $summary | ConvertTo-Json -Depth 20 | Set-Content $summaryPath -Encoding utf8
    Write-Output "Created summary #$($created.number): $($created.url)"
}
foreach($spec in $topLevel){
    if($spec.key -in @($summary.linked)){continue}
    $childId=$published.issues[$spec.key].id
    $q='mutation($input:AddSubIssueInput!){addSubIssue(input:$input){issue{id number} subIssue{id number parent{id number}}}}'
    $result=Invoke-GitHubGraphQL $q @{input=@{issueId=$summary.issueId;subIssueId=$childId;replaceParent=$true}}
    if(-not $result.addSubIssue.subIssue.id){throw "Failed to link $($spec.key)"}
    $summary.linked += $spec.key
    $summary | ConvertTo-Json -Depth 20 | Set-Content $summaryPath -Encoding utf8
    Write-Output "Linked $($spec.key) #$($published.issues[$spec.key].number) ($($summary.linked.Count)/$($topLevel.Count))"
    Start-Sleep -Milliseconds 700
}
if($summary.projectItemId){
    $q='mutation($input:UpdateProjectV2ItemFieldValueInput!){updateProjectV2ItemFieldValue(input:$input){clientMutationId}}'
    Invoke-GitHubGraphQL $q @{input=@{projectId=$config.project.id;itemId=$summary.projectItemId;fieldId=$statusField.id;value=@{singleSelectOptionId=$backlog.id}}} | Out-Null
}
Write-Output "Summary complete: #$($summary.number), linked $($summary.linked.Count) top-level issues."
