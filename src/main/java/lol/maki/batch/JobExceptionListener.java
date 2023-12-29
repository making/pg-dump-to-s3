package lol.maki.batch;

import java.util.List;
import java.util.stream.Collectors;

import lol.maki.batch.alert.AlertSender;
import lol.maki.batch.alert.AlertType;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JobExceptionListener implements JobExecutionListener {

	private final AlertSender alertSender;

	private final String applicationName;

	public JobExceptionListener(AlertSender alertSender, @Value("${spring.application.name}") String applicationName) {
		this.alertSender = alertSender;
		this.applicationName = applicationName;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		final List<Throwable> exceptions = jobExecution.getAllFailureExceptions();
		if (exceptions.isEmpty()) {
			return;
		}
		final String messages = exceptions.stream().map(e -> "* " + e.getMessage()).collect(Collectors.joining("\n"));
		this.alertSender.sendAlert(AlertType.FAILURE, "Job", null, this.applicationName, messages);
	}

}
