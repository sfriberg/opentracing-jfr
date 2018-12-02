package io.opentracing.contrib.jfr.internal;

import com.oracle.jrockit.jfr.ContentType;
import com.oracle.jrockit.jfr.EventDefinition;
import com.oracle.jrockit.jfr.TimedEvent;
import com.oracle.jrockit.jfr.ValueDefinition;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jfr.JFRTracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

@SuppressWarnings("deprecation")
@EventDefinition(path = "OpenTracing/Scope", name = "Open Tracing Scope", description = "Open Tracing scope exposed as a JFR event", stacktrace = true, thread = true)
public class JFRScope extends TimedEvent implements Scope, TextMap {

	private final static Logger LOG = LoggerFactory.getLogger(JFRTracer.class);

	private final Tracer tracer;
	private final JFRScopeManager manager;
	private final Scope scope;
	private final Span span;
	private final boolean finishSpanOnClose;
	private final Scope parent;

	@ValueDefinition(name = "Trace ID", description = "Trace ID that will be the same for all spans that are part of the same trace", contentType = ContentType.None)
	private String traceId;

	@ValueDefinition(name = "Span ID", description = "Span ID that will be unique for every span")
	private String spanId;

	@ValueDefinition(name = "Parent Span ID", description = "ID of the parent span. Null if root span")
	private String parentSpanId;

	@ValueDefinition(name = "Operation Name", description = "Operation name of the span")
	private String name;

	JFRScope(Tracer tracer, JFRScopeManager manager, Scope scope, String operationName, Span span, boolean finishSpanOnClose) {
		this.tracer = tracer;
		this.scope = scope;
		this.span = span;
		this.name = operationName;
		this.finishSpanOnClose = finishSpanOnClose;
		this.parent = manager.activeScope.get();
		this.manager = manager;
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
	public void close() {
		scope.close();
		if (shouldWrite()) {
			tracer.inject(span.context(), Format.Builtin.TEXT_MAP, this);
			this.end();
			this.commit();
		}
		manager.activeScope.set(parent);
		if (finishSpanOnClose && span instanceof JFRSpan) {
			((JFRSpan) span).finishJFR();
		}
	}

	@Override
	public Span span() {
		return span;
	}
}
