package com.github.j5ik2o.motherbase.accounts.commandProcessor

import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName }

final case class RenameAccountRequest(accountId: AccountId, name: AccountName) extends CommandRequest
