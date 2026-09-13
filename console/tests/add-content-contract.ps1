$ErrorActionPreference = "Stop"
$path = Join-Path $PSScriptRoot "..\src\views\ingestion\index.vue"
$source = Get-Content -Raw $path

foreach ($label in @("Add Content", "选择来源", "扫描或上传", "预览识别结果", "重复项和映射", "确认并开始导入", "查看 Activity")) {
  if ($source -notmatch [regex]::Escape($label)) { throw "Missing Add Content journey label: $label" }
}

foreach ($legacyLabel in @("元数据同步", "导入任务", "失败项目")) {
  if ($source -notmatch [regex]::Escape($legacyLabel)) { throw "Legacy ingestion capability disappeared from implementation: $legacyLabel" }
}

if ($source -notmatch "router\.push\('/activity'\)") { throw "Add Content must link to Activity" }
if ($source -notmatch "router\.push\('/library'\)") { throw "Add Content must link to Library" }

Write-Output "Add Content contract checks passed."
