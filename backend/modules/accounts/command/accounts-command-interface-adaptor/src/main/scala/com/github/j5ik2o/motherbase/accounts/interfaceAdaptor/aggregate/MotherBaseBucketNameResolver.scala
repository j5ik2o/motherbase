package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import com.github.j5ik2o.akka.persistence.s3.base.model.PersistenceId
import com.github.j5ik2o.akka.persistence.s3.resolver.SnapshotBucketNameResolver
import com.typesafe.config.Config

class MotherBaseBucketNameResolver(config: Config) extends SnapshotBucketNameResolver {
  private val bucketName = config.getString("motherbase.bucket-name")

  override def resolve(persistenceId: PersistenceId): String = bucketName
}
