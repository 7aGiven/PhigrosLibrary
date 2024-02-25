cmake -DZLIB_LIBRARY=%~dp0..\zlib.lib -DZLIB_INCLUDE_DIR=%~dp0..\ -B build
cmake --build build --config Release
