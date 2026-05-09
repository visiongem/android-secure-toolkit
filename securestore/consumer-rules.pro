# consumer ProGuard rules for securestore
# 公开 API 通过反射/JNI 不会调用，因此默认混淆即可。
# 若调用方在 KeyStoreType 上做反射，请自行 -keep。
