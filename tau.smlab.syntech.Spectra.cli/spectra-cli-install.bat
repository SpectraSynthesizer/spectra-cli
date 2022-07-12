@echo off
set SPECTRA_PATH=C:\Program Files (x86)\Spectra\
if not exist "%SPECTRA_PATH%" mkdir "%SPECTRA_PATH%"
xcopy lib\cudd.dll "%SPECTRA_PATH%" /R /Y
xcopy lib\spectra-cli.jar "%SPECTRA_PATH%" /R /Y
setx /M PATH "%PATH%;%SPECTRA_PATH%"
doskey spectra=java -jar "%SPECTRA_PATH%spectra-cli.jar" $* 