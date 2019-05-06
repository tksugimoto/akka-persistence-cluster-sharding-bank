package generator

import com.typesafe.config.ConfigFactory
import slick.codegen.SourceCodeGenerator

object SlickModelGenerator {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load().getConfig("slick-codegen")

    val profile = config.getString("profile")

    val jdbcDriver = config.getString("db.driver")
    val url = config.getString("db.url")
    val user = config.getString("db.user")
    val password = config.getString("db.password")

    val outputDir = "src/main/scala"
    val pkg = "io.github.tksugimoto.bank.account.models"

    SourceCodeGenerator.main(
      Array(profile, jdbcDriver, url, outputDir, pkg, user, password),
    )
  }
}
