package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import java.util.HashMap;


public class S3ToDynamoHandler {
	
	//private final S3Client s3;
	//private final DynamoDbClient ddb;
	//private final String bucketName;
	//private final String objectTimestamp;
	//private final String tableName;
	//private final Region region = Region.US_WEST_1;
	
	
	/*public S3ToDynamoHandler() {
		
		ddb = DynamoDbClient.builder()
				.credentialsProvider(DeafultCredentialsProvider.create())
				.region(region)
				.build();
		
		s3 = S3Client.builder()
				.httpClientBuilder(ApacheHttpClient.builder())
				.credentialsProvider(DefaultCredentialsProvider.create())
				.region(region)
				.build();
	}*/
	
	public static void main(String[] args) {
		
		final String usage = "\n" +
		"Usage:\n" +
		"    <tableName> <bucketName> <timestamp> \n\n" +
		"Where:\n" +
		"    tableName - The Amazon DynamoDB table to create.\n\n" +
		"    bucketName - The name of the S3 Bucket in which the objects are located.\n\n" +
		"    timestamp - The timestamp used to identify S3 objects to import to the DynamoDB table.\n" ;
	
		if (args.length != 3) {
			System.out.println(usage);
			System.exit(1);
		}
		
		String tableName = args[0];
		String bucketName = args[1];
		String timestamp = args[2];
		
		Region region = Region.US_WEST_1;
		
		DynamoDbClient ddb = DynamoDbClient.builder()
				.credentialsProvider(DefaultCredentialsProvider.create())
				.region(region)
				.build();
		
		S3Client s3 = S3Client.builder()
				.httpClientBuilder(ApacheHttpClient.builder())
				.credentialsProvider(DefaultCredentialsProvider.create())
				.region(region)
				.build();
		
		copyS3ObjectsToDDB(s3, ddb, tableName, bucketName, timestamp);
		
		ddb.close();
		s3.close();
	}
	
	public void copyS3ObjectsToDDB(S3Client s3, DynamoDbClient ddb, String tableName, String bucketName, String timestamp) {
		
		 try {
			 ListObjectsRequest listObjects = ListObjectsRequest
					 .builder()
					 .bucket(bucketName)
					 .build();
			 	ListObjectsResponse res = s3.listObjects(listObjects);
			 	List<S3Object> objects = res.contents();
			 	
			 	int objCounter = 1;
			 	while (objCounter <= objects.size()) {
			 		boolean foundObject = false;
				 	for (S3Object s3Obj : objects) {
				 		objName = s3Obj.key();
				 		if (objName.startsWith(objectTimestamp) && 
				 				objName.endsWith(Integer.toString(objCounter))) {
				 			if (objCounter == 1) {
				 				System.out.println("Creating table " + tableName);
				 				String result = createTable(ddb, tableName);
				 				System.out.println("New table is " + result);
				 			}
				 			sendObjectToDDB(s3Obj, tableName);
				 			foundObject = true;
				 			break;
				 		}
				 	}
				 	if (!foundObject) {
				 		System.out.println("Total objects located: "
				 				+ Integer.toString(objCounter - 1));
				 		break;
				 	}
				 	objCounter++;
			 	}
		 } catch (S3Exception e) {
			 System.err.println(e.awsErrorDetails().errorMessage());
			 System.exit(1);
		 }
	}
	
	public void sendObjectToDDB(S3Object s3Obj, String tableName) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(s3Obj.getObjectContent()));
		
		String line;
		while ((line = reader.readLine()) != null) {
			String[] fields = line.split(",");
			if (fields.length != 18) {
				System.out.println("Error in parsing line in s3Object with key " + s3Obj.key());
			}
			HashMap<String,AttributeValue> itemValues = new HashMap<>();
			itemValues.put("id", AttributeValue.builder().n(fields[0]).build());
			itemValues.put("dol", AttributeValue.builder().n(fields[1]).build());
			itemValues.put("county", AttributeValue.builder().s(fields[2]).build());
			itemValues.put("city", AttributeValue.builder().s(fields[3]).build());
			itemValues.put("state", AttributeValue.builder().s(fields[4]).build());
			itemValues.put("postal_code", AttributeValue.builder().s(fields[5]).build());
			itemValues.put("model_year", AttributeValue.builder().n(fields[6]).build());
			itemValues.put("make", AttributeValue.builder().s(fields[7]).build());
			itemValues.put("model", AttributeValue.builder().s(fields[8]).build());
			itemValues.put("ev_type", AttributeValue.builder().s(fields[9]).build());
			itemValues.put("cafv", AttributeValue.builder().s(fields[10]).build());
			itemValues.put("electric_range", AttributeValue.builder().n(fields[11]).build());
			itemValues.put("base_msrp", AttributeValue.builder().n(fields[12]).build());
			itemValues.put("district_num", AttributeValue.builder().n(fields[13]).build());
			itemValues.put("electric_util_desc", AttributeValue.builder().s(fields[14]).build());
			itemValues.put("census_2020_tract", AttributeValue.builder().n(fields[15]).build());
			itemValues.put("longitude", AttributeValue.builder().n(fields[16]).build());
			itemValues.put("latitude", AttributeValue.builder().n(fields[17]).build());
			
			PutItemRequest request = PutItemRequest.builder()
		            .tableName(tableName)
		            .item(itemValues)
		            .build();
			
			try {
		            PutItemResponse response = ddb.putItem(request);
		            //System.out.println(tableName +" was successfully updated. The request id is "+response.responseMetadata().requestId());

		        } catch (ResourceNotFoundException e) {
		            System.err.format("Error: The Amazon DynamoDB table \"%s\" can't be found.\n", tableName);
		            System.err.println("Be sure that it exists and that you've typed its name correctly!");
		            System.exit(1);
		        } catch (DynamoDbException e) {
		            System.err.println(e.getMessage());
		            System.exit(1);
		        }
		}
		reader.close();
	}
	
	public String createTable(DynamoDbClient ddb, String tableName) {
		
		DynamoDbWaiter dbWaiter = ddb.waiter();
        	CreateTableRequest request = CreateTableRequest.builder()
        		.attributeDefinitions(
				AttributeDefinition.builder()
					.attributeName("id")
					.attributeType(ScalarAttributeType.N)
					.build(),
				AttributeDefinition.builder()
					.attributeName("dol")
					.attributeType(ScalarAttributeType.N)
					.build(),
				AttributeDefinition.builder()
					.attributeName("county")
					.attributeType(ScalarAttributeType.S)
					.build(),
				AttributeDefinition.builder()
					.attributeName("city")
					.attributeType(ScalarAttributeType.S)
					.build(),
				AttributeDefinition.builder()
					.attributeName("state")
					.attributeType(ScalarAttributeType.S)
					.build(),
				AttributeDefinition.builder()
					.attributeName("postal_code")
					.attributeType(ScalarAttributeType.S)
					.build(),
				AttributeDefinition.builder()
					.attributeName("model_year")
					.attributeType(ScalarAttributeType.N)
					.build(),
				AttributeDefinition.builder()
					.attributeName("make")
					.attributeType(ScalarAttributeType.S)
					.build(),
				 AttributeDefinition.builder()
					.attributeName("model")
					.attributeType(ScalarAttributeType.S)
					.build(),
				AttributeDefinition.builder()
					.attributeName("ev_type")
					.attributeType(ScalarAttributeType.S)
					.build(),
				AttributeDefinition.builder()
					.attributeName("cafv")
					.attributeType(ScalarAttributeType.S)
					.build(),
				AttributeDefinition.builder()
					.attributeName("electric_range")
					.attributeType(ScalarAttributeType.N)
					.build(),
				AttributeDefinition.builder()
					.attributeName("base_msrp")
					.attributeType(ScalarAttributeType.N)
					.build(),
				AttributeDefinition.builder()
					.attributeName("district_num")
					.attributeType(ScalarAttributeType.N)
					.build(),
				AttributeDefinition.builder()
					.attributeName("electric_util_desc")
					.attributeType(ScalarAttributeType.S)
					.build(),
				AttributeDefinition.builder()
					.attributeName("census_2020_tract")
					.attributeType(ScalarAttributeType.N)
					.build(),
				AttributeDefinition.builder()
					.attributeName("longitude")
					.attributeType(ScalarAttributeType.N)
					.build(),
				AttributeDefinition.builder()
					.attributeName("latitude")
					.attributeType(ScalarAttributeType.N)
					.build()) 
			.keySchema(KeySchemaElement.builder()
					.attributeName("id")
					.keyType(KeyType.HASH)
					.build())
			.tableName(tableName)
			.build();
        
	        String newTable = "";
	        try {
	            CreateTableResponse response = ddb.createTable(request);
	            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
	                .tableName(tableName)
	                .build();
	
	            // Wait until the Amazon DynamoDB table is created.
	            WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
	            waiterResponse.matched().response().ifPresent(System.out::println);
	            newTable = response.tableDescription().tableName();
	            return newTable;
	
	        } catch (DynamoDbException e) {
	            System.err.println(e.getMessage());
	            System.exit(1);
	        }
	        return "";
	}
}
