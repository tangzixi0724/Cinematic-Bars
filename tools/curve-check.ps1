# Mirror of CinematicBarsAnimation.tick plus the mixin offset maths.
# Sanity-checks every selectable curve for stalls before shipping.
function CB_Raw([string]$n, [double]$u) {
  if ($n -eq 'linear') { return $u }
  if ($n -eq 'smoothstep') { return $u * $u * (3 - 2 * $u) }
  if ($n -eq 'smootherstep') { return $u * $u * $u * ($u * ($u * 6 - 15) + 10) }
  if ($n -eq 'ease_in_quad') { return $u * $u }
  if ($n -eq 'ease_in_out') { if ($u -lt 0.5) { return 2 * $u * $u } else { return 1 - [math]::Pow(-2 * $u + 2, 2) / 2 } }
  if ($n -eq 'ease_out_quad') { return 1 - (1 - $u) * (1 - $u) }
  if ($n -eq 'ease_out_cubic') { return 1 - [math]::Pow(1 - $u, 3) }
  if ($n -eq 'ease_out_quart') { return 1 - [math]::Pow(1 - $u, 4) }
  if ($n -eq 'ease_out_expo') { return 1 - [math]::Pow(2, -10 * $u) }
  return $u
}

function CB_Shaped([string]$n, [double]$u, [double]$durationMs, [double]$travelPx, [double]$fps, [double]$minStepPx) {
  if ($u -ge 1) { return 1.0 }
  $shaped = CB_Raw $n $u
  $frames = [math]::Max(1.0, $durationMs * $fps / 1000.0)
  $minStep = $minStepPx / $travelPx / $frames
  $floor = 1.0 - (1.0 - $u) * $frames * $minStep
  if ($floor -gt $shaped) { return $floor }
  return $shaped
}

$fps = 60.0
$durationMs = 450.0
$guiHeight = 240.0          # typical GUI-scaled height at 1080p / guiScale 2
$barHeight = $guiHeight * 0.08
$feather = $barHeight * 0.22
$travel = $barHeight + $feather
$frames = [int][math]::Round($durationMs * $fps / 1000.0)

"bar {0:N1} px + feather {1:N1} px = travel {2:N1} GUI px over {3} frames" -f $barHeight, $feather, $travel, $frames
"constant speed would be {0:N2} px/frame; floor is 0.5 px/frame" -f ($travel / $frames)
""
"{0,-15} {1,8} {2,8} {3,8} {4,6} {5}" -f 'curve', 'last', 'min', 'peak', 'tiny', 'verdict'

foreach ($n in @('linear', 'smoothstep', 'smootherstep', 'ease_in_quad', 'ease_in_out', 'ease_out_quad', 'ease_out_cubic', 'ease_out_quart', 'ease_out_expo')) {
  $prev = 0.0
  $last = 0.0
  $min = 999.0
  $peak = 0.0
  $tiny = 0
  $mono = $true

  for ($f = 1; $f -le $frames; $f++) {
    $u = [double]$f / [double]$frames
    $p = CB_Shaped $n $u $durationMs $travel $fps 0.5
    $step = [math]::Round(($p - $prev) * $travel, 4)
    if ($step -lt -0.001) { $mono = $false }
    if ($f -eq $frames) { $last = $step }
    if ($f -lt $frames) {
      if ($step -lt 0.05) { $tiny++ }
      if ($step -lt $min) { $min = $step }
    }
    if ($step -gt $peak) { $peak = $step }
    $prev = $p
  }

  if (-not $mono) {
    $verdict = 'NOT MONOTONIC'
  } elseif ($tiny -gt 0) {
    $verdict = 'stalls=' + $tiny
  } elseif ($last -lt 0.2) {
    $verdict = 'weak landing'
  } else {
    $verdict = 'OK'
  }

  "{0,-15} {1,8:N2} {2,8:N3} {3,8:N2} {4,6} {5}" -f $n, $last, $min, $peak, $tiny, $verdict
}

""
"last = distance covered by the final frame, in GUI px"
"min  = smallest step in any non-final frame, in GUI px"
"tiny = non-final frames moving less than 0.05 px (visible stall)"
