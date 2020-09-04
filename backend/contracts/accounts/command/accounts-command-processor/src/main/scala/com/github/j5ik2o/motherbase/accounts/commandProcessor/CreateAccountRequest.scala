package com.github.j5ik2o.motherbase.accounts.commandProcessor

import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName, EmailAddress }

final case class CreateAccountRequest(accountId: AccountId, name: AccountName, emailAddress: EmailAddress)
    extends CommandRequest
