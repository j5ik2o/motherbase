package com.github.j5ik2o.motherbase.commandProcessor

import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountId

trait CommandRequest {
  def accountId: AccountId
}
