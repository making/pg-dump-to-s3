package lol.maki.batch.tasklet;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lol.maki.batch.PgDumpProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class DumpTasklet implements Tasklet {

	private final Logger logger = LoggerFactory.getLogger(DumpTasklet.class);

	private final Clock clock;

	private final PgDumpProps pgDumpProps;

	public DumpTasklet(Clock clock, PgDumpProps pgDumpProps) {
		this.clock = clock;
		this.pgDumpProps = pgDumpProps;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		final ExecutionContext context = contribution.getStepExecution().getJobExecution().getExecutionContext();
		final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
		final String timestamp = LocalDateTime.now(this.clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		final Path dump = tmpDir.resolve("%s".formatted(timestamp));
		final ProcessBuilder processBuilder = new ProcessBuilder("pg_dump", "-U", this.pgDumpProps.username(), "-h",
				this.pgDumpProps.host(), "-p", String.valueOf(this.pgDumpProps.port()), this.pgDumpProps.database())
			.directory(tmpDir.toFile())
			.redirectOutput(dump.toFile());
		final Map<String, String> environment = processBuilder.environment();
		environment.put("PGPASSWORD", this.pgDumpProps.password());
		logger.info("command=\"{}\"", String.join(" ", processBuilder.command()));
		final Process process = processBuilder.start();
		final boolean finished = process.waitFor(30, TimeUnit.SECONDS);
		if (finished) {
			context.put("dump", dump.toAbsolutePath().toString());
			int exitValue = process.exitValue();
			logger.info("Exit code = {}", exitValue);
			if (exitValue != 0) {
				throw new IllegalStateException("pg_dump finished unsuccessfully!");
			}
		}
		else {
			process.destroy();
			throw new IllegalStateException("pg_dump cancelled!");
		}
		return RepeatStatus.FINISHED;
	}

}
