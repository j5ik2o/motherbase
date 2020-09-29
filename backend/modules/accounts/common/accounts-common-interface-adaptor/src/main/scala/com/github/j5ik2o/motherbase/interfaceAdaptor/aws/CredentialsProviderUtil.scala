package com.github.j5ik2o.motherbase.interfaceAdaptor.aws

import com.amazonaws.auth.{
  AWSCredentialsProvider,
  AWSStaticCredentialsProvider,
  BasicAWSCredentials,
  DefaultAWSCredentialsProviderChain
}

object CredentialsProviderUtil {

  def createCredentialsProvider(
      accessKeyId: Option[String],
      secretAccessKey: Option[String]
  ): AWSCredentialsProvider = {
    (accessKeyId, secretAccessKey) match {
      case (Some(aki), Some(sak)) =>
        new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(aki, sak)
        )
      case _ =>
        new DefaultAWSCredentialsProviderChain()
    }
  }

}
