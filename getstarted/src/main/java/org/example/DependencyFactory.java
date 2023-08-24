
package org.example;

import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.regions.Region;

/**
 * The module containing all dependencies required by the {@link Handler}.
 */
public class DependencyFactory {
    final static Region r1 = Region.US_EAST_1;
    final static Region r2 = Region.US_EAST_2;

    private DependencyFactory() {
    }

    /**
     * @return an instance of S3Client in region us east 1
     */
    public static S3Client s3Client1() {
        return S3Client.builder()
                .httpClientBuilder(ApacheHttpClient.builder())
                .region(r1)
                .build();
    }

    /**
     * @return an instance of S3Client in region us east 2
     */
    public static S3Client s3Client2() {
        return S3Client.builder()
                .httpClientBuilder(ApacheHttpClient.builder())
                .region(r2)
                .build();
    }

    /**
     * @return an instance of rdsClient in region us east 1
     */
    public static RdsClient rdsClient2() {
        return RdsClient.builder()
                .region(r1)
                .build();
    }
}
