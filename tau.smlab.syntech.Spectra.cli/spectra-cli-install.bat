@echo off
set SPECTRA_PATH=C:\Program Files (x86)\Spectra\
if not exist "%SPECTRA_PATH%" mkdir "%SPECTRA_PATH%"
xcopy bin\cudd.dll "%SPECTRA_PATH%" /R /Y
xcopy bin\spectra-cli.jar "%SPECTRA_PATH%" /R /Y
setx /M PATH "%PATH%;%SPECTRA_PATH%"
doskey spectra=java -jar "%SPECTRA_PATH%spectra-cli.jar" $* 