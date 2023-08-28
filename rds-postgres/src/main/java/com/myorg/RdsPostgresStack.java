package com.myorg;

import software.constructs.Construct;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.amazon.awscdk.services.dynamodb.*;

public class RdsPostgresStack extends Stack {
        public RdsPostgresStack(final Construct scope, final String id) {
                this(scope, id, null);
        }

        public RdsPostgresStack(final Construct scope, final String id, final StackProps props) {
                super(scope, id, props);

                // call to create s3 and upload an csv to it
                Bucket carInfoBucket = addCsv("/Users/ntashi/Development/sparc/rds-postgres/csvfile");

                // creates vpc to pass to db create
                final Vpc vpc = Vpc.Builder.create(this, id + "-vpc")
                                .build();

                // creates db engine to pass to db create
                final IInstanceEngine rdsInstanceEngine = DatabaseInstanceEngine.postgres(
                                PostgresInstanceEngineProps.builder()
                                                .version(PostgresEngineVersion.VER_15_3)
                                                .build());

                // creates rds postgres db instance
                final DatabaseInstance rdsInstance = DatabaseInstance.Builder.create(this, id + "-rdsdb")
                                .databaseName("carinfords")
                                .vpc(vpc)
                                // since subnet is isolated, this instance won't route traffic to the internet
                                // without other resources in the VPC that are connected
                                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                                .instanceType(software.amazon.awscdk.services.ec2.InstanceType.of(
                                                InstanceClass.BURSTABLE3,
                                                InstanceSize.MICRO))
                                .engine(rdsInstanceEngine)
                                .instanceIdentifier(id + "-rds")
                                // .credentials(Map.of("masterUsername", "masterPass"))
                                .removalPolicy(RemovalPolicy.DESTROY) // makes sure 'cdk destroy' removes all artifacts
                                .s3ImportBuckets(Arrays.asList(new Bucket(this, "ImportBucket", BucketProps.builder() //
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())))
                                .build();

                // add lambdas here [SDK]
                // allocate lambda functions https://hevodata.com/learn/aws-cdk-lambda/ [TO-DO]

                // back up of rds db [SDK]

                // read s3 backup into new dynamodb instance [SDK]
                // instantiate new dynamodb instance [TO-DO]
                // Table dynamoTable = Table.Builder.create(this, "car-info-dynamo")
                // .partitionKey(Attribute.builder().name("dol").type(AttributeType.STRING).build())
                // .billingMode(BillingMode.PAY_PER_REQUEST)
                // .removalPolicy(RemovalPolicy.DESTROY)
                // .pointInTimeRecovery(true)
                // .tableClass(TableClass.STANDARD_INFREQUENT_ACCESS)
                // .build();

                // table.addLocalSecondaryIndex(LocalSecondaryIndexProps.builder()
                // .indexName("statusIndex")
                // .sortKey(SortKey.builder()
                // .name("status")
                // .type(AttributeType.STRING)
                // .build())
                // .projectionType(ProjectionType.ALL)
                // .build());
        }

        /*
         * Creates bucket and loads given csv into S3; returns such bucket
         */
        private Bucket addCsv(String filepath) {
                Bucket carInfoBucket = Bucket.Builder.create(this, "car-info-bucket")
                                .build();
                BucketDeployment deployment = BucketDeployment.Builder.create(this, "DeployWebsite")
                                .sources(Collections.singletonList(Source.asset(filepath)))
                                .destinationBucket(carInfoBucket)
                                .build();
                return carInfoBucket;
        }
}
