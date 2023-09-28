import cats.effect.{IO, IOApp}
import cats.syntax.traverse.*

import solvers.Solver

object Main extends IOApp.Simple:
  val solver = Solver(1000)
  def run: IO[Unit] =
    for
      solution <- solver.use(_.solve(Solver.domainString, Solver.problemString))
      _ <- IO.println("The plan:") >> IO.println(solution.plan)
      _ <- IO.println("The states") >> IO.println(solution.states)
    yield ()
