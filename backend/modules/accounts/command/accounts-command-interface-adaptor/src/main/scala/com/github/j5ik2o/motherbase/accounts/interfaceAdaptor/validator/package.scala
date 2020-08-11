package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor

import cats.data.ValidatedNel
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.error.InterfaceError

package object validator {
  type ValidationResult[A] = ValidatedNel[InterfaceError, A]
}
