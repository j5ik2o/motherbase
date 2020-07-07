import Settings._


val root = (project in file("."))
  .settings(baseSettings)
  .settings(
    name := "motherbase"
  )
