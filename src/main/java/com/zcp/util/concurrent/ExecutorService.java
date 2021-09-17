package com.zcp.util.concurrent;

import java.util.List;

/**
 * 线程池类接口
 */
public interface ExecutorService {

     void execute(Runnable command) ;

     void shutdown() ;

     List<Runnable> shutdownNow() ;

     boolean isShutdown() ;

     boolean isTerminated() ;

}
