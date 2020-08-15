package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.Behavior
import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountId

class AccountAggregatesSpec
    extends AccountAggregateInMemorySpec {

  override def behavior(accountId: AccountId): Behavior[AccountProtocol.Command] =
    AccountAggregates(_.value.asString)(AccountAggregate(_))
}
