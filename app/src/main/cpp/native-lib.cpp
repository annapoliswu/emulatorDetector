#include <jni.h>
#include <string>


/*
https://gist.github.com/okamayana/79c98545eb99c4877979

     Without extern "C", your native functions' signatures will not match their declarations in Java (at runtime).
     Long story short, you need this statement for every method if you are writing native C++ and not C.

    Method signature: JNI functions need to be named in the following manner:

    JNIEXPORT <RETURN_TYPE> JNICALL Java_<PACKAGE_NAME>_<JAVA_CLASS>_<METHOD_NAME>(
            JNIEnv *env, jobject obj, <METHOD_PARAMETERS>...) {
        ...
    }
 */

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_emulatordetector_MainActivity_getNativeString(JNIEnv *env, jobject obj) {
    return env->NewStringUTF("Hello World! From native code!\n");
}
