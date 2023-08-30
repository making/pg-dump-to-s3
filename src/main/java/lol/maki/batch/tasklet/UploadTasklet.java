package lol.maki.batch.tasklet;

import java.io.File;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.messages.Item;
import lol.maki.batch.PgDumpProps;
import lol.maki.batch.S3Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

@Component
@JobScope
public class UploadTasklet implements Tasklet {
	private final Logger logger = LoggerFactory.getLogger(UploadTasklet.class);

	private final MinioClient minioClient;

	private final Clock clock;

	private final S3Props s3Props;

	private final PgDumpProps pgDumpProps;


	public UploadTasklet(MinioClient minioClient, Clock clock, S3Props s3Props, PgDumpProps pgDumpProps) {
		this.minioClient = minioClient;
		this.clock = clock;
		this.s3Props = s3Props;
		this.pgDumpProps = pgDumpProps;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		final Path path = Path.of(contribution.getStepExecution()
				.getJobExecution()
				.getExecutionContext()
				.getString("dump"));
		final LocalDateTime dateTime = LocalDateTime.parse(path.getFileName().toString());
		if (!this.minioClient.bucketExists(BucketExistsArgs.builder().bucket(this.s3Props.bucket()).build())) {
			logger.info("Create bucket ({})", this.s3Props.bucket());
			this.minioClient.makeBucket(MakeBucketArgs.builder().bucket(this.s3Props.bucket()).build());
		}
		final LocalDate today = dateTime.toLocalDate();
		final Iterable<Result<Item>> objects = this.minioClient.listObjects(ListObjectsArgs.builder()
				.bucket(this.s3Props.bucket())
				.recursive(true)
				.build());
		for (Result<Item> object : objects) {
			final String objectName = object.get().objectName();
			final File objectFile = new File(objectName);
			final LocalDate backupDate = LocalDate.parse(objectName.endsWith("/") ? objectFile.getName() : objectFile.getParent());
			if (backupDate.isBefore(today.minusDays(this.s3Props.retention().toDays()))) {
				logger.info("Deleting {} ...", objectName);
				this.minioClient.removeObject(RemoveObjectArgs.builder()
						.bucket(this.s3Props.bucket())
						.object(objectName)
						.build());
			}
		}
		final String objectName = "%s/%s-%s.sql".formatted(today, this.s3Props.filePrefix(), this.pgDumpProps.database());
		logger.info("Uploading {} to {} ...", path.toAbsolutePath(), objectName);
		this.minioClient.uploadObject(UploadObjectArgs.builder()
				.bucket(this.s3Props.bucket())
				.object(objectName)
				.filename(path.toAbsolutePath().toString())
				.build());
		FileSystemUtils.deleteRecursively(path.toFile());
		return RepeatStatus.FINISHED;
	}
}
