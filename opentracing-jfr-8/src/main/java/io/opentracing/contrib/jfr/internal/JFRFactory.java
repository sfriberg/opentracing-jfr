package io.opentracing.contrib.jfr.internal;

import com.oracle.jrockit.jfr.FlightRecorder;
import com.oracle.jrockit.jfr.InvalidEventDefinitionException;
import com.oracle.jrockit.jfr.InvalidValueException;
import com.oracle.jrockit.jfr.Producer;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

@SuppressWarnings("deprecation")
public final class JFRFactory {

	private static final Logger LOG = LoggerFactory.getLogger(JFRFactory.class);
	static final ExecutorService EXECUTOR = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.DAYS,
			new ArrayBlockingQueue<>(50),
			new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "JFRTracer Span Events" + ThreadLocalRandom.current().nextInt());
			thread.setDaemon(true);
			return thread;
		}
	},
			(r, e) -> {
				LOG.warn("Dropped JFR OpenTracing Span");
			});

	private static volatile Producer producer;

	private JFRFactory() {
	}

	private static boolean init() {
		if (FlightRecorder.isActive()) {
			if (isNull(producer)) {
				synchronized (JFRTracerImpl.class) {
					if (isNull(producer)) {
						try {
							Producer p = new Producer("OpenTracing", "OpenTracing JFR Events", "http://opentracing.io/");
							p.addEvent(JFRSpan.class);
							p.addEvent(JFRScope.class);
							p.register();
							producer = p;
						} catch (URISyntaxException | InvalidValueException | InvalidEventDefinitionException ex) {
							LOG.error("Unable to register JFR producer.", ex);
							return false;
						}
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static Span startJFRSpan(Tracer tracer, Span span, String name) {
		if (init()) {
			JFRSpan event = new JFRSpan(tracer, span, name);
			EXECUTOR.execute(event::begin);
			return event;
		} else {
			return span;
		}
	}

	public static Scope startJFRScope(Tracer tracer, JFRScopeManager manager, Scope scope, String operationName, Span span, boolean finishSpanOnClose) {
		if (init()) {
			JFRScope jfrScope = new JFRScope(tracer, manager, scope, operationName, span, finishSpanOnClose);
			jfrScope.begin();
			return jfrScope;
		} else {
			return scope;
		}
	}
}
