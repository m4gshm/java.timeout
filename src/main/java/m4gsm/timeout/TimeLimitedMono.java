package m4gsm.timeout;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import m4gsm.timeout.TimeLimitExecutor.Context;
import m4gsm.timeout.TimeLimitExecutor.ContextImpl;
import m4gsm.timeout.TimeLimitExecutor.DeadlineExceedFunction;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
public class TimeLimitedMono<T> extends Mono<T> {
    public static final Runnable DO_NOTHING = () -> {
    };

    Instant deadline;
    Clock<Instant> clock;
    TimeoutsFormula timeoutsFormula;
    Mono<T> mono;

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        var context = actual.currentContext();
        Instant dl = null;
        if (deadline != null) {
            context = context.put(DeadlineHolder.class, new DeadlineHolder(deadline));
            dl = deadline;
        } else if (!context.hasKey(DeadlineHolder.class)) {
            val threadDeadline = TimeLimitExecutorImpl.getThreadDeadline();
            if (threadDeadline != null) {
                context = context.put(DeadlineHolder.class, new DeadlineHolder(threadDeadline));
                dl = threadDeadline;
            }
        }
        mono.subscribe(new TimeLimitedSubscriber<T>(context, dl, actual));
    }

    @RequiredArgsConstructor
    static class DeadlineHolder {
        final Instant deadline;
    }

    class TimeLimitedSubscriber<T> implements CoreSubscriber<T>, Scannable {
        final reactor.util.context.Context context;
        final Instant deadline;
        final CoreSubscriber<? super T> actual;
        final DeadlineExceedFunction<T> CALL_ON_ERROR = (c, d) -> {
            onError(DeadlineExceededException.newDeadlineExceededException(c, d));
            return null;
        };
        final Context<T> dlContext;
        volatile Subscription parent;

        TimeLimitedSubscriber(reactor.util.context.Context context, Instant deadline, CoreSubscriber<? super T> actual) {
            this.context = context;
            this.deadline = deadline;
            this.actual = actual;
            dlContext = new ContextImpl<>(deadline, clock, timeoutsFormula, DO_NOTHING, CALL_ON_ERROR);
        }

        @Override
        public reactor.util.context.Context currentContext() {
            return context;
        }

        private void localDL(Runnable r) {
            val owner = TimeLimitExecutorImpl.setThreadDeadline(deadline);
            try {
                r.run();
            } finally {
                if (owner) TimeLimitExecutorImpl.clearDeadline();
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            parent = s;
            localDL(() -> dlContext.run(() -> actual.onSubscribe(s)));
        }


        @Override
        public void onNext(T t) {
            localDL(() -> actual.onNext(t));
        }

        @Override
        public void onError(Throwable t) {
            localDL(() -> actual.onError(t));
        }

        @Override
        public void onComplete() {
            localDL(actual::onComplete);
        }

        @Override
        public Object scanUnsafe(Attr key) {
            if (key == Attr.ACTUAL) return this.actual;
            if (key == Attr.PARENT) return this.parent;
            return null;
        }
    }
}
