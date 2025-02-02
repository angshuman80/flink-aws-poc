**AWS flink POC**

*flink-aws-kinesis*
 - subscribes records from a Kinesis stream (ExampleInputStream) and pushes to sink (ExampleOutputStream)

*flink-aws-ddbstream*
  - subcribes to DynamoDBstream and and pushes to kinesis sink (ExampleOutputStream)
  - Please update the ARNs in the config file under /srs/main/resources

**Steps to create Kinesis stream**

aws kinesis create-stream --stream-name ExampleInputStream --shard-count 1 --region us-east-1

aws kinesis create-stream --stream-name ExampleOutputStream --shard-count 1 --region us-east-1

