param([switch]$SkipPdf)
$ErrorActionPreference = 'Stop'
$reportDir = Split-Path -Parent $PSScriptRoot
$path = Join-Path $reportDir 'BaoCao_DailyMart_CT221H_TiengViet.docx'
$pdf = Join-Path $reportDir 'BaoCao_DailyMart_CT221H_TiengViet.pdf'
$word = $null
$document = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $word.AutomationSecurity = 3
    $document = $word.Documents.Open($path, $false, $false)
    $null = $document.Fields.Update()
    for ($pass = 0; $pass -lt 2; $pass++) {
        foreach ($toc in $document.TablesOfContents) { $toc.Update() }
        $document.Repaginate()
    }
    $document.Save()
    if (-not $SkipPdf) { $document.ExportAsFixedFormat($pdf, 17) }
    $summary = @{
        pages = $document.ComputeStatistics(2)
        words = $document.ComputeStatistics(0)
        tablesOfContents = $document.TablesOfContents.Count
        tables = $document.Tables.Count
        figures = $document.InlineShapes.Count
        docx = $path
        pdf = $pdf
    }
    $summary | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $reportDir 'CT221H_assets\document_validation.json')
    $summary | ConvertTo-Json
} finally {
    if ($null -ne $document) { $document.Close(0); [void][Runtime.InteropServices.Marshal]::ReleaseComObject($document) }
    if ($null -ne $word) { $saveOption = 0; $word.Quit([ref]$saveOption); [void][Runtime.InteropServices.Marshal]::ReleaseComObject($word) }
    [GC]::Collect()

}
