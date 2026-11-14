Target amd64-unicode
ManifestDPIAware true
!define APP "Liquid"
!define VER "1.0.0"
!include "MUI2.nsh"
!include "x64.nsh"
!include "LogicLib.nsh"

Name "${APP}"
Caption "Liquid ${VER} — Instalator"
OutFile "..\dist\Liquid_Setup_${VER}.exe"
InstallDir "$PROGRAMFILES64\${APP}"
InstallDirRegKey HKLM "Software\${APP}" "InstallDir"
RequestExecutionLevel admin
SetCompressor /SOLID lzma
BrandingText "Liquid ${VER}"
ShowInstDetails show

VIProductVersion "1.0.0.0"
VIAddVersionKey /LANG=1045 "ProductName" "Liquid"
VIAddVersionKey /LANG=1045 "FileDescription" "Instalator Liquid"
VIAddVersionKey /LANG=1045 "FileVersion" "${VER}"
VIAddVersionKey /LANG=1045 "ProductVersion" "${VER}"
VIAddVersionKey /LANG=1045 "CompanyName" "Liquid"
VIAddVersionKey /LANG=1045 "LegalCopyright" "Liquid 2026"

!define MUI_ICON "..\assets\liquid.ico"
!define MUI_UNICON "..\assets\liquid.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "wizard.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "wizard.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP "header.bmp"
!define MUI_HEADERIMAGE_UNBITMAP "header.bmp"
!define MUI_ABORTWARNING
!define MUI_COMPONENTSPAGE_SMALLDESC

!define MUI_WELCOMEPAGE_TITLE "Witaj w Liquid"
!define MUI_WELCOMEPAGE_TEXT "Liquid to Twoje centrum dowodzenia komputerem:$\r$\n$\r$\n•  Parametry komputera + werdykt, czy setup jest dobry$\r$\n•  Stopnie urządzeń — temperatury CPU, GPU, dysków na żywo$\r$\n•  Licznik FPS do wybranej gry z overlayem i benchmarkiem$\r$\n$\r$\nKliknij Dalej, aby kontynuować."
!define MUI_FINISHPAGE_TITLE "Liquid jest gotowy!"
!define MUI_FINISHPAGE_TEXT "Instalacja zakończyła się pomyślnie.$\r$\n$\r$\nSkrót: Ctrl + Shift + F10 włącza overlay FPS w grze."
!define MUI_FINISHPAGE_RUN "$INSTDIR\Liquid.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Uruchom Liquid teraz"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

!insertmacro MUI_LANGUAGE "Polish"

Function .onInit
  ${IfNot} ${RunningX64}
    MessageBox MB_ICONSTOP "Liquid wymaga 64-bitowego systemu Windows 10 lub 11."
    Abort
  ${EndIf}
  SetRegView 64
FunctionEnd

Function un.onInit
  SetRegView 64
FunctionEnd

Section "Liquid (wymagane)" SecMain
  SectionIn RO
  SetShellVarContext all
  DetailPrint "Zamykam uruchomiony Liquid..."
  nsExec::Exec 'taskkill /F /IM Liquid.exe'
  Sleep 500
  SetOutPath "$INSTDIR"
  File "..\dist\app\Liquid.exe"
  File "..\assets\liquid.ico"
  SetOutPath "$INSTDIR\drivers"
  File "PawnIO_setup.exe"
  SetOutPath "$INSTDIR"

  WriteUninstaller "$INSTDIR\Odinstaluj.exe"
  CreateDirectory "$SMPROGRAMS\Liquid"
  CreateShortcut "$SMPROGRAMS\Liquid\Liquid.lnk" "$INSTDIR\Liquid.exe" "" "$INSTDIR\liquid.ico"
  CreateShortcut "$SMPROGRAMS\Liquid\Odinstaluj Liquid.lnk" "$INSTDIR\Odinstaluj.exe"

  WriteRegStr HKLM "Software\${APP}" "InstallDir" "$INSTDIR"
  !define UNKEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\Liquid"
  WriteRegStr HKLM "${UNKEY}" "DisplayName" "Liquid"
  WriteRegStr HKLM "${UNKEY}" "DisplayVersion" "${VER}"
  WriteRegStr HKLM "${UNKEY}" "Publisher" "Liquid"
  WriteRegStr HKLM "${UNKEY}" "DisplayIcon" "$INSTDIR\liquid.ico"
  WriteRegStr HKLM "${UNKEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "${UNKEY}" "UninstallString" '"$INSTDIR\Odinstaluj.exe"'
  WriteRegDWORD HKLM "${UNKEY}" "NoModify" 1
  WriteRegDWORD HKLM "${UNKEY}" "NoRepair" 1
  WriteRegDWORD HKLM "${UNKEY}" "EstimatedSize" 160000
SectionEnd

Section "Skrót na pulpicie" SecDesktop
  SetShellVarContext all
  CreateShortcut "$DESKTOP\Liquid.lnk" "$INSTDIR\Liquid.exe" "" "$INSTDIR\liquid.ico"
SectionEnd

Section "Sterownik PawnIO (temperatury CPU)" SecPawn
  DetailPrint "Instaluję sterownik PawnIO (odczyt czujników procesora)..."
  nsExec::ExecToLog '"$INSTDIR\drivers\PawnIO_setup.exe" -install -silent'
  Pop $0
  ${If} $0 != 0
    DetailPrint "PawnIO: kod wyjścia $0 (możesz zainstalować go później z poziomu aplikacji)."
  ${EndIf}
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecMain} "Aplikacja Liquid: parametry komputera, temperatury, licznik FPS."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop} "Dodaje ikonę Liquid na pulpicie."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecPawn} "Darmowy, podpisany sterownik potrzebny do odczytu temperatury procesora. Zalecane."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Section "Uninstall"
  SetShellVarContext all
  nsExec::Exec 'taskkill /F /IM Liquid.exe'
  nsExec::Exec 'schtasks /Delete /F /TN "Liquid"'
  Sleep 500
  Delete "$INSTDIR\Liquid.exe"
  Delete "$INSTDIR\liquid.ico"
  Delete "$INSTDIR\drivers\PawnIO_setup.exe"
  RMDir "$INSTDIR\drivers"
  Delete "$INSTDIR\Odinstaluj.exe"
  RMDir "$INSTDIR"
  Delete "$DESKTOP\Liquid.lnk"
  Delete "$SMPROGRAMS\Liquid\Liquid.lnk"
  Delete "$SMPROGRAMS\Liquid\Odinstaluj Liquid.lnk"
  RMDir "$SMPROGRAMS\Liquid"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Liquid"
  DeleteRegKey HKLM "Software\${APP}"
  SetShellVarContext current
  MessageBox MB_YESNO|MB_ICONQUESTION "Usunąć także ustawienia Liquid?" IDNO done
    RMDir /r "$APPDATA\Liquid"
  done:
SectionEnd
