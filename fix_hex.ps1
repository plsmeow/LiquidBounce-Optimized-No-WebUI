Get-ChildItem "C:\Users\plsmeow\Desktop\mc\LiquidBounce-nextgen\src\main\kotlin\net\ccbluex\liquidbounce\gui\screen" -Recurse -Filter *.kt | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    # Replace 8-digit hex literals with .toInt() - these exceed Int.MAX_VALUE in Kotlin
    $content = [regex]::Replace($content, '(?<!\.toInt\(\))0x[0-9a-fA-F]{8}', '$0.toInt()')
    Set-Content $_.FullName -Value $content -NoNewline
}
Write-Host "Done"