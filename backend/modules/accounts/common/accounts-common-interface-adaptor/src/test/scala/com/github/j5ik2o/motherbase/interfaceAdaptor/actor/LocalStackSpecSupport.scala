package com.github.j5ik2o.motherbase.interfaceAdaptor.actor

import com.dimafeng.testcontainers.FixedHostPortGenericContainer
import org.testcontainers.containers.wait.strategy.Wait

trait LocalStackSpecSupport {
  protected val localStackImageVersion = "0.9.5"
  protected val localStackImageName    = s"localstack/localstack:$localStackImageVersion"
  protected def localStackPort: Int

  protected lazy val localStackContainer = new FixedHostPortGenericContainer(
    imageName = localStackImageName,
    exposedPorts = Seq(),
    env = Map("SERVICES" -> "cloudwatch"),
    command = Seq(),
    classpathResourceMapping = Seq(),
    waitStrategy = Some(Wait.forLogMessage(".*Ready\\.\n", 1)),
    exposedHostPort = localStackPort,
    exposedContainerPort = 4582
  )

}
