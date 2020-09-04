package com.github.j5ik2o.motherbase.accounts.commandProcessor

import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountId

trait CommandResponse {
  def accountId: AccountId
}
