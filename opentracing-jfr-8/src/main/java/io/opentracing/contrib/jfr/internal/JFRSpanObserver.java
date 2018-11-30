package io.opentracing.contrib.jfr.internal;

import com.oracle.jrockit.jfr.ContentType;
import com.oracle.jrockit.jfr.EventDefinition;
import com.oracle.jrockit.jfr.TimedEvent;
import com.oracle.jrockit.jfr.ValueDefinition;
import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.SpanObserver;

import java.util.Map;

@SuppressWarnings("deprecation")
@EventDefinition(path = "OpenTracing/Span", name = "Open Tracing Span", description = "Open Tracing spans exposed as a JFR event", stacktrace = true, thread = true)
public class JFRSpanObserver extends TimedEvent implements SpanObserver {

	@ValueDefinition(name = "Trace ID", description = "Trace ID that will be the same for all spans that are part of the same trace", contentType = ContentType.None)
	private final String traceId;

	@ValueDefinition(name = "Span ID", description = "Span ID that will be unique for every span", contentType = ContentType.None)
	private final String spanId;

	@ValueDefinition(name = "Name", description = "Operation name of the span", contentType = ContentType.None)
	private String name;

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
		end();
		commit();
	}
}
