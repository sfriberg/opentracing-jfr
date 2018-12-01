package io.opentracing.contrib.jfr;

import io.opentracing.Tracer;
import io.opentracing.contrib.jfr.internal.JFRTracerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.nonNull;

public final class JFRTracer {

	private final static Logger LOG = LoggerFactory.getLogger(JFRTracer.class);

	private JFRTracer() {
	}

	public static Tracer wrap(Tracer tracer) {
		try {
			if (nonNull(Class.forName("jdk.jfr.FlightRecorder", false, JFRTracer.class.getClassLoader()))) {
				return new JFRTracerImpl(tracer);
			}
		} catch (ClassNotFoundException ex) {
			LOG.error("Unabled to find FlightRecorder when wrapping tracer", ex);
		}

		return tracer;
	}
}
