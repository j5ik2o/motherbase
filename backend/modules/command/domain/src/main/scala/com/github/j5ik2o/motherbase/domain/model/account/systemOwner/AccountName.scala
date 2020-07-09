package com.github.j5ik2o.motherbase.domain.model.account.systemOwner

import cats.implicits._
import com.github.j5ik2o.motherbase.domain.model.account.systemOwner.AccountName._
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.api.{ RefType, Refined }
import eu.timepit.refined.boolean._
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._

object AccountName {

  type AsString =
    String Refined And[MatchesRegex[W.`"[a-z][a-zA-Z0-9]+"`.T], Size[Interval.Closed[W.`1`.T, W.`255`.T]]]

  sealed trait DomainError

  final case class FormatError(message: String) extends DomainError

}

final case class AccountName(breachEncapsulationOfValue: AccountName.AsString) {

  def withSuffix(suffix: String): Either[DomainError, AccountName] = {
    RefType
      .applyRef[AccountName.AsString](s"${breachEncapsulationOfValue.value}$suffix")
      .leftMap[DomainError](FormatError).map(new AccountName(_))
  }

}
