package com.github.j5ik2o.motherbase.accounts.domain.accounts

import java.time.Instant

final case class Account(
    id: AccountId,
    deleted: Boolean,
    name: AccountName,
    emailAddress: EmailAddress,
    createdAt: Instant,
    updatedAt: Instant
) {

  def rename(name: AccountName, updatedAt: Instant): Account = {
    require(canRename(name), "Invalid state")
    copy(name = name, updatedAt = updatedAt)
  }

  def canRename(name: AccountName): Boolean = !deleted

  def canDestroy: Boolean = !deleted

  def destroy(updatedAt: Instant): Account = {
    require(canDestroy, "Invalid state")
    copy(deleted = true, updatedAt = updatedAt)
  }
}

object Account {

  def canCreate(name: AccountName, emailAddress: EmailAddress): Boolean = true

  def apply(
      id: AccountId,
      name: AccountName,
      emailAddress: EmailAddress,
      createdAt: Instant
  ): Account =
    new Account(id, false, name, emailAddress, createdAt, createdAt)
}
