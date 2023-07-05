package lol.maki.batch.alert;

import java.util.function.Consumer;

import org.slf4j.Logger;

public enum AlertType {

	SUCCESS(":white_check_mark: *Incident for `%s` resolved*", "#00ff00",
			logger -> logger.info("Firing alert (Success)")),
	FAILURE(":rotating_light: *New Incident for `%s`*", "#ff0000", logger -> logger.warn("Firing alert (Failure)"));

	private final String textTemplate;

	private final String color;

	private final Consumer<Logger> log;

	AlertType(String textTemplate, String color, Consumer<Logger> log) {
		this.textTemplate = textTemplate;
		this.color = color;
		this.log = log;
	}

	public String textTemplate() {
		return this.textTemplate;
	}

	public String color() {
		return this.color;
	}

	public void log(Logger logger) {
		this.log.accept(logger);
	}

}