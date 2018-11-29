package io.opentracing.contrib.jfr;

import io.opentracing.Tracer;
import io.opentracing.contrib.api.TracerObserver;
import io.opentracing.contrib.api.tracer.APIExtensionsTracer;
import io.opentracing.contrib.jfr.internal.JFRTracerObserver;
import io.opentracing.contrib.jfr.internal.NoopTracerObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.nonNull;

public final class OpenTracingJFR {

	private final static Logger LOG = LoggerFactory.getLogger(OpenTracingJFR.class);

	private OpenTracingJFR() {
	}

	public static Tracer decorate(Tracer tracer) {
		var apiExtensionsTracer = new APIExtensionsTracer(tracer);
		apiExtensionsTracer.addTracerObserver(createJFRTracerObserver());
		return apiExtensionsTracer;
	}

	private static TracerObserver createJFRTracerObserver() {
		try {
			if (nonNull(Class.forName("jdk.jfr.FlightRecorder", false, TracerObserver.class.getClassLoader()))) {
				return new JFRTracerObserver();
			}
		} catch (ClassNotFoundException ex) {
			LOG.error("Unabled to find FlightRecorder when registering tracer observer", ex);
		}

		return NoopTracerObserver.INSTANCE;
	}
}
