package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.directives

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import cats.implicits._
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName, EmailAddress }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.CreateAccountRequestJson
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.rejections
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.rejections.ValidationsRejection
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.validator.{ ValidateUtils, ValidationResult, Validator }
import com.github.j5ik2o.motherbase.commandProcessor.CreateAccountRequest

trait AccountValidateDirectives {

  protected def validateSystemAccountId(value: String): Directive1[AccountId] = {
    ValidateUtils
      .validateAccountId(value)
      .fold({ errors => reject(ValidationsRejection(errors)) }, provide)
  }

  protected def validateSystemAccountName(value: String): Directive1[AccountName] = {
    ValidateUtils
      .validateAccountName(value)
      .fold({ errors => reject(rejections.ValidationsRejection(errors)) }, provide)
  }

  protected def validateEmailAddress(value: String): Directive1[EmailAddress] = {
    ValidateUtils
      .validateEmailAddress(value)
      .fold({ errors => reject(rejections.ValidationsRejection(errors)) }, provide)
  }

  protected def validateJsonRequest[A, B](value: A)(implicit V: Validator[A, B]): Directive1[B] =
    V.validate(value)
      .fold({ errors => reject(rejections.ValidationsRejection(errors)) }, provide)

}

object AccountValidateDirectives {
  import ValidateUtils._

  implicit object CreateAccountRequestJsonValidator extends Validator[CreateAccountRequestJson, CreateAccountRequest] {

    override def validate(
        value: CreateAccountRequestJson
    ): ValidationResult[CreateAccountRequest] = {
      (
        validateAccountId(value.accountId),
        validateAccountName(value.name),
        validateEmailAddress(value.emailAddress)
      ).mapN {
        case (accountId, name, emailAddress) =>
          CreateAccountRequest(
            accountId,
            name,
            emailAddress
          )
      }
    }
  }

}
