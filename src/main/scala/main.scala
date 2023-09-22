import cats.effect.{IO, IOApp}
import cats.syntax.traverse.*

import solvers.Solver

object Main extends IOApp.Simple:
  val solver = Solver(1000)
  def run: IO[Unit] =
    for
      actions <- solver.use(_.solve(Solver.domainString, Solver.problemString))
      _ <- actions.map(a => IO.println(s"${a.getName()}: ${a.getParameters().toList} [${a.getDuration()}]")).sequence
    yield ()
