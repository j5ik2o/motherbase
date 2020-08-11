package com.github.j5ik2o.motherbase.commandProcessor

import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName, EmailAddress }

case class CreateAccountRequest(accountId: AccountId, name: AccountName, emailAddress: EmailAddress)
    extends CommandRequest
