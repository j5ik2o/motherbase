package com.github.j5ik2o.motherbase.accounts.commandProcessor

import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountError, AccountId }

final case class CreateAccountResponse(accountId: AccountId, error: Option[AccountError]) extends CommandResponse
