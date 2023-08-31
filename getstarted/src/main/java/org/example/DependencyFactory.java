
package org.example;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.regions.Region;

/**
 * The module containing all dependencies required by the {@link Handler}.
 */
public class DependencyFactory {
    final static Region r1 = Region.US_WEST_1;
    final static Region r2 = Region.US_WEST_2;

    private DependencyFactory() {
    }

    /**
     * @return an instance of S3Client in region us east 2
     */
    public static S3Client s3Client1() {
        return S3Client.builder()
                .httpClientBuilder(ApacheHttpClient.builder())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(r1)
                .build();
    }

    /**
     * @return an instance of S3Client in region us west 2
     */
    // public static S3Client s3Client2() {
    // return S3Client.builder()
    // .httpClientBuilder(ApacheHttpClient.builder())
    // .region(r2)
    // .build();
    // }

    /**
     * @return an instance of rdsClient in region us east 2
     */
    public static RdsClient rdsClient1() {
        return RdsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(r1)
                .build();
    }
}
