package com.github.j5ik2o.motherbase.accounts.domain.accounts

trait AccountError {
  def message: String
}

object AccountError {

  case object CantCreateAccount extends AccountError {
    override def message: String = "Can't create the account"
  }

  case object CantDestroyAccount extends AccountError {
    override def message: String = "Can't destroy the account"
  }

  case object CantGetName extends AccountError {
    override def message: String = "Can't get account name"
  }

}
