{
	"target_defaults": {
		"sources": ["main.c"]
	},
	"conditions": [
		['OS=="linux"', {"targets": [{
			"target_name": "phigros_linux",
			"libraries": ["../../phigros.a"]
		}]}],
        ['OS=="win"', {"targets": [{
			"target_name": "phigros_win",
			"libraries": ["../../phigros.lib"]
		}]}]
	]
}
