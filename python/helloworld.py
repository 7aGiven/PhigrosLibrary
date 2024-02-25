import ctypes

ssl = ctypes.CDLL("libssl.so.3", ctypes.RTLD_GLOBAL)
print(ssl)
ctypes.CDLL("../build/libphigros.so")
