module OpenTracingJFR {
	requires io.opentracing.api;
	requires io.opentracing.util;
	requires io.opentracing.noop;
	requires slf4j.api;
	requires opentracing.api.extensions;
	requires opentracing.api.extensions.tracer;
	requires jdk.jfr;

	exports io.opentracing.contrib.jfr;
}
