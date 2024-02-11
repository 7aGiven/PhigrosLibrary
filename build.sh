g++ -c -fPIC src/score.cpp
gcc -c -fPIC -I src src/cJSON.c src/phigros.c
cp src/libzip.a phigros.a
ar r phigros.a cJSON.o phigros.o score.o
rm cJSON.o phigros.o score.o
