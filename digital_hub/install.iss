[Setup]
AppName=digital_hub
AppVersion=0.1
DefaultDirName={pf}\digital_hub
DefaultGroupName=digital_hub
OutputDir=.
OutputBaseFilename=setup

[Files]
Source: "D:\FreeForge-Digital-Engine\components\digital_hub\build\Desktop_Qt_6_7_2_MSVC2019_64bit-RelWithDebInfo\digital_hub.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "D:\FreeForge-Digital-Engine\components\digital_hub\build\Desktop_Qt_6_7_2_MSVC2019_64bit-RelWithDebInfo\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\digital_hub"; Filename: "{app}\digital_hub.exe"