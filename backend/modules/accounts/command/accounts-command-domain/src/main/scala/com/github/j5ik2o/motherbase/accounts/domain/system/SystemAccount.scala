package com.github.j5ik2o.motherbase.accounts.domain.system

import java.time.Instant

final case class SystemAccount(
    id: SystemAccountId,
    deleted: Boolean,
    name: SystemAccountName,
    createdAt: Instant,
    updatedAt: Instant
) {
  def canDestroy: Boolean = !deleted

  def destroy(updatedAt: Instant): SystemAccount = copy(deleted = true, updatedAt = updatedAt)
}

object SystemAccount {

  def canCreate(name: SystemAccountName): Boolean = true

  def apply(id: SystemAccountId, name: SystemAccountName, createdAt: Instant): SystemAccount =
    new SystemAccount(id, false, name, createdAt, createdAt)
}
