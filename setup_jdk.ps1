$Url = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_windows_hotspot_17.0.13_11.zip"
$ZipFile = "jdk.zip"
$DestDir = "jdk_extract"

Write-Host "Downloading Portable JDK 17..."
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri $Url -OutFile $ZipFile

Write-Host "Extracting..."
Expand-Archive -Path $ZipFile -DestinationPath $DestDir -Force

$ExtractedFolder = Get-ChildItem -Path $DestDir | Select-Object -First 1
Move-Item -Path "$($ExtractedFolder.FullName)" -Destination "jdk" -Force

Remove-Item $ZipFile -Force
Remove-Item $DestDir -Recurse -Force

Write-Host "JDK Setup Complete. Location: $(Get-Item jdk | Select-Object -ExpandProperty FullName)"
