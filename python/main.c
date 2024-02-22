#define PY_SSIZE_T_CLEAN
#include <Python.h>
#include "../phigros.h"

PyObject* loads;
PyObject* dumps;

static PyObject* MethodNickname(PyObject *self, PyObject *args) {
	char *sessionToken, *nickname;
	PyArg_ParseTuple(args, "s", &sessionToken);
	short len = get_nickname(sessionToken, &nickname);
	PyObject* value;
	if (len == 0) {
		PyErr_SetString(PyExc_RuntimeError, nickname);
		value = Py_None;
	} else {
		value = Py_BuildValue("s#", nickname, len);
	}
	free(nickname);
	return value;
}

static PyObject* MethodSummary(PyObject *self, PyObject *args) {
	char* sessionToken;
	PyArg_ParseTuple(args, "s", &sessionToken);
	cJSON* summary = get_summary(sessionToken);
	char* str = cJSON_PrintUnformatted(summary);
	cJSON_Delete(summary);
	PyObject* json = Py_BuildValue("s", str);
	free(str);
	return PyObject_CallOneArg(loads, json);
}

static PyObject* MethodSave(PyObject *self, PyObject *args) {
	char* url;
	PyArg_ParseTuple(args, "s", &url);
	BIO* save_bio = download_save(url);
	cJSON* save = parse_save(save_bio);
	BIO_free(save_bio);
	char* str = cJSON_PrintUnformatted(save);
	cJSON_Delete(save);
	PyObject* json = Py_BuildValue("s", str);
	free(str);
	return PyObject_CallOneArg(loads, json);
}

static PyObject* MethodDifficulty(PyObject *self, PyObject *args) {
	char* path;
	PyArg_ParseTuple(args, "s", &path);
	load_difficulty(path);
	return Py_None;
}

static PyObject* MethodB19(PyObject *self, PyObject *args) {
	PyObject* obj;
	Py_ssize_t len;
	PyArg_ParseTuple(args, "O", &obj);
	char* str = PyUnicode_AsUTF8AndSize(PyObject_CallOneArg(dumps, obj), &len);
	cJSON* gameRecord = cJSON_ParseWithLength(str, len);
	free(str);
	cJSON* b19 = get_b19(gameRecord);
	cJSON_Delete(gameRecord);
	str = cJSON_PrintUnformatted(b19);
	cJSON_Delete(b19);
	PyObject* json = Py_BuildValue("s", str);
	free(str);
	return PyObject_CallOneArg(loads, json);
}

static PyObject* MethodRe8(PyObject *self, PyObject *args) {
	char* sessionToken;
	PyArg_ParseTuple(args, "s", &sessionToken);
	cJSON* summary = get_summary(sessionToken);
	char* url = cJSON_GetObjectItemCaseSensitive(summary, "url")->valuestring;
	BIO* save_bio = download_save(url);
	cJSON* save = parse_save(save_bio);
	BIO_free(save_bio);
	cJSON* gameProgress = cJSON_GetObjectItemCaseSensitive(save, "gameProgress");
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8UnlockBegin"), 0);
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8UnlockSecondPhase"), 0);
	cJSON_SetBoolValue(cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8Passed"), 0);
	cJSON_GetObjectItemCaseSensitive(gameProgress, "chapter8SongUnlocked")->valueint = 0;
	char* save_buf;
	short len = gen_save(&save_buf, save);
	cJSON_Delete(save);
	upload_save(sessionToken, save_buf, len, summary);
	free(save_buf);
	cJSON_Delete(summary);
	return Py_None;
}

static PyMethodDef methods[] = {
	{"get_nickname", MethodNickname, METH_VARARGS},
    {"get_summary", MethodSummary, METH_VARARGS},
    {"get_save", MethodSave, METH_VARARGS},
    {"get_b19", MethodB19, METH_VARARGS},
    {"re8", MethodRe8, METH_VARARGS},
    {0}
};

static PyModuleDef module = {
    PyModuleDef_HEAD_INIT,
    "phigros",
    0,
    -1,
    methods
};

PyMODINIT_FUNC PyInit_phigros() {
    loads = PyImport_AddModule("json.loads");
	dumps = PyImport_AddModule("json.dumps");
    return PyModule_Create(&module);
}
