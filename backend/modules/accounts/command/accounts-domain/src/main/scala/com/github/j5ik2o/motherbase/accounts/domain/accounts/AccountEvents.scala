package com.github.j5ik2o.motherbase.accounts.domain.accounts

import java.time.Instant

object AccountEvents {

  sealed trait AccountEvent {
    def accountId: AccountId
  }

  final case class AccountCreated(
      accountId: AccountId,
      name: AccountName,
      emailAddress: EmailAddress,
      occurredAt: Instant
  ) extends AccountEvent

  final case class AccountRenamed(
      accountId: AccountId,
      name: AccountName,
      occurredAt: Instant
  ) extends AccountEvent

  final case class AccountDestroyed(
      accountId: AccountId,
      occurredAt: Instant
  ) extends AccountEvent

}
