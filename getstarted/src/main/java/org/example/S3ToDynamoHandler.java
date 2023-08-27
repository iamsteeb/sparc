package org.example;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*

public class S3ToDynamoHandler {
	private final S3Client s3Client;
	private final S3BucketSource sourceBucket;
	private final DynamoDbClient ddbClient;
	
	public S3ToDynamoHandler(String s3SourceBucket) {
        sourceBucket = S3BucketSource.builder()
        		.s3Bucket(s3SourceBucket)
        		.build();
        ddbClient = DynamoDbClient.builder()
        		.region(Region.US_EAST_1)
        		.build();
    }

	public void importTable() {
		TableCreationParameters params = TableCreationParameters.builder()
				.attributeDefinitions(
					AttributeDefinition.builder()
	                        .attributeName("id")
	                        .attributeType(ScalarAttributeType.N)
	                        .build(),
	                AttributeDefinition.builder()
	                        .attributeName("dol")
	                        .attributeType(ScalarAttributeType.N)
	                        .build()
                    AttributeDefinition.builder()
	                        .attributeName("county")
	                        .attributeType(ScalarAttributeType.S)
	                        .build(),
	                AttributeDefinition.builder()
	                        .attributeName("city")
	                        .attributeType(ScalarAttributeType.S)
	                        .build()
                    AttributeDefinition.builder()
	                        .attributeName("state")
	                        .attributeType(ScalarAttributeType.S)
	                        .build(),
	                AttributeDefinition.builder()
	                        .attributeName("postal_code")
	                        .attributeType(ScalarAttributeType.N)
	                        .build()
                    AttributeDefinition.builder()
	                        .attributeName("model_year")
	                        .attributeType(ScalarAttributeType.N)
	                        .build(),
	                AttributeDefinition.builder()
	                        .attributeName("make")
	                        .attributeType(ScalarAttributeType.S)
	                        .build()
                    AttributeDefinition.builder()
	                        .attributeName("model")
	                        .attributeType(ScalarAttributeType.S)
	                        .build(),
	                AttributeDefinition.builder()
	                        .attributeName("ev_type")
	                        .attributeType(ScalarAttributeType.S)
	                        .build()
                    AttributeDefinition.builder()
	                        .attributeName("cafv")
	                        .attributeType(ScalarAttributeType.S)
	                        .build(),
	                AttributeDefinition.builder()
	                        .attributeName("electric_range")
	                        .attributeType(ScalarAttributeType.N)
	                        .build()
                    AttributeDefinition.builder()
	                        .attributeName("base_msrp")
	                        .attributeType(ScalarAttributeType.N)
	                        .build(),
	                AttributeDefinition.builder()
	                        .attributeName("district_num")
	                        .attributeType(ScalarAttributeType.N)
	                        .build()
	                AttributeDefinition.builder()
	                        .attributeName("electric_util_desc")
	                        .attributeType(ScalarAttributeType.S)
	                        .build(),
	                AttributeDefinition.builder()
	                        .attributeName("census_2020_tract")
	                        .attributeType(ScalarAttributeType.N)
	                        .build()
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
				.tableName("dynamodb-db")
				.build();
				
		
		ImportTablesRequest request = ImportTablesRequest.builder()
				.inputFormat(InputFormat.CSV)
				.s3BucketSource(sourceBucket)
				.tableCreationParameters(params)
				.build();
		
		ImportTableResponse response = ddbClient.importTable(request);
		
	}
}
