package io.opentracing.contrib.jfr.internal;

import com.oracle.jrockit.jfr.ContentType;
import com.oracle.jrockit.jfr.EventDefinition;
import com.oracle.jrockit.jfr.TimedEvent;
import com.oracle.jrockit.jfr.ValueDefinition;
import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.SpanObserver;
import io.opentracing.contrib.jfr.OpenTracingJFR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
@EventDefinition(path = "OpenTracing/Span", name = "Open Tracing Span", description = "Open Tracing spans exposed as a JFR event", stacktrace = true, thread = true)
public class JFRSpanObserver extends TimedEvent implements SpanObserver {

	private final static Logger LOG = LoggerFactory.getLogger(OpenTracingJFR.class);

	private final static ExecutorService EXECUTOR = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.DAYS, new ArrayBlockingQueue<>(50), new RejectedExecutionHandler() {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			LOG.warn("Dropped JFR OpenTracing Span");
		}
	});

	@ValueDefinition(name = "Trace ID", description = "Trace ID that will be the same for all spans that are part of the same trace", contentType = ContentType.None)
	private final String traceId;

	@ValueDefinition(name = "Span ID", description = "Span ID that will be unique for every span")
	private final String spanId;

	@ValueDefinition(name = "Name", description = "Operation name of the span")
	private String name;

	@ValueDefinition(name = "Start Thread", description = "Thread starting the span")
	private final Thread startThread;

	@ValueDefinition(name = "Finish Thread", description = "Thread finishing the span")
	private Thread finishThread;

	/**
	 * Create a new Span Event
	 *
	 * @param traceId trace id for the current span
	 * @param spanId span id for the current span
	 * @param name operation name of the current span
	 */
	JFRSpanObserver(String traceId, String spanId, String name) {
		this.traceId = traceId;
		this.spanId = spanId;
		this.name = name;
		this.startThread = Thread.currentThread();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTraceId() {
		return traceId;
	}

	public String getSpanId() {
		return spanId;
	}

	public String getName() {
		return name;
	}

	public Thread getStartThread() {
		return startThread;
	}

	public Thread getFinishThread() {
		return finishThread;
	}

	@Override
	public void onSetOperationName(SpanData sd, String string) {
		setName(string);
	}

	@Override
	public void onSetTag(SpanData sd, String string, Object o) {
	}

	@Override
	public void onSetBaggageItem(SpanData sd, String string, String string1) {
	}

	@Override
	public void onLog(SpanData sd, long l, Map<String, ?> map) {
	}

	@Override
	public void onLog(SpanData sd, long l, String string) {
	}

	@Override
	public void onFinish(SpanData sd, long l) {
		this.finishThread = Thread.currentThread();
		EXECUTOR.execute(() -> {
			this.end();
			this.commit();
		});
	}

	public void start() {
		EXECUTOR.execute(this::begin);
	}
}
