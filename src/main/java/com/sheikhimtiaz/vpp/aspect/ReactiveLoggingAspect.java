package com.sheikhimtiaz.vpp.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * Aspect to log method calls in reactive components
 */
@Aspect
@Component
@Slf4j
public class ReactiveLoggingAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)" +
            " || within(@org.springframework.stereotype.Service *)" +
            " || within(@org.springframework.stereotype.Repository *)")
    public void springBeanPointcut() {

    }

    @Pointcut("execution(reactor.core.publisher.* com.sheikhimtiaz.vpp..*.*(..))")
    public void reactiveMethodPointcut() {

    }

    @Around("springBeanPointcut() && reactiveMethodPointcut()")
    public Object logAroundReactive(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." +
                joinPoint.getSignature().getName();

        if (log.isDebugEnabled()) {
            log.debug("Enter: {} with arguments: {}", methodName, Arrays.toString(joinPoint.getArgs()));
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Object result = joinPoint.proceed();

        if (result instanceof Mono) {
            return ((Mono<?>) result).doOnSuccess(value -> {
                stopWatch.stop();
                log.debug("Exit: {} with result type: {}, execution time: {} ms",
                        methodName,
                        value != null ? value.getClass().getSimpleName() : "null",
                        stopWatch.getTotalTimeMillis());
            }).doOnError(error -> {
                stopWatch.stop();
                log.error("Error in {}: {} - execution time: {} ms",
                        methodName,
                        error.getMessage(),
                        stopWatch.getTotalTimeMillis(),
                        error);
            });
        } else if (result instanceof Flux) {
            return ((Flux<?>) result).doOnComplete(() -> {
                stopWatch.stop();
                log.debug("Completed: {} - execution time: {} ms",
                        methodName,
                        stopWatch.getTotalTimeMillis());
            }).doOnError(error -> {
                stopWatch.stop();
                log.error("Error in {}: {} - execution time: {} ms",
                        methodName,
                        error.getMessage(),
                        stopWatch.getTotalTimeMillis(),
                        error);
            });
        } else if (result instanceof Publisher) {
            log.debug("Method {} returned a Publisher that is not Mono or Flux", methodName);
            return result;
        } else {
            stopWatch.stop();
            log.debug("Exit: {} with non-reactive result - execution time: {} ms",
                    methodName,
                    stopWatch.getTotalTimeMillis());
            return result;
        }
    }
}