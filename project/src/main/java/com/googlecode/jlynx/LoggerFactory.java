package com.googlecode.jlynx;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

final class LoggerFactory {

	static final String jlynx = "jlynx";

	static {

		Logger logger = Logger.getLogger(jlynx);
		if (logger.getHandlers().length == 0) {
			logger.setUseParentHandlers(false);
			logger.addHandler(new ConsoleHandler());
		}
		for (Handler h : logger.getHandlers()) {
			if (!(h.getFormatter() instanceof SimpleLogFormatter))
				try {
					h.setFormatter(SimpleLogFormatter.class.newInstance());
					logger.fine("Logger formatter: "
							+ h.getFormatter().getClass());
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		LogManager.getLogManager().addLogger(logger);
		logger.info("Logging initialized");

	}

	static Logger getLogger() {

		Logger logger = LogManager.getLogManager().getLogger(jlynx);

		logger.setLevel(Level.parse(Config.loggingLevel == null ? "INFO"
				: Config.loggingLevel));

		logger.info("Logging level is set to " + logger.getLevel().getName());

		return logger;
	}

	static class SimpleLogFormatter extends Formatter {

		private static final DateFormat format = new SimpleDateFormat(
				"yyyy-MM-dd hh:mm:ss.SSS a z");
		private static final String lineSep = System
				.getProperty("line.separator");

		/**
		 * A Custom format implementation that is designed for brevity.
		 */
		public String format(LogRecord record) {
			String loggerName = record.getLoggerName();
			if (loggerName == null) {
				loggerName = "root";
			}
			synchronized (this) {
				StringBuilder output = new StringBuilder()
						.append(format.format(new Date(record.getMillis())))
						.append(' ').append(record.getLevel().getName())
						.append(' ').append(Thread.currentThread().getName())
						.append(' ').append(record.getMessage())
						.append(" [")
						// .append(record.getSourceClassName())
						.append(loggerName).append('#')
						.append(record.getSourceMethodName()).append("]")
						.append(lineSep);
				return output.toString();
			}

		}
	}

}
