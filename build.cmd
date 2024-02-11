cl.exe /c /I src src\cJSON.c src\phigros.c src\score.cpp
lib.exe /OUT:phigros.lib src\zip.lib cJSON.obj phigros.obj score.obj
del cJSON.obj phigros.obj score.obj
