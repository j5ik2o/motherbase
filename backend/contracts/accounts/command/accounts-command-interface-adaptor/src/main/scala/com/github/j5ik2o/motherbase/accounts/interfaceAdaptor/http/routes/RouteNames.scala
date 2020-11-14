package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes

object RouteNames {

  final val Base = "accounts"

  final val CreateAccount = s"/$Base/create"

  final def RenameAccount(id: String) = s"/$Base/$id"

}
