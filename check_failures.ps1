Get-ChildItem -Path "app/build/test-results/testDebugUnitTest" -Filter "*.xml" | ForEach-Object {
    $file = $_.FullName
    [xml]$xml = Get-Content $file
    foreach ($tc in $xml.testsuite.testcase) {
        if ($tc.failure) {
            Write-Output "FAIL: $($tc.classname) -> $($tc.name)"
            Write-Output "MSG: $($tc.failure.message)"
            Write-Output "DETAILS: $($tc.failure.'#text')"
        }
    }
}
