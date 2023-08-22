package org.example;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.io.InputStream;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.rds.*;
import software.amazon.awssdk.services.rdsdata.*;
import software.amazon.awssdk.awscore.exception.*;

public class Handler {
    private final S3Client s3Client;

    public Handler() {
        s3Client = DependencyFactory.s3Client();
    }

    public void sendRequest() {
        // AmazonS3Client s3Connection = new AmazonS3Client(credentials);
        Bucket targetBucket = null;
        String bucketId = "car-info-bucket";

        // find bucket containing csv to be written
        List<Bucket> buckets = s3Client.listBuckets();
        for (Bucket bucket : buckets) {
            if (bucket.getName.contains(bucketId)) {
                targetBucket = bucket;
                break;
            }
        }

        AmazonRdsClient rdsConnection = new AmazonRdsClient(credentials);
        Database targetDatabase = null;
        String databaseId = "-rdsdb";

        // find specific rds database to write to
        List<Database> databases = rdsConnection.DescribeReservedDbInstancesResponse();
        for (Database db : databases) {
            if (db.getName.contains(databaseId)) {
                targetDatabase = db;
                break;
            }
        }

        // prepare query to create table in postgresql
        String createTableSql = "CREATE TABLE  (" +
                "DOL INT UNIQUE," +
                "County VARCHAR(255)," +
                "City VARCHAR(255)," +
                "State VARCHAR(255)," +
                "Postal Code INT," +
                "Model Year INT," +
                "Make VARCHAR(255)," +
                "Model VARCHAR(255)," +
                "Electric Vehicle Type VARCHAR(255)," +
                "CAFV VARCHAR(255)," +
                "Electric Range INT," +
                "Base MSRP INT," +
                "Legislative District INT," +
                "Electric Utility VARCHAR(255)," +
                "2020 Census Tract INT," +
                "Longitude VARCHAR(255)," +
                "latitude VARCHAR(255)," +
                "PRIMARY KEY (DOL)," +
                ")";

        ExecuteStatementRequest executeStatementRequest = ExecuteStatementRequest.builder()
                .resourceArn(resourceArn)
                .secretArn(secretArn)
                .database(databaseName)
                .sql(createTableSql)
                .build();

        // add wait command for above logic

        // execute statement to build table in postgres
        rdsConnection.executeStatement(executeStatementRequest);

        rdsConnection.close();

        streamS3ToRds(targetDatabase, targetBucket);
        s3Client.close();
    }

    /*
     * Streams csv file located in s3 bucket to database instance
     * Source:
     * https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-select.html
     * https://stackoverflow.com/questions/49284704/how-do-i-use-a-outputstream-
     * object-to-insert-records-in-a-database
     */
    public void streamS3ToRds(Database db, Bucket s3) {
        final String bucketName = s3.getName;
        final String keyName = "carInfoThousandRows.csv";

        // final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

        try {
            S3Object obj = s3Client.getObject(bucketName, keyName); // get csv from s3
            S3ObjectInputStream s3is = obj.getObjectContent(); // get the content from the csv
                                                                      // 
                                                                      // 
            ByteArrayOutputStream baos = new ByteArrayOutputStream(); // initialize byte array stream necessary for blob-format insert
            request.setReportOutputStream(baos); // request writes into output stream
            byte[] byte_buf = baos.toByteArray(); // convert to byte array
            // byte[] read_buf = new byte[1024];
            // int read_len = 0;
            // read_len = s3is.read(read_buf)) > 0) {
            //     baos.write(read_buf, 0, read_len);
            // }
            s3is.close();
            baos.close();
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {

         
        }
    }

    // RELIC
    public static void createBucket(S3Client s3Client, String bucketName) {
        try {
            s3Client.createBucket(CreateBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .build());
            System.out.println("Creating bucket: " + bucketName);
            s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            System.out.println(bucketName + " is ready.");
            System.out.printf("%n");
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    // RELIC
    public static void cleanUp(S3Client s3Client, String bucketName, String keyName) {
        System.out.println("Cleaning up...");
        try {
            System.out.println("Deleting object: " + keyName);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(keyName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            System.out.println(keyName + " has been deleted.");
            System.out.println("Deleting bucket: " + bucketName);
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
            s3Client.deleteBucket(deleteBucketRequest);
            System.out.println(bucketName + " has been deleted.");
            System.out.printf("%n");
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    
    

    