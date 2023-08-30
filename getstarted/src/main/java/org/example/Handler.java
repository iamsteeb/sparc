package org.example;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.io.InputStreamReader;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.rds.*;
import software.amazon.awssdk.services.rds.model.*;

public class Handler {
    private final S3Client s3Client1;
    private final RdsClient rdsClient1;
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    public Handler() {
        s3Client1 = DependencyFactory.s3Client1();
        rdsClient1 = DependencyFactory.rdsClient1();
    }

    public void sendRequest() {

        /////////////////
        // FOR TESTING - change output to logger for debugging
        String filePath = "/Users/ntashi/Development/sparc/getstarted/src/main/resources/logfile.log";
        FileOutputStream fileOutputStream = null;
        PrintStream printStream = null;
        try {
            // Create a FileOutputStream to write to the specified file
            fileOutputStream = new FileOutputStream(filePath);

            // Create a PrintStream from the FileOutputStream
            printStream = new PrintStream(fileOutputStream);

            // Redirect the standard output to the PrintStream
            System.setOut(printStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /////////////////

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

        // Example code for interacting with RDS
        String databaseId = "rdspostgresstack";

        // Describe RDS instances
        DescribeDbInstancesRequest describeRequest = DescribeDbInstancesRequest.builder().build();
        DescribeDbInstancesResponse describeResponse = rdsClient1.describeDBInstances(describeRequest);

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

        String jdbcURL = "jdbc:postgresql://" + targetDatabase.endpoint().address() + ":"
                + targetDatabase.endpoint().port() + "/"
                + targetDatabase.dbName();

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(jdbcURL, "postgres", "password");
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createTableSql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        streamS3ToRds(targetDatabase, targetBucket, connection);

        s3Client1.close();
        rdsClient1.close();
        try {
            printStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
     * Streams csv file located in s3 bucket to database instance
     */
    public void streamS3ToRds(DBInstance targetDatabase, Bucket s3, Connection connection) {
        // String selectQuery = "SELECT * FROM S3Object";
        final String bucketName = s3.name();
        final String objectKey = "carInfo.csv";

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
        for (int i = 1; i < parsedData.size(); i++) {
            String insertQuery = generateInsertQuery(parsedData.get(i));
            System.out.println(insertQuery);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(insertQuery);
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
        String columns = "dol, county, city, state, postal_code, model_year, make, model, ev_type, cafv, electric_range, base_msrp, district_num, electric_util_desc, census_2020_tract, longitude, latitude";
        String values = "'" + rowData[0] + "', '" + rowData[1] + "', '" + rowData[2] + "', '" + rowData[3] + "', '"
                + rowData[4] + "', '" + rowData[5] + "', '" + rowData[6] + "', '" + rowData[7] + "', '" + rowData[8]
                + "', '" + rowData[9] + "', '" + rowData[10] + "', '" + rowData[11] + "', '" + rowData[12] + "', '"
                + rowData[13] + "', '" + rowData[14] + "', '" + rowData[15] + "', '" + rowData[16] + "'";
        return "INSERT INTO carInfo (" + columns + ") VALUES (" + values + ")";
    }
}
