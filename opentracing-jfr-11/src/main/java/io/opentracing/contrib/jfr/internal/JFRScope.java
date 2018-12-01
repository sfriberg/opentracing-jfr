package io.opentracing.contrib.jfr.internal;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jfr.JFRTracer;
import io.opentracing.contrib.jfr.internal.JFRScopeManager;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

@Category("OpenTracing")
@Label("OpenTracing JFR Event")
@Description("Open Tracing spans exposed as a JFR event")
public class JFRScope extends jdk.jfr.Event implements Scope, TextMap {

	private final static Logger LOG = LoggerFactory.getLogger(JFRTracer.class);

	private final Tracer tracer;
	private final JFRScopeManager manager;
	private final Scope scope;
	private final JFRSpan jfrSpan;
	private final boolean finishSpanOnClose;
	private final JFRScope parent;

	@Label("Trace ID")
	@Description("Trace ID that will be the same for all spans that are part of the same trace")
	private String traceId;

	@Label("Span ID")
	@Description("Span ID that will be unique for every span")
	private String spanId;

	@Label("Parent Span ID")
	@Description("ID of the parent span. Null if root span")
	private String parentSpanId;

	@Label("Name")
	@Description("Operation name of the span")
	private String name;

	public JFRScope(Tracer tracer, JFRScopeManager manager, Scope scope, JFRSpan jfrSpan, boolean finishSpanOnClose) {
		this.tracer = tracer;
		this.scope = scope;
		this.jfrSpan = jfrSpan;
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
		finishJFR();
		manager.activeScope.set(parent);
		if (finishSpanOnClose) {
			jfrSpan.finishJFR();
		}
	}

	@Override
	public Span span() {
		return jfrSpan;
	}

	private void finishJFR() {
		if (shouldCommit()) {
			this.name = jfrSpan.getName();
			tracer.inject(jfrSpan.context(), Format.Builtin.TEXT_MAP, this);
			end();
			commit();
		}
	}
}
