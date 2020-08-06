package com.github.j5ik2o.motherbase.accounts.domain.event

import java.time.Instant

import com.github.j5ik2o.motherbase.accounts.domain.system.{EmailAddress, SystemAccountId, SystemAccountName}

object SystemAccountEvents {

  sealed trait Event {
    def systemAccountId: SystemAccountId
  }

  final case class SystemAccountCreated(
      systemAccountId: SystemAccountId,
      name: SystemAccountName,
      emailAddress: EmailAddress,
      occurredAt: Instant
  ) extends Event

  final case class SystemAccountDestroyed(
      systemAccountId: SystemAccountId,
      occurredAt: Instant
  ) extends Event

}
