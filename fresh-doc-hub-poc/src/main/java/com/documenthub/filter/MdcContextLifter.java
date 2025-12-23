package com.documenthub.filter;

import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subscriber wrapper that copies Reactor Context to MDC for proper logging context propagation.
 * This ensures MDC values are available in all reactive operators.
 */
public class MdcContextLifter<T> implements CoreSubscriber<T> {

    private final CoreSubscriber<T> delegate;

    public MdcContextLifter(CoreSubscriber<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        copyContextToMdc();
        delegate.onSubscribe(subscription);
    }

    @Override
    public void onNext(T t) {
        copyContextToMdc();
        delegate.onNext(t);
    }

    @Override
    public void onError(Throwable throwable) {
        copyContextToMdc();
        delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
        copyContextToMdc();
        delegate.onComplete();
    }

    @Override
    public Context currentContext() {
        return delegate.currentContext();
    }

    private void copyContextToMdc() {
        Context context = delegate.currentContext();
        if (!context.isEmpty()) {
            Map<String, String> mdcContext = context.stream()
                    .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
                    .collect(Collectors.toMap(
                            entry -> (String) entry.getKey(),
                            entry -> (String) entry.getValue()
                    ));
            MDC.setContextMap(mdcContext);
        }
    }
}
