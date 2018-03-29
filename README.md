# Android ANR详解

标签： Android ANR trace文件

---

###1、ANR定义及分类
ANR：Application Not Responding，应用无响应
触发ANR的必要条件是主线程阻塞。
分为以下3类：
InputDispatcher Timeout：触发时间5s，是ANR的主要类型
Broadcast Timeout：触发时间，前台10s，后台60s
Service Timeout：触发时间20s

###2、ANR分析手段
严苛模式
开发者模式开启“显示所有ANR”
TraceView
traces文件（/data/anr/traces.txt）

###3、常见的ANR类型
主线程慢代码
主线程IO
锁竞争
死锁
BroadcastReceiver慢代码
具体的实例可以参考我的github上[AnrDemo](https://github.com/AndroidDog/ANRDemo)项目。

###4、ANR log信息
以主线程中的一个大量计算的例子说明下log信息，可以直接通过adb logcat获取，log分析如下：
>**//ANR发生的位置、进程、原因**
03-29 16:45:36.255  1191  1511 I InputDispatcher: Application is not responding: Window{e77f56c u0 com.example.sunxt.anrdemo/com.example.sunxt.anrdemo.MainActivity}.  It has been 8013.8ms since event, 8009.2ms since wait started.  Reason: Waiting to send non-key event because the touched window has not finished processing certain input events that were delivered to it over 500.0ms ago. waitqueue length = 6, head.seq = 93817, Wait queue head age: 8595.0ms.
03-29 16:45:36.255  1191  1511 W InputDispatcher: onANRLocked, waitQueue = 93817  93819  93823  93825  93829  93831  
03-29 16:45:36.278  1191  1511 I WindowManager: Input event dispatching timed out sending to com.example.sunxt.anrdemo/com.example.sunxt.anrdemo.MainActivity.  Reason: Waiting to send non-key event because the touched window has not finished processing certain input events that were delivered to it over 500.0ms ago. waitqueue length = 6, head.seq = 93817, Wait queue head age: 8595.0ms.
......
**//SIG=3触发写traces文件**
03-29 16:45:36.412  1191  1212 I Process : Sending signal. PID: 26876 SIG: 3
03-29 16:45:36.414 26876 26882 I zygote64: Thread[3,tid=26882,WaitingInMainSignalCatcherLoop,Thread*=0x76dce5ca00,peer=0x15cc0020,"Signal Catcher"]: reacting to signal 3
03-29 16:45:36.415 26876 26882 I zygote64: 
03-29 16:45:36.494   586   586 I aptouch_daemon: get_roidata_center, touch:1, down:1(338, 548)
03-29 16:45:36.495  1191  1512 D libfingersense_wrapper: Calling fingersense_classify_touch()
03-29 16:45:36.499  1191  1512 D libfingersense_wrapper: fingersense_classify_touch() finished
03-29 16:45:36.509 26876 26882 I zygote64: Wrote stack traces to '/data/anr/traces.txt'
......
**//ANR发生的位置、进程、原因，与前面有点重复**
03-29 16:45:39.528  1191  1212 E ActivityManager: ANR in com.example.sunxt.anrdemo (com.example.sunxt.anrdemo/.MainActivity)
03-29 16:45:39.528  1191  1212 E ActivityManager: PID: 26876
03-29 16:45:39.528  1191  1212 E ActivityManager: Reason: Input dispatching timed out (Waiting to send non-key event because the touched window has not finished processing certain input events that were delivered to it over 500.0ms ago. waitqueue length = 6, head.seq = 93817, Wait queue head age: 8595.0ms.)
**//CPU负载**
03-29 16:45:39.528  1191  1212 E ActivityManager: Load: 46.01 / 45.89 / 45.87
**//打印ANR发生前时间段内各个进程CPU的使用情况（这里删除大部分，只说明情形）**
03-29 16:45:39.528  1191  1212 E ActivityManager: CPU usage from 39337ms to -1ms ago (2018-03-29 16:44:56.982 to 2018-03-29 16:45:36.320):
03-29 16:45:39.528  1191  1212 E ActivityManager:   27% 1191/system_server: 20% user + 7% kernel / faults: 338445 minor 52 major
03-29 16:45:39.528  1191  1212 E ActivityManager:   3.7% 586/aptouch_daemon: 2.9% user + 0.8% kernel
03-29 16:45:39.528  1191  1212 E ActivityManager:   0.1% 26148/adbd: 0% user + 0.1% kernel / faults: 7661 minor
03-29 16:45:39.528  1191  1212 E ActivityManager:   2.1% 23886/com.qihoo.appstore: 1.3% user + 0.7% kernel / faults: 14370 minor 4251 major
......
**//打印ANR发生后时间段内各个进程CPU的使用情况（这里删除大部分，只说明情形）**
03-29 16:45:39.528  1191  1212 E ActivityManager: CPU usage from 383ms to 902ms later (2018-03-29 16:45:36.702 to 2018-03-29 16:45:37.221):
03-29 16:45:39.528  1191  1212 E ActivityManager:   100% 26876/com.example.sunxt.anrdemo: 100% user + 0% kernel
03-29 16:45:39.528  1191  1212 E ActivityManager:     98% 26876/e.sunxt.anrdemo: 98% user + 0% kernel
......

从log可以看出ANR的类型，CPU的使用情况，如果CPU使用量接近100%，说明当前设备很忙，有可能是主线程做了耗时操作，如大量计算等导致了ANR；如果CPU使用量很少，说明主线程被BLOCK了，可能是锁竞争或者死锁等；如果IOwait很高，说明ANR有可能是主线程在进行I/O操作造成的。

###4、traces文件的结构
traces文件的结构一般如下，以BroadcastReceiver中的耗时代码为例：
>**//显示ANR发生的进程ID、时间和包名**
----- pid 9472 at 2018-03-29 15:32:14 -----
Cmd line: com.example.sunxt.anrdemo
**//一些GC等Object信息，通常可以忽略**
......
**//ANR方法线程堆栈信息，需要重点关注主线程，包括线程状态等**
DALVIK THREADS (23):
"main" prio=5 tid=1 Suspended
  | group="main" sCount=1 dsCount=0 obj=0x75a0ffb8 self=0x7f890b8800
  | sysTid=9472 nice=0 cgrp=default sched=0/0 handle=0x7f8d239e58
  | state=S schedstat=( 9761228968 135313338 1792 ) utm=972 stm=4 core=0 HZ=100
  | stack=0x7fe3be1000-0x7fe3be3000 stackSize=8MB
  | held mutexes=
  kernel: __switch_to+0x74/0x8c
  kernel: futex_wait_queue_me+0xcc/0x158
  kernel: futex_wait+0x120/0x20c
  kernel: do_futex+0x184/0xa48
  kernel: SyS_futex+0x88/0x19c
  kernel: cpu_switch_to+0x48/0x2f0
  native: #00 pc 00019d14  /system/lib64/libc.so (syscall+28)
  native: #01 pc 000d27dc  /system/lib64/libart.so (_ZN3art17ConditionVariable4WaitEPNS_6ThreadE+140)
  native: #02 pc 0030cbc0  /system/lib64/libart.so (_ZN3art6Thread16FullSuspendCheckEv+1516)
  native: #03 pc 0039a108  /system/lib64/libart.so (_ZN3artL12GoToRunnableEPNS_6ThreadE+608)
  native: #04 pc 000a7270  /system/lib64/libart.so (_ZN3art12JniMethodEndEjPNS_6ThreadE+24)
  native: #05 pc 00dac734  /system/framework/arm64/boot.oat (Java_android_graphics_Matrix_native_1postRotate__JFFF+184)
  **//真正导致ANR的问题点，一看就知道是在发生在onReceive回调中，具体问题结合代码再查找**
  at android.graphics.Matrix.native_postRotate!(Native method)
  at android.graphics.Matrix.postRotate(Matrix.java:484)
  at com.example.sunxt.anrdemo.MainActivity.slowCode(MainActivity.java:156)
  at com.example.sunxt.anrdemo.MainActivity.access\$000(MainActivity.java:26)
  at com.example.sunxt.anrdemo.MainActivity\$NetworkReceiver.onReceive(MainActivity.java:199)
  at android.app.LoadedApk$ReceiverDispatcher\$Args.run(LoadedApk.java:908)
  at android.os.Handler.handleCallback(Handler.java:815)
  at android.os.Handler.dispatchMessage(Handler.java:104)
  at android.os.Looper.loop(Looper.java:194)
  at android.app.ActivityThread.main(ActivityThread.java:5714)
  at java.lang.reflect.Method.invoke!(Native method)
  at java.lang.reflect.Method.invoke(Method.java:372)
  at com.android.internal.os.ZygoteInit\$MethodAndArgsCaller.run(ZygoteInit.java:984)
  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:779)
  ......
  **//省略不常关注的堆栈打印，主要其他的线程信息**
  ......
  
###6、如何避免和解决ANR
- 可以从以下几方面来避免ANR
1.UI线程尽量只做跟UI相关的工作;
2.耗时的工作（比如数据库操作，I/O，连接网络或者别的有可能阻碍UI线程的操作）把它放入单独的线程处理;
3.尽量用Handler来处理UI thread和别的thread之间的交互;
4.实在绕不开主线程，可以尝试通过Handler延迟加载;
5.广播中如果有耗时操作，建议放在IntentService中去执行，或者通过goAsync() + HandlerThread分发执行。

- 分析ANR的重点
1.cpu占用率方面
可以通过分析各进程的CPU时间占用率，来判断是否为某些进程长期占用CPU导致该进程无法获取到足够的CPU处理时间，而导致ANR重点关注下CPU的负载，即logcat中Load1(Load: 0.42 / 0.27 / 0.25，即cpu平均负载)，各个进程总的CPU时间占用率，用户CPU时间占用率，核心态CPU时间占用率，以及iowait CPU时间占用率。
2.内存方面
主要看当前应用native和dalvik层内存使用情况
结合系统给每个应用分配的最大内存来分析







