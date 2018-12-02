package io.opentracing.contrib.jfr.internal;

import com.oracle.jrockit.jfr.ContentType;
import com.oracle.jrockit.jfr.EventDefinition;
import com.oracle.jrockit.jfr.TimedEvent;
import com.oracle.jrockit.jfr.ValueDefinition;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static io.opentracing.contrib.jfr.internal.JFRFactory.EXECUTOR;

@SuppressWarnings("deprecation")
@EventDefinition(path = "OpenTracing/Span", name = "Open Tracing Span", description = "Open Tracing spans exposed as a JFR event", stacktrace = true, thread = true)
public class JFRSpan extends TimedEvent implements Span, TextMap {

	private static final Logger LOG = LoggerFactory.getLogger(JFRSpan.class);

	private final Tracer tracer;
	private final Span span;

	@ValueDefinition(name = "Trace ID", description = "Trace ID that will be the same for all spans that are part of the same trace", contentType = ContentType.None)
	private String traceId;

	@ValueDefinition(name = "Span ID", description = "Span ID that will be unique for every span")
	private String spanId;

	@ValueDefinition(name = "Parent Span ID", description = "ID of the parent span. Null if root span")
	private String parentSpanId;

	@ValueDefinition(name = "Operation Name", description = "Operation name of the span")
	private String name;

	@ValueDefinition(name = "Start Thread", description = "Thread starting the span")
	private final Thread startThread;

	@ValueDefinition(name = "Finish Thread", description = "Thread finishing the span")
	private Thread finishThread;

	/**
	 * Create a new Span Event
	 */
	JFRSpan(Tracer tracer, Span span, String name) {
		this.name = name;
		this.startThread = Thread.currentThread();
		this.tracer = tracer;
		this.span = span;
	}

	public String getTraceId() {
		return traceId;
	}

	public String getSpanId() {
		return spanId;
	}

	public String getParentSpanId() {
		return parentSpanId;
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

	public Span unwrap() {
		return span;
	}

	@Override
	public Iterator<Entry<String, String>> iterator() {
		return Collections.emptyIterator();
	}

	/**
	 * Supports injection with MockTracer, uber-trace-id, and B3 headers
	 *
	 * @param key
	 * @param value
	 */
	@Override
	public void put(String key, String value) {
		switch (key) {
			case "X-B3-TraceId":
			case "traceid":
				this.traceId = value;
				break;
			case "X-B3-SpanId":
			case "spanid":
				this.spanId = value;
				break;
			case "X-B3-ParentSpanId":
				this.parentSpanId = value;
				break;
			case "X-B3-Sampled":
				break;
			case "uber-trace-id":
				String[] values = value.split(":");
				this.traceId = values[0];
				this.spanId = values[1];
				this.parentSpanId = values[2].equals("0") ? null : values[2];
				break;
			default:
				LOG.warn("Unsupported injection key: " + key);
		}
	}

	@Override
	public SpanContext context() {
		return span.context();
	}

	@Override
	public Span setTag(String key, String value) {
		return span.setTag(key, value);
	}

	@Override
	public Span setTag(String key, boolean value) {
		return span.setTag(key, value);
	}

	@Override
	public Span setTag(String key, Number value) {
		return span.setTag(key, value);
	}

	@Override
	public Span log(Map<String, ?> fields) {
		return span.log(fields);
	}

	@Override
	public Span log(long timestampMicroseconds, Map<String, ?> fields) {
		return span.log(timestampMicroseconds, fields);
	}

	@Override
	public Span log(String event) {
		return span.log(event);
	}

	@Override
	public Span log(long timestampMicroseconds, String event) {
		return span.log(timestampMicroseconds, event);
	}

	@Override
	public Span setBaggageItem(String key, String value) {
		return span.setBaggageItem(key, value);
	}

	@Override
	public String getBaggageItem(String key) {
		return span.getBaggageItem(key);
	}

	@Override
	public Span setOperationName(String operationName) {
		this.name = operationName;
		return span.setOperationName(operationName);
	}

	@Override
	public void finish() {
		finishJFR();
		span.finish();
	}

	@Override
	public void finish(long finishMicros) {
		finishJFR();
		span.finish();
	}

	void finishJFR() {
		EXECUTOR.execute(() -> {
			if (shouldWrite()) {
				tracer.inject(span.context(), Format.Builtin.TEXT_MAP, this);
				this.finishThread = Thread.currentThread();
				this.end();
				this.commit();
			}
		});
	}
}
