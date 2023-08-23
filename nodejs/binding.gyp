{
	"target_defaults": {
		"sources": ["main.cpp"]
	},
	"conditions": [
		['OS=="linux"', {"targets": [{
			"target_name": "PhigrosLibrary_linux",
			"libraries": ["-lzip"]
		}]}],
        ['OS=="win"', {"targets": [{"target_name": "PhigrosLibrary_win"}]}]
	]
}
