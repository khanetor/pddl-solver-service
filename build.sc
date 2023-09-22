import mill._, scalalib._, scalafmt._

object root extends RootModule with ScalaModule with ScalafmtModule {
  def scalaVersion = "3.3.1"

  def ivyDeps = Agg(
    ivy"co.fs2::fs2-core:3.9.2",
    ivy"co.fs2::fs2-io:3.9.2"
  )

  def unmanagedClasspath = T {
    if (!os.exists(millSourcePath / "libs")) Agg()
    else Agg.from(os.list(millSourcePath / "libs").map(PathRef(_)))
  }

  object test extends ScalaTests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.11")
    def testFramework = "utest.runner.Framework"
  }

}
