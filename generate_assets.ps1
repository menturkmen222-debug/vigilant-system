
$drawableDir = "app/src/main/res/drawable"
New-Item -ItemType Directory -Force -Path $drawableDir

function Create-Vector ($name, $pathData, $color) {
    $content = @"
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="100dp"
    android:height="100dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0">
    <path
        android:fillColor="$color"
        android:pathData="$pathData"/>
</vector>
"@
    Set-Content -Path "$drawableDir/$name.xml" -Value $content
}

# Hands (Gray/Black)
$handPath = "M12,2C12,2 8,6 8,10C8,14 10,16 12,18C14,16 16,14 16,10C16,6 12,2 12,2M12,24C8,24 4,20 4,16L20,16C20,20 16,24 12,24"
$hands = @("hand_pointing", "hand_writing", "hand_cartoon", "hand_marker", "hand_open")
foreach ($h in $hands) { Create-Vector $h $handPath "#FF424242" }

# Backgrounds (Just a full rect, effectively)
$bgPath = "M0,0h24v24h-24z"
# Colors: White, Green, Beige, Gray, Brown
Create-Vector "bg_whiteboard" $bgPath "#FFFFFFFF"
Create-Vector "bg_chalkboard" $bgPath "#FF2E7D32"
Create-Vector "bg_paper" $bgPath "#FFFFF3E0"
Create-Vector "bg_grid" "M0,0h24v24h-24z M2,2h20v1h-20z M2,6h20v1h-20z M2,10h20v1h-20z" "#FFF5F5F5" # Crude grid
Create-Vector "bg_cork" $bgPath "#FF8D6E63"

# Doodles (Black lines mostly)
$arrowPath = "M19,15l-1.41,-1.41L13,18.17V2H11v16.17l-4.59,-4.59L5,15l7,7L19,15z"
$starPath = "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2L9.19,8.63L2,9.24l5.46,4.73L5.82,21L12,17.27z"
$checkPath = "M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7z"

Create-Vector "doodle_arrow" $arrowPath "#FF000000"
Create-Vector "doodle_arrow_curved" "M19,15l-1.41,-1.41L13,18.17V2H11v16.17l-4.59,-4.59L5,15l7,7L19,15z" "#FF000000"
Create-Vector "doodle_checkmark" $checkPath "#FF4CAF50"
Create-Vector "doodle_cross" "M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41z" "#FFF44336"
Create-Vector "doodle_star" $starPath "#FFFFEB3B"
Create-Vector "doodle_heart" "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5C2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z" "#FFFF1744"
Create-Vector "doodle_lightbulb" "M9,21c0,0.55 0.45,1 1,1h4c0.55,0 1,-0.45 1,-1v-1H9V21z M12,2C8.14,2 5,5.14 5,9c0,2.38 1.19,4.47 3,5.74V17c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1v-2.26c1.81,-1.27 3,-3.36 3,-5.74C19,5.14 15.86,2 12,2z" "#FFFFEB3B"
Create-Vector "doodle_question" "M11,18h2v-2h-2V18z M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2z M12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8s8,3.59 8,8S16.41,20 12,20z M12,6c-2.21,0 -4,1.79 -4,4h2c0,-1.1 0.9,-2 2,-2s2,0.9 2,2c0,2 -3,1.75 -3,5h2c0,-2.25 3,-2.5 3,-5C16,7.79 14.21,6 12,6z" "#FF2196F3"
Create-Vector "doodle_exclamation" "M11,15h2v2h-2V15z M11,7h2v6h-2V7z M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2z" "#FFF44336"
Create-Vector "doodle_circle" "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2z" "#FF2196F3"
Create-Vector "doodle_speech" "M20,2H4C2.9,2 2,2.9 2,4v18l4,-4h14c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2z" "#FF9E9E9E"
Create-Vector "doodle_thought" "M20,2H4C2.9,2 2,2.9 2,4v18l4,-4h14c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2z" "#FF9E9E9E"

# Icons
$iconPath = "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2z"
$icons = @("icon_phone", "icon_laptop", "icon_tablet", "icon_clock", "icon_calendar", "icon_mail", "icon_chart_bar", "icon_chart_pie", "icon_chart_line", "icon_money", "icon_target", "icon_trophy")
foreach ($i in $icons) { Create-Vector $i $iconPath "#FF607D8B" }

# People
$personPath = "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4s-4,1.79 -4,4S9.79,12 12,12z M12,14c-2.67,0 -8,1.34 -8,4v2h16v-2C20,15.34 14.67,14 12,14z"
$people = @("person_standing", "person_presenting", "person_thinking", "person_working", "person_celebrating", "team_meeting", "team_collaboration")
foreach ($p in $people) { Create-Vector $p $personPath "#FF3F51B5" }

# Shapes
$rectPath = "M3,3h18v18h-18z"
$shapes = @("shape_rectangle", "shape_circle", "shape_triangle", "shape_diamond", "shape_hexagon", "shape_cloud", "shape_banner")
foreach ($s in $shapes) { Create-Vector $s $rectPath "#FF9C27B0" }

Write-Host "Assets generated."
