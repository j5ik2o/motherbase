package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.validator

import cats.implicits._
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName, EmailAddress }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.error.{
  EmailAddressError,
  SystemAccountIdFormatError,
  SystemAccountNameError
}
import org.slf4j.LoggerFactory

object ValidateUtils {

  private val logger = LoggerFactory.getLogger(getClass)

  def validateAccountId(value: String): ValidationResult[AccountId] = {
    try {
      AccountId(value).valid
    } catch {
      case ex: Throwable =>
        SystemAccountIdFormatError("Invalid system account id", Some(ex)).invalidNel
    }
  }

  def validateAccountName(value: String): ValidationResult[AccountName] = {
    try {
      AccountName(value).valid
    } catch {
      case ex: Throwable =>
        logger.error("occurred error", ex)
        SystemAccountNameError("Invalid system account name", Some(ex)).invalidNel
    }
  }

  def validateEmailAddress(value: String): ValidationResult[EmailAddress] = {
    try {
      EmailAddress(value).valid
    } catch {
      case ex: Throwable =>
        logger.error("occurred error", ex)
        EmailAddressError("Invalid email address", Some(ex)).invalidNel
    }
  }

}
