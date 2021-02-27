#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>
#include <ucontext.h>
#include <jni.h>


/*
void AlignTrapHandler( int signum , siginfo_t *info , void * sc) {
    printf ("hardware! \ n");
    exit(0);
}

int main( void) {
// setup align trap handler
    struct sigaction act;
    memset(&act , 0 , sizeof (act));
    act.sa_sigaction = AlignTrapHandler;
    act.sa_flags = SA_SIGINFO;
    sigaction (SIGSEGV, &act , NULL) ;

    // trigger unaligned vectorization
    asm ("mov %rsp , %rax \n"
         "inc %rax \n"
         "movntps %xmm0,(% rax )") ;

    printf("emulator! \ n") ;
    return 0;
}

JNIEXPORT <RETURN_TYPE> JNICALL Java_<PACKAGE_NAME>_<JAVA_CLASS>_<METHOD_NAME>(
        JNIEnv *env, jobject obj, <METHOD_PARAMETERS>...) {
...
}
 */
