package com.github.j5ik2o.motherbase.accounts.domain.accounts

import java.time.Instant

object AccountEvents {

  sealed trait Event {
    def accountId: AccountId
  }

  final case class AccountCreated(
      accountId: AccountId,
      name: AccountName,
      emailAddress: EmailAddress,
      occurredAt: Instant
  ) extends Event

  final case class AccountDestroyed(
      accountId: AccountId,
      occurredAt: Instant
  ) extends Event

}
