module OpenTracingJFR {
	requires opentracing.api;
	requires opentracing.util;
	requires opentracing.noop;
	requires slf4j.api;
	requires jdk.jfr;

	exports io.opentracing.contrib.jfr;
}
