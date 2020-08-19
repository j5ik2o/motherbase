package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.validate

import cats.implicits._
import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountId
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.validator.ValidateUtils.{
  validateAccountName,
  validateEmailAddress
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.validator.{ ValidationResult, Validator }
import com.github.j5ik2o.motherbase.commandProcessor.CreateAccountRequest

trait ValidateSupport {

  protected def validateRequest[A, B](value: A)(implicit V: Validator[A, B]): ValidationResult[B] =
    V.validate(value)

}

object ValidateSupport {
  import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{ CreateAccountRequest => GRPCCreateAccountRequest }

  implicit object CreateAccountRequestGrpcValidator extends Validator[GRPCCreateAccountRequest, CreateAccountRequest] {

    override def validate(value: GRPCCreateAccountRequest): ValidationResult[CreateAccountRequest] = {
      (
        validateAccountName(value.name),
        validateEmailAddress(value.emailAddress)
      ).mapN {
        case (name, emailAddress) =>
          CreateAccountRequest(
            AccountId(),
            name,
            emailAddress
          )
      }

    }
  }
}
