cd %0\..
if exist upgrade-lib\nul goto ugl
goto nugl
:ugl
xcopy /y /i lib old-lib
xcopy /y /i upgrade-lib lib
del /q upgrade-lib
rmdir /q upgrade-lib
:nugl
if exist upgrade-jarnal.jar goto uj
goto nuj
:uj
xcopy /y jarnal.jar old-jarnal.jar
xcopy /y upgrade-jarnal.jar jarnal.jar
del /q upgrade-jarnal.jar
:nuj
start javaw -Dfile.encoding=UTF-8 -Xmx256m -jar jarnal.jar -t templates/annotate.jaj -b %1 %2 %3 %4 %5
