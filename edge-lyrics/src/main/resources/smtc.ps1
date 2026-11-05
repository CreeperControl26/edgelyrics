# Polls Windows' System Media Transport Controls (what Edge uses for the media overlay)
# and prints one JSON line every 500 ms.
$ErrorActionPreference = 'SilentlyContinue'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and
    $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, $type) {
    $t = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))
    $t.Wait(-1) | Out-Null
    $t.Result
}
[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType = WindowsRuntime]
$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

while ($true) {
    $out = @{ active = $false }
    try {
        $best = $null
        foreach ($s in $mgr.GetSessions()) {
            if ($s.SourceAppUserModelId -notmatch 'edge') { continue }
            if ($s.GetPlaybackInfo().PlaybackStatus -eq 'Playing') { $best = $s; break }
            if (-not $best) { $best = $s }
        }
        if ($best) {
            $p = Await ($best.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            $tl = $best.GetTimelineProperties()
            $playing = ($best.GetPlaybackInfo().PlaybackStatus -eq 'Playing')
            $pos = $tl.Position.TotalSeconds
            if ($playing -and $tl.LastUpdatedTime.Year -gt 2000) {
                $pos += ([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalSeconds
            }
            $out = @{
                active = $true; playing = $playing
                title = $p.Title; artist = $p.Artist; album = $p.AlbumTitle
                position = $pos; duration = $tl.EndTime.TotalSeconds
            }
        }
    } catch {}
    [Console]::Out.WriteLine(($out | ConvertTo-Json -Compress))
    [Console]::Out.Flush()
    Start-Sleep -Milliseconds 500
}
