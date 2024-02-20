{
	"target_defaults": {
		"sources": ["main.c", "../src/cJSON.c", "../src/phigros.c", "../src/score.cpp"],
		"include_dirs": ["../src"]
	},
	"conditions": [
		['OS=="linux"', {"targets": [{
			"target_name": "phigros_linux",
			"libraries": ["../../src/libzip.a"]
		}]}],
        ['OS=="win"', {"targets": [{
			"target_name": "phigros_win",
			"libraries": ["../../src/zip.lib"]
		}]}]
	]
}
