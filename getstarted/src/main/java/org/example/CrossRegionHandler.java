package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.transfer.s3.*;

/* The S3 object should be copied to a different region (e.g. from us-east-1 to
* us-west-2) periodically and labeled in such a way that it can be uniquely identified
based on time the "backup" was taken.*/
public class CrossRegionHandler {
    private final S3Client s3Client1;
    private final S3Client s3Client2;

    public CrossRegionHandler() {
        s3Client1 = DependencyFactory.s3Client1();
        s3Client2 = DependencyFactory.s3Client2();
    }

    public String copyObject(S3TransferManager transferManager, String bucketName, String key, String destinationBucket,
            String destinationKey) {

        CreateBucketConfiguration bucket = CreateBucketConfiguration.builder()
                .locationConstraint("us-east-2")
                .build();

        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(key)
                .destinationBucket(destinationBucket)
                .destinationKey(destinationKey)
                .build();

        CopyRequest copyRequest = CopyRequest.builder()
                .copyObjectRequest(copyObjectRequest)
                .build();

        Copy copy = transferManager.copy(copyRequest);

        CompletedCopy completedCopy = copy.completionFuture().join();
        return completedCopy.response().copyObjectResult().eTag();
    }

}
