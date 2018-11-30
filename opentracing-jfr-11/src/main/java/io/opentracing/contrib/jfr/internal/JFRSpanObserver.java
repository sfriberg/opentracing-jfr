package io.opentracing.contrib.jfr.internal;

import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.SpanObserver;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;

import java.util.Map;

@Category("OpenTracing")
@Label("OpenTracing JFR Event")
@Description("Open Tracing spans exposed as a JFR event")
public class JFRSpanObserver extends jdk.jfr.Event implements SpanObserver {

	@Label("Trace ID")
	@Description("Trace ID that will be the same for all spans that are part of the same trace")
	private final String traceId;

	@Label("Span ID")
	@Description("Span ID that will be unique for every span")
	private final String spanId;

	@Label("Name")
	@Description("Operation name of the span")
	private String name;

	@Label("Start Thread")
	@Description("Thread starting the span")
	private final Thread startThread;

	@Label("Finish Thread")
	@Description("Thread finishing the span")
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

	public String getTraceId() {
		return traceId;
	}

	public String getSpanId() {
		return spanId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		end();
		commit();
	}
}
