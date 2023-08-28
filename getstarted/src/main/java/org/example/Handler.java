package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.rds.*;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.rdsdata.*;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementRequest;
import software.amazon.awssdk.awscore.exception.*;

public class Handler {
    private final S3Client s3Client1;
    private final RdsClient rdsClient2;
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    public Handler() {
        s3Client1 = DependencyFactory.s3Client1();
        rdsClient2 = DependencyFactory.rdsClient2();
    }

    public void sendRequest() {
        // AmazonS3Client s3Connection = new AmazonS3Client(credentials);
        Bucket targetBucket = null;
        String bucketId = "carinfobucket";

        // find bucket containing csv to be written
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse buckets = s3Client1.listBuckets(listBucketsRequest);

        for (Bucket b : buckets.buckets()) {
            if (b.name().contains(bucketId)) {
                targetBucket = b;
                break;
            }
        }

        // Create an RDS client
        RdsClient rdsClient = RdsClient.builder().build();

        // Example code for interacting with RDS
        String databaseId = "rdspostgresstack";

        // Describe RDS instances
        DescribeDbInstancesRequest describeRequest = DescribeDbInstancesRequest.builder().build();
        DescribeDbInstancesResponse describeResponse = rdsClient2.describeDBInstances(describeRequest);

        List<DBInstance> dbInstances = describeResponse.dbInstances();
        DBInstance targetDatabase = null;

        for (DBInstance dbInstance : dbInstances) {
            if (dbInstance.dbInstanceIdentifier().contains(databaseId)) {
                targetDatabase = dbInstance;
                break;
            }
        }

        // prepare query to create table in postgresql
        String createTableSql = "CREATE TABLE carInfo (" +
                "id SERIAL," +
                "dol INT UNIQUE," +
                "county VARCHAR(24)," +
                "city VARCHAR(48)," +
                "state VARCHAR(4)," +
                "postal_code INT," +
                "model_year SMALLINT," +
                "make VARCHAR(40)," +
                "model VARCHAR(50)," +
                "ev_type VARCHAR(60)," +
                "cafv VARCHAR(100)," +
                "electric_range SMALLINT," +
                "base_msrp INT," +
                "district_num SMALLINT," +
                "electric_util_desc VARCHAR(200)," +
                "census_2020_tract BIGINT," +
                "longitude REAL," +
                "latitude REAL" +
                ")";

        // System.out.println("target db: " + targetDatabase.dbName());
        // System.out.flush();
        // System.out.println("targetBucket: " + targetBucket.name());
        // System.out.flush();
        logger.debug("target db: " + targetDatabase.dbInstanceIdentifier());
        logger.debug("targetBucket: " + targetBucket.name());

        // execute statement to build table in postgres
        // ExecuteStatementRequest executeStatementRequest =
        // ExecuteStatementRequest.builder()
        // .database(targetDatabase.dbName())
        // .sql(createTableSql)
        // .build();

        String jdbcURL = "jdbc:postgresql://" + targetDatabase.endpoint() + ":" + targetDatabase.dbInstancePort() + "/"
                + targetDatabase.dbName();
        // System.out.println("JDBC: " + jdbcURL);
        // System.out.flush();
        logger.debug("JDBC: " + jdbcURL);

        try (Connection connection = DriverManager.getConnection(jdbcURL, "postgres", "password")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createTableSql);
            } catch (SQLException e) {
                // add exception handling
            }
        } catch (SQLException e) {
            // add exception handling
        }
        // add wait command for above logic

        streamS3ToRds(targetDatabase, targetBucket);

        s3Client1.close();
        rdsClient.close();
    }

    /*
     * Streams csv file located in s3 bucket to database instance
     * Source:
     * https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-select.html
     * https://stackoverflow.com/questions/49284704/how-do-i-use-a-outputstream-
     * object-to-insert-records-in-a-database
     */
    public void streamS3ToRds(DBInstance targetDatabase, Bucket s3) {
        // String selectQuery = "SELECT * FROM S3Object";
        final String bucketName = s3.name();
        final String objectKey = "carInfoThousandRows.csv";

        // ~~GET DATA FROM S3~~
        // Request to filter the contents of an Amazon S3 object
        GetObjectRequest getObject = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        ResponseInputStream<GetObjectResponse> responseInputStream = s3Client1.getObject(getObject);
        List<String[]> parsedData = parseCSV(responseInputStream);

        // ~~IMPORT DATA TO RDS~~
        // Generate and execute 'insert' queries
        String jdbcURL = "jdbc:postgresql://" + targetDatabase.endpoint() + ":" + targetDatabase.dbInstancePort() + "/"
                + targetDatabase.dbName();
        // should be:
        // jdbc:postgresql://t16-db1.cqewj0ljgvkk.us-east-2.rds.amazonaws.com:3306/t16-db1
        try (Connection connection = DriverManager.getConnection(jdbcURL, "postgres", "password")) {
            for (String[] rowData : parsedData) {
                String insertQuery = generateInsertQuery(rowData);
                try (Statement statement = connection.createStatement()) {
                    statement.executeQuery(insertQuery);
                } catch (SQLException e) {
                    // add exception handling
                    continue;
                }
                connection.close();
            }
        } catch (SQLException e) {
            // add exception handling
        }
    }

    // Parse CSV data from the ResponseInputStream
    private static List<String[]> parseCSV(
            ResponseInputStream<GetObjectResponse> inputStream) {
        List<String[]> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Assuming CSV format, you may need to adapt this based on your data
                String[] rowData = line.split(",");
                data.add(rowData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    // Generate SQL INSERT query
    private static String generateInsertQuery(String[] rowData) {
        String columns = "column1, column2, column3"; // Adjust column names
        String values = "'" + rowData[0] + "', '" + rowData[1] + "', '" + rowData[2] + "'"; // Adjust values
        return "INSERT INTO carInfo (" + columns + ") VALUES (" + values + ")";
    }
}
