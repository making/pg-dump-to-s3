package lol.maki.batch.tasklet;

import am.ik.s3.Content;
import am.ik.s3.ListBucketResult;
import am.ik.s3.S3Content;
import am.ik.s3.S3Request;
import am.ik.s3.S3RequestBuilder;
import am.ik.s3.S3RequestBuilders;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lol.maki.batch.AwsProps;
import lol.maki.batch.PgDumpProps;
import lol.maki.batch.S3Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
@JobScope
public class UploadTasklet implements Tasklet {

	private final Logger logger = LoggerFactory.getLogger(UploadTasklet.class);

	private final RestClient restClient;

	private final S3Props s3Props;

	private final PgDumpProps pgDumpProps;

	private final Supplier<S3RequestBuilders.Method> s3RequestSupplier;

	public UploadTasklet(RestClient.Builder restClientBuilder, AwsProps awsProps, S3Props s3Props,
			PgDumpProps pgDumpProps) {
		this.restClient = restClientBuilder.defaultStatusHandler(status -> {
			if (status == HttpStatus.NOT_FOUND) {
				return false;
			}
			return status.isError();
		}, (req, res) -> {
			throw new ResponseStatusException(res.getStatusCode(), res.getStatusText());
		}).build();
		this.s3Props = s3Props;
		this.pgDumpProps = pgDumpProps;
		this.s3RequestSupplier = () -> S3RequestBuilder.s3Request()
			.endpoint(URI.create(s3Props.hostname()))
			.region(awsProps.region())
			.accessKeyId(awsProps.accessKeyId())
			.secretAccessKey(awsProps.secretAccessKey());
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		final Path path = Path
			.of(contribution.getStepExecution().getJobExecution().getExecutionContext().getString("dump"));
		final LocalDateTime dateTime = LocalDateTime.parse(path.getFileName().toString());
		S3Request bucketExistsRequest = this.s3RequestSupplier.get()
			.method(HttpMethod.HEAD)
			.path(b -> b.bucket(this.s3Props.bucket()))
			.build();
		if (this.s3Props.checkBucket() && this.restClient.head()
			.uri(bucketExistsRequest.uri())
			.headers(bucketExistsRequest.headers())
			.retrieve()
			.toBodilessEntity()
			.getStatusCode() != HttpStatus.OK) {
			logger.info("Create bucket ({})", this.s3Props.bucket());
			S3Request putBucketRequest = this.s3RequestSupplier.get()
				.method(HttpMethod.PUT)
				.path(b -> b.bucket(this.s3Props.bucket()))
				.build();
			restClient.put()
				.uri(putBucketRequest.uri())
				.headers(putBucketRequest.headers())
				.retrieve()
				.toBodilessEntity();
		}
		final LocalDate today = dateTime.toLocalDate();
		S3Request listBucketRequest = this.s3RequestSupplier.get()
			.method(HttpMethod.GET)
			.path(b -> b.bucket(this.s3Props.bucket()))
			.build();
		ListBucketResult bucketResult = restClient.get()
			.uri(listBucketRequest.uri())
			.headers(listBucketRequest.headers())
			.retrieve()
			.body(ListBucketResult.class);
		if (bucketResult != null && bucketResult.contents() != null) {
			for (Content object : bucketResult.contents()) {
				final String objectName = object.key();
				final File objectFile = new File(objectName);
				final LocalDate backupDate = LocalDate
					.parse(objectName.endsWith("/") ? objectFile.getName() : objectFile.getParent());
				if (backupDate.isBefore(today.minusDays(this.s3Props.retention().toDays()))) {
					logger.info("Deleting {} ...", objectName);
					S3Request deleteObjectRequest = this.s3RequestSupplier.get()
						.method(HttpMethod.DELETE)
						.path(b -> b.bucket(this.s3Props.bucket()).key(objectName))
						.build();
					restClient.delete()
						.uri(deleteObjectRequest.uri())
						.headers(deleteObjectRequest.headers())
						.retrieve()
						.toBodilessEntity();
				}
			}
		}
		final String objectName = "%s/%s-%s.sql".formatted(today, this.s3Props.filePrefix(),
				this.pgDumpProps.database());
		logger.info("Uploading {} to {} ...", path.toAbsolutePath(), objectName);

		String body = Files.readString(path);
		S3Request putObjectRequest = this.s3RequestSupplier.get()
			.method(HttpMethod.PUT)
			.path(b -> b.bucket(this.s3Props.bucket()).key(objectName))
			.content(S3Content.of(body, MediaType.TEXT_PLAIN))
			.build();
		restClient.put()
			.uri(putObjectRequest.uri())
			.headers(putObjectRequest.headers())
			.body(body)
			.retrieve()
			.toBodilessEntity();

		FileSystemUtils.deleteRecursively(path.toFile());
		return RepeatStatus.FINISHED;
	}

}
