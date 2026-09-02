Get-ChildItem -Path "app/build/test-results/testDebugUnitTest" -Filter "*.xml" | ForEach-Object {
    $file = $_.FullName
    [xml]$xml = Get-Content $file
    foreach ($tc in $xml.testsuite.testcase) {
        if ($tc.failure) {
            Write-Output "===================================="
            Write-Output "FAIL: $($tc.classname).$($tc.name)"
            Write-Output "MSG: $($tc.failure.message)"
            $lines = ($tc.failure.'#text' -split "`n") | Select-Object -First 5
            Write-Output "STACK: $($lines -join "`n")"
        }
    }
}
