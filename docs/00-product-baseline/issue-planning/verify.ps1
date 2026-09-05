$ErrorActionPreference='Stop'
. "$PSScriptRoot/github-api.ps1"
$manifest=Get-Content "$PSScriptRoot/manifest.json" -Raw | ConvertFrom-Json -AsHashtable
$config=Get-Content "$PSScriptRoot/github-config.json" -Raw | ConvertFrom-Json -AsHashtable
$checkpoint=Get-Content "$PSScriptRoot/published.json" -Raw | ConvertFrom-Json -AsHashtable
$query=@'
query($cursor:String) {
 repository(owner:"ikaros-dev",name:"ikaros") {
  issues(first:50,after:$cursor,orderBy:{field:CREATED_AT,direction:DESC}) {
   pageInfo { hasNextPage endCursor }
   nodes {
    id number title url body state issueType { name } parent { id number }
    labels(first:100) { nodes { name } }
    subIssues(first:100) { totalCount nodes { id number } }
    blockedBy(first:100) { totalCount nodes { id number } }
    issueFieldValues(first:20) {
     nodes { ... on IssueFieldSingleSelectValue { name value field { ... on IssueFieldSingleSelect { name } } } }
    }
    projectItems(first:20) {
     nodes {
      id project { id title url }
      fieldValues(first:20) {
       nodes { ... on ProjectV2ItemFieldSingleSelectValue { name field { ... on ProjectV2SingleSelectField { name } } } }
      }
     }
    }
   }
  }
 }
}
'@
$actualByKey=@{};$duplicateKeys=[System.Collections.Generic.List[string]]::new();$cursor=$null
do {
    $page=(Invoke-GitHubGraphQL $query @{cursor=$cursor}).repository.issues
    foreach($actual in $page.nodes) {
        if($actual.body -match '<!-- ikaros-v2-plan:([A-Z][0-9]{2}(?:-[0-9]{2})?) -->') {
            $key=$matches[1]
            if($actualByKey.ContainsKey($key)){$duplicateKeys.Add($key)}
            $actualByKey[$key]=$actual
        }
    }
    $cursor=$page.pageInfo.endCursor
} while($page.pageInfo.hasNextPage)
$failures=[System.Collections.Generic.List[object]]::new();$records=[System.Collections.Generic.List[object]]::new()
foreach($spec in $manifest.issues) {
    $actual=$actualByKey[$spec.key];$errors=[System.Collections.Generic.List[string]]::new()
    if(-not $actual){$failures.Add(@{key=$spec.key;errors=@('Missing issue')});continue}
    if($actual.title -ne $spec.title){$errors.Add('Title mismatch')}
    if($actual.state -ne 'OPEN'){$errors.Add('Unexpected state')}
    if($actual.issueType.name -ne $spec.type){$errors.Add('Issue Type mismatch')}
    if($spec.parent) {
        if($actual.parent.id -ne $actualByKey[$spec.parent].id){$errors.Add('Parent mismatch')}
    } elseif($actual.parent) {$errors.Add('Feature parent has an unexpected parent')}
    $expectedChildren=@($manifest.issues | Where-Object parent -eq $spec.key | ForEach-Object {$actualByKey[$_.key].id})
    $actualChildren=@($actual.subIssues.nodes | ForEach-Object {$_.id})
    if((($expectedChildren | Sort-Object) -join ',') -ne (($actualChildren | Sort-Object) -join ',')){$errors.Add('Sub-issue set mismatch')}
    $expectedDeps=@($spec.dependencies | ForEach-Object {$actualByKey[$_].id})
    $actualDeps=@($actual.blockedBy.nodes | ForEach-Object {$_.id})
    if((($expectedDeps | Sort-Object) -join ',') -ne (($actualDeps | Sort-Object) -join ',')){$errors.Add('Blocked-by set mismatch')}
    $actualLabels=@($actual.labels.nodes | ForEach-Object {$_.name})
    foreach($label in $spec.labels){if($label -notin $actualLabels){$errors.Add("Missing label $label")}}
    $priority=($actual.issueFieldValues.nodes | Where-Object {$_.field.name -eq 'Priority'}).value
    $effort=($actual.issueFieldValues.nodes | Where-Object {$_.field.name -eq 'Effort'}).value
    if($priority -ne $spec.priority){$errors.Add('Priority mismatch')}
    if($effort -ne $spec.effort){$errors.Add('Effort mismatch')}
    $projectItem=$actual.projectItems.nodes | Where-Object {$_.project.id -eq $config.project.id}
    if(-not $projectItem){$errors.Add('Missing Ikaros Project membership')}
    $status=($projectItem.fieldValues.nodes | Where-Object {$_.field.name -eq 'Status'}).name
    if($status -notlike "*$($spec.status)"){$errors.Add('Project Status mismatch')}
    if($actual.body -match '\{\{PARENT\}\}|\{\{DEPENDENCIES\}\}|同批创建后回填'){$errors.Add('Unresolved body reference')}
    foreach($dep in $spec.dependencies){if(-not $actual.body.Contains($actualByKey[$dep].url)){$errors.Add("Missing body dependency link $dep")}}
    if($errors.Count){$failures.Add(@{key=$spec.key;number=$actual.number;errors=$errors.ToArray()})}
    $records.Add(@{key=$spec.key;number=$actual.number;url=$actual.url;title=$actual.title;parentNumber=$actual.parent.number;type=$actual.issueType.name;priority=$priority;effort=$effort;status=$status;project=$config.project.url;labels=$actualLabels;subIssues=@($actual.subIssues.nodes.number);blockedBy=@($actual.blockedBy.nodes.number);passed=($errors.Count -eq 0)})
}
$report=@{checkedAt=[DateTime]::UtcNow.ToString('o');expected=$manifest.counts;found=$actualByKey.Count;duplicateKeys=$duplicateKeys.ToArray();failures=$failures.ToArray();records=$records.ToArray()}
$report | ConvertTo-Json -Depth 30 | Set-Content "$PSScriptRoot/verification.json" -Encoding utf8
$lines=[System.Collections.Generic.List[string]]::new()
$lines.Add('# Ikaros V2 GitHub Issue 创建结果')
$lines.Add('')
$lines.Add("项目：[Ikaros Project]($($config.project.url))。没有创建总跟踪 Issue。")
$lines.Add('')
$lines.Add("计划：$($manifest.counts.parents) 个大功能，$($manifest.counts.children) 个子功能，共 $($manifest.counts.total) 个 Issue。")
$lines.Add('')
$lines.Add('| 计划编号 | GitHub Issue | 父 Issue | Type | Priority | Effort | 状态 | 验证 |')
$lines.Add('|---|---|---|---|---|---|---|---|')
foreach($r in $records){$parent=if($r.parentNumber){"[#$($r.parentNumber)](https://github.com/ikaros-dev/ikaros/issues/$($r.parentNumber))"}else{'—'};$verified=if($r.passed){'通过'}else{'待修复'};$lines.Add("| $($r.key) | [#$($r.number) $($r.title)]($($r.url)) | $parent | $($r.type) | $($r.priority) | $($r.effort) | $($r.status) | $verified |")}
$lines.Add('')
$lines.Add('Start date / Target date 未设置：没有约定开发起止日期。Priority 和 Effort 使用仓库已有 Fields，Status 使用 Project 已有选项。')
$lines.Add('')
$lines.Add('基线盘点不等同于运行验收。已有实现作为复用基础，相关任务要求补齐实际差距或提供可重复的验收证据。')
$lines | Set-Content "$PSScriptRoot/RESULTS.md" -Encoding utf8
[pscustomobject]@{Expected=$manifest.counts.total;Found=$actualByKey.Count;Parents=$manifest.counts.parents;Children=$manifest.counts.children;DuplicateMarkers=$duplicateKeys.Count;FailedIssues=$failures.Count;PassedIssues=($records | Where-Object passed).Count} | ConvertTo-Json
if($failures.Count){$failures | Select-Object -First 20 | ConvertTo-Json -Depth 10}
if($failures.Count -or $duplicateKeys.Count){exit 1}
