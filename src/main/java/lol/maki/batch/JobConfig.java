package lol.maki.batch;

import io.micrometer.observation.ObservationRegistry;
import lol.maki.batch.tasklet.DumpTasklet;
import lol.maki.batch.tasklet.UploadTasklet;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JobConfig {
	@Bean
	public Step dumpStep(JobRepository jobRepository, DumpTasklet dumpTasklet, PlatformTransactionManager transactionManager) {
		return new StepBuilder("Dump", jobRepository)
				.tasklet(dumpTasklet, transactionManager)
				.build();
	}

	@Bean
	public Step uploadStep(JobRepository jobRepository, UploadTasklet uploadTasklet, PlatformTransactionManager transactionManager) {
		return new StepBuilder("Upload", jobRepository)
				.tasklet(uploadTasklet, transactionManager)
				.build();
	}

	@Bean
	public Job backupJob(JobRepository jobRepository, Step dumpStep, Step uploadStep, JobExceptionListener jobExceptionListener, ObservationRegistry observationRegistry) {
		return new JobBuilder("BackupJob", jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(dumpStep)
				.next(uploadStep)
				.observationRegistry(observationRegistry)
				.listener(jobExceptionListener)
				.build();
	}
}
