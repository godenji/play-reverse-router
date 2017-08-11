import sbt._
import Keys._

trait MyBuildSettings
  extends BuildSettings with Transformers with MyProperties {

  override def _settings(
    moduleName: String, appDeps: Seq[ModuleID] = Seq()
  ): Seq[Setting[_]] = {
    super._settings(moduleName, appDeps) ++ Seq()
  }
}
