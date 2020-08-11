package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.validator

trait Validator[A, B] {
  def validate(value: A): ValidationResult[B]
}
