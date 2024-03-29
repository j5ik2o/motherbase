data "aws_region" "current" {}

resource "aws_ecs_cluster" "ecs_cluster" {
  count = var.enabled ? 1 : 0
  name = var.gatling_ecs_cluster_name
  tags = {
    Name = var.gatling_ecs_cluster_name
    Owner = var.owner
  }
}

resource "aws_iam_role" "gatling_ecs_task_execution_role" {
  count = var.enabled ? 1 : 0
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": [
          "ecs-tasks.amazonaws.com"
        ]
      },
      "Action": [
        "sts:AssumeRole"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "gatling_ecs_policy" {
  count = var.enabled ? 1 : 0
  name = "${var.prefix}-gatling-ecs-policy"
  path = "/"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:*"
      ],
      "Resource": [
        "arn:aws:s3:::${var.gatling_s3_log_bucket_name}",
        "arn:aws:s3:::${var.gatling_s3_log_bucket_name}/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecs:*",
        "iam:Get*",
        "iam:List*",
        "iam:PassRole",
        "ecs:ListClusters",
        "ecs:ListContainerInstances",
        "ecs:DescribeContainerInstances",
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
EOF

}

resource "aws_iam_role_policy_attachment" "gatling_attach_ec2_policy" {
  count = var.enabled ? 1 : 0
  role = element(concat(aws_iam_role.gatling_ecs_task_execution_role[*].name, list("")), 0)
  policy_arn = aws_iam_policy.gatling_ecs_policy[0].arn
}

resource "aws_ecs_task_definition" "gatling_aggregate_runner" {
  count = var.enabled ? 1 : 0
  family = "${var.prefix}-gatling-aggregate-runner"
  requires_compatibilities = [
    "FARGATE"]
  network_mode = "awsvpc"
  task_role_arn = element(concat(aws_iam_role.gatling_ecs_task_execution_role[*].arn, list("")), 0)
  execution_role_arn = element(concat(aws_iam_role.gatling_ecs_task_execution_role[*].arn, list("")), 0)
  cpu = "512"
  memory = "1024"
  container_definitions = <<EOF
[
  {
    "name": "gatling-aggregate-runner",
    "essential": true,
    "image": "${element(concat(aws_ecr_repository.gatling_aggregate_runner_ecr[*].repository_url, list("")), 0)}",
    "environment": [
      { "name": "AWS_REGION", "value": "${data.aws_region.current.name}" },
      { "name": "GATLING_S3_BUCKET_NAME", "value": "${var.gatling_s3_log_bucket_name}" }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "${element(concat(aws_cloudwatch_log_group.gatling_log_group[*].name, list("")), 0)}",
        "awslogs-region": "ap-northeast-1",
        "awslogs-stream-prefix": "${var.prefix}-gatling-aggregate-runner"
      }
    }
  }
]
EOF

}

resource "aws_ecs_task_definition" "gatling_runner" {
  count = var.enabled ? 1 : 0
  family = "${var.prefix}-gatling-runner"
  requires_compatibilities = [
    "FARGATE"]
  network_mode = "awsvpc"
  task_role_arn = element(concat(aws_iam_role.gatling_ecs_task_execution_role[*].arn, list("")), 0)
  execution_role_arn = element(concat(aws_iam_role.gatling_ecs_task_execution_role[*].arn, list("")), 0)
  cpu = "512"
  memory = "1024"
  container_definitions = <<EOF
[
  {
    "name": "gatling-runner",
    "essential": true,
    "image": "${element(concat(aws_ecr_repository.gatling_runner_ecr[*].repository_url, list("")), 0)}",
    "environment": [
      { "name": "AWS_REGION", "value": "${data.aws_region.current.name}" },
      { "name": "GATLING_S3_BUCKET_NAME", "value": "${var.gatling_s3_log_bucket_name}" }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group":  "${element(concat(aws_cloudwatch_log_group.gatling_log_group[*].name, list("")), 0)}",
        "awslogs-region": "ap-northeast-1",
        "awslogs-stream-prefix": "${var.prefix}-gatling-runner"
      }
    }
  }
]
EOF

}

resource "aws_ecs_task_definition" "gatling_s3_reporter" {
  count = var.enabled ? 1 : 0
  family = "${var.prefix}-gatling-s3-reporter"
  requires_compatibilities = [
    "FARGATE"]
  network_mode = "awsvpc"
  task_role_arn = element(concat(aws_iam_role.gatling_ecs_task_execution_role[*].arn, list("")), 0)
  execution_role_arn = element(concat(aws_iam_role.gatling_ecs_task_execution_role[*].arn, list("")), 0)
  cpu = "512"
  memory = "1024"
  container_definitions = <<EOF
[
  {
    "name": "gatling-s3-reporter",
    "essential": true,
    "image": "${element(concat(aws_ecr_repository.gatling_s3_reporter_ecr[*].repository_url, list("")), 0)}",
    "environment": [
      { "name": "AWS_REGION", "value": "${data.aws_region.current.name}" },
      { "Name": "S3_GATLING_BUCKET_NAME", "Value": "${var.gatling_s3_log_bucket_name}" }
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group":  "${element(concat(aws_cloudwatch_log_group.gatling_log_group[*].name, list("")), 0)}",
        "awslogs-region": "ap-northeast-1",
        "awslogs-stream-prefix": "${var.prefix}-gatling-s3-reporter"
      }
    }
  }
]
EOF

}

resource "aws_cloudwatch_log_group" "gatling_log_group" {
  count = var.enabled ? 1 : 0
  name = "/ecs/logs/${var.prefix}-gatling-ecs-group"
}


resource "aws_s3_bucket" "gatling" {
  count = var.enabled ? 1 : 0
  bucket = var.gatling_s3_log_bucket_name
  acl = "public-read"
  region = data.aws_region.current.name

  website {
    index_document = "index.html"
    error_document = "error.html"
  }

  tags = {
    Name = "${var.prefix}-${var.gatling_s3_log_bucket_name}"
    Owner = var.owner
  }
}

resource "aws_s3_bucket_policy" "gatling" {
  count = var.enabled ? 1 : 0
  bucket = aws_s3_bucket.gatling[0].id

  policy = <<POLICY
{
    "Version": "2012-10-17",
    "Id": "Policy1550473277080",
    "Statement": [{
      "Sid": "Allow-from-specific-IP",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${var.gatling_s3_log_bucket_name}/*",
      "Condition": {
        "IpAddress": {
           "aws:SourceIp": [
             "219.117.249.210/32",
             "150.249.210.146/32",
             "219.117.247.123/32",
             "159.28.115.123/32"
           ]
        }
      }
    },
    {
      "Sid": "Allow-from-specific-VPC",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${var.gatling_s3_log_bucket_name}/*",
      "Condition": {
        "StringEquals": {
           "aws:SourceVpc": [
             "${var.vpc_id}"
           ]
        }
      }
    }
    ]
}
POLICY
}
