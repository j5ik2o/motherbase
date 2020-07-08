output "gatling_ecs_cluster_name" {
  value = element(concat(aws_ecs_cluster.ecs_cluster[*].name, list("")), 0)
}

output "gatling_runner_ecr_arn" {
  value = element(concat(aws_ecs_cluster.ecs_cluster[*].arn, list("")), 0)
}

output "gatling_runner_ecr_repository_url" {
  value = element(concat(aws_ecr_repository.gatling_runner_ecr[*].repository_url, list("")), 0)
}

output "gatling_s3_reporter_ecr_arn" {
  value = element(concat(aws_ecr_repository.gatling_s3_reporter_ecr[*].arn, list("")), 0)
}

output "gatling_s3_reporter_ecr_repository_url" {
  value = element(concat(aws_ecr_repository.gatling_s3_reporter_ecr[*].repository_url, list("")), 0)
}

output "gatling_aggregate_runner_ecr_arn" {
  value = element(concat(aws_ecr_repository.gatling_aggregate_runner_ecr[*].arn, list("")), 0)
}

output "gatling_aggregate_runner_ecr_repository_url" {
  value = element(concat(aws_ecr_repository.gatling_aggregate_runner_ecr[*].repository_url, list("")), 0)
}


