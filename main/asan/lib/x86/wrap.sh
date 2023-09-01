#!/system/bin/sh
HERE="$(cd "$(dirname "$0")" && pwd)"
export ASAN_OPTIONS=log_to_syslog=false,allow_user_segv_handler=1,detect_stack_use_after_return=1,detect_container_overflow=0,color=never
ASAN_LIB=$(ls $HERE/libclang_rt.asan-*-android.so)
if [ -f "$HERE/libc++_shared.so" ]; then
    # Workaround for https://github.com/android-ndk/ndk/issues/988.
    export LD_PRELOAD="$ASAN_LIB $HERE/libc++_shared.so"
else
    export LD_PRELOAD="$ASAN_LIB"
fi
"$@"
