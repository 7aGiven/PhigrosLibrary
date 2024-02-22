from setuptools import setup, Extension
setup(name="phigros_module",version="0.0.1",ext_modules=[Extension("phigros",
    ["../src/cJSON.c", "../src/phigros.c", "../src/score.cpp", "main.c"],
    include_dirs=["../src/"],
    library_dirs=["../src/", "/home/given/.local/usr/lib/x86_64-linux-gnu/"],
    libraries=["ssl", "crypto", "zip"]
)])